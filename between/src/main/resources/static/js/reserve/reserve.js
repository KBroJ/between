    // --- 전역 변수 ---
    let flatpickrInstance = null;
    let selectedDate = null; // "YYYY-MM-DD"
    let currentYear = new Date().getFullYear();
    let currentMonth = new Date().getMonth() + 1; // 1~12
    let selectedPlanType = 'HOURLY'; // HOURLY, DAILY, MONTHLY
    let availableCoupons = [];
    let selectedCoupon = null;
    let modificationMode = false; // 현재 변경 모드인지 여부
    let originalResNo = null;    // 변경 대상 예약 번호
    let originalPlanType = null; // 변경 전 요금제 (변경 불가 확인용)
    let originalSeatInfo = null; // 변경 전 좌석 정보 { id, name, type }
    let originalTimes = [];      // 변경 전 시간 정보 (시간제)

    // --- DOM 요소 캐싱 ---
    const calendarInput = document.getElementById('calendar-input');
    const calendarIcon = document.querySelector('.calendar-icon');
    const planRadios = document.querySelectorAll('input[name="planType"]');
    const seatMapView = document.getElementById('seat-map-view');
    const timeSelectionCard = document.getElementById('time-selection-card');
    const timeSelectionGuide = document.getElementById('time-selection-guide');
    const timeSlotsDiv = document.getElementById('time-slots');
    const reservationDetailsDiv = document.getElementById('reservation-details');
    const totalCountSpan = document.getElementById('total-count');
    const totalPriceSpan = document.getElementById('total-price');
    const paymentButton = document.getElementById('payment-button'); // 원래 버튼 ID 사용
    const couponSelect = document.getElementById('couponSelect');
    const discountInfoDiv = document.getElementById('discount-info');
    const modificationNotice = document.getElementById('modification-notice');


    // --- 유틸리티 함수 ---
    /** 서버 API 호출 함수 */
    async function fetchData(url, options = {}) {
        try {
            const response = await fetch(url, options);
            if (!response.ok) { throw new Error(`HTTP ${response.status}`); }
            if (response.status === 204) return null;
            return await response.json();
        } catch (error) { console.error(`Workspace error for ${url}:`, error); throw error; }
    }
    // generateUUID 함수 제거됨

    // --- Flatpickr (달력) 관련 함수 ---
    function initializeCalendar(enabledDates = [], initialDate = null) { if (!calendarInput) return; if (flatpickrInstance) flatpickrInstance.destroy(); flatpickrInstance = flatpickr(calendarInput, { locale: "ko", dateFormat: "Y-m-d", minDate: "today", enable: enabledDates, defaultDate: initialDate, onChange: handleDateChange, onMonthChange: handleMonthYearChange, onYearChange: handleMonthYearChange, onClose: handleCalendarClose }); updateCalendarInput(initialDate); }
    function updateCalendarInput(dateStr) { if (dateStr && /^\d{4}-\d{2}-\d{2}$/.test(dateStr)) { if(calendarInput) calendarInput.value = dateStr; selectedDate = dateStr; } else { if(calendarInput) calendarInput.value = ''; selectedDate = null; } }
    function handleDateChange(selectedDates, dateStr, instance) { console.log("날짜 선택됨:", dateStr); updateCalendarInput(dateStr); loadSeatStatus(); updateSummary(); }
    function handleMonthYearChange(selectedDates, dateStr, instance) { const ny = instance.currentYear, nm = instance.currentMonth + 1; if (currentYear !== ny || currentMonth !== nm) { console.log(`달력 변경: ${ny}-${nm}`); currentYear = ny; currentMonth = nm; loadCalendarDataAndInitialize(ny, nm); } }
    function handleCalendarClose(selectedDates, dateStr, instance) { if (!calendarInput?.value) { updateCalendarInput(null); loadSeatStatus(); updateSummary(); } }
    function loadCalendarDataAndInitialize(year, month, initialDateToSelect = null) {
         console.log(`달력 로드 요청: ${year}-${month}`);
         if(calendarInput) calendarInput.placeholder = "로딩 중...";
         const apiUrl = `/api/calendar?year=${year}&month=${month}`;
         fetchData(apiUrl).then(data => {
             console.log("받은 달력 데이터:", data); let enabled = [];
             try { if (data?.weeks?.length) { enabled = data.weeks.flatMap(w => w.days || []).filter(d => d?.selectable === true).map(d => d.dateString); } else { console.error("달력 구조 오류"); }
             } catch (e) { console.error("enabledDates 생성 오류:", e); }
             console.log("선택 가능 날짜:", enabled);
             let initial = null; if (initialDateToSelect && enabled.includes(initialDateToSelect)) initial = initialDateToSelect; else if (selectedDate && enabled.includes(selectedDate)) initial = selectedDate;
             initializeCalendar(enabled, initial); if(calendarInput) calendarInput.placeholder = "날짜 선택";
             if (initial) { loadSeatStatus(); updateSummary(); } else { updateCalendarInput(null); /* 초기화 */ }
         }).catch(e => { console.error('달력 API 오류:', e); if(calendarInput) calendarInput.placeholder = "로드 실패"; initializeCalendar([], null); });
     }
    function openCalendar() { flatpickrInstance?.open(); }

// --- 요금제 관련 함수 ---
 /** 요금제 변경 처리 */
