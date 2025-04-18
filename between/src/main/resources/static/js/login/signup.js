document.addEventListener('DOMContentLoaded', function() {

    const form = document.getElementById('signupForm');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    const nameInput = document.getElementById('name');
    const phoneNumberInput = document.getElementById('phoneNo');
    const checkEmailBtn = document.getElementById('checkEmailBtn');
    const sendVerificationBtn = document.getElementById('sendVerificationBtn');
    const verificationSection = document.getElementById('verificationSection');
    const verificationCodeInput = document.getElementById('verificationCode');
    const countdownSpan = document.getElementById('countdown');
    const registerBtn = document.getElementById('registerBtn');
    const resendVerificationBtn = document.getElementById('resendVerificationBtn'); // *** 재전송 버튼 추가

    // 메시지 요소들
    const emailMessage = document.getElementById('emailMessage');
    const passwordMessage = document.getElementById('passwordMessage');
    const confirmPasswordMessage = document.getElementById('confirmPasswordMessage');
    const nameMessage = document.getElementById('nameMessage');
    const phoneMessage = document.getElementById('phoneMessage');
    const verificationMessage = document.getElementById('verificationMessage');

    // 비밀번호 보이기/숨기기 버튼
    const togglePasswordBtn = document.getElementById('togglePassword');
    const toggleConfirmPasswordBtn = document.getElementById('toggleConfirmPassword');

    let countdownTimer;
    let isEmailVerified = false;        // 이메일 중복 확인 여부
    let isPhoneVerified = false;        // 휴대폰 번호 인증 여부 (최종 제출 시 사용)
    let isVerificationExpired = false;  // 인증번호 입력 카운트다운 만료 여부

// 이메일 중복 확인 버튼 클릭 이벤트
    checkEmailBtn.addEventListener('click', function() {
        const email = emailInput.value;
        if (!email) {
            emailMessage.textContent = '이메일을 입력해주세요.';
            emailMessage.style.color = '#dc3545';
            return;
        }

        if (!isValidEmail(email)) {
            emailMessage.textContent = '유효한 이메일 형식이 아닙니다.';
            emailMessage.style.color = '#dc3545';
            return;
        }

        fetch('/checkEmail', {  // POST 방식
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ email: email })
        })
        .then(response => {
            if (!response.ok) {
              throw new Error('HTTP status ' + response.status);
            }
            return response.json();
        })
        .then(data => {

            console.log("checkEmail||RES" + JSON.stringify(data));

            if (data.available) {
                emailMessage.textContent = '사용 가능한 이메일입니다.';
                emailMessage.style.color = 'green';
                isEmailVerified = true;
            } else {
                emailMessage.textContent = '이미 사용 중인 이메일입니다.';
                emailMessage.style.color = '#dc3545';
                isEmailVerified = false;
            }
        })
        .catch(error => {
        });
    });

