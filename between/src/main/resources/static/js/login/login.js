document.addEventListener('DOMContentLoaded', function() {

    // 로그인 > Remember Me 기능을 위한 쿠키 설정 및 관리
    const emailInput = document.getElementById('email'); // HTML의 이메일 필드 ID와 일치
    const rememberMeCheckbox = document.getElementById('remember_me'); // HTML의 체크박스 ID와 일치
    const loginForm = document.getElementById('loginForm'); // HTML의 form ID와 일치

    const REMEMBERED_EMAIL_COOKIE_NAME = 'rememberedUserEmailForField'; // 쿠키 이름
    const REMEMBER_PREFERENCE_COOKIE_NAME = 'rememberMePreferenceForField'; // 쿠키 이름

    // 쿠키 설정 함수
    function setCookie(name, value, days) {
        let expires = "";

        if (days) {
            const date = new Date();
            date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
            expires = "; expires=" + date.toUTCString();
        }

        let cookieString = name + "=" + (value || "")  + expires + "; path=/; SameSite=Lax";

        // HTTPS 환경에서만 Secure 플래그 추가
        if (window.location.protocol === "https:") {
           cookieString += "; Secure";
        }
        document.cookie = cookieString;
    }

    // 쿠키 읽기 함수
    function getCookie(name) {
        const nameEQ = name + "=";
        const ca = document.cookie.split(';');

        for(let i = 0; i < ca.length; i++) {
            let c = ca[i];
            while (c.charAt(0) === ' ') c = c.substring(1, c.length);
            if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length, c.length);
        }
        return null;
    }

    // 쿠키 삭제 함수
    function eraseCookie(name) {
        let cookieString = name +'=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT; SameSite=Lax';
        if (window.location.protocol === "https:") {
            cookieString += "; Secure";
        }
        document.cookie = cookieString;
    }

    // 페이지 로드 시: 쿠키 확인 및 이메일/체크박스 설정
    if (emailInput && rememberMeCheckbox) {
        const rememberPreference = getCookie(REMEMBER_PREFERENCE_COOKIE_NAME);

        if (rememberPreference === 'true') {
            const rememberedEmail = getCookie(REMEMBERED_EMAIL_COOKIE_NAME);

            if (rememberedEmail) {
                emailInput.value = decodeURIComponent(rememberedEmail); // 쿠키 값 디코딩
            }

            rememberMeCheckbox.checked = true;
        } else {
            rememberMeCheckbox.checked = false;
            // 명시적으로 'false'일 때 또는 쿠키가 없을 때 이메일 쿠키를 삭제할 수도 있지만,
            // 사용자가 체크를 풀고 로그인하지 않은 상태로 페이지를 떠났다가 다시 돌아왔을 때
            // 이메일이 남아있길 원할 수도 있으므로, 여기서는 체크박스 상태만 반영합니다.
            // 이메일 쿠키 삭제는 아래 폼 제출 시점에서 명확히 처리합니다.
        }
    }

    // 로그인 폼 제출 시: 쿠키 저장 또는 삭제
    if (loginForm && emailInput && rememberMeCheckbox) {
        loginForm.addEventListener('submit', function() {
            // 이 이벤트는 폼이 실제로 서버로 제출되기 직전에 발생합니다.
            if (rememberMeCheckbox.checked) {
                const emailValue = emailInput.value;
                if (emailValue) { // 이메일 값이 있을 때만 저장
                    setCookie(REMEMBERED_EMAIL_COOKIE_NAME, encodeURIComponent(emailValue), 30); // 이메일 인코딩하여 저장 (30일)
                    setCookie(REMEMBER_PREFERENCE_COOKIE_NAME, 'true', 30);
                } else {
                    // 이메일이 비어있는데 '기억하기'가 체크된 경우, 쿠키 삭제
                    eraseCookie(REMEMBERED_EMAIL_COOKIE_NAME);
                    eraseCookie(REMEMBER_PREFERENCE_COOKIE_NAME);
                }
            } else {
                // '기억하기'가 체크되지 않았으면 쿠키 삭제
                eraseCookie(REMEMBERED_EMAIL_COOKIE_NAME);
                eraseCookie(REMEMBER_PREFERENCE_COOKIE_NAME);
            }
        // 폼의 기본 제출 동작은 여기서 막지 않으므로, 정상적으로 서버에 제출됩니다.
        });
    }
});