function handlePlanChange() {
  const selectedRadio = document.querySelector(
    'input[name="planType"]:checked'
  );
  if (!selectedRadio) return;
  const newlySelectedPlan = selectedRadio.value;

  // 변경 모드이고 원래 요금제가 있는데 다른 것을 선택 시도한 경우
  if (
    modificationMode &&
    originalPlanType &&
    newlySelectedPlan !== originalPlanType
  ) {
    alert(
      `요금제는 변경할 수 없습니다 (${originalPlanType} 유지). 예약을 취소하고 새로 예약해주세요.`
    );
    // 원래 요금제로 라디오 버튼 복원
    planRadios.forEach((radio) => {
      radio.checked = radio.value === originalPlanType;
    });
    return; // 함수 종료, selectedPlanType 변경 안 함
  }

  selectedPlanType = newlySelectedPlan; // 전역 변수 업데이트
  console.log("요금제 설정:", selectedPlanType);

  if (!timeSelectionCard || !timeSelectionGuide) return;
  if (selectedPlanType === "HOURLY") {
    timeSelectionCard.classList.remove("disabled");
    timeSelectionCard.style.display = "block";
    timeSelectionGuide.textContent =
      "좌석/룸과 날짜를 선택하면 시간이 표시됩니다.";
    // 좌석/날짜가 이미 선택되어 있다면 시간 로드
    if (selectedDate && document.querySelector(".seat-item.selected"))
      loadAvailableTimes();
  } else {
    timeSelectionCard.classList.add("disabled");
    if (selectedPlanType === "DAILY")
      timeSelectionGuide.textContent =
        "일일권은 당일 운영시간 동안 이용 가능합니다.";
    else
      timeSelectionGuide.textContent = "월정액권은 시간 선택이 불필요합니다.";
    clearTimeSelection();
  }
  updateSummary();
  calculateTotal();
}


     // --- 쿠폰 관련 함수 ---
     /** 쿠폰 목록 로드 */
     async function loadAvailableCoupons() { console.log("쿠폰 로드 중..."); if(couponSelect) couponSelect.innerHTML = '<option value="">로딩 중...</option>'; try { await new Promise(r => setTimeout(r, 300)); const c = [{ id: 'D1000', name: '1,000원 할인', type: 'amount', value: 1000 }, { id: 'P10', name: '10% 할인', type: 'percent', value: 10 }]; availableCoupons = c; populateCouponDropdown(); } catch (e) { console.error("쿠폰 로드 실패:", e); if(couponSelect) couponSelect.innerHTML = '<option value="">로드 실패</option>'; } }
     /** 쿠폰 드롭다운 채우기 */
     function populateCouponDropdown() { if(!couponSelect) return; couponSelect.innerHTML = '<option value="">쿠폰을 선택하세요</option>'; availableCoupons.forEach(c => { const o = document.createElement('option'); o.value = c.id; o.textContent = c.name; o.dataset.type = c.type; o.dataset.value = c.value; couponSelect.appendChild(o); }); if (selectedCoupon) couponSelect.value = selectedCoupon.id; }
     /** 쿠폰 선택 변경 */
     function handleCouponChange() { const opt = couponSelect?.options[couponSelect.selectedIndex]; if(discountInfoDiv) discountInfoDiv.textContent = ''; if (!opt?.value) { selectedCoupon = null; } else { selectedCoupon = { id: opt.value, name: opt.textContent, type: opt.dataset.type, value: parseFloat(opt.dataset.value) || 0 }; console.log("쿠폰 선택됨:", selectedCoupon); /* 임시 표시 */ } calculateTotal(); updateSummary(); }

     // --- 좌석/시간 관련 함수 ---
     /** 좌석 상태 로드 */
     function loadSeatStatus() { const date = selectedDate; clearTimeSelection(); if(!seatMapView) return; seatMapView.innerHTML = '<p class="text-muted small p-3">...</p>'; if (!date) { seatMapView.innerHTML = '<p>날짜를 선택해주세요.</p>'; return; } console.log(`좌석 로드: ${date}`); const apiUrl = `/api/seats?date=${date}`; fetchData(apiUrl).then(d => renderSeats(d)).catch(e => { if(seatMapView) seatMapView.innerHTML = '<p>좌석 로드 실패</p>'; }); }
     /** 좌석 정보 렌더링  */
         function renderSeats(seats = []) {
             if (!seatMapView) {
                 console.error("좌석 배치도 영역(seatMapView)을 찾을 수 없습니다!");
                 return;
             }
             seatMapView.innerHTML = ''; // 이전 좌석 비우기

             // --- Grid 컨테이너 스타일은 CSS에서 설정하는 것을 권장 ---
             // seatMapView.style.display = 'grid';
             // seatMapView.style.gridTemplateColumns = 'repeat(12, 1fr)'; // 예: 기본 12 컬럼
             // seatMapView.style.gridAutoRows = 'minmax(45px, auto)';
             // seatMapView.style.gap = '8px';
             // ------------------------------------------------------

             if (!seats || seats.length === 0) {
                 seatMapView.innerHTML = '<p class="text-muted small p-3">등록된 좌석 정보가 없습니다.</p>';
                 return;
             }
             console.log("좌석 렌더링 시작 (변경 모드:", isModificationMode);

             // 변경 모드일 경우 원본 좌석 ID 가져오기 (문자열)
             const originalSeatIdStr = isModificationMode ? String(originalReservationData?.itemId) : null;

             seats.forEach(item => {
                 // 1. 좌석 div 요소 생성 및 기본 정보 설정
                 const div = document.createElement('div');
                 div.textContent = item.name; // 좌석 이름
                 const typeClass = item.type?.toLowerCase() || 'seat'; // 타입 클래스
                 const statusClass = item.status ? item.status.toLowerCase() : 'unavailable'; // 상태 클래스
                 div.className = `${typeClass} ${statusClass} seat-item`; // 기본 클래스 설정
                 div.dataset.seatId = item.id; // 데이터 속성
                 div.dataset.seatName = item.name;
                 div.dataset.seatType = item.type;

                 // 2. DB 위치 정보(gridRow, gridColumn) 스타일 적용
                 const isValidGridValue = (val) => val && typeof val === 'string' && val.trim() !== '' && val.toLowerCase() !== 'null';
                 if (isValidGridValue(item.gridRow)) {
                     div.style.gridRow = item.gridRow;
                 } else if (item.gridRow != null && item.gridRow !== '') {
                      console.warn(`  -> Seat ${item.name}: 유효하지 않은 gridRow ('${item.gridRow}')`);
                 }
                 if (isValidGridValue(item.gridColumn)) {
                     div.style.gridColumn = item.gridColumn;
                 } else if (item.gridColumn != null && item.gridColumn !== '') {
                      console.warn(`  -> Seat ${item.name}: 유효하지 않은 gridColumn ('${item.gridColumn}')`);
                 }

                 // 3. 상태에 따른 이벤트 리스너 및 커서 설정
                 if (statusClass === 'available' && typeClass !== 'area') {
                     div.style.cursor = 'pointer';
                     div.addEventListener('click', () => selectSeat(div));
                 } else {
                     div.style.cursor = 'not-allowed';
                     div.style.opacity = '0.6';
                     if(statusClass === 'static') div.style.opacity = '0.8';
                 }

                 // 4. 변경 모드일 때, 기존 예약 좌석 특별 표시
                 if (isModificationMode && item.id === originalSeatIdStr) {
                     div.classList.add('originally-booked'); // CSS 클래스 추가
                     console.log(`[renderSeats] 기존 예약 좌석 발견! ID: ${item.id}, 클래스 추가 시도.`);
                     console.log(`기존 예약 좌석(${item.name}) 특별 표시 적용됨`);
                 }

                 // 5. 생성된 좌석 div를 좌석판에 추가
                 seatMapView.appendChild(div);
             });

             // 변경 모드이고 초기 선택 상태 로직을 추가했다면 관련 함수 호출
             // 예: if (isModificationMode && originalSeatIdStr) { updateSummary(); }
         }

     /** 좌석 선택 (단일) */
     function selectSeat(seatElement) { if (seatElement.classList.contains('unavailable') || seatElement.classList.contains('static')) return; const current = document.querySelector('.seat-item.selected'); if (current === seatElement) { seatElement.classList.remove('selected'); clearTimeSelection(); updateSummary(); calculateTotal(); return; } if (current) current.classList.remove('selected'); seatElement.classList.add('selected'); if (selectedPlanType === 'HOURLY') loadAvailableTimes(); else clearTimeSelection(); updateSummary(); }
     /** 시간 선택 초기화 */
     function clearTimeSelection() { if(timeSlotsDiv) timeSlotsDiv.innerHTML = ''; document.querySelectorAll('input[name="times"]:checked').forEach(cb => cb.checked = false); if(timeSelectionGuide) timeSelectionGuide.textContent = '시간제 선택 시...'; }

         function loadAvailableTimes() {
             if(!timeSlotsDiv || !timeSelectionGuide) return;
             timeSlotsDiv.innerHTML = '';
             timeSelectionGuide.textContent = '좌석/룸과 날짜를 선택하면 예약 가능한 시간이 표시됩니다.';
             const selectedSeat = document.querySelector('.seat-item.selected');
             const date = selectedDate; // 전역 변수 사용

             if (selectedPlanType !== 'HOURLY' || !selectedSeat || !date) {
                  if(selectedPlanType !== 'HOURLY') timeSelectionGuide.textContent = '시간제 요금제를 선택해주세요.';
                  else if (!selectedSeat) timeSelectionGuide.textContent = '좌석/룸을 선택해주세요.';
                  else if (!date) timeSelectionGuide.textContent = '날짜를 선택해주세요.';
                  return;
             }

             timeSelectionGuide.textContent = '';
             timeSlotsDiv.innerHTML = '<p class="text-muted small"><i class="fas fa-spinner fa-spin me-1"></i> 시간 로딩 중...</p>';
             const seatId = selectedSeat.dataset.seatId;
             console.log(`예약 가능 시간 로드: 항목 ${seatId}, 날짜 ${date}`);

             // --- 오늘 날짜 확인 및 현재 시간 가져오기 ---
             const todayStr = new Date().toISOString().split('T')[0];
             const isSelectedDateToday = (date === todayStr);
             const currentHour = isSelectedDateToday ? new Date().getHours() : -1; // 오늘이면 현재 시간(시), 아니면 -1
             console.log(`선택일이 오늘인가? ${isSelectedDateToday}, 현재 시간(시): ${currentHour}`);
             // ------------------------------------------

             const apiUrl = `/api/times?date=${date}&seatId=${seatId}`;

             fetchData(apiUrl)
                 .then(times => {
                     // renderTimeSlots 호출 시 isSelectedDateToday와 currentHour 전달
                     renderTimeSlots(times, isSelectedDateToday, currentHour);
                 })
                 .catch(error => { if(timeSlotsDiv) timeSlotsDiv.innerHTML = `<p class="text-danger small">시간 정보 로드 실패</p>`; })
                 .finally(updateSummary); // 시간 로드 후 항상 요약 업데이트
         }


