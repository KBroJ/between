// static/js/admin/reservation/reservation-detail.js

document.addEventListener('DOMContentLoaded', function () {

    // --- 공통 변수 ---
    const overlay = document.getElementById('modalOverlay');
    const reservationUpdateForm = document.getElementById('reservationUpdateForm');
    let reservationIdToCancel = null; // 취소할 예약 ID 저장 변수

    // --- 모달 요소 가져오기 ---
    const confirmUpdateModal = document.getElementById('confirmUpdateModalContainer');
    const cancelReasonModal = document.getElementById('cancelReasonModalContainer');

    // --- 수정 관련 요소 ---
    const updateButton = document.getElementById('updateButton');                                                       // 수정 완료 버튼 (페이지 내)
    // confirmUpdateModal 내부 요소들
    const updateReasonInput = document.getElementById('updateReasonInput');                                             // 수정 사유 입력란
    const updateReasonValidationMessage = document.getElementById('updateReasonValidationMessage');                     // 유효성 메시지
    const submitUpdateButton = document.getElementById('submitUpdateButton');                                           // 수정 사유 모달 내 '수정 진행' 버튼
    const cancelUpdateBtn = confirmUpdateModal ? confirmUpdateModal.querySelector('.modal-cancel-button') : null;       // 수정 확인 모달 내 '취소' 버튼
    const closeUpdateModalBtn = confirmUpdateModal ? confirmUpdateModal.querySelector('.modal-close-button') : null;    // 수정 확인 모달 내 'X' 버튼

    // --- 취소 관련 요소 ---
    const cancelButton = document.getElementById('cancelButton'); // 예약 취소 버튼 (페이지 내)
    const submitCancellationButton = document.getElementById('submitCancellationButton'); // 취소 사유 모달 내 '취소 진행' 버튼
    const cancellationReasonInput = document.getElementById('cancellationReasonInput'); // 취소 사유 입력란
    const cancelReasonForm = document.getElementById('cancelReasonForm'); // 취소 사유 폼 (실제 submit 안 함)
    const cancelReasonValidationMessage = document.getElementById('cancelReasonValidationMessage'); // 유효성 메시지
    const cancelReasonModalCloseBtns = cancelReasonModal ? cancelReasonModal.querySelectorAll('.modal-cancel-button, .modal-close-button') : []; // 취소 모달 닫기 버튼들

    // --- 모달 열기 함수 ---
    function openModal(modalElement) {
        if (!modalElement || !overlay) return;
        overlay.classList.add('active');
        modalElement.classList.add('active');
    }

    // --- 모달 닫기 함수 ---
    function closeModal(modalElement) {

        if (!modalElement || !overlay) return;  // 모달 요소가 없으면 종료
        overlay.classList.remove('active');
        modalElement.classList.remove('active');

        // 수정 모달 닫을 때 초기화
        if (modalElement.id === 'confirmUpdateModalContainer' && updateReasonInput) {
            updateReasonInput.value = ''; // 입력 내용 비우기
            updateReasonInput.classList.remove('invalid');
            if (updateReasonValidationMessage) {
                updateReasonValidationMessage.classList.remove('show');
            }
        }

        // 취소 모달 닫을 때 초기화
        if (modalElement.id === 'cancelReasonModalContainer' && cancellationReasonInput) {
            cancellationReasonInput.value = ''; // 입력 내용 비우기
            cancellationReasonInput.classList.remove('invalid');
            if (cancelReasonValidationMessage) {
                cancelReasonValidationMessage.classList.remove('show');
            }
        }
    }

// --- 이벤트 리스너 설정 ---

    // 페이지 내 '수정' 버튼 -> 수정 사유 입력 모달 열기
    if (updateButton && confirmUpdateModal) {
        updateButton.addEventListener('click', function() {
            // 모달 열기 전, 혹시 모를 이전 입력 값 및 유효성 상태 초기화
            if (updateReasonInput) updateReasonInput.value = '';
            if (updateReasonInput) updateReasonInput.classList.remove('invalid');
            if (updateReasonValidationMessage) updateReasonValidationMessage.classList.remove('show');
            openModal(confirmUpdateModal);
        });
    }

    // 수정 사유 모달 내 '수정 진행' 버튼 -> 유효성 검사 및 메인 폼에 사유 추가 후 제출
    if (submitUpdateButton && reservationUpdateForm && updateReasonInput && updateReasonValidationMessage) {
        submitUpdateButton.addEventListener('click', function() {
            const reason = updateReasonInput.value.trim();

            // 수정 사유 유효성 검사
            if (!reason) { // required 속성이 있다면 필수 입력
                updateReasonInput.classList.add('invalid');
                updateReasonValidationMessage.classList.add('show');
                return; // 중단
            } else {
                updateReasonInput.classList.remove('invalid');
                updateReasonValidationMessage.classList.remove('show');
            }

            // TODO 예약 정보(날짜, 좌석 등) 변경 사항 유효성 검사 로직 추가 고민중..

            // 수정 사유를 메인 폼(reservationUpdateForm)에 hidden 필드로 추가
            // 서버에서 "updateReason"이라는 이름으로 받을 수 있도록 합니다.
            let hiddenReasonField = reservationUpdateForm.querySelector('input[name="updateReason"]');
            if (!hiddenReasonField) { // 없으면 새로 생성
                hiddenReasonField = document.createElement('input');
                hiddenReasonField.type = 'hidden';
                hiddenReasonField.name = 'updateReason'; // Controller에서 받을 파라미터 명
                reservationUpdateForm.appendChild(hiddenReasonField);
            }
            hiddenReasonField.value = reason; // 수정 사유 값 할당

            // 메인 폼 제출 (실제 예약 정보 + 수정 사유)
            reservationUpdateForm.submit();
        });
    }

    // 수정 사유 모달 내 '취소' 또는 'X' 버튼 -> 모달 닫기
    if(cancelUpdateBtn) cancelUpdateBtn.addEventListener('click', () => closeModal(confirmUpdateModal));
    if(closeUpdateModalBtn) closeUpdateModalBtn.addEventListener('click', () => closeModal(confirmUpdateModal));


    // 예약 취소 버튼 (페이지) -> 취소 사유 모달 열기 및 ID 저장
    if (cancelButton && cancelReasonModal) {
        cancelButton.addEventListener('click', function() {
            reservationIdToCancel = this.dataset.resno; // 예약 ID 저장

            if (cancellationReasonInput) cancellationReasonInput.value = '';
            if (cancellationReasonInput) cancellationReasonInput.classList.remove('invalid');
            if (cancelReasonValidationMessage) cancelReasonValidationMessage.classList.remove('show');
            openModal(cancelReasonModal);
        });
    }

    // 취소 사유 모달 내 '취소 진행' 버튼 -> 유효성 검사 및 API 호출
    if (submitCancellationButton && cancellationReasonInput && cancelReasonValidationMessage) {

        submitCancellationButton.addEventListener('click', function() {
            const reason = cancellationReasonInput.value.trim();

            // 유효성 검사
            if (!reason) {
                cancellationReasonInput.classList.add('invalid');
                cancelReasonValidationMessage.classList.add('show');
                return; // 중단
            } else {
                cancellationReasonInput.classList.remove('invalid');
                cancelReasonValidationMessage.classList.remove('show');
            }

            if (!reservationIdToCancel) {
                alert('오류: 취소할 예약 ID를 찾을 수 없습니다.');
                closeModal(cancelReasonModal); // 오류 시 모달 닫기
                return;
            }

            console.log("예약 번호:", reservationIdToCancel, "취소 사유:", reason);
            alert(`[임시] 예약 번호 ${reservationIdToCancel} 취소 API 호출\n사유: ${reason}\n(실제 API 연동 필요)`);

            closeModal(cancelReasonModal); // 임시로 바로 닫기 (실제로는 API 응답 후 처리)

            // --- 실제 취소 API 호출 로직 (Fetch API 예시) ---
            /*
            fetch(`/api/admin/reservations/${reservationIdToCancel}/cancel`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ cancellationReason: reason })
            })
            .then(response => {
                 if (!response.ok) {
                    return response.json().then(err => { throw new Error(err.message || `예약 취소 실패 (${response.status})`); });
                 }
                 return { success: true }; // 임시
            })
            .then(data => {
                alert('예약이 성공적으로 취소되었습니다.');
                closeModal(cancelReasonModal);
                window.location.reload();
            })
            .catch(error => {
                console.error('Error cancelling reservation:', error);
                alert('예약 취소 중 오류 발생: ' + error.message);
                // 실패 시 모달을 닫거나 유지할 수 있음
                // closeModal(cancelReasonModal);
            });
            */
        });
    }

    // 취소 사유 모달 내 '닫기' 또는 'X' 버튼 -> 모달 닫기
    cancelReasonModalCloseBtns.forEach(btn => {
        btn.addEventListener('click', () => closeModal(cancelReasonModal));
    });

    // 오버레이 클릭 시 모달 닫기 (선택 사항)
    if (overlay) {
        overlay.addEventListener('click', function(event) {
            // 클릭된 요소가 오버레이 자체일 때만 닫기 (모달 내부 클릭은 무시)
            if (event.target === overlay) {
                closeModal(confirmUpdateModal); // 열려있는 모든 모달을 닫거나, 특정 모달만 닫도록 로직 추가 가능
                closeModal(cancelReasonModal);
            }
        });
    }

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
});