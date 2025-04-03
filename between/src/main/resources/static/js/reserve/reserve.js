 // --- 전역 변수 ---
  let flatpickrInstance = null;
  let selectedDate = null; // YYYY-MM-DD
  let currentYear = new Date().getFullYear();
  let currentMonth = new Date().getMonth() + 1; // 1~12
  let selectedPlanType = 'HOURLY'; // HOURLY, DAILY, MONTHLY

  // --- DOM 요소 ---
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
  const paymentButton = document.getElementById('payment-button');

  // --- 유틸리티 함수 ---
  /** 서버 API 호출 함수 */
  async function fetchData(url, options = {}) {
      try {
          const response = await fetch(url, options);
          if (!response.ok) {
              console.error(`API Error: ${response.status} for ${url}`);
              let errorData = { message: `HTTP error! status: ${response.status}` };
              try { errorData = await response.json(); } catch (e) {}
              throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
          }
          return await response.json();
      } catch (error) {
          console.error(`Workspace error for ${url}:`, error);
          throw error;
      }
  }

  // --- Flatpickr (달력) 관련 함수 ---
  /** Flatpickr 초기화 */
  function initializeCalendar(enabledDates = [], initialDate = null) {
      if (flatpickrInstance) flatpickrInstance.destroy();
      flatpickrInstance = flatpickr(calendarInput, {
          locale: "ko", dateFormat: "Y-m-d", minDate: "today",
          enable: enabledDates, defaultDate: initialDate,
          onChange: handleDateChange, onMonthChange: handleMonthYearChange,
          onYearChange: handleMonthYearChange, onClose: handleCalendarClose
      });
      updateCalendarInput(initialDate);
  }

  /** Flatpickr input 값 업데이트 및 전역 변수 설정 */
  function updateCalendarInput(dateStr) {
      if (dateStr && /^\d{4}-\d{2}-\d{2}$/.test(dateStr)) {
           calendarInput.value = dateStr; selectedDate = dateStr;
      } else {
           calendarInput.value = ''; selectedDate = null;
      }
  }

  /** Flatpickr 날짜 선택 처리 */
  function handleDateChange(selectedDates, dateStr, instance) {
      console.log("날짜 선택됨:", dateStr);
      updateCalendarInput(dateStr);
      loadSeatStatus();
      updateSummary();
  }

  /** Flatpickr 월/연도 변경 처리 */
  function handleMonthYearChange(selectedDates, dateStr, instance) {
      const newYear = instance.currentYear;
      const newMonth = instance.currentMonth + 1;
      if (currentYear !== newYear || currentMonth !== newMonth) {
          console.log(`달력 월/연도 변경: ${newYear}-${newMonth}`);
          currentYear = newYear; currentMonth = newMonth;
          loadCalendarDataAndInitialize(newYear, newMonth);
      }
  }

  /** Flatpickr 닫힐 때 처리 */
  function handleCalendarClose(selectedDates, dateStr, instance) {
      if (!calendarInput.value) { updateCalendarInput(null); loadSeatStatus(); updateSummary(); }
  }

  /** 서버에서 달력 데이터 로드 및 초기화 */
  function loadCalendarDataAndInitialize(year, month, initialDateToSelect = null) {
      console.log(`달력 데이터 로드 요청: ${year}-${month}`);
      calendarInput.placeholder = "달력 로딩 중...";
      const apiUrl = `/api/calendar?year=${year}&month=${month}`; // 백엔드 포트 포함 필요시 수정

      fetchData(apiUrl)
      .then(calendarData => {
          console.log("받은 calendarData:", calendarData);
          let enabledDates = [];
          try {
               if (calendarData && calendarData.weeks && Array.isArray(calendarData.weeks)) {
                  enabledDates = calendarData.weeks.flatMap(week => week.days || [])
                      .filter(day => day && day.selectable === true)
                      .map(day => day.dateString);
               } else { console.error("calendarData 구조 오류"); }
          } catch (error) { console.error("enabledDates 생성 오류:", error); }
          console.log("필터링 결과 enabledDates:", enabledDates);

          let effectiveInitialDate = null;
          if (initialDateToSelect && enabledDates.includes(initialDateToSelect)) { effectiveInitialDate = initialDateToSelect; }
          else if (selectedDate && enabledDates.includes(selectedDate)) { effectiveInitialDate = selectedDate; }

          initializeCalendar(enabledDates, effectiveInitialDate);
          calendarInput.placeholder = "날짜를 선택하세요";

          if (effectiveInitialDate) { loadSeatStatus(); updateSummary(); }
          else { updateCalendarInput(null); /* ... 초기화 ... */ }
      })
      .catch(error => {
           console.error('달력 API 호출/처리 오류:', error);
           calendarInput.placeholder = "달력 로드 실패"; initializeCalendar([], null);
      });
  }

  /** 달력 아이콘 클릭 시 Flatpickr 열기 */
  function openCalendar() { flatpickrInstance?.open(); }

  // --- 요금제 관련 함수 ---
  /** 요금제 변경 처리 (일일권 로직 추가) */
  function handlePlanChange() {
      const selectedRadio = document.querySelector('input[name="planType"]:checked');
      if (!selectedRadio) return;
      selectedPlanType = selectedRadio.value;
      console.log("요금제 변경:", selectedPlanType);

      if (selectedPlanType === 'HOURLY') {
          timeSelectionCard.classList.remove('disabled');
          timeSelectionCard.style.display = 'block';
          timeSelectionGuide.textContent = '좌석/룸과 날짜를 선택하면 예약 가능한 시간이 표시됩니다.';
          loadAvailableTimes(); // 시간 로드 시도
      } else { // DAILY 또는 MONTHLY
          timeSelectionCard.classList.add('disabled'); // 시간 선택 비활성화
          if (selectedPlanType === 'DAILY') { timeSelectionGuide.textContent = '일일권은 당일 운영시간 동안 이용 가능합니다.'; }
          else { timeSelectionGuide.textContent = '월정액권은 별도의 시간 선택이 필요 없습니다.'; }
          clearTimeSelection(); // 시간 선택 초기화
      }
      updateSummary(); calculateTotal(); // 요약 및 금액 업데이트
  }

  // --- 좌석/시간 관련 함수 ---
  /** 좌석 상태 로드 */
  function loadSeatStatus() {
      const date = selectedDate;
      clearTimeSelection();
      seatMapView.innerHTML = '<p class="text-muted small p-3"><i class="fas fa-spinner fa-spin me-1"></i> 좌석 로딩 중...</p>';
      if (!date) { seatMapView.innerHTML = '<p class="text-muted small p-3">날짜를 선택해주세요.</p>'; return; }
      console.log(`좌석 로드: 날짜 ${date}`);
      const apiUrl = `/api/seats?date=${date}`; // 백엔드 포트 포함 필요시 수정

      fetchData(apiUrl)
          .then(seatsData => renderSeats(seatsData))
          .catch(error => { seatMapView.innerHTML = '<p class="text-danger small p-3">좌석 정보 로드 실패</p>'; });
  }

      /** 좌석 정보 렌더링 (DB 위치 정보 사용) */
    function renderSeats(seats = []) {
        const seatMapView = document.getElementById('seat-map-view');
        seatMapView.innerHTML = ''; // 초기화

        // Grid 컨테이너 스타일 설정 (CSS에서 설정했다면 여기선 생략 가능)
        seatMapView.style.display = 'grid';
        seatMapView.style.gridTemplateColumns = 'repeat(3, 1fr)'; // CSS와 일치시키기
        seatMapView.style.gridAutoRows = 'minmax(80px, auto)';   // CSS와 일치시키기
        seatMapView.style.gap = '15px';                          // CSS와 일치시키기
        seatMapView.style.maxWidth = '400px';                    // CSS와 일치시키기
        seatMapView.style.marginLeft = 'auto';                   // CSS와 일치시키기
        seatMapView.style.marginRight = 'auto';

        if (!seats || seats.length === 0) {
            seatMapView.innerHTML = '<p class="text-muted small p-3">등록된 좌석 정보가 없습니다.</p>';
            return;
        }
        console.log("좌석 렌더링 시작 (DB 위치 정보 사용)");

        seats.forEach(item => {
            const div = document.createElement('div');
            div.textContent = item.name;
            const typeClass = item.type?.toLowerCase() || 'seat';
            const statusClass = item.status ? item.status.toLowerCase() : 'unavailable';
            div.className = `${typeClass} ${statusClass} seat-item`;
            div.dataset.seatId = item.id; div.dataset.seatName = item.name; div.dataset.seatType = item.type;

            // --- !!! DB에서 가져온 위치 정보 적용 !!! ---
            if (item.gridRow) div.style.gridRow = item.gridRow;       // 예: '1' 또는 '1 / 2'
            if (item.gridColumn) div.style.gridColumn = item.gridColumn; // 예: '1' 또는 '1 / 3'
            // ---------------------------------------------

            if (statusClass === 'available' && typeClass !== 'area') {
                div.style.cursor = 'pointer';
                div.addEventListener('click', () => selectSeat(div));
            } else {
                div.style.cursor = 'not-allowed';
                div.style.opacity = '0.6';
                if(statusClass === 'static') div.style.opacity = '0.8';
            }
            seatMapView.appendChild(div);
        });
    }

  /** 좌석 선택 처리 (단일 선택) */
  function selectSeat(seatElement) {
      if (seatElement.classList.contains('unavailable') || seatElement.classList.contains('static')) return;
      const currentSelected = document.querySelector('.seat-item.selected');
      if (currentSelected === seatElement) {
           seatElement.classList.remove('selected'); // 선택 해제
           clearTimeSelection(); updateSummary(); calculateTotal(); return;
      }
      if (currentSelected) { currentSelected.classList.remove('selected'); } // 이전 선택 해제
      seatElement.classList.add('selected'); // 새로 선택
      if (selectedPlanType === 'HOURLY') { loadAvailableTimes(); } else { clearTimeSelection(); }
      updateSummary(); // calculateTotal 호출됨
  }

  /** 시간 선택 영역 초기화 */
  function clearTimeSelection() {
      timeSlotsDiv.innerHTML = '';
      document.querySelectorAll('input[name="times"]:checked').forEach(cb => cb.checked = false);
      timeSelectionGuide.textContent = '시간제 선택 시, 좌석/룸과 날짜를 선택하면 예약 가능한 시간이 표시됩니다.';
  }

  /** 예약 가능 시간 로드 (시간제일 때만) */
  function loadAvailableTimes() {
      timeSlotsDiv.innerHTML = ''; timeSelectionGuide.textContent = '좌석/룸과 날짜를 선택하면 예약 가능한 시간이 표시됩니다.';
      const selectedSeat = document.querySelector('.seat-item.selected');
      const date = selectedDate;
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
      const apiUrl = `/api/times?date=${date}&seatId=${seatId}`; // 백엔드 포트 포함 필요시 수정

      fetchData(apiUrl)
          .then(times => renderTimeSlots(times))
          .catch(error => timeSlotsDiv.innerHTML = `<p class="text-danger small">시간 정보 로드 실패</p>`)
          .finally(() => updateSummary());
  }

  /** 시간 슬롯 렌더링 */
  function renderTimeSlots(times = []) {
      timeSlotsDiv.innerHTML = '';
      if (!times || times.length === 0) { timeSlotsDiv.innerHTML = '<p class="text-warning small">예약 가능한 시간이 없습니다.</p>'; return; }
      times.forEach(timeInfo => {
          const timeValue = typeof timeInfo === 'object' ? timeInfo.startTime : timeInfo;
          const timeDisplay = typeof timeInfo === 'object' ? `${timeInfo.startTime} - ${timeInfo.endTime}` : `${timeValue} - ${String(parseInt(timeValue.split(':')[0]) + 1).padStart(2,'0')}:00`;
          const label = document.createElement('label'); label.classList.add('me-2', 'mb-2');
          const input = document.createElement('input'); input.type = 'checkbox'; input.name = 'times'; input.value = timeValue; input.id = `time-${timeValue.replace(':', '')}`; input.classList.add('d-none');
          const span = document.createElement('span'); span.textContent = timeDisplay; span.classList.add('btn', 'btn-outline-primary', 'btn-sm');
          input.addEventListener('change', (e) => {
               span.classList.toggle('btn-primary', input.checked);
               span.classList.toggle('btn-outline-primary', !input.checked);
               updateSummary(); // 요약 업데이트
          });
          label.appendChild(input); label.appendChild(span); timeSlotsDiv.appendChild(label);
      });
  }

  // --- 요약 및 결제 관련 함수 ---
  /** 예약 요약 정보 업데이트 (일일권 로직 포함) */
  function updateSummary() {
      const selectedItemElement = document.querySelector('.seat-item.selected');
      const dateStr = selectedDate || '-';
      const selectedTimeElements = document.querySelectorAll('input[name="times"]:checked');
      const selectedTimes = Array.from(selectedTimeElements).map(cb => cb.nextElementSibling.textContent.trim());
      let planName = ''; let durationInfo = ''; let itemType = '좌석'; let itemName = '-';
      if (selectedItemElement) { itemName = selectedItemElement.dataset.seatName || '-'; itemType = selectedItemElement.dataset.seatType === 'ROOM' ? '룸' : '좌석'; }

      switch (selectedPlanType) {
          case 'HOURLY': planName = '시간제'; break;
          case 'DAILY': planName = '일일권'; durationInfo = `(${dateStr})`; break;
          case 'MONTHLY':
              planName = '월정액권';
              if (selectedDate) { try { const sd = new Date(selectedDate); const ed = new Date(sd); ed.setMonth(sd.getMonth()+1); ed.setDate(sd.getDate()); durationInfo = `(${dateStr} ~ ${ed.toISOString().split('T')[0]})`; } catch(e){ durationInfo = '(기간오류)';} }
              break;
      }
      reservationDetailsDiv.innerHTML = `
          <p class="mb-2"><strong><i class="fas fa-tags fa-fw me-2 text-secondary"></i>요금제:</strong> <span class="text-primary fw-medium ms-1">${planName} ${durationInfo}</span></p>
          <p class="mb-2"><strong><i class="fas fa-calendar-day fa-fw me-2 text-secondary"></i>${selectedPlanType === 'MONTHLY' ? '시작일' : '날짜'}:</strong> <span class="text-primary fw-medium ms-1">${dateStr}</span></p>
          <p class="mb-2"><strong><i class="fas fa-check-circle fa-fw me-2 text-secondary"></i>선택:</strong> <span class="text-primary fw-medium ms-1">${selectedItemElement ? `${itemType} ${itemName}` : '-'}</span></p>
          ${selectedPlanType === 'HOURLY' && selectedItemElement ? (selectedTimes.length > 0 ? `<p class="mb-2"><strong><i class="fas fa-clock fa-fw me-2 text-secondary"></i>시간 (${selectedTimes.length}시간):</strong> <span class="text-primary fw-medium ms-1">${selectedTimes.join(', ')}</span></p>` : '<p class="text-muted small mb-2">예약할 시간을 선택해주세요.</p>') : ''}
      `;
      calculateTotal();
  }

  /** 총 금액 계산 (일일권 로직 포함) */
  function calculateTotal() {
      const selectedTimesCount = document.querySelectorAll('input[name="times"]:checked').length;
      const selectedItem = document.querySelector('.seat-item.selected');
      let totalPrice = 0; let totalCount = 0;
      if (selectedItem) {
          totalCount = 1;
          switch (selectedPlanType) {
              case 'HOURLY': totalPrice = selectedTimesCount * 2000; if (selectedTimesCount === 0) totalCount = 0; break; // !!! 시간당 가격 !!!
              case 'DAILY': totalPrice = 10000; break; // !!! 일일권 가격 !!!
              case 'MONTHLY': totalPrice = 99000; break; // !!! 월정액권 가격 !!!
          }
      }
      if(totalCountSpan) totalCountSpan.textContent = totalCount;
      if(totalPriceSpan) totalPriceSpan.textContent = totalPrice.toLocaleString();
  }

  /** 결제 진행 처리 */
  function proceedToPayment() {
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
           totalPrice: parseInt(totalPriceSpan?.textContent.replace(/,/g, '') || '0')
       };
      console.log("결제 진행 데이터:", reservationData);
      // --- !!! 실제 서버 전송 로직 구현 필요 !!! ---
      // fetch('/api/reservations', { method: 'POST', ... body: JSON.stringify(reservationData) }) ...
      alert("결제 기능은 아직 구현되지 않았습니다.");
  }

  // --- 초기화 및 이벤트 리스너 설정 ---
  /** 페이지 로드 시 실행 */
  document.addEventListener('DOMContentLoaded', function() {
      console.log("DOM 로드 완료. 초기화 시작.");
      // 이벤트 리스너 등록
      calendarIcon?.addEventListener('click', openCalendar);
      planRadios.forEach(radio => {
          radio.addEventListener('change', handlePlanChange);
      });
      paymentButton?.addEventListener('click', proceedToPayment);

      const initialYear = new Date().getFullYear();
      const initialMonth = new Date().getMonth() + 1;
      const todayStr = new Date().toISOString().split('T')[0];
      console.log('loadCalendarDataAndInitialize 호출 전:', initialYear, initialMonth);
      loadCalendarDataAndInitialize(initialYear, initialMonth, todayStr); // 오늘 날짜 기본 선택
      handlePlanChange(); // 초기 요금제(HOURLY) 상태 반영
      updateSummary(); // 초기 요약 업데이트
  });