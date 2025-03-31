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

    // 메시지 요소들
    const emailMessage = document.getElementById('emailMessage');
    const passwordMessage = document.getElementById('passwordMessage');
    const confirmPasswordMessage = document.getElementById('confirmPasswordMessage');
    const nameMessage = document.getElementById('nameMessage');
    const phoneMessage = document.getElementById('phoneMessage');
    const verificationMessage = document.getElementById('verificationMessage');

    let countdownTimer;
    let isEmailVerified = false;
    let isVerificationExpired = false;

    // 이메일 중복 확인 버튼 클릭 이벤트
    checkEmailBtn.addEventListener('click', function() {
        const email = emailInput.value;
        if (!email) {
            emailMessage.textContent = '이메일을 입력해주세요.';
            return;
        }

        if (!isValidEmail(email)) {
            emailMessage.textContent = '유효한 이메일 형식이 아닙니다.';
            return;
        }

        // 실제 구현에서는 서버에 요청을 보내야 합니다
//        fetch('/checkEmail?email=' + encodeURIComponent(email))   // GET 방식
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
            // 테스트용 코드 (실제로는 서버 통신 필요)
            emailMessage.textContent = '사용 가능한 이메일입니다.';
            emailMessage.style.color = 'green';
            isEmailVerified = true;
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

    // 인증하기 버튼 클릭 이벤트
    sendVerificationBtn.addEventListener('click', function() {
        // 모든 필수 입력값 검증
        if (!validateAllInputs()) {
            return;
        }

        // 이메일 중복 확인 여부 검증
        if (!isEmailVerified) {
            emailMessage.textContent = '이메일 중복 확인이 필요합니다.';
            return;
        }

        // 실제 구현에서는 서버에 요청을 보내야 합니다
        fetch('/send-verification', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({phoneNo: phoneNumberInput.value})
        })
        .then(response => response.json())
        .then(() => {
            verificationSection.style.display = 'block';
            registerBtn.style.display = 'block';
            sendVerificationBtn.style.display = 'none';
            startCountdown(180); // 3분 카운트다운

            alert('인증번호가 발송되었습니다.');
        })
        .catch(() => {
            // 테스트용 코드 (실제로는 서버 통신 필요)
            verificationSection.style.display = 'block';
            registerBtn.style.display = 'block';
            sendVerificationBtn.style.display = 'none';
            startCountdown(180); // 3분 카운트다운

            alert('인증번호가 발송되었습니다.');
        });
    });

    // 카운트다운 타이머 시작 함수
    function startCountdown(duration) {
        let timer = duration;
        countdownSpan.style.display = 'inline';

        // 이전 타이머가 있으면 초기화
        if (countdownTimer) {
            clearInterval(countdownTimer);
        }

        countdownTimer = setInterval(function() {
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
                registerBtn.style.display = 'none';
                sendVerificationBtn.style.display = 'block';
                verificationMessage.textContent = '인증 시간이 만료되었습니다. 다시 인증해주세요.';
            }
        }, 1000);
    }

    // 폼 제출 이벤트
    form.addEventListener('submit', function(event) {
        event.preventDefault();

        // 인증번호 확인 (실제로는 서버에서 검증해야 함)
        const verificationCode = verificationCodeInput.value;
        if (!verificationCode) {
            verificationMessage.textContent = '인증번호를 입력해주세요.';
            return;
        }

        // 여기서는 예시로 "1234"가 올바른 인증번호라고 가정
        if (verificationCode === "1234") {
            // 실제 구현에서는 서버에 폼 데이터를 제출합니다
            this.submit();
        } else {
            verificationMessage.textContent = '인증번호가 올바르지 않습니다.';
        }
    });

    // 모든 필수 입력값 검증 함수
    function validateAllInputs() {
        let isValid = true;

        // 이메일 검증
        if (!emailInput.value) {
            emailMessage.textContent = '이메일을 입력해주세요.';
            isValid = false;
        } else if (!isValidEmail(emailInput.value)) {
            emailMessage.textContent = '유효한 이메일 형식이 아닙니다.';
            isValid = false;
        }

        // 비밀번호 검증
        if (!passwordInput.value) {
            passwordMessage.textContent = '비밀번호를 입력해주세요.';
            isValid = false;
        } else if (passwordInput.value.length < 8) {
            passwordMessage.textContent = '비밀번호는 8자 이상이어야 합니다.';
            isValid = false;
        }

        // 비밀번호 확인 검증
        if (!confirmPasswordInput.value) {
            confirmPasswordMessage.textContent = '비밀번호 확인을 입력해주세요.';
            isValid = false;
        } else if (passwordInput.value !== confirmPasswordInput.value) {
            confirmPasswordMessage.textContent = '비밀번호가 일치하지 않습니다.';
            isValid = false;
        }

        // 이름 검증
        if (!nameInput.value) {
            nameMessage.textContent = '이름을 입력해주세요.';
            isValid = false;
        }

        // 휴대폰 번호 검증
        if (!phoneNumberInput.value) {
            phoneMessage.textContent = '휴대폰 번호를 입력해주세요.';
            isValid = false;
        } else if (!isValidPhoneNumber(phoneNumberInput.value)) {
            phoneMessage.textContent = '유효한 휴대폰 번호 형식이 아닙니다.';
            isValid = false;
        }

        return isValid;
    }

    // 이메일 유효성 검사 함수
    function isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    // 휴대폰 번호 유효성 검사 함수
    function isValidPhoneNumber(phoneNo) {
        // 한국 휴대폰 번호 형식 (010-1234-5678 또는 01012345678)
        const phoneRegex = /^01([0|1|6|7|8|9])-?([0-9]{3,4})-?([0-9]{4})$/;
        return phoneRegex.test(phoneNo);
    }



    // 입력 필드 이벤트 리스너 (실시간 검증)
    emailInput.addEventListener('input', function() {
        if (this.value) {
            emailMessage.textContent = '';
        }
        isEmailVerified = false; // 이메일이 변경되면 다시 중복 확인 필요
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
        if (this.value) {
            nameMessage.textContent = '';
        }
    });

    phoneNumberInput.addEventListener('input', function() {
        if (this.value) {
            phoneMessage.textContent = '';
        }
    });
});