/** 시간 슬롯 렌더링 */
function renderTimeSlots(timeSlots = [], isSelectedDateToday, currentHour) {
    console.log("--- renderTimeSlots 함수 시작 ---"); // 로그 추가
    if(!timeSlotsDiv) {
        console.error("renderTimeSlots 오류: timeSlotsDiv 요소를 찾을 수 없습니다!");
        return;
    }
    timeSlotsDiv.innerHTML = ''; // 이전 내용 클리어

    // 받은 데이터 로깅
    console.log("renderTimeSlots 가 받은 데이터:", timeSlots);
    console.log(`오늘 날짜 여부: ${isSelectedDateToday}, 현재 시간(시): ${currentHour}`);

    // 데이터 유효성 검사
    if (!timeSlots) {
        timeSlotsDiv.innerHTML = '<p class="text-warning small">시간 정보가 없습니다 (null/undefined).</p>';
        console.log("renderTimeSlots 종료: timeSlots 데이터 없음");
        return;
    }
    if (!Array.isArray(timeSlots)) {
        timeSlotsDiv.innerHTML = '<p class="text-danger small">시간 정보 형식이 잘못되었습니다 (배열 아님).</p>';
        console.error("renderTimeSlots 종료: timeSlots 데이터가 배열이 아님:", timeSlots);
        return;
    }
    if (timeSlots.length === 0) {
        timeSlotsDiv.innerHTML = '<p class="text-warning small">예약 가능한 시간이 없습니다.</p>';
        console.log("renderTimeSlots 종료: timeSlots 배열이 비어있음");
        return;
    }

    let hasAvailableSlot = false; // 선택 가능한 슬롯 확인용 플래그
    try { // 렌더링 로직 전체를 try-catch로 감싸 에러 확인
        console.log("시간 슬롯 렌더링 루프 시작...");
        timeSlots.forEach((slot, index) => {
            console.log(`  [${index}] 처리 시작:`, slot); // 각 슬롯 객체 확인

            // 슬롯 데이터 유효성 검사
            if (!slot || typeof slot !== 'object') {
                console.warn(`  [${index}] 잘못된 슬롯 데이터 발견, 건너<0xEB><0x9B><0x84>니다:`, slot);
                return; // continue to next iteration
            }

            const timeValue = slot.startTime;
            const slotStatus = slot.status; // status 값 미리 가져오기

            // timeValue 유효성 검사
            if (!timeValue || typeof timeValue !== 'string' || !/^\d{2}:\d{2}$/.test(timeValue)) {
                 console.warn(`  [<span class="math-inline">\{index\}\] 잘못된 startTime 형식 \('</span>{timeValue}'), 건너<0xEB><0x9B><0x84>니다.`);
                 return; // continue
             }
            // status 유효성 검사 (선택 사항)
            if (!slotStatus || typeof slotStatus !== 'string') {
                 console.warn(`  [<span class="math-inline">\{index\}\] 잘못된 status 값 \('</span>{slotStatus}'), 기본값으로 처리합니다.`);
                 // status = 'UNAVAILABLE'; // 예: 기본적으로 비활성화 처리
             }

            const timeDisplay = `${timeValue} - ${String(parseInt(timeValue.split(':')[0]) + 1).padStart(2,'0')}:00`;
            const label = document.createElement('label'); label.classList.add('me-2', 'mb-2');
            const input = document.createElement('input'); input.type = 'checkbox'; input.name = 'times'; input.value = timeValue; input.id = `time-${timeValue.replace(':', '')}`; input.classList.add('d-none');
            const span = document.createElement('span'); span.textContent = timeDisplay;

            let isDisabled = false; // 최종 비활성화 여부
            let isOriginallyBooked = isModificationMode && (originalReservationData?.selectedTimes || []).includes(timeValue); // 기존 예약 시간 여부

            // 오늘이고 지난 시간인지 확인
            if (isSelectedDateToday) {
                try { const slotHour = parseInt(timeValue.split(':')[0]); if (slotHour < currentHour) isDisabled = true; }
                catch(e) { console.error(`  [${index}] 시간 비교 오류:`, e); isDisabled = true; }
            }

            // 백엔드 상태 기반 처리
            switch(slotStatus) { // slot.status 사용
                case 'AVAILABLE':
                    if (!isDisabled) { // 과거 시간이 아니라면
                        hasAvailableSlot = true; span.classList.add('btn', 'btn-outline-primary', 'btn-sm', 'available'); input.disabled = false; label.style.cursor = 'pointer';
                        input.addEventListener('change', (e) => { span.classList.toggle('btn-primary', input.checked); span.classList.toggle('btn-outline-primary', !input.checked); updateSummary(); });
                    } else { /* 과거 시간이면 PAST 스타일 적용 */ span.classList.add('btn', 'btn-light', 'btn-sm', 'disabled'); span.style.textDecoration = 'line-through'; label.style.cursor = 'not-allowed'; input.disabled = true; }
                    break;
                case 'BOOKED':
                    span.classList.add('btn', 'btn-secondary', 'btn-sm', 'booked'); span.textContent = "예약 완료"; isDisabled = true; label.style.cursor = 'not-allowed'; input.disabled = true;
                    break;
                case 'PAST':
                default:
                    span.classList.add('btn', 'btn-light', 'btn-sm', 'disabled'); span.style.textDecoration = 'line-through'; isDisabled = true; label.style.cursor = 'not-allowed'; input.disabled = true;
                    break;
            }

            // 변경 모드 시 기존 예약 시간 표시
            if (isOriginallyBooked) {
            console.log(`[renderTimeSlots] 기존 예약 시간 발견! 시간: ${timeValue}, 클래스 추가 시도.`);
            span.classList.add('originally-booked-time'); }

            label.appendChild(input); label.appendChild(span); timeSlotsDiv.appendChild(label);
            console.log(`  [${index}] 처리 완료: ${timeValue}, Status: ${slotStatus}, Disabled: ${isDisabled}`);
        }); // End of forEach
        console.log("시간 슬롯 렌더링 루프 종료.");

    } catch (renderError) { // 렌더링 루프 중 에러 발생 시
         console.error("renderTimeSlots 실행 중 오류 발생:", renderError);
         timeSlotsDiv.innerHTML = '<p class="text-danger small">시간 표시에 오류가 발생했습니다.</p>';
    }

    // 선택 가능한 슬롯 없을 시 메시지 표시
    if (!hasAvailableSlot && timeSlots.some(s => s?.status === 'AVAILABLE')) {
         const msg = document.createElement('p'); msg.className='text-warning small w-100 mt-2'; msg.textContent='현재 선택 가능한 시간이 없습니다.'; timeSlotsDiv.appendChild(msg);
    }

    console.log("--- renderTimeSlots 함수 종료 ---");
}

     // --- 요약 및 금액 계산 함수 ---
     /** 할인 전 기본 가격 */
     function calculateBasePrice() { const c=document.querySelectorAll('input[name="times"]:checked').length; const i=document.querySelector('.seat-item.selected'); let p=0; if(i){ switch(selectedPlanType){ case 'HOURLY': p=c*2000;break; case 'DAILY': p=10000;break; case 'MONTHLY': p=99000;break; }} return p; }
     /** 쿠폰 할인액 */
     function calculateDiscount(basePrice) { let d=0; if (selectedCoupon&&basePrice>0){ if(selectedCoupon.type==='amount')d=selectedCoupon.value; else if(selectedCoupon.type==='percent')d=Math.floor(basePrice*(selectedCoupon.value/100)); d=Math.min(basePrice,d); } return d; }
     /** 예약 요약 업데이트 */
     function updateSummary() { const item=document.querySelector('.seat-item.selected'); const date=selectedDate||'-'; const times=Array.from(document.querySelectorAll('input[name="times"]:checked')).map(cb=>cb.nextElementSibling.textContent.trim()); let plan='', duration='', itemType='좌석', name='-'; if(item){name=item.dataset.seatName||'-';itemType=item.dataset.seatType==='ROOM'?'룸':'좌석';} switch(selectedPlanType){case 'HOURLY':plan='시간제';break;case 'DAILY':plan='일일권';duration=`(${date})`;break;case 'MONTHLY':plan='월정액권';if(selectedDate){try{const s=new Date(selectedDate);const e=new Date(s);e.setMonth(s.getMonth()+1);e.setDate(s.getDate());duration=`(${date} ~ ${e.toISOString().split('T')[0]})`;}catch(e){duration='(기간오류)';}}break;} let discTxt=''; const base=calculateBasePrice(); let disc=calculateDiscount(base); if(selectedCoupon&&item&&base>0&&disc>0)discTxt=`<p class="mb-2 text-danger"><strong><i class="fas fa-tags fa-fw me-2"></i>쿠폰 할인:</strong> <span class="fw-medium ms-1">- ${disc.toLocaleString()}원 (${selectedCoupon.name})</span></p>`; if(reservationDetailsDiv) reservationDetailsDiv.innerHTML = `<p class="mb-2"><strong><i class="fas fa-tags fa-fw me-2 text-secondary"></i>요금제:</strong> <span class="text-primary fw-medium ms-1">${plan} ${duration}</span></p><p class="mb-2"><strong><i class="fas fa-calendar-day fa-fw me-2 text-secondary"></i>${selectedPlanType==='MONTHLY'?'시작일':'날짜'}:</strong> <span class="text-primary fw-medium ms-1">${date}</span></p><p class="mb-2"><strong><i class="fas fa-check-circle fa-fw me-2 text-secondary"></i>선택:</strong> <span class="text-primary fw-medium ms-1">${item?`${itemType} ${name}`:'-'}</span></p>${selectedPlanType==='HOURLY'&&item?(times.length>0?`<p class="mb-2"><strong><i class="fas fa-clock fa-fw me-2 text-secondary"></i>시간 (${times.length}시간):</strong> <span class="text-primary fw-medium ms-1">${times.join(', ')}</span></p>`:'<p class="text-muted small mb-2">시간 선택 필요</p>'):''}${discTxt}`; calculateTotal(); }
     /** 총 금액 계산 */
     function calculateTotal() {
             const selectedItem = document.querySelector('.seat-item.selected');
             let totalPrice = 0; // 최종 결제 금액
             let totalCount = 0; // 예약 건수

             if (selectedItem) { // 좌석/룸이 선택된 경우에만 계산 시작
                 totalCount = 1; // 기본 1건
                 const basePrice = calculateBasePrice(); // 할인 전 기본 요금 계산
                 const couponDiscount = calculateDiscount(basePrice); // 쿠폰 할인액 계산
                 let priceAfterCoupon = basePrice - couponDiscount; // 쿠폰 적용 후 가격

                 if (typeof LOGGED_IN_USER_AUTH !== 'undefined' && LOGGED_IN_USER_AUTH === "임직원") {
                     console.log("임직원 확인됨 -> 최종 가격 0원 처리");
                     finalPrice = 0; // 최종 가격을 0으로 설정

                     if (selectedPlanType === 'HOURLY' && document.querySelectorAll('input[name="times"]:checked').length === 0) {
                         totalCount = 0; // 시간 미선택 시 0건
                     }
                 } else {
                     // 임직원이 아니면 쿠폰 적용된 가격 사용
                     finalPrice = priceAfterCoupon;
                     // 시간제이고 시간 미선택 시 0건 처리
                     if (selectedPlanType === 'HOURLY' && document.querySelectorAll('input[name="times"]:checked').length === 0) {
                         totalCount = 0;
                     }
                 }

             } else {
                 // 선택된 좌석/룸이 없으면 0건, 0원
                 totalCount = 0;
                 finalPrice = 0;
             }

             // 최종 계산된 건수와 금액을 화면에 업데이트
             if(totalCountSpan) totalCountSpan.textContent = totalCount;
             if(totalPriceSpan) totalPriceSpan.textContent = finalPrice.toLocaleString(); // 0 또는 실제 금액
         }



    // --- 결제 관련 함수 (카카오페이 준비 호출) ---
    function proceedToPayment() {
        console.log(`proceedToPayment 호출됨 (현재 변경 모드: ${isModificationMode})`);
         if (!paymentButton) {
             console.error("결제 버튼 요소를 찾을 수 없습니다.");
             return;
         }

        if (paymentButton.disabled) {
            console.log("이미 처리 중인 요청입니다."); // 중복 클릭 시 로그 확인
            return; // 함수 종료
        }
        const originalButtonText = isModificationMode ? '<i class="fas fa-save me-2"></i> 변경 저장하기' : '<i class="fas fa-credit-card me-2"></i> 예약 및 결제하기';
        paymentButton.disabled = true; // 클릭 즉시 비활성화
        paymentButton.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i> 예약 처리 중...';

        const totalCount = parseInt(totalCountSpan?.textContent || '0');
        if (totalCount === 0) { alert('예약할 항목과 요금제를 선택해주세요.'); return; }
        const selectedItem = document.querySelector('.seat-item.selected');
        const itemType = selectedItem?.dataset.seatType === 'ROOM' ? 'ROOM' : 'SEAT';
        const itemId = selectedItem?.dataset.seatId;
        const itemName = selectedItem?.dataset.seatName || '선택 좌석';
        const selectedTimeValues = Array.from(document.querySelectorAll('input[name="times"]:checked')).map(cb => cb.value);
        const finalPrice = parseInt(totalPriceSpan?.textContent.replace(/,/g, '') || '0');

        if (!itemId || !selectedDate) { alert('항목 또는 시작 날짜가 선택되지 않았습니다.'); return; }
        if (selectedPlanType === 'HOURLY' && selectedTimeValues.length === 0) { alert('시간제는 시간을 선택해야 합니다.'); return; }
        /*if (finalPrice <= 0 && selectedPlanType !== 'FREE_PLAN') { alert('결제할 금액이 없습니다.'); return; }*/
         const currentUserId = (typeof LOGGED_IN_USER_ID !== 'undefined') ? LOGGED_IN_USER_ID : null;
        if (!currentUserId) { // null 또는 0 등 falsy 값 체크
            alert('사용자 정보를 가져올 수 없습니다. 다시 로그인해주세요.');
            paymentButton.disabled = false; paymentButton.innerHTML = originalButtonText; return;
        }

        // 백엔드 /api/payment/kakao/ready 로 보낼 데이터
        const paymentReadyData = {
             itemId: parseInt(itemId),
             itemName: `${itemType} ${itemName}`,
             quantity: 1,
             totalAmount: finalPrice,
             userId: String(currentUserId), // 문자열 ID로 전달 (카카오페이 요구사항)
             planType: selectedPlanType,
             reservationDate: selectedDate,
             selectedTimes: selectedPlanType === 'HOURLY' ? selectedTimeValues : [],
             couponId: selectedCoupon?.id || null
         };

        console.log("백엔드로 카카오페이 결제 준비 요청:", paymentReadyData);
        if(paymentButton) { paymentButton.disabled = true; paymentButton.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i> 결제 준비 중...'; }

        const csrfToken = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
        const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");
        const headers = { 'Content-Type': 'application/json' };
        if (csrfToken && csrfHeader) { headers[csrfHeader] = csrfToken; }

        // --- 모드에 따른 분기 처리 ---
       if (isModificationMode) {
           console.log("예약 변경 API 호출 준비");
           // 원본 예약 정보 확인 (originalReservationData는 인라인 스크립트에서 생성)
           if (!originalReservationData?.resNo) {
                alert("변경할 원본 예약 정보를 찾을 수 없습니다. 페이지를 새로고침 해주세요.");
                paymentButton.disabled = false; paymentButton.innerHTML = originalButtonText; return;
           }

           // 백엔드 변경 API(@RequestBody)로 보낼 데이터
           const modificationData = {
               itemId: parseInt(itemId),        // 변경된 좌석 ID (Long)
               // itemType: itemType,         // 백엔드 DTO에 필요시 포함
               planType: selectedPlanType,       // 요금제 (변경 안 되지만 검증용)
               reservationDate: selectedDate,    // 변경된 날짜
               selectedTimes: selectedPlanType === 'HOURLY' ? selectedTimeValues : [], // 변경된 시간
               couponId: selectedCoupon?.id || null // 변경된 쿠폰
               // userId는 백엔드에서 Security Context로 확인하므로 보낼 필요 없음
               // totalPrice는 백엔드에서 재계산
           };

           const originalResNo = originalReservationData.resNo;
           const updateApiUrl = `/api/reservations/${originalResNo}`;
           console.log("백엔드로 예약 변경 요청:", updateApiUrl, modificationData);

           fetch(updateApiUrl, {
               method: 'PUT', // 변경은 PUT 또는 PATCH 사용
               headers: headers, // CSRF 헤더 포함
               body: JSON.stringify(modificationData)
           })
           .then(response => {
                // 에러 응답 처리
                if (response.status === 409) { return response.json().then(err => { throw new Error(err.message || '변경하려는 시간에 이미 예약이 있습니다.'); }); }
                else if (response.status === 403) { throw new Error('예약을 변경할 권한이 없습니다.'); }
                else if (response.status === 404) { throw new Error('변경할 예약 정보를 찾을 수 없습니다.'); }
                else if (!response.ok) { return response.json().then(err => { throw new Error(err.message || `변경 실패 (${response.status})`); }); }
                // 성공 응답 처리
                if (response.status === 204) return null; // No Content
                return response.json(); // 내용 있는 성공 응답 (optional)
           })
           .then(result => {
                console.log("예약 변경 성공:", result);
                alert("예약이 성공적으로 변경되었습니다!");
                window.location.href = "/mypage/reservations";
           })
           .catch(error => {
                console.error("예약 변경 오류:", error);
                alert(`예약 변경 실패: ${error.message}`);
                // 오류 시 버튼 다시 활성화
                if(paymentButton) { paymentButton.disabled = false; paymentButton.innerHTML = originalButtonText; }
           });

       } else {
           // ====================================
           // === 신규 예약 처리 (카카오페이 준비) ===
           // ====================================
           // 백엔드 /api/payment/kakao/ready 로 보낼 데이터
           const paymentReadyData = {
                itemId: parseInt(itemId), // 백엔드 DTO 타입에 맞게 (Long)
                itemName: `${itemType} ${itemName}`, // 카카오페이 표시용 상품명
                quantity: 1, // 수량
                totalAmount: finalPrice, // 최종 결제 금액
                userId: String(currentUserId), // 카카오페이 partner_user_id (String)
                // --- 예약 정보도 함께 전달 (백엔드 /ready에서 예약 생성 위해) ---
                planType: selectedPlanType,
                reservationDate: selectedDate,
                selectedTimes: selectedPlanType === 'HOURLY' ? selectedTimeValues : [],
                couponId: selectedCoupon?.id || null
                // ---------------------------------------------------
            };

              console.log("백엔드로 카카오페이 결제 준비 요청:", paymentReadyData);
              if(paymentButton) { paymentButton.disabled = true; paymentButton.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i> 결제 준비 중...'; }


              const csrfToken = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
              const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");
              const headers = { 'Content-Type': 'application/json' };
              if (csrfToken && csrfHeader) { headers[csrfHeader] = csrfToken; }

              fetch('/api/payment/kakao/ready', {
                  method: 'POST',
                  headers: headers,
                  body: JSON.stringify(paymentReadyData)
              })
              .then(response => {
                  if (!response.ok) { return response.json().then(err => { throw new Error(err.message || `결제 준비 실패 (${response.status})`); }); }
                  return response.json();
              })
              .then(result => {
                  console.log('백엔드 결제 준비 성공 응답:', result);
                  if (result.success) {
                        if (result.paymentSkipped == true) {
                            alert('임직원 할인 적용으로 예약 처리가 완료되었습니다.');
                            const orderId = result.orderId || ("RESERVE_" + result.reservationId);
                            const successPageUrl = `/payment-success?orderId=${encodeURIComponent(orderId)}`;
                            window.location.href = successPageUrl;
                        } else if (result.next_redirect_pc_url) { // <<<--- 카카오 URL 확인
                            window.location.href = result.next_redirect_pc_url; // 이동
                        } else {
                            throw new Error('카카오페이 URL 수신 오류');
                        }
                    } else { throw new Error('카카오페이 URL 수신 오류'); }
              })
              .catch(error => {
                  console.error('결제 준비 오류:', error);
                  alert(`결제 준비 중 오류: ${error.message}`);
                  if(paymentButton) { paymentButton.disabled = false; paymentButton.innerHTML = '<i class="fas fa-credit-card me-2"></i> 결제하기'; }
              });
          }
      }


    /** (신규) 예약 변경 API 호출 함수 */
    function proceedToPaymentForUpdate() {
      console.log(`예약 변경 내용 저장/결제 시도 (ResNo: ${originalResNo})`);
      if (!originalResNo) {
        alert("변경할 예약 정보가 없습니다.");
        return;
      } // 변경 대상 확인
      const selectedItem = document.querySelector(".seat-item.selected");
      const newItemId = selectedItem?.dataset.seatId;
      const newSelectedTimes = Array.from(
        document.querySelectorAll('input[name="times"]:checked')
      ).map((cb) => cb.value);
      const finalPrice = parseInt(
        totalPriceSpan?.textContent.replace(/,/g, "") || "0"
      );
      const currentSelectedPlan = document.querySelector(
        'input[name="planType"]:checked'
      )?.value;

      // --- 유효성 검사 ---
      if (!newItemId || !selectedDate) {
        alert("새로운 좌석/룸 또는 날짜를 선택해야 합니다.");
        return;
      }
      if (currentSelectedPlan !== originalPlanType) {
        alert(`요금제는 변경할 수 없습니다 (${originalPlanType} 유지).`);
        return;
      }
      if (selectedPlanType === "HOURLY" && newSelectedTimes.length === 0) {
        alert("시간제는 시간을 선택해야 합니다.");
        return;
      }

      if (!currentUserId) {
        alert("사용자 정보를 가져올 수 없습니다.");
        return;
      }
      // --- ---

      // 백엔드 PUT /api/reservations/{resNo} 로 보낼 데이터
      const updateRequestData = {
        itemId: parseInt(newItemId),
        planType: selectedPlanType,
        reservationDate: selectedDate,
        selectedTimes: selectedPlanType === "HOURLY" ? newSelectedTimes : [],
        couponId: selectedCoupon?.id || null,
        userId: currentUserId,
        totalPrice: finalPrice, // 백엔드 검증용
      };

      console.log(
        `백엔드로 예약 변경 요청 (ResNo: ${originalResNo}):`,
        updateRequestData
      );
      if (paymentButton) {
        paymentButton.disabled = true;
        paymentButton.innerHTML =
          '<i class="fas fa-spinner fa-spin me-2"></i> 변경 처리 중...';
      }

      // --- 실제 백엔드 예약 변경 API 호출 ---
      const apiUrl = `/api/reservations/${originalResNo}`;
      const csrfToken = document
        .querySelector("meta[name='_csrf']")
        ?.getAttribute("content");
      const csrfHeader = document
        .querySelector("meta[name='_csrf_header']")
        ?.getAttribute("content");
      const headers = { "Content-Type": "application/json" };
      if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
      }

      fetch(apiUrl, {
        method: "PUT",
        headers: headers,
        body: JSON.stringify(updateRequestData),
      })
        .then((response) => {
          if (!response.ok) {
            return response.json().then((err) => {
              throw new Error(err.message || "변경 실패");
            });
          }
          return response.json();
        })
        .then((result) => {
          console.log("백엔드 예약 변경 성공 응답:", result);
          if (result.success) {
            if (result.paymentRequired) {
              alert(
                "예약 내용이 변경되어 추가 결제가 필요합니다. 카카오페이로 이동합니다."
              );
              // 백엔드에서 받은 새 주문 정보로 카카오페이 준비 API 다시 호출
              callKakaoPayReady(
                result.newOrderId,
                result.newOrderName,
                result.newAmount,
                result.customerKey
              );
            } else {
              // 가격 변동 없거나 환불 등 (여기선 단순 성공 처리)
              alert("예약 내용이 성공적으로 변경되었습니다.");
              window.location.href = "/my/reservations";
            }
          } else {
            throw new Error(result.message || "알 수 없는 변경 오류");
          }
        })
        .catch((error) => {
          console.error("예약 변경 처리 중 오류:", error);
          alert(`예약 변경 실패: ${error.message}`);
          if (paymentButton) {
            paymentButton.disabled = false;
            paymentButton.innerHTML =
              '<i class="fas fa-check me-2"></i> 변경 내용 저장/결제';
          }
        });
    }

    /** (신규) 카카오페이 준비 API 호출 함수 (변경 건용) */
    function callKakaoPayReady(orderId, orderName, amount, customerKey) {
      const paymentReadyData = {
        partnerOrderId: orderId,
        partnerUserId: customerKey,
        itemName: orderName,
        quantity: 1,
        totalAmount: amount,
        taxFreeAmount: 0,
      };
      console.log(
        "백엔드로 카카오페이 결제 준비 요청 (변경 건):",
        paymentReadyData
      );
      const csrfToken = document
        .querySelector("meta[name='_csrf']")
        ?.getAttribute("content");
      const csrfHeader = document
        .querySelector("meta[name='_csrf_header']")
        ?.getAttribute("content");
      const headers = { "Content-Type": "application/json" };
      if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
      }

      fetch("/api/payment/kakao/ready", {
        method: "POST",
        headers: headers,
        body: JSON.stringify(paymentReadyData),
      })
        .then((response) => {
          if (!response.ok) {
            return response.json().then((err) => {
              throw new Error(err.message || "준비 실패");
            });
          }
          return response.json();
        })
        .then((result) => {
          if (result.next_redirect_pc_url) {
            window.location.href = result.next_redirect_pc_url;
          } else {
            throw new Error("URL 수신 오류");
          }
        })
        .catch((error) => {
          alert(`결제 준비 실패: ${error.message}`);
          if (paymentButton) {
            paymentButton.disabled = false;
            paymentButton.innerHTML =
              '<i class="fas fa-check me-2"></i> 변경 내용 저장/결제';
          }
        });
    }

 /** (신규) 변경 대상 예약 정보 로드 및 화면 설정 */
