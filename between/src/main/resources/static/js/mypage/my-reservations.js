/**
 * 예약 취소 확인 함수
 * @param {string} resNo - 예약 번호
 * @param {string} cancelUrl - 취소를 처리할 서버 URL
 */
function confirmCancel(resNo, cancelUrl) {
    if (confirm('예약 번호 ' + resNo + ' 예약을 정말로 취소하시겠습니까?\n취소 후에는 복구할 수 없습니다.')) {
        // 사용자가 '확인'을 누르면 취소 URL로 이동 (또는 fetch API로 POST 요청 전송)
        // 여기서는 간단하게 페이지 이동 방식으로 구현
        // POST 방식으로 변경하려면 아래 주석 참고
        location.href = cancelUrl;

        /* // fetch API를 사용하여 POST 방식으로 취소 요청 보내는 예시
        fetch(cancelUrl, {
            method: 'POST', // 또는 'DELETE' 등 서버에서 정의한 메소드
            headers: {
                // 필요시 CSRF 토큰 등 헤더 추가
                // 'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').getAttribute('content')
            }
        })
        .then(response => {
            if (response.ok) {
                alert('예약이 성공적으로 취소되었습니다.');
                location.reload(); // 페이지 새로고침하여 변경사항 반영
            } else {
                // 서버 응답에서 에러 메시지 추출 시도
                response.json().then(data => {
                    alert('예약 취소 중 오류가 발생했습니다: ' + (data.message || '서버 오류'));
                }).catch(() => {
                    alert('예약 취소 중 오류가 발생했습니다.');
                });
            }
        })
        .catch(error => {
            console.error('Error during cancellation:', error);
            alert('네트워크 오류 또는 처리 중 문제가 발생하여 예약을 취소할 수 없습니다.');
        });
        */
    } else {
        // 사용자가 '취소'를 누르면 아무 작업도 하지 않음
        console.log('Reservation cancellation cancelled by user.');
    }
}

// 페이지 로드 시 실행될 수 있는 추가 초기화 코드 (필요한 경우)
document.addEventListener('DOMContentLoaded', function() {
    console.log('My reservations page loaded.');

    // ✨ 테이블 행 클릭 이벤트 처리 추가
    const tableRows = document.querySelectorAll('.reservation-table-wrapper tbody tr.data-row');

    tableRows.forEach(row => {
        row.addEventListener('click', function(event) {
            // 클릭된 요소가 '취소' 버튼이거나 버튼 내부 요소인지 확인
            if (event.target.closest('.btn-cancel')) {
                // 취소 버튼 클릭 시 행 클릭 이벤트는 무시
                return;
            }

            // 행에 저장된 상세 페이지 URL 가져오기
            const detailUrl = event.currentTarget.dataset.detailUrl;

            // URL이 존재하면 해당 URL로 이동
            if (detailUrl) {
                window.location.href = detailUrl;
            } else {
                console.error('Detail URL not found for this row.');
            }
        });
    });

    // 예: 날짜 입력 필드 기본값 설정 등
});