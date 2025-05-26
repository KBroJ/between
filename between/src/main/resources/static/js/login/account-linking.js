document.addEventListener('DOMContentLoaded', function () {
    const sendLinkOtpBtn = document.getElementById('sendLinkOtpBtn');
    const otpVerificationSection = document.getElementById('verify-otp-section'); // ⭐ ID 일치 확인
    const linkOtpCodeInput = document.getElementById('linkOtpCode');
    const linkOtpTimerSpan = document.getElementById('linkOtpTimer');
    const resendLinkOtpBtn = document.getElementById('resendLinkOtpBtn');
    const verifyAndLinkBtn = document.getElementById('verifyAndLinkBtn');

    const linkErrorMessageElement = document.getElementById('linking-error-message');
    const linkSuccessMessageElement = document.getElementById('linking-success-message');
    const otpMessageElement = document.getElementById('otp-message'); // OTP 입력 필드 하단 메시지

    const initialActionSection = document.getElementById('initial-action-section');

    let otpTimerInterval;
    let otpEndTime;
    const OTP_DURATION = 180; // 3분 (초)
    let isOtpSent = false; // OTP가 성공적으로 발송되었는지 추적

    // CSRF 토큰 (기존과 동일)
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeaderName = document.querySelector('meta[name="_csrf_header"]')?.content;
    const headers = { 'Content-Type': 'application/json' };
    if (csrfToken && csrfHeaderName) {
        headers[csrfHeaderName] = csrfToken;
    }

    function clearAllMessages() {
        linkErrorMessageElement.style.display = 'none';
        linkErrorMessageElement.textContent = '';
        linkSuccessMessageElement.style.display = 'none';
        linkSuccessMessageElement.textContent = '';
        if (otpMessageElement) { // otpMessageElement가 없을 수도 있으므로 null 체크
            otpMessageElement.style.display = 'none';
            otpMessageElement.textContent = '';
        }
    }

    function showMessage(area, message, isError) { // area: 'global', 'otp'
        clearAllMessages();
        let element;
        if (area === 'global') {
            element = isError ? linkErrorMessageElement : linkSuccessMessageElement;
        } else { // 'otp'
            element = otpMessageElement;
        }
        const color = isError ? '#dc3545' : (area === 'success' ? '#28a745' : '#555');

        element.textContent = message;
        if (area !== 'otp' || isError) { // otp 일반 메시지는 기본 색상 유지
             element.style.color = color;
        }
        element.style.display = 'block';
    }

    function startOtpTimer(durationInSeconds) {
        if (otpTimerInterval) clearInterval(otpTimerInterval);
        otpEndTime = Date.now() + durationInSeconds * 1000;

        linkOtpTimerSpan.style.display = 'inline'; // ⭐ 타이머 보이게
        if(verifyAndLinkBtn) verifyAndLinkBtn.disabled = false;
        if(linkOtpCodeInput) linkOtpCodeInput.disabled = false;
        if(resendLinkOtpBtn) {
            resendLinkOtpBtn.disabled = false; // 재전송 버튼 활성화
            resendLinkOtpBtn.textContent = '재전송';
        }

        updateOtpTimerDisplay();
        otpTimerInterval = setInterval(updateOtpTimerDisplay, 1000);
    }

    function updateOtpTimerDisplay() {
        const remainingMs = otpEndTime - Date.now();
        const remainingSec = Math.max(0, Math.round(remainingMs / 1000));
        const min = Math.floor(remainingSec / 60);
        const sec = remainingSec % 60;
        linkOtpTimerSpan.textContent = `${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`;

        if (remainingSec <= 0) {
            clearInterval(otpTimerInterval);
            linkOtpTimerSpan.textContent = "시간 만료";
            showMessage('otp', "인증 시간이 만료되었습니다. '재전송' 버튼을 눌러주세요.");
            if(verifyAndLinkBtn) verifyAndLinkBtn.disabled = true;
            if(linkOtpCodeInput) linkOtpCodeInput.disabled = true;
            if (resendLinkOtpBtn) resendLinkOtpBtn.disabled = false;
        }
    }

    function handleSendOtp(isResend = false) {
        const buttonToDisable = isResend ? resendLinkOtpBtn : sendLinkOtpBtn;
        const originalButtonText = buttonToDisable.textContent;
        buttonToDisable.disabled = true;
        buttonToDisable.textContent = isResend ? '재전송 중...' : '발송 중...';
        clearAllMessages();

        fetch("/social/link-account/send-otp", {
            method: 'POST',
            headers: headers,
        })
        .then(response => {
            if (!response.ok) { // 에러 응답 처리
                return response.json().then(errData => {
                    throw { status: response.status, data: errData }; // 커스텀 에러 객체
                });
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                showMessage('otp', data.message || "인증번호가 발송되었습니다.");
                if(initialActionSection) initialActionSection.style.display = 'none'; // 초기 발송 섹션 숨김
                if(otpVerificationSection) otpVerificationSection.style.display = 'block'; // OTP 입력 섹션 보임
                if(linkOtpCodeInput) {
                    linkOtpCodeInput.value = '';
                    linkOtpCodeInput.focus();
                }
                startOtpTimer(OTP_DURATION);
                // sendLinkOtpBtn은 이미 initialActionSection과 함께 숨겨짐
                if (resendLinkOtpBtn) resendLinkOtpBtn.style.display = 'inline-block'; // 재전송 버튼 표시

                isOtpSent = true; // OTP 발송 성공 플래그
            } else {
                showMessage('error', data.message || "인증번호 발송에 실패했습니다.");
                buttonToDisable.disabled = false; // 실패 시 버튼 다시 활성화
                buttonToDisable.textContent = originalButtonText;
                isOtpSent = false;
            }
        })
        .catch(error => {
            console.error("Error sending OTP for linking:", error);
            const errorMessage = error.data && error.data.message ? error.data.message : (error.message || "OTP 발송 중 오류가 발생했습니다.");
            showMessage('error', errorMessage);
            buttonToDisable.disabled = false;
            buttonToDisable.textContent = originalButtonText;
            isOtpSent = false;
        });
    }

    if (sendLinkOtpBtn) {
        sendLinkOtpBtn.addEventListener('click', function() {
            handleSendOtp(false);
        });
    }

    if (resendLinkOtpBtn) {
        resendLinkOtpBtn.addEventListener('click', function() {
            handleSendOtp(true);
        });
    }

    if (verifyAndLinkBtn) {
        verifyAndLinkBtn.addEventListener('click', function () {
            const otp = linkOtpCodeInput.value.trim();
            if (!isOtpSent) { // OTP가 성공적으로 발송된 적이 없는 경우
                showMessage('otp', "먼저 인증번호를 발송해주세요.");
                return;
            }
            if (!otp || otp.length !== 6) {
                showMessage('otp', "인증번호 6자리를 정확히 입력해주세요.");
                return;
            }

            const originalBtnText = this.textContent;
            this.disabled = true;
            this.textContent = '계정 연결 확인 중...';
            clearAllMessages();

            fetch("/social/link-account/verify-and-link", {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({otp: otp})
            })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(errData => {
                        throw { status: response.status, data: errData };
                    });
                }
                return response.json();
            })
            .then(data => {
                if (data.success) {
                    showMessage('success', data.message || "계정이 성공적으로 연결되었습니다.");
                    if (otpTimerInterval) clearInterval(otpTimerInterval);
                    if(linkOtpTimerSpan) linkOtpTimerSpan.textContent = "인증 완료";
                    if(linkOtpCodeInput) linkOtpCodeInput.disabled = true;
                    this.textContent = "연결 완료";
                    if (resendLinkOtpBtn) resendLinkOtpBtn.style.display = 'none';

                    if (data.redirectUrl) {
                        setTimeout(function() {
                            window.location.href = data.redirectUrl; // Thymeleaf URL 아님
                        }, 2000);
                    }
                } else {
                    showMessage('otp', data.message || "계정 연결에 실패했습니다. 인증번호를 확인해주세요.");
                    this.disabled = false;
                    this.textContent = originalBtnText;
                    if(linkOtpCodeInput) linkOtpCodeInput.focus();
                }
            })
            .catch(error => {
                console.error("Error verifying OTP and linking account:", error);
                const errorMessage = error.data && error.data.message ? error.data.message : (error.message || "계정 연결 처리 중 오류가 발생했습니다.");
                showMessage('error', errorMessage);
                this.disabled = false;
                this.textContent = originalBtnText;
            });
        });
    }
});