async function loadReservationForModification(resNo) {
    console.log(`[JS] 수정할 예약 정보 로드 시작 (ResNo: ${resNo})`);
    const apiUrl = `/api/reservations/details/${resNo}`;

    try {
        const reservationData = await fetchData(apiUrl);
        console.log("[JS] API 호출 성공, 받은 데이터:", reservationData);

        if (!reservationData) {
            throw new Error("필수 예약 정보가 누락되었습니다.");
        }

        // --- 데이터 적용 전 확인 ---
        console.log("[JS] 전역 변수 설정 시도:", reservationData.planType, reservationData.seatInfo, reservationData.selectedTimes);
        originalPlanType = reservationData.planType;
        originalSeatInfo = reservationData.seatInfo;
        originalTimes = reservationData.selectedTimes || [];
        const originalDate = reservationData.reservationDate;

        // --- 요금제 설정 확인 ---
        console.log("[JS] 요금제 라디오 버튼 설정 시도:", originalPlanType);
        planRadios.forEach(radio => {
            radio.checked = (radio.value === originalPlanType);
            // console.log(`[JS] Radio ${radio.value} checked: ${radio.checked}`); // 상세 확인
        });
        selectedPlanType = originalPlanType;
        // handlePlanChange(); // 여기서 호출하면 비동기 문제 발생 가능성 있음

        // --- 달력 설정 확인 ---
        console.log("[JS] 달력 초기화 시도:", originalDate);
        const initialYear = parseInt(originalDate.substring(0, 4));
        const initialMonth = parseInt(originalDate.substring(5, 7));
        loadCalendarDataAndInitialize(initialYear, initialMonth, originalDate);

        // --- 쿠폰 로드 및 설정 확인 ---
        console.log("[JS] 쿠폰 로드 및 설정 시도");
        await loadAvailableCoupons(); // await 사용 확인
        if (reservationData.couponId && couponSelect) {
            couponSelect.value = reservationData.couponId;
             console.log("[JS] 기존 쿠폰 선택됨:", reservationData.couponId);
            handleCouponChange();
        }

        // --- 원본 선택 항목 표시 함수 호출 전 확인 ---
        console.log("[JS] highlightOriginalSelection 함수 호출 예정");
        setTimeout(() => {
            highlightOriginalSelection();
        }, 1500);



    } catch (error) {
        console.error("[JS] 예약 정보 로드 실패:", error);
        alert(`예약 정보를 불러오는 중 오류가 발생했습니다: ${error.message}`);
    }
}



 function highlightOriginalSelection() {
   console.log("원본 선택 항목 표시 시도");
   if (!modificationMode || !originalSeatInfo) return;

   // 1. 원본 좌석 찾기 및 선택
   const originalSeatElement = document.querySelector(
     `.seat-item[data-seat-id="${originalSeatInfo.id}"]`
   );
   if (originalSeatElement) {
     console.log("원본 좌석 찾음:", originalSeatElement);
     // 기존 선택 해제 및 원본 선택
     const currentSelected = document.querySelector(".seat-item.selected");
     if (currentSelected) currentSelected.classList.remove("selected");
     originalSeatElement.classList.add("selected");
     originalSeatElement.classList.add("originally-selected"); // 식별용 클래스 추가 (CSS 스타일링 필요)
     originalSeatElement.scrollIntoView({ behavior: "smooth", block: "center" }); // 해당 좌석으로 스크롤 (선택적)

     // 2. 원본 시간 선택 (시간제일 경우)
     if (originalPlanType === "HOURLY" && originalTimes.length > 0) {
       // 시간 슬롯 로드가 완료되었는지 확인 후 실행 필요
       // renderTimeSlots 완료 시점 확인 필요
       console.log("원본 시간 선택 시도:", originalTimes);
       loadAvailableTimes()
         .then(() => {
           // 시간 로드가 완료된 후 실행 (loadAvailableTimes가 Promise 반환 가정)
           originalTimes.forEach((timeValue) => {
             const timeCheckbox = document.getElementById(
               `time-${timeValue.replace(":", "")}`
             );
             const timeSpan = timeCheckbox?.nextElementSibling;
             if (timeCheckbox && !timeCheckbox.disabled) {
               timeCheckbox.checked = true;
               if (timeSpan) {
                 // 스타일 업데이트
                 timeSpan.classList.add("btn-primary");
                 timeSpan.classList.remove("btn-outline-primary");
               }
             } else {
               console.warn(
                 `원본 시간(${timeValue})을 선택할 수 없거나 찾을 수 없습니다.`
               );
             }
           });
           updateSummary(); // 시간 선택 후 요약 업데이트
         })
         .catch((e) => console.error("시간 로드 후 원본 선택 중 오류", e));
     } else {
       updateSummary(); // 좌석만 선택하고 요약 업데이트
     }
   } else {
     console.warn("원본 좌석 요소를 찾을 수 없습니다.");
     updateSummary(); // 좌석 못찾아도 일단 업데이트
   }
 }


    document.addEventListener('DOMContentLoaded', function() {
        console.log("DOM 로드 완료. 초기화 시작.");

        if (typeof isModificationMode !== 'undefined' && isModificationMode === true) {
            console.log("예약 변경 모드로 페이지 로드");
            if (typeof originalReservationData !== 'undefined' && originalReservationData) {
                 console.log("원본 예약 데이터 확인됨:", originalReservationData);

                 selectedDate = originalReservationData.reservationDate;
                 selectedPlanType = originalReservationData.planType;
                // --------------------------------------------------------------
            } else {
                console.error("예약 변경 페이지에 기존 예약한 정보가 없습니다.");
                alert("예약 정보를 불러오는 데 실패했습니다. 다시 시도해주세요.");
                window.location.href = "/";
                return; // 초기화 중단
            }
            // 버튼 텍스트 변경
            if(paymentButton) paymentButton.innerHTML = '<i class="fas fa-save me-2"></i> 변경 저장하기';

        } else {
                console.log("신규 예약 모드로 페이지 로드");
        }

        // 이벤트 리스너 등록
        calendarIcon?.addEventListener('click', openCalendar);
        planRadios.forEach(radio => { radio.addEventListener('change', handlePlanChange); });
        couponSelect?.addEventListener('change', handleCouponChange);
        paymentButton?.addEventListener('click', proceedToPayment);

        // ------------------------------------------

        // --- 초기화 함수 호출 ---
        const initialYear = new Date().getFullYear();
        const initialMonth = new Date().getMonth() + 1;
        // 변경 모드면 원본 날짜, 아니면 오늘 날짜 기본 선택
        const dateToSelect = isModificationMode ? originalReservationData.reservationDate : new Date().toISOString().split('T')[0];
        loadCalendarDataAndInitialize(initialYear, initialMonth, dateToSelect);
        loadAvailableCoupons();
        handlePlanChange(); // 초기 요금제 UI 반영
        updateSummary();
        console.log("초기화 로직 완료.");
    });
