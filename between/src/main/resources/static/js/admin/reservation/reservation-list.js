// /js/admin/reservation/reservations.js

// 문서 로딩 완료 후 스크립트 실행
document.addEventListener('DOMContentLoaded', function() {

    // 예약 목록 테이블 요소 선택 (HTML의 테이블 클래스와 일치해야 함)
    const reservationTable = document.querySelector('.reservation-data-table');

    // 테이블이 페이지에 존재하는 경우에만 내부 로직 실행
    if (reservationTable) {
        // 테이블 본문(tbody) 안의 'data-row' 클래스를 가진 모든 행(tr) 요소들을 선택
        const clickableRows = reservationTable.querySelectorAll('tbody tr.data-row');

        // 선택된 각 행에 대해 클릭 이벤트 리스너 추가
        clickableRows.forEach(row => {
            row.addEventListener('click', function() {
                // 클릭된 행(tr)의 'data-reservation-id' 속성 값 가져오기
                // 'this'는 클릭 이벤트가 발생한 행(tr) 요소를 가리킴
                const reservationId = this.dataset.reservationId; // dataset 속성 사용

                // reservationId 값이 유효한 경우 (존재하는 경우)
                if (reservationId) {
                    // 해당 예약의 상세 페이지 URL 생성
                    const detailPageUrl = '/admin/reservations/' + reservationId;
                    // 생성된 URL로 페이지 이동
                    window.location.href = detailPageUrl;
                } else {
                    // 'data-reservation-id' 속성 값을 찾지 못한 경우 콘솔에 에러 메시지 출력
                    console.error('클릭된 행에서 예약 ID(data-reservation-id)를 찾을 수 없습니다.');
                }
            });
        });
    }

});