// 휴대폰 번호 입력 시 숫자만 허용하고 자동으로 하이픈 추가
    /*
    */
    phoneNumberInput.addEventListener('input', function(e) {
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

        if (this.value) {
            phoneMessage.textContent = '';
        }
    });

    // '인증하기' 버튼 클릭 이벤트
    sendVerificationBtn.addEventListener('click', function() {
        // 이메일, 비밀번호 등 기본 정보 유효성 검사
        if (!validateAllInputs()) {
            return;
        }
        // 이메일 중복 확인 여부 검증
        if (!isEmailVerified) {
            emailMessage.textContent = '이메일 중복 확인이 필요합니다.';
            emailMessage.style.color = '#dc3545';
            return;
        }
        // 휴대폰 번호 유효성 검사 (형식만)
         if (!isValidPhoneNumber(phoneNumberInput.value)) {
             phoneMessage.textContent = '유효한 휴대폰 번호 형식이 아닙니다.';
             phoneMessage.style.color = '#dc3545';
             return;
         }

        requestVerificationCode(); // 인증번호 발송 함수 호출
    });

    // *** '재전송' 버튼 클릭 이벤트 ***
    resendVerificationBtn.addEventListener('click', function() {
        // 휴대폰 번호 유효성 검사 (형식만)
         if (!isValidPhoneNumber(phoneNumberInput.value)) {
             phoneMessage.textContent = '유효한 휴대폰 번호 형식이 아닙니다.';
             phoneMessage.style.color = '#dc3545';
             return;
         }
        requestVerificationCode(); // 인증번호 발송 함수 재호출
    });

    // 휴대폰 번호 인증번호 전송 버튼 클릭 이벤트
    function requestVerificationCode() {
        const phoneNo = phoneNumberInput.value.replace(/[^0-9]/g, '');
        if (!phoneNo) {
            phoneMessage.textContent = '휴대폰 번호를 입력해주세요.';
            phoneMessage.style.color = '#dc3545';
            return; // 전화번호 없으면 중단
        }

        // 서버에 인증번호 발송 요청
        fetch('/send-verification', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ phoneNo: phoneNo })
        })
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json(); // 서버 응답이 있다면 JSON 처리
        })
        .then(() => { // 성공 시 UI 변경
            verificationSection.style.display = 'block'; // 인증번호 입력칸 보이기
            registerBtn.style.display = 'block';       // 회원가입 버튼 보이기
            sendVerificationBtn.style.display = 'none';  // '인증하기' 버튼 숨기기
            resendVerificationBtn.style.display = 'inline-block'; // '재전송' 버튼 보이기
            countdownSpan.style.display = 'inline';    // 카운트다운 보이기
            startCountdown(180); // 3분 카운트다운 시작
            isVerificationExpired = false; // 만료 상태 초기화
            verificationMessage.textContent = ''; // 이전 메시지 초기화
            verificationCodeInput.disabled = false; // 입력칸 활성화
            verificationCodeInput.value = ''; // 입력칸 초기화
            alert('인증번호가 발송되었습니다.');
        })
        .catch(error => {
            console.error('Verification send error:', error);
            phoneMessage.textContent = '인증번호 발송 중 오류가 발생했습니다.';
            phoneMessage.style.color = '#dc3545';
        });
    }

    // 비밀번호 보기/숨기기 토글 함수
    function setupPasswordToggle(toggleBtn, passwordInput) {
        const eyeIcon = toggleBtn.querySelector('.bi-eye-fill');
        const eyeSlashIcon = toggleBtn.querySelector('.bi-eye-slash-fill');

        toggleBtn.addEventListener('click', function() {
            // 입력 필드의 type 속성 확인 및 변경
            const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordInput.setAttribute('type', type);

            // 아이콘 변경
            if (type === 'password') {
                eyeIcon.style.display = 'block';
                eyeSlashIcon.style.display = 'none';
            } else {
                eyeIcon.style.display = 'none';
                eyeSlashIcon.style.display = 'block';
            }
        });
    }

    // 각 비밀번호 필드에 토글 기능 설정
    if (togglePasswordBtn && passwordInput) {
        setupPasswordToggle(togglePasswordBtn, passwordInput);
    }
    if (toggleConfirmPasswordBtn && confirmPasswordInput) {
        setupPasswordToggle(toggleConfirmPasswordBtn, confirmPasswordInput);
    }

    // 카운트다운 타이머 시작 함수
    function startCountdown(duration) {
        let timer = duration;
        countdownSpan.style.display = 'inline';

        // 이전 타이머가 있으면 초기화
        if (countdownTimer) {
            clearInterval(countdownTimer);
        }

        countdownTimer = setInterval(
            function() {
                let minutes = parseInt(timer / 60, 10);
                let seconds = parseInt(timer % 60, 10);

                minutes = minutes < 10 ? "0" + minutes : minutes;
                seconds = seconds < 10 ? "0" + seconds : seconds;

                countdownSpan.textContent = minutes + ":" + seconds;

                if (--timer < 0) {
                    clearInterval(countdownTimer);
                    countdownSpan.textContent = "시간 만료";

                    isVerificationExpired = true;

                    // 회원가입 버튼을 인증하기 버튼으로 변경
                    registerBtn.style.display = 'none';             // 회원가입 버튼 숨김
                    resendVerificationBtn.style.display = 'none';   // 재전송 버튼 숨김
                    sendVerificationBtn.style.display = 'block';    // 인증하기 버튼 보이기
                    verificationMessage.textContent = '인증 시간이 만료되었습니다. 다시 인증해주세요.';
                }
            }, 1000);
    }

    // 폼 제출 이벤트
    form.addEventListener('submit', function(event) {
        event.preventDefault(); // 기본 제출 방지

    // 최종 유효성 검사 (인증번호 제외한 모든 필드)
        if (!validateAllInputs()) {
            return;
        }
        if (!isEmailVerified) {
             emailMessage.textContent = '이메일 중복 확인이 필요합니다.';
             emailMessage.style.color = '#dc3545';
             return;
        }

        const verificationCode = verificationCodeInput.value;
        if (!verificationCode) {
            verificationMessage.textContent = '인증번호를 입력해주세요.';
            verificationMessage.style.color = '#dc3545';
            return;
        }

        // *** 시간 만료 체크 ***
        if (isVerificationExpired) {
            verificationMessage.textContent = '인증 시간이 만료되었습니다. 인증번호를 다시 받아주세요.';
            verificationMessage.style.color = '#dc3545';
            return;
        }

        // 서버에 인증번호 검증 요청
        fetch('/signup/verify-code', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                phoneNo: phoneNumberInput.value.replace(/[^0-9]/g, ''),  // 하이픈 제거
                code: verificationCode
            })
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('HTTP status ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            if (data.valid) {
                // 인증 성공
                verificationMessage.textContent = '인증이 완료되었습니다.';
                verificationMessage.style.color = 'green';
                isPhoneVerified = true;

                // 카운트다운 중지
                if (countdownTimer) {
                    clearInterval(countdownTimer);
                    countdownSpan.textContent = '인증완료';
                    countdownSpan.style.color = 'green';
                    resendVerificationBtn.style.display = 'none'; // 인증 완료 시 재전송 버튼 숨김
                }

                this.submit();
            } else {
                // 인증 실패
                verificationMessage.textContent = '인증번호가 올바르지 않습니다.';
                verificationMessage.style.color = '#dc3545';
                isPhoneVerified = false;
            }
        })
        .catch(error => {
            console.error('Error:', error);
            verificationMessage.textContent = '서버 오류가 발생했습니다.';
            verificationMessage.style.color = '#dc3545';
        });

    });

    // 모든 필수 입력값 검증 함수
    function validateAllInputs() {
        let isValid = true;

        // 이메일 검증
        const email = emailInput.value; // trim 제거 (공백 검사는 실시간 리스너에서)
        emailMessage.textContent = ''; // 메시지 초기화
        emailMessage.style.color = '#dc3545'; // 기본 색상

        if (!email) {
            emailMessage.textContent = '이메일을 입력해주세요.';
            isValid = false;
        } else if (/\s/.test(email)) { // 공백 체크 추가
            emailMessage.textContent = '이메일에 공백은 포함될 수 없습니다.';
            isValid = false;
        } else if (!isValidEmail(email)) {
            emailMessage.textContent = '유효한 이메일 형식이 아닙니다.';
            isValid = false;
        }

        // 비밀번호 검증
        const password = passwordInput.value;
        passwordMessage.textContent = '';
        if (!password) {
            passwordMessage.textContent = '비밀번호를 입력해주세요.';
            isValid = false;
        } else if (!isValidPwd(passwordInput.value) && passwordInput.value.length < 8) {
            passwordMessage.textContent = '비밀번호는 영문, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.';
            isValid = false;
        } else if (!isValidPwd(passwordInput.value)) {
            passwordMessage.textContent = '비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.';
            isValid = false;
        } else if (passwordInput.value.length < 8) {
            passwordMessage.textContent = '비밀번호는 8자 이상이어야 합니다.';
            isValid = false;
        }

        // 비밀번호 확인 검증
        const confirmPassword = confirmPasswordInput.value;
        confirmPasswordMessage.textContent = '';
        if (!confirmPassword) {
            confirmPasswordMessage.textContent = '비밀번호 확인을 입력해주세요.';
            isValid = false;
        } else if (password !== confirmPassword) {
            confirmPasswordMessage.textContent = '비밀번호가 일치하지 않습니다.';
            isValid = false;
        }

        // 이름 검증
        const name = nameInput.value.trim();
        nameMessage.textContent = '';
        if (!name) {
            nameMessage.textContent = '이름을 입력해주세요.';
            isValid = false;
        } else if (!isValidName(name)) {
            nameMessage.textContent = '이름은 한글 또는 영문자만 입력 가능합니다.';
            isValid = false;
        }

        // 휴대폰 번호 검증
        const phoneNo = phoneNumberInput.value;
        phoneMessage.textContent = '';
        if (!phoneNo) {
            phoneMessage.textContent = '휴대폰 번호를 입력해주세요.';
            isValid = false;
        } else if (!isValidPhoneNumber(phoneNo)) {
            phoneMessage.textContent = '유효한 휴대폰 번호 형식이 아닙니다.';
            isValid = false;
        }

        return isValid;
    }

    // 이메일 유효성 검사 함수
    function isValidEmail(email) {
        /*
            - ^: 문자열의 시작, $: 문자열의 끝
            - [^\s@]+: 공백과 '@'를 제외한 문자 1개 이상
            - @: '@' 문자
            - [^\s@]+: 공백과 '@'를 제외한 문자 1개 이상
            - \.: '.' 문자
            - [^\s@]+: 공백과 '@'를 제외한 문자 1개 이상
            => 즉, 이메일 형식이 맞는지 확인하는 정규 표현식
        */
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    // 비밀번호 유효성 검사 함수
    function isValidPwd(pwd) {
    /*
        - ^: 문자열의 시작
        - (?=.*[A-Za-z]): 영문자가 최소 1개 이상 포함
        - (?=.*\d): 숫자가 최소 1개 이상 포함
        - (?=.*[@$!%*#?&]): 특수문자(@, $, !, %, *, #, ?, &)가 최소 1개 이상 포함
        - [A-Za-z\d@$!%*#?&]{8,}: 영문자, 숫자, 특수문자(@, $, !, %, *, #, ?, &) 중 하나 이상을 포함하며, 8자 이상
        - $: 문자열의 끝
        => 즉, 영문자, 숫자, 특수문자가 각각 최소 1개 이상 포함되어야 하며, 전체 길이는 8자 이상이어야 함
    */
        const pwdRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/;
        return pwdRegex.test(pwd);
    }

     // 이름 유효성 검사 함수 추가 (한글, 영문만 허용)
    function isValidName(name) {
        // ^ : 문자열 시작
        // [가-힣A-Za-z] : 한글 또는 영문자
        // + : 1회 이상 반복
        // $ : 문자열 끝
        // 즉, 문자열 전체가 한글 또는 영문자로만 구성되어야 함
        const nameRegex = /^[가-힣A-Za-z]+$/;
        return nameRegex.test(name);
    }

    // 휴대폰 번호 유효성 검사 함수
    function isValidPhoneNumber(phoneNo) {
        // 한국 휴대폰 번호 형식 (010-1234-5678 또는 01012345678)
        // 현재 로직은 숫자 입력 시 자동으로 하이픈 추가 되고 백단에서 받을 땐 하이픈 제거 방식으로 되어 있음
        const phoneRegex = /^01([0|1|6|7|8|9])-?([0-9]{3,4})-?([0-9]{4})$/;
        return phoneRegex.test(phoneNo);
    }


    // 입력 필드 실시간 이벤트 리스너 (실시간 검증)
    emailInput.addEventListener('input', function() {

        const email = this.value;
        emailMessage.textContent = ''; // 입력 시 메시지 초기화
        emailMessage.style.color = '#dc3545';
        isEmailVerified = false; // 이메일 변경 시 중복 확인 상태 초기화

        if (/\s/.test(email)) {                                             // 1. 공백이 포함된 경우
            emailMessage.textContent = '이메일에 공백은 포함될 수 없습니다.';
        } else if (email && !isValidEmail(email.trim())) {                  // 2. 유효하지 않은 이메일 형식이면 메시지 출력
            emailMessage.textContent = '유효한 이메일 형식이 아닙니다.';
        }

    });

    passwordInput.addEventListener('input', function() {

        if (this.value) {
            passwordMessage.textContent = '';
        }

        // 비밀번호 확인 필드가 비어있지 않으면 일치 여부 확인
        if (confirmPasswordInput.value) {
            if (this.value === confirmPasswordInput.value) {
                confirmPasswordMessage.textContent = '';
            } else {
                confirmPasswordMessage.textContent = '비밀번호가 일치하지 않습니다.';
            }
        }
    });

    confirmPasswordInput.addEventListener('input', function() {
        if (this.value && this.value === passwordInput.value) {
            confirmPasswordMessage.textContent = '';
        } else if (this.value) {
            confirmPasswordMessage.textContent = '비밀번호가 일치하지 않습니다.';
        }
    });

    nameInput.addEventListener('input', function() {

        const name = this.value.trim();
        nameMessage.textContent = ''; // 입력 시 메시지 초기화

        if (!name) {
            // 입력값이 없으면 메시지 초기화
            nameMessage.textContent = '';
        } else if (!isValidName(name)) {
            // 유효하지 않은 이름 형식이면 메시지 출력
            nameMessage.textContent = '이름은 한글 또는 영문자만 입력 가능합니다.';
        }

    });

    phoneNumberInput.addEventListener('input', function() {
        if (this.value) {
            phoneMessage.textContent = '';
        }
    });

});
