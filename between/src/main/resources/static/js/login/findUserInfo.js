document.addEventListener('DOMContentLoaded', () => {
    const tabs = document.querySelectorAll('.tab-link');
    const tabContents = document.querySelectorAll('.tab-content');

    // --- 이메일 찾기 관련 요소 ---
    const findEmailForm = document.getElementById('find-email-form');
    const phoneInput = document.getElementById('find-email-phone');
    const otpSection = document.getElementById('otp-section');
    const otpInput = document.getElementById('find-email-otp');
    const otpTimerSpan = document.getElementById('otp-timer');
    const findEmailActionButton = document.getElementById('find-email-action-button'); // 버튼 참조 변경
    const findEmailResultDiv = document.getElementById('find-email-result');
    const findEmailErrorDiv = document.getElementById('find-email-error');

    // --- 비밀번호 찾기 관련 요소 ---
    const findPasswordForm = document.getElementById('find-password-form');
    const findPasswordResultDiv = document.getElementById('find-password-result');
    const findPasswordErrorDiv = document.getElementById('find-password-error');

    // --- 타이머 관련 변수 ---
    let otpTimerInterval = null;
    let otpRemainingTime = 180; // 3분 (초 단위)

    // --- Tab Switching Logic ---
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const targetTab = tab.getAttribute('data-tab');
            tabs.forEach(t => t.classList.remove('active'));
            tabContents.forEach(tc => tc.classList.remove('active'));
            tab.classList.add('active');
            document.getElementById(`${targetTab}-content`).classList.add('active');
            clearMessages();
            resetOtpState(); // 탭 전환 시 OTP 상태 및 버튼 초기화
        });
    });

    // --- 이메일 찾기: 인증하기 버튼 클릭 --- // const findEmailActionButton = document.getElementById('find-email-action-button');
    if (findEmailActionButton) {    // 인증하기 버튼이 있는지 확인

        // 휴대폰 번호 입력 시 숫자만 허용하고 자동으로 하이픈 추가
        phoneInput.addEventListener('input', function(e) {
            // 숫자 이외의 문자 제거
            let value = this.value.replace(/[^0-9]/g, '');

            // 하이픈 추가
            if (value.length > 3 && value.length <= 7) {
                // 010-1234
                value = value.substring(0, 3) + '-' + value.substring(3);
            } else if (value.length > 7) {
                // 010-1234-5678
                value = value.substring(0, 3) + '-' + value.substring(3, 7) + '-' + value.substring(7, 11);
            }

            // 최대 13자리로 제한 (010-1234-5678)
            if (value.length > 13) {
                value = value.substring(0, 13);
            }

            this.value = value;

            /*
            if (this.value) {
                phoneMessage.textContent = '';
            }
            */
        });


        findEmailActionButton.addEventListener('click', async () => {
            const currentStatus = findEmailActionButton.dataset.status; // data-status 속성 값
            clearMessages(); // 메시지 초기화

            // --- '인증하기' 로직 ---
            if (currentStatus === 'idle') {
                const phoneNo = phoneInput.value.replace(/[^0-9]/g, ''); // 하이픈 제거

                // 전화번호 유효성 검사(입력값이 없거나 11자리 숫자인지 확인)
                if (!phoneNo || phoneNo.length < 11) {
                    showMsg('find-email-error', '올바른 휴대폰 번호를 입력해주세요.');
                    return;
                }

                try {
                    showLoadingButton(true, '전송 중...'); // 로딩 표시 (선택적)

                    // 인증번호 생성 및 SMS 발송 API 호출
                    const response = await fetch('/send-verification', {
                        method: 'POST',
//                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
//                        body: new URLSearchParams({ phoneNo: phoneNo })
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(
                            { phoneNo: phoneNo }
                        )
                    });

//                    const result = { success: true }; // 테스트용
                    const result = await response.json();

                    console.log("Send OTP Response:", response, result);
                    console.log("send-verification|인증번호 전송|result"+ JSON.stringify(result)); // 응답예시 : {"success":"true"}

                    if (response.ok && result.success) {
                        phoneInput.disabled = true;
                        otpSection.classList.add('visible');
                        otpInput.value = '';
                        otpInput.focus();
                        startOtpTimer(180);

                        // 버튼 상태 변경
                        findEmailActionButton.textContent = '확인';
                        findEmailActionButton.dataset.status = 'confirming';

                        hideMsg('find-email-error');
                        showMsg('find-email-result', '인증번호를 발송했습니다. 휴대폰을 확인해주세요.');
                    } else {
                        hideMsg('find-email-result');
                        showMsg('find-email-error', '인증번호 발송에 실패했습니다.'); // 테스트용
                    }
                } catch (error) {
                    console.error("Send OTP Error:", error);
                    showMsg('find-email-error', '오류 발생 (네트워크 등)');
                } finally {
                     showLoadingButton(false, '인증하기'); // 로딩 완료 후 버튼 원복 (상태는 confirming 유지)
                }

            } else if (currentStatus === 'confirming') {

                const phoneNo = phoneInput.value.replace(/[^0-9]/g, ''); // 비활성화 상태지만 값은 필요
                const otp = otpInput.value;

                if (!otp || otp.length !== 6) { // 6자리 가정
                    showMsg('find-email-error', '인증번호 6자리를 정확히 입력해주세요.');
                    return;
                }

                try {
                    showLoadingButton(true, '확인 중...'); // 로딩 표시

                    const response = await fetch('/findUserInfo/verify-code', {
                        method: 'POST',
//                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
//                        body: new URLSearchParams({ phoneNo: phoneNo, otp: otp })
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            phoneNo: phoneNo,
                            code: otp
                        })
                    });

//                    const result = { success: true, email: 'tes***@example.com' }; // 테스트용
                    const result = await response.json(); // 응답은 {success: boolean, email: string} 형태

                    console.log("Send OTP Response:", response, result);
                    console.log("verify-code|인증번호 확인|result"+ JSON.stringify(result)); // 응답예시 : {"success":"true"}

                    if (response.ok && result.success) {
                        clearOtpTimer();

                        hideMsg('find-email-error');
                        showMsg('find-email-result', `인증 성공! 회원님의 이메일은 </br> <strong>${result.email}</strong> </br> 입니다.`);
                        resetOtpState(); // 상태 초기화 (버튼 포함)
                    } else if (response.ok && !result.success) {

                        hideMsg('find-email-result');
                        showMsg('find-email-error', `<strong>${result.message}`);

                    } else {
                        hideMsg('find-email-result');
                        showMsg('find-email-error', '인증번호가 올바르지 않거나 만료되었습니다.');
                        otpInput.focus();
                    }
                } catch (error) {
                    console.error("Verify OTP Error:", error);
                    showMsg('find-email-error', '오류 발생 (확인 중 문제 발생)');
                } finally {
                     showLoadingButton(false, '확인'); // 로딩 완료
                     // 성공 시 resetOtpState에서 버튼 텍스트 바뀜, 실패 시 '확인' 유지
                     if(findEmailActionButton.dataset.status === 'idle') {
                         findEmailActionButton.textContent = '인증하기';
                     }
                }
            }
        });
    }

    // --- 비밀번호 찾기 Form Submission (기존 유지) ---
    if (findPasswordForm) {
        findPasswordForm.addEventListener('submit', async (event) => {
            event.preventDefault();
            clearMessages();
            const email = document.getElementById('find-pw-email').value;
            const name = document.getElementById('find-pw-name').value;
            if (!email) {
                 showMsg('find-password-error', '이메일 주소를 입력해주세요.');
                 return;
            }
            // ... 기존 비밀번호 찾기 AJAX 로직 ...
             console.log("Password reset form submitted for:", email); // 임시 로그
             showMsg('find-password-result', '비밀번호 재설정 요청 처리중... (실제 구현 필요)'); // 임시
        });
    }


    // --- Helper Functions ---
    function clearMessages() {
        findEmailResultDiv.style.display = 'none';
        findEmailErrorDiv.style.display = 'none';
        findPasswordResultDiv.style.display = 'none';
        findPasswordErrorDiv.style.display = 'none';
        findEmailResultDiv.innerHTML = '';
        findEmailErrorDiv.innerHTML = '';
        findPasswordResultDiv.innerHTML = '';
        findPasswordErrorDiv.innerHTML = '';
    }

    function showMsg(elementId, message) {
        const element = document.getElementById(elementId);
        if (element) {
            element.innerHTML = message;
            element.style.display = 'block';
        }
    }

    function hideMsg(elementId) {
        const element = document.getElementById(elementId);
        if (element) {
            element.innerHTML = "";
            element.style.display = 'none';
        }
    }

