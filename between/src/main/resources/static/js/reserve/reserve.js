  // --- 전역 변수 ---
  let flatpickrInstance = null;
  let selectedDate = null; // "YYYY-MM-DD"
  let currentYear = new Date().getFullYear();
  let currentMonth = new Date().getMonth() + 1; // 1~12
  let selectedPlanType = 'HOURLY'; // HOURLY, DAILY, MONTHLY
  let availableCoupons = []; // 사용 가능 쿠폰 목록
  let selectedCoupon = null; // 선택된 쿠폰 정보 {id, name, type, value}
  // 토스페이먼츠 관련 변수 제거됨

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
  const paymentButton = document.getElementById('payment-button'); // 버튼 ID 원래대로
  const couponSelect = document.getElementById('couponSelect');
  const discountInfoDiv = document.getElementById('discount-info');
  // 토스페이먼츠 관련 DOM 요소 제거됨

  // --- 유틸리티 함수 ---
  /** 서버 API 호출 함수 */
  async function fetchData(url, options = {}) {
      try {
          const response = await fetch(url, options);
          if (!response.ok) { throw new Error(`HTTP ${response.status}`); }
          if (response.status === 204) return null; // No Content 처리
          return await response.json();
      } catch (error) { console.error(`Workspace error for ${url}:`, error); throw error; }
  }
  // generateUUID 함수 제거됨 (토스 연동 시 필요)

  // --- Flatpickr (달력) 관련 함수 ---
  /** Flatpickr 초기화 */
  function initializeCalendar(enabledDates = [], initialDate = null) {
      if (!calendarInput) return;
      if (flatpickrInstance) flatpickrInstance.destroy();
      flatpickrInstance = flatpickr(calendarInput, { locale: "ko", dateFormat: "Y-m-d", minDate: "today", enable: enabledDates, defaultDate: initialDate, onChange: handleDateChange, onMonthChange: handleMonthYearChange, onYearChange: handleMonthYearChange, onClose: handleCalendarClose });
      updateCalendarInput(initialDate);
  }
  /** Flatpickr input 업데이트 */
  function updateCalendarInput(dateStr) { if (dateStr && /^\d{4}-\d{2}-\d{2}$/.test(dateStr)) { if(calendarInput) calendarInput.value = dateStr; selectedDate = dateStr; } else { if(calendarInput) calendarInput.value = ''; selectedDate = null; } }
  /** Flatpickr 날짜 선택 */
  function handleDateChange(selectedDates, dateStr, instance) { console.log("날짜 선택됨:", dateStr); updateCalendarInput(dateStr); loadSeatStatus(); updateSummary(); }
  /** Flatpickr 월/연도 변경 */
  function handleMonthYearChange(selectedDates, dateStr, instance) { const ny = instance.currentYear, nm = instance.currentMonth + 1; if (currentYear !== ny || currentMonth !== nm) { console.log(`달력 변경: ${ny}-${nm}`); currentYear = ny; currentMonth = nm; loadCalendarDataAndInitialize(ny, nm); } }
  /** Flatpickr 닫힐 때 */
  function handleCalendarClose(selectedDates, dateStr, instance) { if (!calendarInput?.value) { updateCalendarInput(null); loadSeatStatus(); updateSummary(); } }
  /** 달력 데이터 로드 및 초기화 */
  function loadCalendarDataAndInitialize(year, month, initialDateToSelect = null) {
      console.log(`달력 로드 요청: ${year}-${month}`);
      if(calendarInput) calendarInput.placeholder = "로딩 중...";
      const apiUrl = `/api/calendar?year=${year}&month=${month}`; // 백엔드 주소 확인!
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
  /** 달력 아이콘 클릭 */
  function openCalendar() { flatpickrInstance?.open(); }

  // --- 요금제 관련 함수 ---
  /** 요금제 변경 처리 */
  function handlePlanChange() { const radio = document.querySelector('input[name="planType"]:checked'); if (!radio) return; selectedPlanType = radio.value; console.log("요금제 변경:", selectedPlanType); if(!timeSelectionCard || !timeSelectionGuide) return; if (selectedPlanType === 'HOURLY') { timeSelectionCard.classList.remove('disabled'); timeSelectionCard.style.display = 'block'; timeSelectionGuide.textContent = '좌석/룸과 날짜를 선택하면 시간이 표시됩니다.'; loadAvailableTimes(); } else { timeSelectionCard.classList.add('disabled'); if (selectedPlanType === 'DAILY') timeSelectionGuide.textContent = '일일권은 당일 운영시간 동안 이용 가능합니다.'; else timeSelectionGuide.textContent = '월정액권은 시간 선택이 불필요합니다.'; clearTimeSelection(); } updateSummary(); calculateTotal(); }

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
    /** 좌석 정보 렌더링 (DB 위치 정보 사용 - 2x3 배치 기준) */
      function renderSeats(seats = []) {
          if (!seatMapView) return;
          seatMapView.innerHTML = ''; // 이전 좌석 비우기



          if (!seats || seats.length === 0) {
              seatMapView.innerHTML = '<p class="text-muted small p-3">등록된 좌석 정보가 없습니다.</p>';
              return;
          }
          console.log("좌석 렌더링 시작 (DB 위치 정보 사용)");

          seats.forEach(item => {
              // 좌석 요소 생성 및 기본 정보 설정
              const div = document.createElement('div');
              div.textContent = item.name;
              const typeClass = item.type?.toLowerCase() || 'seat';
              const statusClass = item.status ? item.status.toLowerCase() : 'unavailable';
              div.className = `${typeClass} ${statusClass} seat-item`; // CSS 클래스
              div.dataset.seatId = item.id; // 데이터 속성
              div.dataset.seatName = item.name;
              div.dataset.seatType = item.type;

              // --- DB에서 가져온 위치 정보 적용 ---
              const rowVal = item?.gridRow;
              const colVal = item?.gridColumn;
              // 로그로 값 확인 (디버깅 시 주석 해제)
              // console.log(`Seat ${item.name}: DB 값 -> gridRow='${rowVal}', gridColumn='${colVal}' (타입: row=${typeof rowVal}, col=${typeof colVal})`);

              const isValidGridValue = (val) => val && typeof val === 'string' && val.trim() !== '' && val.toLowerCase() !== 'null';

              if (isValidGridValue(rowVal)) {
                  div.style.gridRow = rowVal; // 예: '1', '2'
              } else {
                  console.warn(`  -> Seat ${item.name}: 유효하지 않은 gridRow ('${rowVal}')`);
              }
              if (isValidGridValue(colVal)) {
                  div.style.gridColumn = colVal; // 예: '1', '2', '3'
              } else {
                   console.warn(`  -> Seat ${item.name}: 유효하지 않은 gridColumn ('${colVal}')`);
              }
              // ---------------------------------

              // 상태에 따른 이벤트 및 스타일 설정
              if (statusClass === 'available' && typeClass !== 'area') {
                  div.style.cursor = 'pointer';
                  div.addEventListener('click', () => selectSeat(div));
              } else {
                  div.style.cursor = 'not-allowed';
                  div.style.opacity = '0.6';
                  if(statusClass === 'static') div.style.opacity = '0.8';
              }

              // 최종적으로 좌석판에 추가
              seatMapView.appendChild(div);
          });
      }
  /** 좌석 선택 (단일) */
  function selectSeat(seatElement) { if (seatElement.classList.contains('unavailable') || seatElement.classList.contains('static')) return; const current = document.querySelector('.seat-item.selected'); if (current === seatElement) { seatElement.classList.remove('selected'); clearTimeSelection(); updateSummary(); calculateTotal(); return; } if (current) current.classList.remove('selected'); seatElement.classList.add('selected'); if (selectedPlanType === 'HOURLY') loadAvailableTimes(); else clearTimeSelection(); updateSummary(); }
  /** 시간 선택 초기화 */
  function clearTimeSelection() { if(timeSlotsDiv) timeSlotsDiv.innerHTML = ''; document.querySelectorAll('input[name="times"]:checked').forEach(cb => cb.checked = false); if(timeSelectionGuide) timeSelectionGuide.textContent = '시간제 선택 시...'; }
   /** !!! 예약 가능 시간 로드 (지난 시간 비활성화 로직 추가) !!! */
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

          const apiUrl = `/api/times?date=${date}&seatId=${seatId}`; // !!! 백엔드 주소 확인 !!!

          fetchData(apiUrl)
              .then(times => {
                  // renderTimeSlots 호출 시 isSelectedDateToday와 currentHour 전달
                  renderTimeSlots(times, isSelectedDateToday, currentHour);
              })
              .catch(error => { if(timeSlotsDiv) timeSlotsDiv.innerHTML = `<p class="text-danger small">시간 정보 로드 실패</p>`; })
              .finally(updateSummary); // 시간 로드 후 항상 요약 업데이트
      }

     /** 시간 슬롯 렌더링 (상태 기반 - AVAILABLE, BOOKED, PAST) */
     function renderTimeSlots(timeSlots = []) { // 파라미터 이름 변경 (times -> timeSlots)
         if(!timeSlotsDiv) return;
         timeSlotsDiv.innerHTML = ''; // 이전 내용 클리어

         if (!timeSlots?.length) {
             // API 호출은 성공했으나 데이터가 없는 경우 (백엔드 오류 등)
             timeSlotsDiv.innerHTML = '<p class="text-warning small">시간 정보를 조회할 수 없습니다.</p>';
             return;
         }

         let hasAvailableSlot = false; // 예약 가능한 슬롯이 하나라도 있는지 확인용

         timeSlots.forEach(slot => { // 각 TimeSlotDto 객체 순회
             const timeValue = slot.startTime; // "HH:mm"
             // 종료 시간은 시작 시간 + 1시간으로 가정하여 표시
             const timeDisplay = `${timeValue} - ${String(parseInt(timeValue.split(':')[0]) + 1).padStart(2,'0')}:00`;
             const label = document.createElement('label'); label.classList.add('me-2', 'mb-2');
             const input = document.createElement('input'); input.type = 'checkbox'; input.name = 'times'; input.value = timeValue; input.id = `time-${timeValue.replace(':', '')}`; input.classList.add('d-none');
             const span = document.createElement('span'); span.textContent = timeDisplay; // 기본 텍스트는 시간 표시

             // --- !!! 상태(status)에 따라 스타일 및 동작 결정 !!! ---
             switch(slot.status) {
                 case 'AVAILABLE':
                     hasAvailableSlot = true; // 예약 가능 슬롯 있음!
                     span.classList.add('btn', 'btn-outline-primary', 'btn-sm', 'available');
                     input.disabled = false;
                     label.style.cursor = 'pointer';
                     // 활성화된 슬롯에만 이벤트 리스너 추가
                     input.addEventListener('change', (e) => {
                         span.classList.toggle('btn-primary', input.checked);
                         span.classList.toggle('btn-outline-primary', !input.checked);
                         updateSummary();
                     });
                     break;
                 case 'BOOKED':
                     span.classList.add('btn', 'btn-secondary', 'btn-sm', 'booked'); // 'booked' 클래스 추가
                     span.textContent = "예약 완료"; // !!! 텍스트 변경 !!!
                     input.disabled = true;
                     label.style.cursor = 'not-allowed';
                     break;
                 case 'PAST':
                 default: // PAST 또는 알 수 없는 상태
                     span.classList.add('btn', 'btn-light', 'btn-sm', 'disabled'); // 기존 'disabled' 스타일 활용
                     span.style.textDecoration = 'line-through';
                     input.disabled = true;
                     label.style.cursor = 'not-allowed';
                     break;
             }
             // ----------------------------------------------------

             label.appendChild(input); label.appendChild(span); timeSlotsDiv.appendChild(label);
         });

         // 만약 AVAILABLE 슬롯이 하나도 없다면 안내 문구 표시
         if (!hasAvailableSlot) {
              // timeSlotsDiv.innerHTML = '<p class="text-warning small">선택하신 좌석은 현재 예약 가능한 시간이 없습니다.</p>';
              // 또는 기존 버튼들을 그대로 두되, 안내 문구만 추가
              const noTimeMsg = document.createElement('p');
              noTimeMsg.className = 'text-warning small w-100'; // 전체 너비 차지
              noTimeMsg.textContent = '예약 가능한 시간이 없습니다.';
              timeSlotsDiv.appendChild(noTimeMsg);
         }
     }
  // --- 요약 및 금액 계산 함수 ---
  /** 할인 전 기본 가격 */
  function calculateBasePrice() { const c=document.querySelectorAll('input[name="times"]:checked').length; const i=document.querySelector('.seat-item.selected'); let p=0; if(i){ switch(selectedPlanType){ case 'HOURLY': p=c*2000;break; case 'DAILY': p=10000;break; case 'MONTHLY': p=99000;break; }} return p; }
  /** 쿠폰 할인액 */
  function calculateDiscount(basePrice) { let d=0; if (selectedCoupon&&basePrice>0){ if(selectedCoupon.type==='amount')d=selectedCoupon.value; else if(selectedCoupon.type==='percent')d=Math.floor(basePrice*(selectedCoupon.value/100)); d=Math.min(basePrice,d); } return d; }
  /** 예약 요약 업데이트 */
  function updateSummary() { const item=document.querySelector('.seat-item.selected'); const date=selectedDate||'-'; const times=Array.from(document.querySelectorAll('input[name="times"]:checked')).map(cb=>cb.nextElementSibling.textContent.trim()); let plan='', duration='', itemType='좌석', name='-'; if(item){name=item.dataset.seatName||'-';itemType=item.dataset.seatType==='ROOM'?'룸':'좌석';} switch(selectedPlanType){case 'HOURLY':plan='시간제';break;case 'DAILY':plan='일일권';duration=`(${date})`;break;case 'MONTHLY':plan='월정액권';if(selectedDate){try{const s=new Date(selectedDate);const e=new Date(s);e.setMonth(s.getMonth()+1);e.setDate(s.getDate());duration=`(${date} ~ ${e.toISOString().split('T')[0]})`;}catch(e){duration='(기간오류)';}}break;} let discTxt=''; const base=calculateBasePrice(); let disc=calculateDiscount(base); if(selectedCoupon&&item&&base>0&&disc>0)discTxt=`<p class="mb-2 text-danger"><strong><i class="fas fa-tags fa-fw me-2"></i>쿠폰 할인:</strong> <span class="fw-medium ms-1">- ${disc.toLocaleString()}원 (${selectedCoupon.name})</span></p>`; if(reservationDetailsDiv) reservationDetailsDiv.innerHTML = `<p class="mb-2"><strong><i class="fas fa-tags fa-fw me-2 text-secondary"></i>요금제:</strong> <span class="text-primary fw-medium ms-1">${plan} ${duration}</span></p><p class="mb-2"><strong><i class="fas fa-calendar-day fa-fw me-2 text-secondary"></i>${selectedPlanType==='MONTHLY'?'시작일':'날짜'}:</strong> <span class="text-primary fw-medium ms-1">${date}</span></p><p class="mb-2"><strong><i class="fas fa-check-circle fa-fw me-2 text-secondary"></i>선택:</strong> <span class="text-primary fw-medium ms-1">${item?`${itemType} ${name}`:'-'}</span></p>${selectedPlanType==='HOURLY'&&item?(times.length>0?`<p class="mb-2"><strong><i class="fas fa-clock fa-fw me-2 text-secondary"></i>시간 (${times.length}시간):</strong> <span class="text-primary fw-medium ms-1">${times.join(', ')}</span></p>`:'<p class="text-muted small mb-2">시간 선택 필요</p>'):''}${discTxt}`; calculateTotal(); }
  /** 총 금액 계산 */
  function calculateTotal() { const item=document.querySelector('.seat-item.selected'); let price=0; let count=0; if(item){count=1; const base=calculateBasePrice(); const disc=calculateDiscount(base); price=base-disc; if(selectedPlanType==='HOURLY'&&document.querySelectorAll('input[name="times"]:checked').length===0)count=0;} if(totalCountSpan)totalCountSpan.textContent=count; if(totalPriceSpan)totalPriceSpan.textContent=price.toLocaleString(); }

  // --- 결제 관련 함수 ---
  /** 결제 진행 처리 (토스 연동 없음 - 이전 버전) */
