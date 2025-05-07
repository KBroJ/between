// static/js/admin/reservation/reservation-detail.js

document.addEventListener('DOMContentLoaded', function () {
    const cancelButton = document.querySelector('.btn-cancel-reservation');

    if (cancelButton) {
        cancelButton.addEventListener('click', function () {
            const resNo = this.dataset.resno; // HTML의 data-resno 속성 값 가져오기
            const reason = prompt("예약을 취소하시겠습니까? 취소 사유를 입력해주세요 (선택 사항):");

            // 사용자가 취소 버튼을 누르거나 아무것도 입력하지 않고 확인을 누르지 않은 경우
            // prompt는 취소 시 null, 확인 시 빈 문자열 "" 또는 입력값을 반환
            if (reason === null) { // 사용자가 '취소'를 누름
                alert("예약 취소가 중단되었습니다.");
                return;
            }

            // 여기에 실제 취소 처리를 위한 AJAX 요청 로직 추가
            // 예시: /api/admin/reservations/{resNo}/cancel 와 같은 엔드포인트로 POST 요청
            console.log("예약 번호:", resNo, "취소 사유:", reason);
            alert(`예약 번호 ${resNo}에 대한 취소 요청을 서버로 전송합니다. (실제 API 연동 필요)\n사유: ${reason}`);

            // --- 실제 AJAX 요청 예시 (Fetch API 사용) ---
            /*
            fetch(`/api/admin/reservations/${resNo}/cancel`, { // API 엔드포인트 확인 필요
                method: 'POST', // 또는 PUT, DELETE 등 API 스펙에 맞게
                headers: {
                    'Content-Type': 'application/json',
                    // Spring Security CSRF 토큰 필요시 헤더에 추가
                    // 'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').getAttribute('content')
                },
                body: JSON.stringify({
                    cancellationReason: reason
                })
            })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => { throw new Error(err.message || '예약 취소에 실패했습니다.') });
                }
                return response.json();
            })
            .then(data => {
                if (data.success) {
                    alert('예약이 성공적으로 취소되었습니다.');
                    window.location.href = '/admin/reservationList'; // 목록 페이지로 리다이렉션
                } else {
                    alert('예약 취소 실패: ' + (data.message || '알 수 없는 오류'));
                }
            })
            .catch(error => {
                console.error('Error cancelling reservation:', error);
                alert('오류 발생: ' + error.message);
            });
            */
        });
    }

    // 결제 상세 내역 펼치기/접기 토글 (HTML <details> 태그 사용시 불필요)
    // 만약 <details> 태그 대신 div와 JavaScript로 구현한다면 아래와 같이 추가
    /*
    const paymentDetailToggle = document.getElementById('paymentDetailToggle'); // 예시 ID
    const paymentDetailContent = document.getElementById('paymentDetailContent'); // 예시 ID

    if (paymentDetailToggle && paymentDetailContent) {
        paymentDetailToggle.addEventListener('click', function(event) {
            event.preventDefault();
            if (paymentDetailContent.style.display === 'none' || paymentDetailContent.style.display === '') {
                paymentDetailContent.style.display = 'block';
                this.textContent = '상세 결제 내역 숨기기';
            } else {
                paymentDetailContent.style.display = 'none';
                this.textContent = '상세 결제 내역 보기';
            }
        });
    }
    */
});