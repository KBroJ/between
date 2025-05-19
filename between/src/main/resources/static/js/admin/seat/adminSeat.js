// --- DOM 요소 ---
const seatTableBody = document.getElementById('seat-list-body');
const floorFilterSelect = document.getElementById('floorFilter');
const editFaqModalElement = document.getElementById('editFaqModal'); // Modal 관련 요소들

const editSeatModalElement = document.getElementById('editSeatModal');
const editSeatForm = document.getElementById('seat-edit-modal-form');
const editSeatNoInput = document.getElementById('edit-seat-seatno');
const editSeatNmInput = document.getElementById('edit-seatNm');
const editFloorInput = document.getElementById('edit-floor');
const editSeatSortSelect = document.getElementById('edit-seatSort');
const editGridRowInput = document.getElementById('edit-gridRow');
const editGridColumnInput = document.getElementById('edit-gridColumn');
const editUseAtCheckbox = document.getElementById('edit-useAt');
const editHourlyPriceInput = document.getElementById('edit-hourlyPrice');
const editDailyPriceInput = document.getElementById('edit-dailyPrice');
const editMonthlyPriceInput = document.getElementById('edit-monthlyPrice');

const editSeatLoadingDiv = document.getElementById('edit-seat-loading');
const editSeatErrorDiv = document.getElementById('edit-seat-error');
const saveSeatChangesBtn = document.getElementById('saveSeatChangesBtn');

let editSeatModalInstance = null;

// --- 유틸리티 함수 ---
async function fetchData(url, options = {}) {
    try {
        const response = await fetch(url, options);
        if (!response.ok) {
            let errorMsg = `HTTP 오류! 상태: ${response.status}`;
            try { const errData = await response.json(); errorMsg = errData.message || errorMsg; } catch (e) {}
            throw new Error(errorMsg);
        }
        if (response.status === 204) return null; // No Content
        return await response.json();
    } catch (error) { console.error(`Workspace 오류 (${url}):`, error); throw error; }
}

// --- 핵심 기능 함수 ---

/** 가격 포맷팅 헬퍼 */
function formatPrice(price) {
    if (price == null) return '-'; // null 또는 undefined면 '-' 표시
    return price.toLocaleString('ko-KR'); // 숫자면 콤마 + '원' (여기선 숫자만)
}

