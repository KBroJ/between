document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('signupForm');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    const nameInput = document.getElementById('name');
    const phoneNumberInput = document.getElementById('phoneNumber');
    const checkDuplicationBtn = document.getElementById('checkDuplication');
    const verifyButton = document.getElementById('verifyButton');
    const verificationSection = document.getElementById('verificationSection');
    const verificationCodeInput = document.getElementById('verificationCode');
    const countdownTimer = document.getElementById('countdownTimer');

    // 각 입력란의 검증 메시지 엘리먼트 생성
    const validationElements = {
        email: createValidationElement(emailInput, 'after'),
        password: createValidationElement(passwordInput),
        confirmPassword: createValidationElement(confirmPasswordInput),
        name: createValidationElement(nameInput),
        phoneNumber: createValidationElement(phoneNumberInput)
    };

    let isEmailUnique = false;
    let isPhoneVerified = false;
    let verificationCountdown;

    // 검증 메시지 엘리먼트 생성 함수
    function createValidationElement(inputElement, position = 'after') {
        const validationElement = document.createElement('span');
        validationElement.classList.add('text-sm', 'text-red-500', 'mt-1');
        
        if (inputElement.id === 'email') {
            // 이메일 입력란의 경우, 부모의 부모 요소에 추가합니다.
            inputElement.parentNode.parentNode.appendChild(validationElement);
        } else if (inputElement.id === 'verificationCode') {
            inputElement.parentNode.parentNode.appendChild(validationElement);
        } else if (position === 'after') {
            inputElement.parentNode.insertBefore(validationElement, inputElement.nextSibling);
        } else {
            inputElement.parentNode.appendChild(validationElement);
        }
        
        return validationElement;
    }

    // 입력란 검증 함수들
    function validateEmail() {
        const email = emailInput.value.trim();
        if (!email) {
            validationElements.email.textContent = '이메일을 입력해주세요.';
            return false;
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            validationElements.email.textContent = '올바른 이메일 형식이 아닙니다.';
            return false;
        }

        validationElements.email.textContent = '';
        return true;
    }

    function validatePassword() {
        const password = passwordInput.value;
        const confirmPassword = confirmPasswordInput.value;

        if (!password) {
            validationElements.password.textContent = '비밀번호를 입력해주세요.';
            return false;
        }

        if (password.length < 8) {
            validationElements.password.textContent = '비밀번호는 최소 8자 이상이어야 합니다.';
            return false;
        }

        if (confirmPassword && password !== confirmPassword) {
            validationElements.confirmPassword.textContent = '비밀번호가 일치하지 않습니다.';
            return false;
        }

        validationElements.password.textContent = '';
        validationElements.confirmPassword.textContent = '';
        return true;
    }

    function validateConfirmPassword() {
        const password = passwordInput.value;
        const confirmPassword = confirmPasswordInput.value;

        if (!confirmPassword) {
            validationElements.confirmPassword.textContent = '비밀번호 확인을 입력해주세요.';
            return false;
        }

        if (password !== confirmPassword) {
            validationElements.confirmPassword.textContent = '비밀번호가 일치하지 않습니다.';
            return false;
        }

        validationElements.confirmPassword.textContent = '';
        return true;
    }

    function validateName() {
        const name = nameInput.value.trim();
        if (!name) {
            validationElements.name.textContent = '이름을 입력해주세요.';
            return false;
        }

        validationElements.name.textContent = '';
        return true;
    }

    function validatePhoneNumber() {
        const phoneNumber = phoneNumberInput.value.replace(/\D/g, '');
        if (!phoneNumber) {
            validationElements.phoneNumber.textContent = '휴대폰 번호를 입력해주세요.';
            return false;
        }

        if (phoneNumber.length !== 11) {
            validationElements.phoneNumber.textContent = '올바른 휴대폰 번호 형식이 아닙니다.';
            return false;
        }

        validationElements.phoneNumber.textContent = '';
        phoneNumberInput.value = phoneNumber.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3');
        return true;
    }


