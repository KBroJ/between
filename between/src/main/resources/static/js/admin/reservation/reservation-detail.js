// static/js/admin/reservation/reservation-detail.js

document.addEventListener('DOMContentLoaded', function () {

    // --- 상세 결제 내역 펼치기/접기 로직 ---
    const totalPaymentToggle = document.getElementById('totalPaymentToggle');
    const detailedPaymentInfo = document.getElementById('detailedPaymentInfo');
    const toggleIcon = totalPaymentToggle ? totalPaymentToggle.querySelector('.toggle-icon') : null;

    if (totalPaymentToggle && detailedPaymentInfo && toggleIcon) {
        totalPaymentToggle.addEventListener('click', function() {
            // 'open' 클래스를 토글하여 CSS 애니메이션 트리거
            detailedPaymentInfo.classList.toggle('open');
            toggleIcon.classList.toggle('open'); // 아이콘 회전 클래스 토글
        });
    }

//=====================================================================================================================

    // --- 공통 변수 ---
    const overlay = document.getElementById('modalOverlay');
    const reservationUpdateForm = document.getElementById('reservationUpdateForm');
    let reservationIdToCancel = null; // 취소할 예약 ID 저장 변수

    // --- 페이지 내 버튼 ---
    const updateButton = document.getElementById('updateButton'); // 페이지 내 '수정' 버튼
    const cancelButton = document.getElementById('cancelButton');   // 페이지 내 '예약 취소' 버튼

    // --- 공통 모달 요소 가져오기 ---
    const reasonModal = document.getElementById('reasonModalContainer');
    const reasonModalTitle = document.getElementById('reasonModalTitle');
    const reasonModalLabel = document.getElementById('reasonModalLabel');
    const reasonInput = document.getElementById('reasonInput'); // 공통 사유 입력 필드
    const reasonValidationMessage = document.getElementById('reasonValidationMessage');
    const submitReasonButton = document.getElementById('submitReasonButton');
    const reasonModalCloseBtns = reasonModal ? reasonModal.querySelectorAll('.modal-cancel-button, .modal-close-button') : [];

    // --- CSRF 토큰 ---
    const csrfToken = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
    const csrfHeaderName = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");
    // 디버깅용
    console.log("CSRF Header Name from meta:", csrfHeaderName);
    console.log("CSRF Token from meta:", csrfToken);

    // API 기본 경로 설정
    const API_ADMIN_BASE_URL = "/admin/reserve";

    // --- Flatpickr 초기화 ---
    const resStartInput = document.getElementById('resStart');
    const resEndInput = document.getElementById('resEnd');

    // Flatpickr 라이브러리가 로드되었는지 확인
    if (typeof flatpickr !== "undefined") {
        if (flatpickr.l10ns && flatpickr.l10ns.ko) {
            flatpickr.localize(flatpickr.l10ns.ko); // 한국어 로케일 적용
        }

        const commonFlatpickrConfig = {
            enableTime: true,         // 시간 선택 활성화
            minuteIncrement: 60,      // 1시간 단위로만 선택 가능
            dateFormat: "Y-m-d H:i",  // input 필드에 표시 및 저장될 형식 (Flatpickr 내부 형식)
            time_24hr: true,          // 24시간 형식 사용
            // allowInput: true,      // 직접 입력 허용 여부 (선택 사항)
            // 운영 시간 제한 (선택 사항 - 필요시 주석 해제 및 시간 조정)
            // minTime: "09:00",
            // maxTime: "22:00", // 종료 시간은 보통 exclusive하게 설정될 수 있으므로 주의
        };

        if (resStartInput) {
            flatpickr(resStartInput, {
                ...commonFlatpickrConfig,
                // HTML의 th:value에 의해 초기 시간이 설정되므로, defaultHour는 여기서 필수 아님
                // defaultHour: resStartInput.value && resStartInput.value.includes(" ") ? parseInt(resStartInput.value.split(" ")[1].split(":")[0]) : 9,
            });
        }

        if (resEndInput) {
            flatpickr(resEndInput, {
                ...commonFlatpickrConfig,
                // defaultHour: resEndInput.value && resEndInput.value.includes(" ") ? parseInt(resEndInput.value.split(" ")[1].split(":")[0]) : 10,
            });
        }
    } else {
        console.warn("Flatpickr 라이브러리가 로드되지 않았습니다. 날짜/시간 선택 기능이 제한될 수 있습니다.");
    }


    // 공통 모달 열기
    function openModal(modalElement) {
        if (!modalElement || !overlay) return;
        overlay.classList.add('active');
        modalElement.classList.add('active');
    }

    // 공통 모달 닫기
    function closeModal(modalElement) {

        if (!modalElement || !overlay) return;  // 모달 요소가 없으면 종료
        overlay.classList.remove('active');
        modalElement.classList.remove('active');

        // 공통 모달 닫을 때 초기화
        if (modalElement.id === 'reasonModalContainer' && reasonInput) {
            reasonInput.value = '';
            reasonInput.classList.remove('invalid');
            if (reasonValidationMessage) {
                reasonValidationMessage.classList.remove('show');
            }
        }
    }

    // 페이지 내 '수정' 버튼 -> 공통 모달 열기 (수정용)
    if (updateButton && reasonModal) {
        updateButton.addEventListener('click', function() {
            reasonModalTitle.textContent = '예약 정보 수정';
            reasonModalLabel.innerHTML = '수정 사유 <span class="required-mark">*</span>';
            reasonInput.placeholder = '수정 사유를 입력해주세요. (예: 시간 변경, 좌석 변경 등)';
            reasonInput.setAttribute('required', 'required'); // 필수 입력으로 설정
            submitReasonButton.textContent = '수정 진행';
            submitReasonButton.className = 'btn btn-primary';
            reasonModal.dataset.actionType = 'update';
            openModal(reasonModal);
        });
    }

    // 페이지 내 '예약 취소' 버튼 -> 공통 모달 열기 (취소용)
    if (cancelButton && reasonModal) {
        cancelButton.addEventListener('click', function() {
            reservationIdToCancel = this.dataset.resno;
            reasonModalTitle.textContent = '예약 취소';
            reasonModalLabel.innerHTML = '취소 사유 <span class="required-mark">*</span>';
            reasonInput.placeholder = '취소 사유를 반드시 입력해주세요.';
            reasonInput.setAttribute('required', 'required'); // 필수 입력으로 설정
            submitReasonButton.textContent = '취소 진행';
            submitReasonButton.className = 'btn btn-danger';
            reasonModal.dataset.actionType = 'cancel';
            openModal(reasonModal);
        });
    }

    // 공통 모달 내 '진행' 버튼 클릭 시
    if (submitReasonButton  && reasonModal) {
        submitReasonButton.addEventListener('click', async function() {

            const reason = reasonInput.value.trim();
            const actionType = reasonModal.dataset.actionType;

            // 사유 입력란 유효성 검사
            if (!reason) {
                reasonInput.classList.add('invalid');
                reasonValidationMessage.classList.add('show');
                return;
            } else {
                reasonInput.classList.remove('invalid');
                reasonValidationMessage.classList.remove('show');
            }

            // 버튼 비활성화 (중복 제출 방지)
            this.disabled = true;
            const originalButtonText = this.textContent;
            this.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> 처리 중...';

            // CSRF 토큰이 있는 경우 헤더에 추가
            const headers = {};
            if (csrfToken && csrfHeaderName) {
                headers[csrfHeaderName] = csrfToken;
            }

            if (actionType === 'update') {

                // form 태그의 data-resno 속성에서 resNo 추출
                const resNo = reservationUpdateForm.dataset.resno;

                // FormData 객체로 폼 데이터 수집
                const formData = new FormData(reservationUpdateForm);

                // Flatpickr로 인해 input 필드의 값은 "YYYY-MM-DD HH:mm" 형식이 됨.
                // 이를 백엔드가 기대하는 "YYYY-MM-DDTHH:mm:ss" 형식으로 변환하여 FormData에 덮어쓴다.
                const currentResStartValue = formData.get('resStart'); // Flatpickr가 설정한 "YYYY-MM-DD HH:mm" 형식
                const currentResEndValue = formData.get('resEnd');

                if (!currentResStartValue || !currentResEndValue) {
                    alert('예약 시작 및 종료 시간을 올바르게 선택(또는 입력)해주세요.');
                    this.disabled = false; this.innerHTML = originalButtonText;
                    closeModal(reasonModal); // 모달도 닫아줌
                    return;
                }

                // "YYYY-MM-DDTHH:00:00" 형식으로 변환
                const formattedResStart = currentResStartValue.replace(" ", "T") + ":00";
                const formattedResEnd = currentResEndValue.replace(" ", "T") + ":00";

                formData.set('resStart', formattedResStart);
                formData.set('resEnd', formattedResEnd);
                formData.set('moReason', reason); // @RequestParam("moReason")과 일치

                console.log('호출 주소 확인 : ', `${API_ADMIN_BASE_URL}/${resNo}/update`);
                try {
                    const response = await fetch(`${API_ADMIN_BASE_URL}/${resNo}/update`, {
                        method: 'POST',
                        headers: headers, // CSRF 토큰 포함된 헤더
                        body: formData
                    });

                    const result = await response.json();
                    console.log('예약 변경 결과:', result);

                    if (response.ok && result.success) {
                        alert(result.message || '예약이 성공적으로 변경되었습니다.');
                        window.location.reload(); // 또는 result.redirectUrl로 이동
                    } else {
                        throw new Error(result.message || `예약 변경 실패 (${response.status})`);
                    }
                } catch (error) {
                    console.error('예약 변경 오류:', error);
                    alert('예약 변경 중 오류 발생: ' + error.message);
                } finally {
                    closeModal(reasonModal);
                    this.disabled = false; // 버튼 활성화
                    this.innerHTML = originalButtonText;
                }

            } else if (actionType === 'cancel') {

                if (!reservationIdToCancel) {
                    alert('오류: 취소할 예약 ID를 찾을 수 없습니다.');
                    closeModal(reasonModal);
                    this.disabled = false;
                    this.innerHTML = originalButtonText;
                    return;
                }

                // JSON 요청 시 Content-Type 추가
                headers['Content-Type'] = 'application/json';

                try {
                    const response = await fetch(`${API_ADMIN_BASE_URL}/${reservationIdToCancel}/cancel`, {
                        method: 'POST',
                        headers: headers, // CSRF 토큰 포함된 헤더
                        body: JSON.stringify({ moReason: reason }) // ReservationReqDto 형식에 맞게 moReason 전송
                    });

                    const result = await response.json();

                    if (response.ok && result.success) {
                        alert(result.message || '예약이 성공적으로 취소되었습니다.');
//                        window.location.href = '/admin/reservationList'; // 취소 후 목록 페이지로 이동 (예시)
                        window.location.reload();
                    } else {
                        throw new Error(result.message || `예약 취소 실패 (${response.status})`);
                    }
                } catch (error) {
                    console.error('예약 취소 오류:', error);
                    alert('예약 취소 중 오류 발생: ' + error.message);
                } finally {
                    closeModal(reasonModal);
                    this.disabled = false;
                    this.innerHTML = originalButtonText;
                }

            }
        });
    }

    // 공통 모달 닫기 버튼들 (modal-cancel-button, modal-close-button)에 대한 이벤트 리스너도 설정 필요
    reasonModalCloseBtns.forEach(btn => {
        btn.addEventListener('click', () => closeModal(reasonModal));
    });

    // 오버레이 클릭 시 모달 닫기
    if (overlay) {
        overlay.addEventListener('click', function(event) {
            // 클릭된 요소가 오버레이 자체일 때만 닫기 (모달 내부 클릭은 무시)
            if (event.target === overlay) {
                closeModal(reasonModal);
            }
        });
    }

//=====================================================================================================================

});