document.addEventListener('DOMContentLoaded', function() {

    // 테이블 행 클릭 이벤트 처리
    const userTable = document.querySelector('.user-data-table'); // 변경된 테이블 클래스 선택자
    if (userTable) {
        // tbody 안의 data-row 클래스를 가진 tr 요소들을 선택
        const clickableRows = userTable.querySelectorAll('tbody tr.data-row'); // 변경된 행 클래스 선택자

        clickableRows.forEach(row => {
            row.addEventListener('click', function() {
                // 클릭된 행(tr)의 data-user-id 속성 값 가져오기
                const userId = this.dataset.userId;

                if (userId) {
                    // 상세 페이지 URL 생성 및 이동
                    window.location.href = '/admin/users/' + userId;
                } else {
                    console.error('클릭된 행에서 사용자 ID(data-user-id)를 찾을 수 없습니다.');
                }
            });
        });
    }

    // TODO: 필요시 날짜 선택기 초기화 또는 다른 JS 로직 추가
    // --- 기타 JS: 검색 버튼 클릭 시 동작 등 ---
    // 현재는 form submit으로 처리되지만, 필요시 여기에 AJAX 검색 로직 추가 가능
    // const filterForm = document.querySelector('.filter-section');
    // if (filterForm) {
    //     filterForm.addEventListener('submit', function(event) {
    //         event.preventDefault(); // 기본 submit 동작 방지
    //         console.log('검색 조건:', new FormData(filterForm));
    //         // 여기에 AJAX 로직 구현...
    //     });
    // }
});