/** 백엔드 API를 호출하여 좌석 목록을 가져와 테이블에 동적으로 표시합니다. */
async function loadSeats() {
    if (!seatTableBody) { console.error("좌석 목록 tbody 요소를 찾을 수 없습니다!"); return; }
    seatTableBody.innerHTML = `<tr><td colspan="10" class="text-center text-muted py-4"><i class="fas fa-spinner fa-spin me-1"></i> 목록 로딩 중...</td></tr>`; // colspan 수정

    try {
        const selectedFloorValue = floorFilterSelect ? floorFilterSelect.value : '';
        // const apiUrl = `/api/admin/seats${selectedFloor ? '?floor=' + selectedFloor : ''}`;
         let apiUrl = `/api/admin/seats`;

          if (selectedFloorValue) {
                apiUrl += `?floor=${selectedFloorValue}`;
          }
        console.log(">>> 좌석 목록 요청 API URL:", apiUrl);
        const seatList = await fetchData(apiUrl);

        seatTableBody.innerHTML = ''; // 로딩 메시지 제거


            if (!seatList || !Array.isArray(seatList) || seatList.length === 0) {
                const message = selectedFloorValue ? `해당 층(${selectedFloorValue}층)에 등록된 좌석이 없습니다.` : "등록된 좌석이 없습니다.";
                seatTableBody.innerHTML = `<tr><td colspan="10" class="text-center text-muted py-4">${message}</td></tr>`;
                return;
            }

        seatList.forEach((seat, index) => {
            const row = seatTableBody.insertRow();

            row.insertCell().textContent = seat.seatNo || (index + 1); // PK 또는 순번
            row.insertCell().textContent = seat.floor || '-';
            row.insertCell().textContent = seat.seatNm || '(이름 없음)';
            row.insertCell().textContent = seat.seatSort || '-';
            const gridCell = row.insertCell(); gridCell.textContent = `${seat.gridRow||'-'}/${seat.gridColumn||'-'}`;
            gridCell.textContent = `${seat.gridRow || '-'}/${seat.gridColumn || '-'}`;
            row.insertCell().textContent = formatPrice(seat.hourlyPrice); // 가격 포맷팅
            row.insertCell().textContent = formatPrice(seat.dailyPrice);
            row.insertCell().textContent = formatPrice(seat.monthlyPrice);
            const useAtCell = row.insertCell(); useAtCell.innerHTML = seat.useAt ? '<span class="text-success-emphasis">사용</span>' : '<span class="text-danger-emphasis">미사용</span>';
            const actionsCell = row.insertCell(); actionsCell.className = 'text-center action-buttons';
            const editLink = document.createElement('a');
            editLink.href = `/admin/seats/edit/${seat.seatNo}`;
            editLink.dataset.seatId = seat.seatNo;
            editLink.className = 'btn btn-sm btn-outline-secondary btn-xs';
            editLink.title = '수정';
            editLink.innerHTML = '<i class="fas fa-edit"></i>';
            editLink.dataset.bsToggle = 'modal';         // Bootstrap 속성
            editLink.dataset.bsTarget = '#editSeatModal'; // Modal ID

            const deleteButton = document.createElement('button');
            deleteButton.type = 'button';
            deleteButton.className = 'btn btn-sm btn-outline-danger btn-xs'; deleteButton.title = '삭제';
            deleteButton.innerHTML = '<i class="fas fa-trash-alt"></i>';
            deleteButton.dataset.seatId = seat.seatNo;
            deleteButton.dataset.seatName = seat.seatNm;
            deleteButton.onclick = () => confirmDeleteSeat(seat.seatNo, seat.seatNm);

            actionsCell.appendChild(editLink);
            actionsCell.appendChild(deleteButton);
        });

    } catch (error) {
        console.error("좌석 목록 로드 실패:", error);
        seatTableBody.innerHTML = `<tr><td colspan="10" class="text-center text-danger py-4">좌석 목록 로드 실패</td></tr>`; // colspan 수정
    }
}

    async function loadFloorFilterOptions() {
        if (!floorFilterSelect) return; // 필터 요소 없으면 종료
        console.log("층 필터 옵션 로드 시도...");

        try {
            const floorsData = await fetchData('/api/floors'); // 예: GET /api/floors

            // 기존 옵션 (첫 번째 "전체 층" 제외하고) 비우기
            while (floorFilterSelect.options.length > 1) {
                floorFilterSelect.remove(1);
            }

            if (floorsData && Array.isArray(floorsData) && floorsData.length > 0) {
                floorsData.forEach(floorInfo => {
                    // floorInfo 객체는 { floor: 1, floorName: "1층" } 형태를 가정
                    const option = document.createElement('option');
                    option.value = floorInfo.floor;      // 실제 층 번호
                    option.textContent = floorInfo.floorName; // 화면에 보일 층 이름
                    floorFilterSelect.appendChild(option);
                });
                console.log("층 필터 옵션 로드 완료.");
            } else {
                 console.warn("층 정보를 가져오지 못했거나 형식이 잘못되었습니다. '전체 층'만 표시됩니다.");
            }
        } catch (error) {
            console.error("층 필터 옵션 로드 실패:", error);
            // 에러 발생 시 기본 "전체 층" 옵션만 남도록 처리
        }
    }


