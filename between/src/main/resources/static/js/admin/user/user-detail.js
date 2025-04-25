document.addEventListener('DOMContentLoaded', function() {
    const updateUserBtn = document.getElementById('updateUserBtn');
    const deleteUserBtn = document.getElementById('deleteUserBtn');
    const goToListBtn = document.getElementById('goToListBtn');

    // '수정' 버튼 클릭 이벤트
    if (updateUserBtn) {
        updateUserBtn.addEventListener('click', function() {
            const userGradeSelect = document.getElementById('userGrade');
            const userStatusSelect = document.getElementById('userStatus');

            const newGrade = userGradeSelect.value;
            const newStatus = userStatusSelect.value;

            // 서버로 보낼 데이터 준비
            const updateData = {
                id: userId,
                grade: newGrade,
                status: newStatus
            };

            console.log('수정할 데이터:', updateData); // 디버깅용 로그

            // 서버에 수정 요청 보내기 (Fetch API 사용 예시)
            // 실제 API 엔드포인트는 서버 구현에 맞춰야 합니다.
            fetch('/api/admin/users/update', { // 예시 API 경로
                method: 'PUT', // 또는 'POST'
                headers: {
                    'Content-Type': 'application/json',
                    // 필요 시 인증 헤더 추가 (예: CSRF 토큰)
                    // 'X-CSRF-TOKEN': csrfToken
                },
                body: JSON.stringify(updateData)
            })
            .then(response => {
                if (!response.ok) {
                    // 서버 응답이 실패(4xx, 5xx) 상태일 때
                    return response.json().then(err => { throw new Error(err.message || '회원 정보 수정에 실패했습니다.') });
                }
                // 서버 응답이 성공(2xx) 상태일 때
                return response.json(); // 성공 응답이 JSON 형태일 경우
            })
            .then(data => {
                // 성공 처리 로직
                alert(data.message || '회원 정보가 성공적으로 수정되었습니다.');
                // 필요 시 페이지 새로고침 또는 UI 일부 업데이트
                // location.reload();
            })
            .catch(error => {
                // 네트워크 오류 또는 위에서 throw된 오류 처리
                console.error('Error updating user:', error);
                alert(error.message || '오류가 발생하여 회원 정보를 수정하지 못했습니다.');
            });
        });
    }

    // '탈퇴 처리' 버튼 클릭 이벤트
    if (deleteUserBtn) {
        deleteUserBtn.addEventListener('click', function() {
            if (confirm('정말로 이 회원을 탈퇴 처리하시겠습니까?\n이 작업은 되돌릴 수 없을 수 있습니다.')) {
                console.log('탈퇴 처리할 사용자 ID:', userId); // 디버깅용 로그

                // 서버에 탈퇴 요청 보내기 (Fetch API 사용 예시)
                // 실제 API 엔드포인트는 서버 구현에 맞춰야 합니다.
                fetch(`/api/admin/users/${userId}`, { // 예시 API 경로 (RESTful 방식)
                    method: 'DELETE',
                     headers: {
                        // 필요 시 인증 헤더 추가
                        // 'X-CSRF-TOKEN': csrfToken
                     }
                })
                .then(response => {
                    if (!response.ok) {
                         return response.json().then(err => { throw new Error(err.message || '회원 탈퇴 처리에 실패했습니다.') });
                    }
                     // 성공 응답이 본문 없이 올 수도 있음 (204 No Content 등)
                     // 또는 성공 메시지를 포함한 JSON 응답일 수도 있음
                     return response.text().then(text => text ? JSON.parse(text) : {}); // 응답 본문 확인
                })
                .then(data => {
                    // 성공 처리 로직
                    alert(data.message || '회원이 성공적으로 탈퇴 처리되었습니다.');
                    // 회원 목록 페이지로 리디렉션
                    window.location.href = '/admin/users'; // 목록 페이지 경로 예시
                })
                .catch(error => {
                    // 오류 처리 로직
                    console.error('Error deleting user:', error);
                    alert(error.message || '오류가 발생하여 회원을 탈퇴 처리하지 못했습니다.');
                });
            }
        });
    }

    // '목록으로' 버튼 클릭 이벤트
    if (goToListBtn) {
        goToListBtn.addEventListener('click', function() {
            // 회원 목록 페이지로 이동
            window.location.href = '/admin/userList'; // 목록 페이지 경로 예시
        });
    }

});