// --- 인증번호 유효시간 타이머 ---
    function startOtpTimer(durationInSeconds) {
        clearOtpTimer(); // 기존 타이머 중지
        otpEndTime = Date.now() + durationInSeconds * 1000; // 종료 시각 계산 및 저장

        updateTimerDisplay(); // 즉시 한번 표시 (0초 딜레이 방지)

        // 1초마다 남은 시간 업데이트 시도
        otpTimerInterval = setInterval(updateTimerDisplay, 1000);
    }

    function updateTimerDisplay() {
        const remainingMilliseconds = otpEndTime - Date.now(); // 현재 시간 기준 남은 시간 계산
        const remainingSeconds = Math.round(remainingMilliseconds / 1000); // 초 단위로 변환

        if (remainingSeconds <= 0) {
            // 타이머 종료
            otpTimerSpan.textContent = "00:00"; // 0초 표시
            clearOtpTimer(); // 인터벌 중지
            resetOtpState(); // 버튼 등 상태 초기화
            hideMsg('find-email-result'); // 성공 메시지 숨김
            showMsg('find-email-error', '인증 시간이 만료되었습니다. 다시 시도해주세요.'); // 만료 메시지
        } else {
            // 남은 시간 표시 (MM:SS 형식)
            const minutes = Math.floor(remainingSeconds / 60);
            const seconds = remainingSeconds % 60;
            otpTimerSpan.textContent = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
        }
    }

    function clearOtpTimer() {
        clearInterval(otpTimerInterval); // setInterval 중지
        otpTimerInterval = null; // 인터벌 ID 초기화
        otpTimerSpan.textContent = ""; // 타이머 텍스트 지우기
    }

    // --- 상태 초기화 함수 (버튼 포함) ---
    function resetOtpState() {
        clearOtpTimer();                        // 이메일찾기 > 인증번호 타이머 초기화
        otpSection.classList.remove('visible'); // 이메일찾기 > 인증번호 입력 영역 숨기기
        otpInput.value = '';
        phoneInput.disabled = false;
        // 버튼 상태 초기화
        if(findEmailActionButton) {
            findEmailActionButton.textContent = '인증하기';
            findEmailActionButton.dataset.status = 'idle';
            findEmailActionButton.disabled = false; // 로딩 중 비활성화 해제
        }
    }

    // --- (선택적) 로딩 버튼 표시 함수 ---
    function showLoadingButton(isLoading, loadingText) {
        if (findEmailActionButton) {
            findEmailActionButton.disabled = isLoading;
            if(isLoading) {
                // 원래 텍스트 저장 (필요하다면)
                // findEmailActionButton.dataset.originalText = findEmailActionButton.textContent;
                findEmailActionButton.textContent = loadingText || '처리 중...';
            } else {
                // 상태에 따라 텍스트 복원 (resetOtpState 또는 각 로직에서 처리하므로 여기선 비활성화만 해제)
                // findEmailActionButton.textContent = findEmailActionButton.dataset.originalText || '인증하기';
                // delete findEmailActionButton.dataset.originalText;
            }
        }
    }


     // --- Email Masking Function (기존 유지) ---
    function maskEmail(email) {
        // ... (기존 마스킹 로직) ...
        if (!email || email.indexOf('@') === -1) return email;
        const [localPart, domain] = email.split('@');
        if (localPart.length <= 3) {
            return '***@' + domain;
        } else {
            return localPart.substring(0, 3) + '***@' + domain;
        }
    }

    // 초기 상태 설정 (페이지 로드 시)
    resetOtpState();

});