document.addEventListener('DOMContentLoaded', function () {
    const seatForm = document.getElementById('seat-registration-form');
    const registerButton = document.getElementById('registerSeatBtn'); // 버튼 ID 확인

    if (seatForm && registerButton) {
        seatForm.addEventListener('submit', async function (event) {
            event.preventDefault(); // 기본 폼 전송 방지
            event.stopPropagation();

            // 유효성 검사 (Bootstrap 기본 검사 사용 시)
            // if (!seatForm.checkValidity()) {
            //     seatForm.classList.add('was-validated');
            //     return;
            // }

            // 1. 폼 데이터 수집
            const seatNm = document.getElementById('seatNm').value.trim();
            const floor = parseInt(document.getElementById('floor').value);
            const seatSort = document.getElementById('seatSort').value;
            const gridRow = document.getElementById('gridRow').value.trim() || null; // 빈 문자열이면 null
            const gridColumn = document.getElementById('gridColumn').value.trim() || null; // 빈 문자열이면 null
            const useAt = document.getElementById('useAt').checked;

            // 2. 가격 정보 수집 및 PriceDto 리스트 생성
            const prices = [];
            const hourlyPriceInput = document.getElementById('hourlyPrice').value.trim();
            const dailyPriceInput = document.getElementById('dailyPrice').value.trim();
            const monthlyPriceInput = document.getElementById('monthlyPrice').value.trim();

            if (hourlyPriceInput) { prices.push({ type: "HOURLY", price: hourlyPriceInput }); }
            if (dailyPriceInput) { prices.push({ type: "DAILY", price: dailyPriceInput }); }
            if (monthlyPriceInput) { prices.push({ type: "MONTHLY", price: monthlyPriceInput }); }

            // 백엔드 SeatRequestDto 형식에 맞게 데이터 구성
            const seatData = {
                seatNm: seatNm,
                floor: floor,
                seatSort: seatSort,
                gridRow: gridRow,
                gridColumn: gridColumn,
                useAt: useAt,
                prices: prices // 가격 정보 리스트 포함
            };

            console.log("서버로 전송할 좌석 데이터:", seatData);

            // 버튼 비활성화 및 로딩 표시
            const originalButtonHtml = registerButton.innerHTML;
            registerButton.disabled = true;
            registerButton.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i> 등록 중...';

            // CSRF 토큰 준비
            const csrfToken = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
            const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");
            const headers = { 'Content-Type': 'application/json' };
            if (csrfToken && csrfHeader) { headers[csrfHeader] = csrfToken; }

            try {

                const apiUrl = '/api/admin/seats'; // POST 방식
                const response = await fetch(apiUrl, {
                    method: 'POST',
                    headers: headers,
                    body: JSON.stringify(seatData)
                });

                if (!response.ok) {
                    const errorResult = await response.json().catch(() => ({ message: `등록 실패 (${response.status})` }));
                    if (response.status === 400 && errorResult.errors) {
                        console.error("Validation errors:", errorResult.errors);
                        let errorMessages = "입력값을 확인해주세요:\n";
                        for (const field in errorResult.errors) {
                            errorMessages += `- ${errorResult.errors[field]}\n`;
                        }
                        throw new Error(errorMessages.trim());
                    }
                    throw new Error(errorResult.message || `좌석 등록 실패 (${response.status})`);
                }

                const result = await response.json();
                console.log("좌석 등록 성공:", result);
                alert('좌석이 성공적으로 등록되었습니다.');
                window.location.href = '/admin/adminSeat';

            } catch (error) {
                console.error('좌석 등록 처리 중 오류:', error);
                alert(`등록 중 오류 발생: ${error.message}`);
            } finally {
                // 버튼 원래대로 복원
                registerButton.disabled = false;
                registerButton.innerHTML = originalButtonHtml;
            }
        });
    } else {
        console.error("좌석 등록 폼 또는 버튼을 찾을 수 없습니다!");
    }
});