// 이벤트 리스너 등록
    // 이메일 입력 및 검증
    emailInput.addEventListener('blur', () => {
        const email = emailInput.value.trim();
        if (!email) {
            validationElements.email.textContent = '이메일을 입력해주세요.';
            checkDuplicationBtn.disabled = true;
            isEmailUnique = false;
        } else {
            // 이메일 형식 검증
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(email)) {
                validationElements.email.textContent = '올바른 이메일 형식이 아닙니다.';
                checkDuplicationBtn.disabled = true;
                isEmailUnique = false;
            } else {
                validationElements.email.textContent = '';
                checkDuplicationBtn.disabled = false;
            }
        }
    });

    // 비밀번호 입력 및 검증
    passwordInput.addEventListener('blur', () => {
        const password = passwordInput.value;
        if (!password) {
            validationElements.password.textContent = '비밀번호를 입력해주세요.';
        } else if (password.length < 8) {
            validationElements.password.textContent = '비밀번호는 최소 8자 이상이어야 합니다.';
        } else {
            validationElements.password.textContent = '';
        }
        
        // 비밀번호 확인란과 비교
        const confirmPassword = confirmPasswordInput.value;
        if (confirmPassword && password !== confirmPassword) {
            validationElements.confirmPassword.textContent = '비밀번호가 일치하지 않습니다.';
        }
    });

    // 비밀번호 확인 입력 및 검증
    confirmPasswordInput.addEventListener('blur', () => {
        const password = passwordInput.value;
        const confirmPassword = confirmPasswordInput.value;
        
        if (!confirmPassword) {
            validationElements.confirmPassword.textContent = '비밀번호 확인을 입력해주세요.';
        } else if (password !== confirmPassword) {
            validationElements.confirmPassword.textContent = '비밀번호가 일치하지 않습니다.';
        } else {
            validationElements.confirmPassword.textContent = '';
        }
    });

    // 이름 입력 및 검증
    nameInput.addEventListener('blur', () => {
        const name = nameInput.value.trim();
        if (!name) {
            validationElements.name.textContent = '이름을 입력해주세요.';
        } else {
            validationElements.name.textContent = '';
        }
    });

    // 휴대폰 번호 입력 및 검증
    phoneNumberInput.addEventListener('blur', () => {
        const phoneNumber = phoneNumberInput.value.replace(/\D/g, '');
        if (!phoneNumber) {
            validationElements.phoneNumber.textContent = '휴대폰 번호를 입력해주세요.';
        } else if (phoneNumber.length !== 11) {
            validationElements.phoneNumber.textContent = '올바른 휴대폰 번호 형식이 아닙니다.';
        } else {
            validationElements.phoneNumber.textContent = '';
        }
        
        // 입력 시 자동 하이픈 추가
        phoneNumberInput.value = phoneNumber.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3');
    });


    // 이메일 중복 확인
    checkDuplicationBtn.addEventListener('click', () => {
        if (validateEmail()) {
            // TODO: 백엔드 API 호출하여 이메일 중복 확인
            // 현재는 시뮬레이션
            validationElements.email.textContent = '사용 가능한 이메일입니다.';
            validationElements.email.classList.remove('text-red-500');
            validationElements.email.classList.add('text-green-500');
            checkDuplicationBtn.disabled = true;
            emailInput.readOnly = true;
            isEmailUnique = true;
        }
    });

    // 휴대폰 번호 입력 시 자동 하이픈 추가
    phoneNumberInput.addEventListener('input', () => {
        const phoneNumber = phoneNumberInput.value.replace(/\D/g, '');
        phoneNumberInput.value = phoneNumber.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3');
    });

    // 인증하기 버튼 클릭 이벤트
    verifyButton.addEventListener('click', () => {
        // 모든 입력란 검증
        const isEmailValid = validateEmail();
        const isPasswordValid = validatePassword();
        const isConfirmPasswordValid = validateConfirmPassword();
        const isNameValid = validateName();
        const isPhoneNumberValid = validatePhoneNumber();

        if (isEmailValid && isPasswordValid && isConfirmPasswordValid && 
            isNameValid && isPhoneNumberValid && isEmailUnique) {
            // 휴대폰 인증 절차 시작
            verificationSection.classList.remove('hidden');
            verifyButton.textContent = '회원가입';
            startVerificationCountdown();
        }
    });

    // 인증번호 카운트다운 함수
    function startVerificationCountdown() {
        let time = 180; // 3분
        clearInterval(verificationCountdown);
        
        verificationCountdown = setInterval(() => {
            const minutes = Math.floor(time / 60);
            const seconds = time % 60;
            countdownTimer.textContent = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
            
            if (time <= 0) {
                clearInterval(verificationCountdown);
                countdownTimer.textContent = '만료';
                verificationSection.classList.add('hidden');
                verifyButton.textContent = '인증하기';
                isPhoneVerified = false;
            }
            time--;
        }, 1000);
    }

    // 회원가입 버튼 클릭 이벤트 (인증 후 변경된 버튼)
    verifyButton.addEventListener('click', (e) => {
        // 회원가입 로직
        if (verifyButton.textContent === '회원가입') {
            const verificationCode = verificationCodeInput.value;
            
            // TODO: 인증번호 검증 로직 추가
            if (!verificationCode) {
                const verificationValidation = validationElements.verificationCode || createValidationElement(verificationCodeInput);
                verificationValidation.textContent = '인증번호를 입력해주세요.';
                return;
            }

            // 회원가입 데이터 준비
            const signupData = {
                email: emailInput.value,
                password: passwordInput.value,
                name: nameInput.value,
                phoneNumber: phoneNumberInput.value,
                verificationCode: verificationCode
            };

            console.log('회원가입 데이터:', signupData);
            alert('회원가입이 완료되었습니다.');
        }
    });
});