/*  function proceedToPayment() {
      const totalCount = parseInt(totalCountSpan?.textContent || '0');
      if (totalCount === 0) { alert('예약할 항목과 요금제를 선택해주세요.'); return; }
      const selectedItem = document.querySelector('.seat-item.selected');
      const itemType = selectedItem?.dataset.seatType === 'ROOM' ? 'ROOM' : 'SEAT';
      const itemId = selectedItem?.dataset.seatId;
      const selectedTimeValues = Array.from(document.querySelectorAll('input[name="times"]:checked')).map(cb => cb.value);
      if (!itemId || !selectedDate) { alert('항목 또는 시작 날짜가 선택되지 않았습니다.'); return; }
      if (selectedPlanType === 'HOURLY' && selectedTimeValues.length === 0) { alert('시간제는 시간을 선택해야 합니다.'); return; }
      const reservationData = {
           itemId: itemId, itemType: itemType, planType: selectedPlanType,
           reservationDate: selectedDate, selectedTimes: selectedPlanType === 'HOURLY' ? selectedTimeValues : [],
           totalPrice: parseInt(totalPriceSpan?.textContent.replace(/,/g, '') || '0'),
           couponId: selectedCoupon?.id || null // 쿠폰 ID 포함
       };
      console.log("결제 진행 데이터 (서버 전송용):", reservationData);
      // --- !!! 실제 서버 전송 로직 구현 필요 !!! ---
      // fetch('/api/reservations', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reservationData) })
      // .then(response => response.json()) // 또는 response.ok 확인
      // .then(result => { alert('예약 요청 완료!'); *//* 성공 처리 *//* })
      // .catch(error => { alert('예약 요청 실패'); *//* 실패 처리 *//* });
      alert("결제 기능(서버 전송)은 아직 구현되지 않았습니다."); // 임시 알림
  }*/

 /** 결제 진행 처리 (백엔드 예약 API 호출 후 결제 생략) */
 function proceedToPayment() {
         // --- !!! 즉시 버튼 비활성화 및 재진입 방지 !!! ---
         if (paymentButton.disabled) {
             console.log("이미 예약 처리 중입니다.");
             return; // 이미 클릭되어 처리 중이면 함수 종료
         }
         paymentButton.disabled = true; // 클릭 즉시 비활성화
         paymentButton.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i> 예약 처리 중...';
         // ---------------------------------------------

         // --- 1. 예약 정보 유효성 검사 ---
         const totalCount = parseInt(totalCountSpan?.textContent || '0');
         if (totalCount === 0) {
             alert('예약할 항목과 요금제를 선택해주세요.');
             // 오류 시 버튼 다시 활성화
             paymentButton.disabled = false;
             paymentButton.innerHTML = '<i class="fas fa-credit-card me-2"></i> 결제하기';
             return;
         }
         const selectedItem = document.querySelector('.seat-item.selected');
         const itemType = selectedItem?.dataset.seatType === 'ROOM' ? 'ROOM' : 'SEAT';
         const itemId = selectedItem?.dataset.seatId;
         const selectedTimeValues = Array.from(document.querySelectorAll('input[name="times"]:checked')).map(cb => cb.value);

         if (!itemId || !selectedDate) {
             alert('항목 또는 시작 날짜가 선택되지 않았습니다.');
             paymentButton.disabled = false; // 오류 시 버튼 활성화
             paymentButton.innerHTML = '<i class="fas fa-credit-card me-2"></i> 결제하기';
             return;
         }
         if (selectedPlanType === 'HOURLY' && selectedTimeValues.length === 0) {
             alert('시간제는 시간을 선택해야 합니다.');
             paymentButton.disabled = false; // 오류 시 버튼 활성화
             paymentButton.innerHTML = '<i class="fas fa-credit-card me-2"></i> 결제하기';
             return;
         }
         const currentUserId = 123; // !!! 임시 사용자 ID, 실제로는 로그인 정보 연동 필요 !!!
         if (!currentUserId) {
             alert('사용자 정보를 가져올 수 없습니다.');
             paymentButton.disabled = false; // 오류 시 버튼 활성화
             paymentButton.innerHTML = '<i class="fas fa-credit-card me-2"></i> 결제하기';
             return;
         }
         // --- 유효성 검사 끝 ---

         // 2. 백엔드로 보낼 데이터 준비
         const reservationRequestData = {
              itemId: parseInt(itemId),
              itemType: itemType,
              planType: selectedPlanType,
              reservationDate: selectedDate,
              selectedTimes: selectedPlanType === 'HOURLY' ? selectedTimeValues : [],
              couponId: selectedCoupon?.id || null,
              userId: currentUserId
          };

         console.log("백엔드로 예약 생성 요청:", reservationRequestData);

         // 3. 백엔드 예약 생성 API 호출
         fetch('/api/reservations', { // !!! 백엔드 주소(포트 포함) 확인 !!!
             method: 'POST',
             headers: {
                 'Content-Type': 'application/json',
                 // CSRF 토큰 필요시 헤더 추가 (이전 답변 참고)
                 // 'X-CSRF-TOKEN': csrfToken
             },
             body: JSON.stringify(reservationRequestData)
         })
         .then(response => {
             // HTTP 상태 코드로 성공/실패 먼저 확인
             if (response.status === 409) { // Conflict (락 실패 등)
                  // 에러 메시지 추출을 위해 response.json() 시도
                  return response.json().then(err => { throw new Error(err.message || '다른 사용자가 예약 중입니다.'); });
             } else if (!response.ok) { // 4xx, 5xx 등 기타 에러
                  // 에러 메시지 추출 시도
                  return response.json().then(err => { throw new Error(err.message || `서버 오류 (${response.status})`); });
             }
             return response.json(); // 성공 (2xx) 시 JSON 파싱
         })
         .then(result => {
             // 백엔드 응답 성공 처리 (result.success 필드 확인)
             console.log('백엔드 예약 생성 성공 응답:', result);
             if (result.success) { // 백엔드가 {success: true, ...} 형태로 응답한다고 가정

                 // --- 예약 성공 후 처리 ---
                 alert('예약이 성공적으로 완료되었습니다!'); // 성공 알림

                 // 즉시 UI 업데이트 (좌석 상태 변경, 선택 초기화 등)
                 const reservedSeatElement = document.querySelector(`.seat-item[data-seat-id="${itemId}"]`);
                 if (reservedSeatElement) {
                     reservedSeatElement.classList.remove('selected', 'available');
                     reservedSeatElement.classList.add('unavailable');
                     reservedSeatElement.style.cursor = 'not-allowed';
                     reservedSeatElement.onclick = null; // 클릭 이벤트 제거
                 } else {
                     console.warn("예약된 좌석 요소를 화면에서 찾지 못했습니다:", itemId);
                     // 필요시 전체 좌석 목록 새로고침
                     // loadSeatStatus();
                 }
                 clearTimeSelection();
                 selectedCoupon = null;
                 if(couponSelect) couponSelect.value = "";
                 if(discountInfoDiv) discountInfoDiv.textContent = '';
                 updateSummary(); // 요약 정보 업데이트 (calculateTotal 호출됨)

                 // --- !!! 중요: 성공 후 버튼 상태 !!! ---
                 // 다음 예약을 위해 버튼 활성화 및 초기화
                 if(paymentButton) {
                     paymentButton.disabled = false;
                     paymentButton.innerHTML = '<i class="fas fa-credit-card me-2"></i> 결제하기';
                 }
                 // ------------------------------------

                 // location.reload(); // 페이지 새로고침은 제거된 상태

                 // --- 실제 결제 연동 시 여기서 토스 결제 시작 ---
                 // console.log("백엔드 예약 성공 -> 토스 결제 시작");
                 // requestTossPayment(result); // 백엔드 응답의 결제 정보 사용
                 // if(paymentWidgetSection) paymentWidgetSection.style.display = 'block';
                 // const paymentBar = document.querySelector('.payment-bar');
                 // if (paymentBar) paymentBar.style.display = 'none';
                 // -------------------------------------------

             } else {
                  // 백엔드가 {success: false, message: "..."} 형태로 응답한 경우
                  throw new Error(result.message || '알 수 없는 예약 오류');
             }
         })
         .catch(error => {
             // fetch 실패 또는 .then 내부에서 throw된 에러 처리
             console.error('예약 생성 또는 처리 중 오류:', error);
             alert(`예약 실패: ${error.message}`); // 실패 알림

             // --- !!! 중요: 에러 시 버튼 다시 활성화 !!! ---
             if(paymentButton) {
                  paymentButton.disabled = false;
                  paymentButton.innerHTML = '<i class="fas fa-credit-card me-2"></i> 결제하기';
              }
             // --------------------------------------
         });
     }
  // --- 초기화 및 이벤트 리스너 설정 ---
  /** 페이지 로드 시 실행 */
