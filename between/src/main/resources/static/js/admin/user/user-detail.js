document.addEventListener('DOMContentLoaded', function() {

    const goToListBtn = document.getElementById('goToListBtn');
    const deleteUserBtn = document.getElementById('deleteUserBtn');
    const updateUserBtn = document.getElementById('updateUserBtn');

    // --- CSRF 토큰 ---
    const csrfToken = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
    const csrfHeaderName = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");
    console.log("CSRF Header Name from meta:", csrfHeaderName); // 디버깅용
    console.log("CSRF Token from meta:", csrfToken);       // 디버깅용

    // --- 사용자 ID 가져오기 ---
    const detailCard = document.querySelector('.detail-card');
    const userNo = detailCard ? detailCard.dataset.userno : null;

    // --- 공통 모달 요소 가져오기 ---
    const overlay = document.getElementById('modalOverlay'); // HTML에 overlay가 있는지 확인
    const reasonModal = document.getElementById('reasonModalContainer');
    const reasonModalTitle = document.getElementById('reasonModalTitle');
    const reasonModalLabel = document.getElementById('reasonModalLabel');
    const reasonInput = document.getElementById('reasonInput');
    const reasonValidationMessage = document.getElementById('reasonValidationMessage');
    const submitReasonButton = document.getElementById('submitReasonButton');
    //
    const reasonModalCloseBtns = reasonModal ?
                                    reasonModal.querySelectorAll('.modal-cancel-button, .modal-close-button') :
                                    [];

// ====================================================================================================================

    // 최근 예약 목록 행 클릭 시 예약 상세 페이지로 이동
    const recentReservationRows = document.querySelectorAll('.reservation-table tbody tr.clickable-row');

    recentReservationRows.forEach(row => {
        row.addEventListener('click', function() {
            const resNo = this.dataset.resno; // tr 태그의 data-resno 속성 값 가져오기
            if (resNo) {
                // 관리자용 예약 상세 페이지 URL로 이동
                // AdminReservationController의 reservationDetailPage 메소드 경로와 일치해야 함
                // 해당 컨트롤러가 @RequestMapping("/admin")이고 메소드가 @GetMapping("/reservationList/{resNo}")라면:
                window.location.href = `/admin/reservationList/${resNo}`;
            } else {
                console.warn("클릭된 행에서 예약 번호(resNo)를 찾을 수 없습니다.");
            }
        });
    });


// ====================================================================================================================

    // --- 비정상적으로 페이지가 로드 되었을 경우(회원고유번호,수정,탈퇴 버튼 유무 체크) ---
    if (!userNo && (updateUserBtn || deleteUserBtn)) {
        console.error("사용자 ID(userNo)를 HTML data-userno 속성에서 가져올 수 없습니다.");
        if(updateUserBtn) updateUserBtn.disabled = true;
        if(deleteUserBtn) deleteUserBtn.disabled = true;
    }

    // 공통 모달 열기
    function openModal(modalElement) {
        if (!modalElement || !overlay) return;
        overlay.classList.add('active');
        modalElement.classList.add('active');
        if (reasonInput) reasonInput.focus(); // 모달 열릴 때 사유 입력란에 포커스
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

    // 공통 모달 닫기 버튼 클릭 시 공통 모달 닫기 실행
    reasonModalCloseBtns.forEach(btn => {
        btn.addEventListener('click', () => closeModal(reasonModal));
    });

    // 오버레이 영역 클릭 시 모달 닫기
    if (overlay) {
        overlay.addEventListener('click', function(event) {
            // 클릭된 요소가 오버레이 자체일 때만 닫기 (모달 내부 클릭은 무시)
            if (event.target === overlay) {
                closeModal(reasonModal);
            }
        });
    }

// =====================================================================================================================

    // '목록으로' 버튼 클릭 이벤트
    if (goToListBtn) {
        goToListBtn.addEventListener('click', function() {
            // 회원 목록 페이지로 이동
            window.location.href = '/admin/user/userList'; // 목록 페이지 경로 예시
        });
    }

    // '탈퇴 처리' 버튼 클릭 이벤트
    if (deleteUserBtn && userNo) {  // 탈퇴 버튼, userNo가 존재할 때만 이벤트 리스너 추가
        deleteUserBtn.addEventListener('click', async function() {
            if (confirm('정말로 이 회원을 탈퇴 처리하시겠습니까?\n이 작업은 되돌릴 수 없을 수 있습니다.')) {
                console.log('탈퇴 처리할 사용자 ID:', userNo); // 디버깅용 로그

                this.disabled = true;
                const originalButtonText = this.textContent;
                this.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> 처리 중...';

                const headers = {};
                if (csrfToken && csrfHeaderName) {
                    headers[csrfHeaderName] = csrfToken;
                }

                try {
                    const response = await fetch(`/admin/user/${userNo}/withdraw`, { // API 경로 확인!
                        method: 'DELETE', // 또는 'POST'를 사용하고 서버에서 상태 변경으로 처리
                        headers: headers
                    });

                    if (response.ok) {
                        if (response.status === 204) {
                            alert('회원이 성공적으로 탈퇴 처리되었습니다.');
                            window.location.href = '/admin/user/userList'; // 목록 페이지 경로 예시
                        } else {
                            const data = await response.json().catch(() => ({})); // JSON 파싱 실패 대비
                            alert(data.message || '회원이 성공적으로 탈퇴 처리되었습니다.');
                            window.location.href = '/admin/user/userList';
                        }
                    } else {
                        const errData = await response.json().catch(() => ({ message: '회원 탈퇴 처리에 실패했습니다.' }));
                        throw new Error(errData.message || `회원 탈퇴 처리에 실패했습니다. (상태: ${response.status})`);
                    }
                } catch (error) {
                    console.error('Error deleting user:', error);
                    alert(error.message || '오류가 발생하여 회원을 탈퇴 처리하지 못했습니다.');
                } finally {
                    this.disabled = false;
                    this.innerHTML = originalButtonText;
                }

            }
        });
    }

    // 페이지 내 '수정' 버튼 -> 공통 모달 열기 (수정용)
    if (updateUserBtn && reasonModal) {
        updateUserBtn.addEventListener('click', function() {
            reasonModalTitle.textContent = '회원 정보 수정';
            reasonModalLabel.innerHTML = '수정 사유 <span class="required-mark">*</span>';
            reasonInput.placeholder = '수정 사유를 입력해주세요. (예: 임직원 권한 등록 등)';
            reasonInput.setAttribute('required', 'required'); // 필수 입력으로 설정
            submitReasonButton.textContent = '수정 진행';
            submitReasonButton.className = 'btn btn-primary';
            reasonModal.dataset.actionType = 'update';
            openModal(reasonModal);
        });
    }

    // 공통 모달 내 '수정 진행' 버튼 클릭 시
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

            const userGradeSelect = document.getElementById('userGrade');
            const userStatusSelect = document.getElementById('userStatus');
            const newGrade = userGradeSelect ? userGradeSelect.value : null;
            const newStatus = userStatusSelect ? userStatusSelect.value : null;

            const updateData = {
                authCd: newGrade,
                userStts: newStatus,
                updateRs: reason
            };

            const headers = {}; // JSON 요청 시 Content-Type 추가
            // CSRF 토큰이 있는 경우 헤더에 추가
            if (csrfToken && csrfHeaderName) {
                headers[csrfHeaderName] = csrfToken;
            }
            // JSON 요청 시 Content-Type 추가
            headers['Content-Type'] = 'application/json';

            if (actionType === 'update') {

                try {
                    const response = await fetch(`/admin/user/${userNo}/update`, {
                        method: 'POST',
                        headers: headers, // CSRF 토큰 포함된 헤더
                        body: JSON.stringify(updateData)
                    });

                    const result = await response.json().catch(() => ({})); // 본문 없는 응답 대비
                    console.log('예약 변경 결과:', result);

                    if (response.ok && result.success) {
                        alert(result.message || '회원 정보가 성공적으로 수정되었습니다.');
                        window.location.reload(); // 또는 result.redirectUrl로 이동
                    } else {
                        throw new Error(result.message || `회원 정보 수정에 실패했습니다. (상태: ${response.status})`);
                    }
                } catch (error) {
                    console.error('Error updating user profile:', error);
                    alert(error.message || '오류가 발생하여 회원 정보를 수정하지 못했습니다.');
                } finally {
                    closeModal(reasonModal);
                    this.disabled = false;
                    this.innerHTML = originalButtonText;
                }

            } else {
                this.disabled = false;
                this.innerHTML = originalButtonText;
                if (actionType) { // 알려지지 않은 actionType일 때만 경고
                 console.warn("Unhandled action type in user-detail.js:", actionType);
                }
            }
        });
    }

});