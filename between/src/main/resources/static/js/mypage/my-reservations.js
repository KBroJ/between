// 예약 취소 확인 함수
async function confirmCancel(resNo, cancelUrl) {
    console.log(`예약 취소 시도: resNo=${resNo}, url=${cancelUrl}`);

    // 사용자에게 정말 취소할지 확인
    if (!confirm("정말 예약을 취소하시겠습니까?")) {
        console.log("사용자 취소");
        return; // 사용자가 '취소' 누르면 함수 종료
    }

    // 확인을 누르면 백엔드에 취소 요청 보내기 (POST 방식 권장)
    try {
        // CSRF 토큰 가져오기 (Spring Security 사용 시)
        const token = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
        const header = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");
        const headers = {
            'Content-Type': 'application/json'
            // 필요시 다른 헤더 추가
        };
        if (token && header) {
            headers[header] = token;
            console.log("CSRF Header Included for cancel request.");
        }

        // fetch API 사용하여 POST 요청 보내기 (URL은 Thymeleaf에서 생성된 cancelUrl 사용)
        const response = await fetch(cancelUrl, {
            method: 'POST',
            headers: headers
            // body: JSON.stringify({ resNo: resNo }) // 필요시 body에 데이터 추가
        });

        // 백엔드 응답 처리
        const result = await response.json(); // 백엔드가 JSON 응답 보낸다고 가정

        if (response.ok && result.success) { // 응답 상태 OK 이고, 백엔드 결과가 success일 때
            alert('예약이 성공적으로 취소되었습니다.');
            location.reload(); // 페이지 새로고침하여 목록 갱신
        } else {
            // 백엔드에서 보낸 에러 메시지 표시
            throw new Error(result.message || `예약 취소 중 오류가 발생했습니다. (상태: ${response.status})`);
        }

    } catch (error) {
        console.error("예약 취소 처리 중 오류:", error);
        alert(`예약 취소 실패: ${error.message}`);
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