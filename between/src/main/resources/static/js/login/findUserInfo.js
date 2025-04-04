document.addEventListener('DOMContentLoaded', () => {
    const tabs = document.querySelectorAll('.tab-link');
    const tabContents = document.querySelectorAll('.tab-content');

    // --- 이메일 찾기 관련 요소 ---
    const findEmailForm = document.getElementById('find-email-form');
    const phoneInput = document.getElementById('find-email-phone');
    const emailOtpSection = document.getElementById('otp-section'); // 이메일 찾기 OTP 섹션
    const emailOtpInput = document.getElementById('find-email-otp');
    const emailOtpTimerSpan = document.getElementById('otp-timer');
    const findEmailActionButton = document.getElementById('find-email-action-button');
    const findEmailResultDiv = document.getElementById('find-email-result');
    const findEmailErrorDiv = document.getElementById('find-email-error');

    // --- 비밀번호 찾기 관련 요소 ---
    const passwordMainForm = document.getElementById('find-password-main-form');
    const pwEmailGroup = document.getElementById('password-email-group');
    const pwEmailInput = document.getElementById('find-pw-email');
    const passwordOtpGroup = document.getElementById('password-otp-group'); // 비밀번호 찾기 OTP 그룹
    const passwordOtpInput = document.getElementById('find-pw-otp');
    const passwordOtpTimerSpan = document.getElementById('password-otp-timer');
    const newPasswordGroup = document.getElementById('password-newpw-group'); // 새 PW 입력 그룹
    const newPasswordInput = document.getElementById('new-password');
    const confirmNewPasswordInput = document.getElementById('confirm-new-password');
    const passwordActionButton = document.getElementById('password-action-button'); // 비밀번호 찾기 통합 액션 버튼
    const findPasswordResultDiv = document.getElementById('find-password-result');
    const findPasswordErrorDiv = document.getElementById('find-password-error');

    // --- 타이머 관련 변수 ---
    let emailOtpTimerInterval = null;
    let emailOtpEndTime = 0;
    let passwordOtpTimerInterval = null;
    let passwordOtpEndTime = 0;

    // --- 비밀번호 재설정용 이메일 변수 ---
    let verifiedEmailForPasswordReset = null;


    // --- Tab Switching Logic ---
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const targetTab = tab.getAttribute('data-tab');
            tabs.forEach(t => t.classList.remove('active'));
            tabContents.forEach(tc => tc.classList.remove('active'));
            tab.classList.add('active');
            document.getElementById(`${targetTab}-content`).classList.add('active');
            clearMessages();
            resetEmailFindState();
            resetPasswordFindState();
        });
    });

    // --- 이메일 찾기 로직 바인딩 ---
    if (findEmailActionButton) {
        phoneInput.addEventListener('input', formatPhoneNumber);
        findEmailActionButton.addEventListener('click', handleEmailFindAction);
    }

    // --- 비밀번호 찾기 로직 바인딩 ---
    if (passwordActionButton) {
        passwordActionButton.addEventListener('click', handlePasswordAction);
    }

    // ======================================================
    // == 이메일 찾기 관련 함수들 ==
    // ======================================================
    function formatPhoneNumber(e) {
        let value = this.value.replace(/[^0-9]/g, '');
        if (value.length > 3 && value.length <= 7) {
            value = value.substring(0, 3) + '-' + value.substring(3);
        } else if (value.length > 7) {
            value = value.substring(0, 3) + '-' + value.substring(3, 7) + '-' + value.substring(7, 11);
        }
        if (value.length > 13) value = value.substring(0, 13);
        this.value = value;
    }

    async function handleEmailFindAction() {
        const currentStatus = findEmailActionButton.dataset.status;
        clearMessages('find-email-error', 'find-email-result');

        if (currentStatus === 'idle') {
            const phoneNo = phoneInput.value.replace(/[^0-9]/g, '');
            if (!phoneNo || phoneNo.length < 11) {
                showMsg('find-email-error', '올바른 휴대폰 번호를 입력해주세요.');
                return;
            }
            try {
                showLoadingButton(findEmailActionButton, true, '전송 중...');
                const response = await fetch('/send-verification', { // 휴대폰 OTP 발송 API
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ phoneNo: phoneNo })
                });
                const result = await response.json();
                if (response.ok && result.success) {
                    phoneInput.disabled = true;
                    emailOtpSection.classList.add('visible');
                    emailOtpInput.value = ''; emailOtpInput.focus();
                    startEmailOtpTimer(180);
                    findEmailActionButton.textContent = '확인';
                    findEmailActionButton.dataset.status = 'confirming';
                    showMsg('find-email-result', '인증번호를 발송했습니다. 휴대폰을 확인해주세요.');
                } else {
                    showMsg('find-email-error', result.message || '인증번호 발송에 실패했습니다.');
                }
            } catch (error) {
                console.error("Send Email Find OTP Error:", error);
                showMsg('find-email-error', '오류 발생 (네트워크 등)');
            } finally {
                showLoadingButton(findEmailActionButton, false);
                 if(findEmailActionButton.dataset.status === 'idle') { // 실패 시 원래 텍스트 복원
                      findEmailActionButton.textContent = '인증하기';
                 }
            }
        } else if (currentStatus === 'confirming') {
            const phoneNo = phoneInput.value.replace(/[^0-9]/g, '');
            const otp = emailOtpInput.value;
            if (!otp || otp.length !== 6) {
                showMsg('find-email-error', '인증번호 6자리를 정확히 입력해주세요.');
                return;
            }
            try {
                showLoadingButton(findEmailActionButton, true, '확인 중...');
                // 이메일 찾기 확인 API (컨트롤러 경로와 일치 필요)
                const response = await fetch('/findUserInfo/verify-code', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ phoneNo: phoneNo, code: otp })
                });
                const result = await response.json();
                if (response.ok && result.success) {
                    clearEmailOtpTimer();
                    showMsg('find-email-result', `인증 성공! 회원님의 이메일은 <br> <strong>${result.email}</strong> <br> 입니다.`);
                    resetEmailFindState(); // 성공 시 초기화
                } else {
                    showMsg('find-email-error', result.message || '인증번호가 올바르지 않거나 만료되었습니다.');
                    emailOtpInput.focus();
                }
            } catch (error) {
                console.error("Verify Email Find OTP Error:", error);
                showMsg('find-email-error', '오류 발생 (확인 중 문제 발생)');
            } finally {
                showLoadingButton(findEmailActionButton, false);
                 if(findEmailActionButton.dataset.status === 'confirming') { // 실패 시 텍스트 '확인' 유지
                      findEmailActionButton.textContent = '확인';
                 }
            }
        }
    }

    // --- 이메일 찾기 타이머 함수들 ---
    function startEmailOtpTimer(durationInSeconds) {
        clearEmailOtpTimer();
        emailOtpEndTime = Date.now() + durationInSeconds * 1000;
        updateEmailTimerDisplay();
        emailOtpTimerInterval = setInterval(updateEmailTimerDisplay, 1000);
    }
    function updateEmailTimerDisplay() {
        const remainingMs = emailOtpEndTime - Date.now();
        const remainingSec = Math.round(remainingMs / 1000);
        if (remainingSec <= 0) {
            emailOtpTimerSpan.textContent = "00:00";
            clearEmailOtpTimer();
            resetEmailFindState();
            showMsg('find-email-error', '인증 시간이 만료되었습니다. 다시 시도해주세요.');
        } else {
            const min = Math.floor(remainingSec / 60);
            const sec = remainingSec % 60;
            emailOtpTimerSpan.textContent = `${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`;
        }
    }
    function clearEmailOtpTimer() {
        clearInterval(emailOtpTimerInterval);
        emailOtpTimerInterval = null;
        if(emailOtpTimerSpan) emailOtpTimerSpan.textContent = "";
    }
    function resetEmailFindState() {
        clearEmailOtpTimer();
        if(emailOtpSection) emailOtpSection.classList.remove('visible');
        if(emailOtpInput) emailOtpInput.value = '';
        if(phoneInput) phoneInput.disabled = false;
        if(findEmailActionButton) {
            findEmailActionButton.textContent = '인증하기';
            findEmailActionButton.dataset.status = 'idle';
            findEmailActionButton.disabled = false;
        }
    }

    // ======================================================
    // == 비밀번호 찾기 관련 함수들 ==
    // ======================================================
    async function handlePasswordAction() {
        const currentStatus = passwordActionButton.dataset.status;
        clearMessages('find-password-error', 'find-password-result');

    // 1. '인증번호 받기'
        if (currentStatus === 'idle') {

            const email = pwEmailInput.value;
            if (!email || !/\S+@\S+\.\S+/.test(email)) {
                showMsg('find-password-error', '올바른 이메일 주소를 입력해주세요.');
                return;
            }

            try {
                showLoadingButton(passwordActionButton, true, '요청 중...');

                // 회원 여부 확인 > 인증번호 생성 > 이메일 발송 > 인증번호 세션 저장
                const response = await fetch('/findUserInfo/reqSendEmail', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email: email })
                });

                const result = await response.json();

                if (response.ok && result.success) {
                    pwEmailInput.disabled = true;
                    passwordOtpGroup.classList.add('visible'); // OTP 그룹 표시
                    passwordOtpInput.value = ''; passwordOtpInput.focus();
                    startPasswordOtpTimer(180);
                    passwordActionButton.textContent = '인증번호 확인';
                    passwordActionButton.dataset.status = 'confirming_otp';
                    showMsg('find-password-result', '인증번호를 이메일로 발송했습니다. 메일을 확인해주세요.');
                } else {
                     showMsg('find-password-error', result.message || '가입된 이메일이 아니거나 오류가 발생했습니다.');
                }
            } catch (error) {
                console.error("Request Password OTP Error:", error);
                showMsg('find-password-error', '오류 발생 (네트워크 등)');
            } finally {
                 showLoadingButton(passwordActionButton, false);
                 if(passwordActionButton.dataset.status === 'idle') {
                     passwordActionButton.textContent = '인증번호 받기';
                 }
            }
        }
    // 2. '인증번호 확인'
        else if (currentStatus === 'confirming_otp') {
            const email = pwEmailInput.value;
            const otp = passwordOtpInput.value;
            if (!otp || otp.length !== 6) {
                showMsg('find-password-error', '인증번호 6자리를 정확히 입력해주세요.');
                return;
            }
            try {
                showLoadingButton(passwordActionButton, true, '확인 중...');
                const response = await fetch('/findUserInfo/verifyPwdCode', { // 이메일 OTP 확인 API
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email: email, code: otp })
                });

                const result = await response.json();

                if (response.ok && result.success) {
                    clearPasswordOtpTimer();
                    verifiedEmailForPasswordReset = email; // 이메일 저장

                    // UI 상태 변경: Step 1(Email), Step 2(OTP) 숨기고 Step 3(NewPW) 표시
                    if(pwEmailGroup) pwEmailGroup.style.display = 'none'; // 이메일 그룹 숨김
                    if(passwordOtpGroup) passwordOtpGroup.classList.remove('visible'); // OTP 그룹 숨김
                    if(newPasswordGroup) newPasswordGroup.classList.add('visible'); // 새 PW 그룹 표시
                    if(newPasswordGroup) newPasswordGroup.style.display = 'block'; // 새 PW 그룹 표시
                    if(newPasswordInput) newPasswordInput.focus();

                    passwordActionButton.textContent = '비밀번호 변경'; // 버튼 텍스트 변경
                    passwordActionButton.dataset.status = 'setting_new_password'; // 상태 변경 (Step 3)
                    showMsg('find-password-result', '인증 성공! 새 비밀번호를 입력해주세요.');
                } else {
                    showMsg('find-password-error', result.message || '인증번호가 올바르지 않거나 만료되었습니다.');
                    passwordOtpInput.focus();
                }
            } catch (error) {
                console.error("Verify Password OTP Error:", error);
                showMsg('find-password-error', '오류 발생 (확인 중 문제 발생)');
            } finally {
                 showLoadingButton(passwordActionButton, false);
                 if(passwordActionButton.dataset.status === 'confirming_otp') {
                     passwordActionButton.textContent = '인증번호 확인';
                 }
            }
        }
    // 3. 비밀번호 변경
        else if (currentStatus === 'setting_new_password') {
            const newPassword = newPasswordInput.value;
            const confirmPassword = confirmNewPasswordInput.value;

            if (!newPassword || !confirmPassword) {
                showMsg('find-password-error', '새 비밀번호와 확인 비밀번호를 모두 입력해주세요.'); return;
            }
            if (newPassword !== confirmPassword) {
                showMsg('find-password-error', '새 비밀번호와 확인 비밀번호가 일치하지 않습니다.'); return;
            }
            if (!verifiedEmailForPasswordReset) {
                 showMsg('find-password-error', '오류: 인증된 이메일 정보가 없습니다. 다시 시도해주세요.');
                 resetPasswordFindState(); return;
            }

            try {
                showLoadingButton(passwordActionButton, true, '변경 중...');

                // 비밀번호 재설정 API 호출
                const response = await fetch('/api/resetPwd', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email: verifiedEmailForPasswordReset, password: newPassword })
                });

                const result = await response.json();

                if (response.ok && result.success) {
                    showMsg('find-password-result', '비밀번호가 성공적으로 변경되었습니다. 1초 후 메인 페이지로 이동합니다.');
                    newPasswordInput.disabled = true;
                    confirmNewPasswordInput.disabled = true;
                    passwordActionButton.disabled = true; // 버튼 비활성화
                    setTimeout(() => { window.location.href = '/'; }, 1000);
                } else {
                    showMsg('find-password-error', result.message || '비밀번호 변경 중 오류가 발생했습니다.');
                }
            } catch (error) {
                 console.error("Reset Password Error:", error);
                 showMsg('find-password-error', '오류 발생 (네트워크 등)');
            } finally {
                 showLoadingButton(passwordActionButton, false);
                 // 성공 시 비활성화됨, 실패 시에만 텍스트 복원 필요
                 if(passwordActionButton.dataset.status === 'setting_new_password' && !passwordActionButton.disabled) {
                     passwordActionButton.textContent = '비밀번호 변경';
                 }
            }
        }
    }

    // --- 비밀번호 찾기 타이머 함수들 ---
    function startPasswordOtpTimer(durationInSeconds) {
        clearPasswordOtpTimer();
        passwordOtpEndTime = Date.now() + durationInSeconds * 1000;
        updatePasswordTimerDisplay();
        passwordOtpTimerInterval = setInterval(updatePasswordTimerDisplay, 1000);
    }
     function updatePasswordTimerDisplay() {
        const remainingMs = passwordOtpEndTime - Date.now();
        const remainingSec = Math.round(remainingMs / 1000);
        if (remainingSec <= 0) {
            passwordOtpTimerSpan.textContent = "00:00";
            clearPasswordOtpTimer();
            resetPasswordFindState();
            showMsg('find-password-error', '인증 시간이 만료되었습니다. 다시 시도해주세요.');
        } else {
            const min = Math.floor(remainingSec / 60);
            const sec = remainingSec % 60;
            passwordOtpTimerSpan.textContent = `${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`;
        }
    }
    function clearPasswordOtpTimer() {
        clearInterval(passwordOtpTimerInterval);
        passwordOtpTimerInterval = null;
        if(passwordOtpTimerSpan) passwordOtpTimerSpan.textContent = "";
    }
    function resetPasswordFindState() {
        clearPasswordOtpTimer();
        verifiedEmailForPasswordReset = null;
        if(pwEmailGroup) pwEmailGroup.style.display = 'block';
        if(passwordOtpGroup) passwordOtpGroup.classList.remove('visible');
        if(newPasswordGroup) newPasswordGroup.classList.remove('visible');
        if(newPasswordGroup) newPasswordGroup.style.display = 'none';
        if(pwEmailInput) { pwEmailInput.disabled = false; pwEmailInput.value = ''; }
        if(passwordOtpInput) passwordOtpInput.value = '';
        if(newPasswordInput) { newPasswordInput.value = ''; newPasswordInput.disabled = false; }
        if(confirmNewPasswordInput) { confirmNewPasswordInput.value = ''; confirmNewPasswordInput.disabled = false; }
        if(passwordActionButton) {
            passwordActionButton.textContent = '인증번호 받기';
            passwordActionButton.dataset.status = 'idle';
            passwordActionButton.disabled = false;
        }
    }

    // ======================================================
    // == 공통 Helper Functions ==
    // ======================================================
    function clearMessages(errorDivId = null, resultDivId = null) {
        const errorDivs = errorDivId ? [document.getElementById(errorDivId)] : [findEmailErrorDiv, findPasswordErrorDiv];
        const resultDivs = resultDivId ? [document.getElementById(resultDivId)] : [findEmailResultDiv, findPasswordResultDiv];
        errorDivs.forEach(div => { if (div) { div.style.display = 'none'; div.innerHTML = ''; } });
        resultDivs.forEach(div => { if (div) { div.style.display = 'none'; div.innerHTML = ''; } });
    }

    function showMsg(elementId, message) {
        const element = document.getElementById(elementId);
        if (element) {
            element.innerHTML = message;
            element.style.display = 'block';
        }
    }

    function hideMsg(elementId) { // 사용되지 않음, clearMessages로 대체 가능
        const element = document.getElementById(elementId);
        if (element) {
            element.innerHTML = "";
            element.style.display = 'none';
        }
    }

    // --- 로딩 버튼 표시 함수 ---
    function showLoadingButton(buttonElement, isLoading, loadingText) {
        if (buttonElement) {
            buttonElement.disabled = isLoading;
            if (isLoading) {
                 buttonElement.dataset.originalText = buttonElement.textContent;
                 buttonElement.textContent = loadingText || '처리 중...';
            } else {
                 if (buttonElement.dataset.originalText) {
                    // 성공/실패에 따라 최종 텍스트는 핸들러에서 결정하므로,
                    // 여기서는 저장된 텍스트를 삭제만 하거나, 핸들러에서 실패 시 복원하도록 함
                    // buttonElement.textContent = buttonElement.dataset.originalText;
                    delete buttonElement.dataset.originalText;
                 }
                 // reset 또는 핸들러에서 최종 텍스트 설정
            }
        }
    }

    // 초기 상태 설정
    resetEmailFindState();
    resetPasswordFindState();

});