/** 좌석 삭제 확인 및 API 호출 함수 */
function confirmDeleteSeat(seatId, seatName) {
    const namePreview = seatName && seatName.length > 20 ? seatName.substring(0, 20) + '...' : seatName;
    if (confirm(`[${namePreview || '선택된 좌석'}] (ID: ${seatId}) 항목을 정말 삭제하시겠습니까?\n연결된 가격 정보도 함께 삭제됩니다. 이미 예약된 내역이 있다면 문제가 발생할 수 있습니다.`)) {
        console.log("삭제 API 호출 시도 - ID:", seatId);
        // !!! 실제 삭제 API 엔드포인트 및 CSRF 처리 확인 !!!
        const deleteUrl = `/api/admin/seats/${seatId}`;
        const csrfToken = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
        const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");
        const headers = {}; // DELETE는 Content-Type 불필요할 수 있음
        if (csrfToken && csrfHeader) { headers[csrfHeader] = csrfToken; }

        fetch(deleteUrl, { method: 'DELETE', headers: headers })
            .then(response => {
                 if (response.ok) { return response.status === 204 ? null : response.json(); }
                 else { return response.json().then(err => { throw new Error(err.message || `삭제 실패`); }); }
             })
             .then(() => { alert("좌석이 삭제되었습니다."); loadSeats(); }) // 성공 시 목록 새로고침
             .catch(error => { console.error("좌석 삭제 오류:", error); alert(`삭제 중 오류: ${error.message}`); });
    }
}

   async function loadSeatDataIntoModal(event) {
        // 필수 DOM 요소 확인
        if (!editSeatForm || !editSeatLoadingDiv || !editSeatErrorDiv || !editSeatNoInput ||
            !editSeatNmInput || !editFloorInput || !editSeatSortSelect || !editGridRowInput ||
            !editGridColumnInput || !editUseAtCheckbox || !editHourlyPriceInput ||
            !editDailyPriceInput || !editMonthlyPriceInput || !saveSeatChangesBtn) {
            console.error("좌석 수정 Modal의 내부 요소를 찾을 수 없습니다."); return;
        }

        const button = event.relatedTarget; // Modal을 연 버튼
        if (!button) { console.warn("Modal 트리거 버튼을 찾을 수 없습니다."); return; }
        const seatId = button.dataset.seatId;
        console.log("수정 Modal 열기, Seat ID:", seatId);

        // UI 초기화: 폼 숨기고, 에러 메시지 숨기고, 로딩 표시
        editSeatForm.style.display = 'none';
        editSeatErrorDiv.style.display = 'none'; editSeatErrorDiv.textContent = '';
        editSeatLoadingDiv.style.display = 'block';
        saveSeatChangesBtn.disabled = true;

        if (!seatId) {
             editSeatErrorDiv.textContent = "좌석 ID를 가져올 수 없습니다.";
             editSeatErrorDiv.style.display = 'block';
             editSeatLoadingDiv.style.display = 'none';
             return;
        }

        try {
            const apiUrl = `/api/admin/seats/${seatId}`;
            const seatData = await fetchData(apiUrl); // 단일 좌석 데이터 (SeatResponseDto 형태 예상)

            if (!seatData) { throw new Error("좌석 데이터를 받지 못했습니다."); }

            // 폼 필드에 데이터 채우기 (SeatResponseDto 필드명에 맞게)
            editSeatNoInput.value = seatData.seatNo || seatId;
            editSeatNmInput.value = seatData.seatNm || '';
            editFloorInput.value = seatData.floor || '';
            editSeatSortSelect.value = seatData.seatSort || '';
            editGridRowInput.value = seatData.gridRow || '';
            editGridColumnInput.value = seatData.gridColumn || '';
            editUseAtCheckbox.checked = seatData.useAt === true; // boolean 값으로 명확히 비교
            editHourlyPriceInput.value = seatData.hourlyPrice != null ? seatData.hourlyPrice : '';
            editDailyPriceInput.value = seatData.dailyPrice != null ? seatData.dailyPrice : '';
            editMonthlyPriceInput.value = seatData.monthlyPrice != null ? seatData.monthlyPrice : '';

            // UI 표시
            editSeatForm.style.display = 'block';
            editSeatLoadingDiv.style.display = 'none';
            saveSeatChangesBtn.disabled = false;

        } catch (error) {
            console.error("Modal 좌석 데이터 로드 실패:", error);
            editSeatErrorDiv.textContent = `좌석 정보 로드 실패: ${error.message}`;
            editSeatErrorDiv.style.display = 'block';
            editSeatLoadingDiv.style.display = 'none';
        }
    }

