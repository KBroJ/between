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

    // --- 수정 관련 요소 ---
    const updateButton = document.getElementById('updateButton');                                                       // 수정 완료 버튼 (페이지 내)
    // confirmUpdateModal 내부 요소들
    const updateReasonInput = document.getElementById('updateReasonInput');                                             // 수정 사유 입력란
    const updateReasonValidationMessage = document.getElementById('updateReasonValidationMessage');                     // 유효성 메시지

    // --- 취소 관련 요소 ---
    const cancelButton = document.getElementById('cancelButton'); // 예약 취소 버튼 (페이지 내)
    const cancellationReasonInput = document.getElementById('cancellationReasonInput'); // 취소 사유 입력란
    const cancelReasonValidationMessage = document.getElementById('cancelReasonValidationMessage'); // 유효성 메시지

    // --- 공통 모달 요소 가져오기 ---
    const reasonModal = document.getElementById('reasonModalContainer');
    const reasonModalTitle = document.getElementById('reasonModalTitle');
    const reasonModalLabel = document.getElementById('reasonModalLabel');
    const reasonInput = document.getElementById('reasonInput'); // 공통 사유 입력 필드
    const reasonValidationMessage = document.getElementById('reasonValidationMessage');
    const submitReasonButton = document.getElementById('submitReasonButton');

    // 페이지 내 '수정' 버튼 -> 공통 모달 열기 (수정용)
    if (updateButton) {
        updateButton.addEventListener('click', function() {
            openReasonModal('update');
        });
    }

    // 페이지 내 '예약 취소' 버튼 -> 공통 모달 열기 (취소용)
    if (cancelButton) {
        cancelButton.addEventListener('click', function() {
            reservationIdToCancel = this.dataset.resno; // 이 값은 어딘가에 저장해두거나, openReasonModal에 전달
            openReasonModal('cancel');
        });
    }

    // 모달 열기 함수를 확장하여 모달 타입에 따라 내용 설정
    function openReasonModal(type) { // type: 'update' 또는 'cancel'
        if (!reasonModal || !overlay) return;

        reasonInput.value = '';
        reasonInput.classList.remove('invalid');
        reasonValidationMessage.classList.remove('show');

        if (type === 'update') {
            reasonModalTitle.textContent = '예약 수정';
            reasonModalLabel.innerHTML = '수정 사유 <span class="required-mark">*</span>'; // HTML 포함 시 innerHTML
            reasonInput.placeholder = '수정 사유를 입력해주세요. (예: 시간 변경)';
            submitReasonButton.textContent = '수정 진행';
            submitReasonButton.className = 'btn btn-primary'; // 클래스 변경
            reasonModal.dataset.actionType = 'update'; // 현재 액션 타입 저장
        } else if (type === 'cancel') {
            reasonModalTitle.textContent = '예약 취소';
            reasonModalLabel.innerHTML = '취소 사유 <span class="required-mark">*</span>';
            reasonInput.placeholder = '취소 사유를 반드시 입력해주세요.';
            submitReasonButton.textContent = '취소 진행';
            submitReasonButton.className = 'btn btn-danger';
            reasonModal.dataset.actionType = 'cancel';
            // 취소 시 필요한 예약 번호(resNo)는 별도로 저장/관리 필요 (예: reasonModal.dataset.resNo = this.dataset.resno)
            // 또는 openReasonModal 호출 시 resNo도 전달
        }

        overlay.classList.add('active');
        reasonModal.classList.add('active');
    }

    // 공통 모달 내 '진행' 버튼 클릭 시
    if (submitReasonButton) {
        submitReasonButton.addEventListener('click', function() {
            const reason = reasonInput.value.trim();
            const actionType = reasonModal.dataset.actionType;

            if (!reason) {
                reasonInput.classList.add('invalid');
                reasonValidationMessage.classList.add('show');
                return;
            } else {
                reasonInput.classList.remove('invalid');
                reasonValidationMessage.classList.remove('show');
            }

            if (actionType === 'update') {
                // 수정 로직: 메인 폼에 사유 추가 후 제출
                let hiddenReasonField = reservationUpdateForm.querySelector('input[name="updateReason"]');
                if (!hiddenReasonField) {
                    hiddenReasonField = document.createElement('input');
                    hiddenReasonField.type = 'hidden';
                    hiddenReasonField.name = 'updateReason';
                    reservationUpdateForm.appendChild(hiddenReasonField);
                }
                hiddenReasonField.value = reason;
                reservationUpdateForm.submit();
                closeModal(reasonModal); // closeModal 함수는 reasonModal에 맞게 동작해야 함
            } else if (actionType === 'cancel') {
                // 취소 로직: API 호출
                if (!reservationIdToCancel) { // reservationIdToCancel는 이전처럼 관리
                    alert('오류: 취소할 예약 ID를 찾을 수 없습니다.');
                    closeModal(reasonModal);
                    return;
                }
                console.log("예약 번호:", reservationIdToCancel, "취소 사유:", reason);
                alert(`[임시] 공통 모달: 예약 번호 ${reservationIdToCancel} 취소 API 호출\n사유: ${reason}`);
                // fetch(...).then(...).catch(...);
                closeModal(reasonModal);
            }
        });
    }

    // 공통 모달 닫기 버튼들 (modal-cancel-button, modal-close-button)에 대한 이벤트 리스너도 설정 필요
    const reasonModalCloseBtns = reasonModal ? reasonModal.querySelectorAll('.modal-cancel-button, .modal-close-button') : [];
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

    // 모달 닫기 함수
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

//=====================================================================================================================

});