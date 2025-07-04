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

// =====================================================================================================================

    // 결제 정보 토글 기능(my-reservation-detail.html 용)
    const togglePaymentButton = document.getElementById('togglePaymentDetails');
    const paymentDetailsCollapsible = document.getElementById('payment-details-collapsible');

    if (togglePaymentButton && paymentDetailsCollapsible) {
        togglePaymentButton.addEventListener('click', function() {
            const isExpanded = this.getAttribute('aria-expanded') === 'true';

            if (isExpanded) {
                // 접기
                this.setAttribute('aria-expanded', 'false');
                this.childNodes[0].nodeValue = "상세 보기 "; // 버튼 텍스트 변경 (첫 번째 자식 노드가 텍스트 노드여야 함)

                paymentDetailsCollapsible.style.maxHeight = '0'; // 높이를 0으로 애니메이션
                paymentDetailsCollapsible.classList.remove('expanded'); // opacity, margin-top 등 제거
                // display: none;은 사용하지 않음. overflow:hidden과 maxHeight:0으로 숨김.
                // HTML의 초기 style="display:none"은 JS 로드 전 숨김 용도.
                // 접힌 후에는 display 속성을 변경하지 않아도, maxHeight와 overflow로 숨겨짐.
            } else {
                // 펼치기
                this.setAttribute('aria-expanded', 'true');
                this.childNodes[0].nodeValue = "간략히 보기 "; // 버튼 텍스트 변경

                // HTML 인라인 스타일 display:none을 해제하고, 내용 계산 및 표시를 위해 grid로 변경
                paymentDetailsCollapsible.style.display = 'grid';

                // requestAnimationFrame을 사용하여 브라우저가 display 변경을 처리하고
                // scrollHeight를 정확히 계산할 시간을 줌
                requestAnimationFrame(() => {
                    // 실제 내용의 높이만큼 maxHeight 설정하여 부드럽게 펼쳐지도록 함
                    paymentDetailsCollapsible.style.maxHeight = paymentDetailsCollapsible.scrollHeight + "px";
                    paymentDetailsCollapsible.classList.add('expanded'); // opacity, margin-top 등 적용
                });
            }
        });
    }

// =====================================================================================================================

    // 카카오 지도 초기화 (my-reservation-detail.html 용)
    // kakao.maps.load() 콜백 함수 내에서 지도 관련 코드 실행
    if (typeof kakao !== 'undefined' && typeof kakao.maps !== 'undefined') {
        kakao.maps.load(function() {
            console.log('Kakao Maps API loaded for reservation detail.');
            const mapContainer = document.getElementById('kakao_map_reservation_detail');

            // 지도를 표시할 div가 실제로 페이지에 존재하는지 확인
            if (mapContainer) {

                // 지도 중심 좌표 설정
                const mapOption = {
                    center: new kakao.maps.LatLng(37.504528, 127.024497),
                    level: 4 // 지도의 확대 레벨
                };

                // 지도를 생성합니다
                const map = new kakao.maps.Map(mapContainer, mapOption);

                // 마커가 표시될 위치입니다
                const markerPosition  = new kakao.maps.LatLng(37.506186, 127.025835);

                // 마커를 생성합니다
                const marker = new kakao.maps.Marker({
                    position: markerPosition
                });

                // 마커가 지도 위에 표시되도록 설정합니다
                marker.setMap(map);

                // (선택) 지도 확대 축소를 제어할 수 있는 줌 컨트롤을 생성합니다
                const zoomControl = new kakao.maps.ZoomControl();
                map.addControl(zoomControl, kakao.maps.ControlPosition.RIGHT);

                console.log('Kakao map for reservation detail page initialized.');
            } else {
                // console.log('Map container #kakao_map_reservation_detail not found on this page.');
            }
        });
    } else {
        console.error("Kakao Maps API script not loaded or kakao object not ready.");
    }


});