async function handleSeatEditFormSubmit(event) {
     event.preventDefault(); // 기본 폼 제출 방지
     if (!editSeatForm || !saveSeatChangesBtn) return;

     const seatNo = editSeatNoInput.value; // 숨겨진 필드에서 ID 가져오기
     // 수정된 데이터 수집
     const pricesDtoList = [];
     if (editHourlyPriceInput.value.trim()) { pricesDtoList.push({ type: "HOURLY", price: editHourlyPriceInput.value.trim() });}
     if (editDailyPriceInput.value.trim()) { pricesDtoList.push({ type: "DAILY", price: editDailyPriceInput.value.trim() });}
     if (editMonthlyPriceInput.value.trim()) { pricesDtoList.push({ type: "MONTHLY", price: editMonthlyPriceInput.value.trim() });}

     const updatedSeatData = { // 백엔드 SeatRequestDto 형식에 맞게
         seatNm: editSeatNmInput.value.trim(),
         floor: parseInt(editFloorInput.value),
         seatSort: editSeatSortSelect.value,
         gridRow: editGridRowInput.value.trim() || null,
         gridColumn: editGridColumnInput.value.trim() || null,
         useAt: editUseAtCheckbox.checked,
         prices: pricesDtoList
     };

     // 간단 유효성 검사
     if (!seatNo || !updatedSeatData.seatNm || !updatedSeatData.floor || !updatedSeatData.seatSort) {
          alert("좌석 이름, 층, 종류는 필수입니다."); return;
     }

     console.log("좌석 수정 내용 저장 시도 - ID:", seatNo, updatedSeatData);
     const originalButtonHtml = saveSeatChangesBtn.innerHTML;
     saveSeatChangesBtn.disabled = true;
     saveSeatChangesBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i> 저장 중...';
     editSeatErrorDiv.style.display = 'none';

     // CSRF 토큰 준비
     const csrfToken = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
     const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");
     const headers = { 'Content-Type': 'application/json' };
     if (csrfToken && csrfHeader) { headers[csrfHeader] = csrfToken; }

     try {
          // !!! 실제 수정 API 엔드포인트 및 메소드(PUT/PATCH) 확인 !!!
          const updateUrl = `/api/admin/seats/${seatNo}`;
          const response = await fetch(updateUrl, {
              method: 'PUT', // 또는 PATCH
              headers: headers,
              body: JSON.stringify(updatedSeatData)
          });

          if (!response.ok) {
              const err = await response.json().catch(()=>({message: `수정 실패(${response.status})`}));
              throw new Error(err.message);
          }

          alert("좌석 정보가 성공적으로 수정되었습니다.");
          editSeatModalInstance.hide(); // Modal 닫기 (Bootstrap 5 JS API)
          loadSeats(); // 목록 새로고침

     } catch (error) {
          console.error("좌석 수정 실패:", error);
          editSeatErrorDiv.textContent = `수정 실패: ${error.message}`;
          editSeatErrorDiv.style.display = 'block';
     } finally {
          // 버튼 원래대로 복원 (성공/실패 모두)
          saveSeatChangesBtn.disabled = false;
          saveSeatChangesBtn.innerHTML = originalButtonHtml;
     }
}

// --- 페이지 로드 시 초기화 ---
document.addEventListener('DOMContentLoaded', async function() {
    console.log("DOM 로드 완료. 좌석 관리 페이지 초기화.");

    // Bootstrap Modal 인스턴스 생성
    if (editSeatModalElement) {
         editSeatModalInstance = new bootstrap.Modal(editSeatModalElement);
         // Modal 열릴 때 이벤트 리스너 등록
         editSeatModalElement.addEventListener('show.bs.modal', loadSeatDataIntoModal);
    } else { console.warn("수정 Modal 요소를 찾을 수 없습니다. (editSeatModal)"); }

    // Modal 내부 폼 submit 이벤트 리스너 등록
    if (editSeatForm) {
         editSeatForm.addEventListener('submit', handleSeatEditFormSubmit);
    } else { console.warn("수정 Modal Form 요소를 찾을 수 없습니다. (seat-edit-modal-form)"); }

    // 층 필터 이벤트 리스너
    if(floorFilterSelect) {
        floorFilterSelect.addEventListener('change', loadSeats);
    }

    // 초기 데이터 로드
    try {
        await loadFloorFilterOptions(); // 층 필터 옵션 먼저 로드
        await loadSeats();              // 그 다음 좌석 목록 로드
    } catch (error) {
        console.error("페이지 초기화 중 오류:", error);
        if(seatTableBody) seatTableBody.innerHTML = `<tr><td colspan="10">페이지 초기화 오류</td></tr>`;
    }
    console.log("좌석 관리 페이지 초기화 완료.");
});