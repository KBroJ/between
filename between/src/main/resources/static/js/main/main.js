$(document).ready(function(){

    //배너 슬라이드
    const swiper = new Swiper('.swiper', {
        loop: true,
        pagination: {
            el: '.swiper-pagination',
            clickable: true
        },
        navigation: {
            nextEl: '.swiper-button-next',
            prevEl: '.swiper-button-prev',
        },
        autoplay: {
            delay: 3000,
        },
    });

    //지도
    const container = document.getElementById('map'); //지도를 담을 영역의 DOM 레퍼런스
    const options = { //지도를 생성할 때 필요한 기본 옵션
        center: new kakao.maps.LatLng(37.506167, 127.025832), //지도의 중심좌표.
        level: 1 //지도의 레벨(확대, 축소 정도)
    };

    const map = new kakao.maps.Map(container, options); //지도 생성 및 객체 리턴


    //팝업
    const popups = document.querySelectorAll('.modal-popup'); // 또는 서버에서 받은 데이터 사용
    popups.forEach(popupElement => {
        const popupId = popupElement.id.replace('popup-', '');
        if (!getCookie('popupDontShow_' + popupId)) {
            popupElement.classList.add('show'); // CSS 클래스 추가 방식
        }
    });
});


function closePopup(popupId) {
    const popupElement = document.getElementById('popup-' + popupId);
    if (popupElement) {
        // popupElement.style.display = 'none'; // 기존 방식
        popupElement.classList.remove('show'); // CSS 클래스 제거 방식
    }
}

function handleDontShowToday(popupId) {
    const checkbox = document.getElementById('dontShowToday-' + popupId);
    if (checkbox.checked) {
        setCookie('popupDontShow_' + popupId, 'true', 1); // 1일 동안 유효한 쿠키
    }
}

// 쿠키 헬퍼 함수 (간단 예시)
/**
 * 하루 안보기 쿠키 세팅
 * @param name popupDontShow_ + 팝업 아이디
 * @param value 'true'
 * @param days  1일
 */
function setCookie(name, value, days) {
    let expires = ""; // 쿠키의 만료 시간을 저장할 변수, 기본값은 빈 문자열

    // 1. 만료 기간(days)이 제공되었는지 확인
    if (days) {
        const date = new Date(); // 현재 날짜와 시간을 가지는 Date 객체 생성

        // 2. 현재 시간에 만료일까지의 시간(밀리초 단위)을 더함
        // days * 24 (시간) * 60 (분) * 60 (초) * 1000 (밀리초)
        date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));

        // 3. Date 객체를 UTC 표준시 문자열 형태로 변환하여 expires 변수에 저장
        // 예: "Tue, 19 Jan 2038 03:14:07 GMT"
        expires = "; expires=" + date.toUTCString();
    }

    // 4. 쿠키 문자열 생성 및 document.cookie에 할당하여 쿠키 설정
    // 형식: "쿠키이름=쿠키값; expires=만료시간; path=경로"
    // (value || ""): value가 undefined, null, 빈 문자열 등 falsy 값이면 빈 문자열로 대체
    // path=/: 쿠키가 현재 도메인의 모든 경로에서 유효하도록 설정
    document.cookie = name + "=" + (value || "")  + expires + "; path=/";
}


/**
 * 하루 안보기 쿠키 가져오기
 * @param name 쿠키명
 * @returns {null|string}
 */
function getCookie(name) {
    // 1. 찾고자 하는 쿠키 이름 뒤에 '='를 붙여서 검색 패턴을 만듦
    // 예: name이 "userId"라면, nameEQ는 "userId="
    const nameEQ = name + "=";

    // 2. document.cookie는 현재 페이지에서 접근 가능한 모든 쿠키를
    //    하나의 문자열로 가지고 있으며, 각 쿠키는 세미콜론(;)으로 구분됨
    //    이 문자열을 세미콜론 기준으로 분리하여 배열로 만듦
    const ca = document.cookie.split(';'); // ca는 cookie array의 약자일 수 있음

    // 3. 분리된 쿠키 배열을 순회하며 원하는 쿠키를 찾음
    for(let i = 0; i < ca.length; i++) {
        let c = ca[i]; // 현재 순회 중인 쿠키 문자열 (예: " someCookie=someValue")

        // 4. 쿠키 문자열 앞쪽에 있을 수 있는 공백 제거
        //    " someCookie=someValue" -> "someCookie=someValue"
        while (c.charAt(0) === ' ') c = c.substring(1, c.length);

        // 5. 현재 쿠키 문자열(c)이 우리가 찾는 쿠키 이름(nameEQ)으로 시작하는지 확인
        //    c.indexOf(nameEQ) === 0: c 문자열에서 nameEQ가 첫 번째 위치(인덱스 0)에 등장하는가?
        if (c.indexOf(nameEQ) === 0) {
            // 6. 쿠키를 찾았다면, "쿠키이름=" 부분을 제외한 순수 값만 추출하여 반환
            //    c.substring(nameEQ.length, c.length)
            //    예: c가 "userId=123"이고 nameEQ가 "userId="이면,
            //        nameEQ.length는 7이므로, c.substring(7, c.length)는 "123"을 반환
            return c.substring(nameEQ.length, c.length);
        }
    }

    // 7. 루프를 모두 돌았는데도 해당 이름의 쿠키를 찾지 못하면 null 반환
    return null;
}