/*
  document.addEventListener('DOMContentLoaded', function() {
      console.log("DOM 로드 완료. 초기화 시작.");
      // 이벤트 리스너 등록
      calendarIcon?.addEventListener('click', openCalendar);
      planRadios.forEach(radio => { radio.addEventListener('change', handlePlanChange); });
      couponSelect?.addEventListener('change', handleCouponChange);
      paymentButton?.addEventListener('click', proceedToPayment); // 결제 버튼에 원래 함수 연결

      // 초기화 함수 호출
      const initialYear = new Date().getFullYear();
      const initialMonth = new Date().getMonth() + 1;
      loadCalendarDataAndInitialize(initialYear, initialMonth, new Date().toISOString().split('T')[0]); // 오늘 날짜 기본 선택
      loadAvailableCoupons(); // 쿠폰 목록 로드
      handlePlanChange(); // 초기 요금제 상태 반영
      updateSummary();
      console.log("초기화 로직 완료.");
  });*/
    document.addEventListener('DOMContentLoaded', function() {
        console.log("DOM 로드 완료.");
        // 이벤트 리스너 등록
        calendarIcon?.addEventListener('click', openCalendar);
        planRadios.forEach(radio => { radio.addEventListener('change', handlePlanChange); });
        couponSelect?.addEventListener('change', handleCouponChange);
        paymentButton?.addEventListener('click', proceedToPayment); // 결제 버튼에 수정된 함수 연결

        // 초기화 함수 호출
        const initialYear = new Date().getFullYear();
        const initialMonth = new Date().getMonth() + 1;
        loadCalendarDataAndInitialize(initialYear, initialMonth, new Date().toISOString().split('T')[0]);
        loadAvailableCoupons();
        handlePlanChange();
        updateSummary();
        console.log("초기화 완료.");
        // 토스 위젯 초기화 로직은 제거하거나 주석 처리합니다.
        // try { if (typeof PaymentWidget !== 'undefined') ... } catch ...
    });