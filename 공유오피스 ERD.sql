DROP TABLE `User`;
DROP TABLE `Auth`;
DROP TABLE `Payment`;
DROP TABLE `Menu`;
DROP TABLE `Banner`;
DROP TABLE `Faq`;
DROP TABLE `Coupon`;
DROP TABLE `Price`;
DROP TABLE `Reservation`;
DROP TABLE `UserCoupon`;
DROP TABLE `Seat`;
DROP TABLE `AuthMenu`;


CREATE TABLE `User` (
	`userNo` bigint NOT NULL AUTO_INCREMENT COMMENT '회원가입 시 식별번호 자동 생성(시퀀스)' ,
	`email` varchar(100) UNIQUE NOT NULL COMMENT '사용자 관점에서 ID로 사용됨',
	`password` varchar(255) NOT NULL COMMENT '영문 대소문자, 특수문자 , 숫자 조합해서 저장 대신 공백 불가',
	`name` varchar(50) NOT NULL,
	`phoneNo` varchar(11) NOT NULL COMMENT '숫자만 저장',
	`userStts` varchar(10) NOT NULL COMMENT '일반, 휴면, 탈퇴',
	`authCd` VARCHAR(10) NOT NULL COMMENT '일반, 임직원, 관리자 등',
	`createDt` timestamp NOT NULL COMMENT '회원가입 생성일자',
	`updateDt` timestamp NULL COMMENT '회원 정보 수정 일자',
	`loginM` varchar(10) NOT NULL COMMENT '일반/구글/네이버 등',
    PRIMARY KEY (userNo)
);

CREATE TABLE `Auth` (
	`authCd`	VARCHAR(255)	NOT NULL,
	`authNm`	VARCHAR(255)	NULL,
	`useAt`	VARCHAR(255)	NULL,
    PRIMARY KEY (authCd)
);

CREATE TABLE `Seat` (
	`seatNo`	bigInt	NOT NULL,
	`seatNm`	varchar(100)	NOT NULL,
	`createDt`	timestamp	NOT NULL,
	`updateDt`	timestamp	NULL,
	`register`	varchar(100)	NOT NULL,
	`useAt`	boolean	NOT NULL	COMMENT 'Y/N',
	`seatSort`	varchar(100)	NULL	COMMENT '개인/회의실',
    PRIMARY KEY (seatNo)
);

CREATE TABLE `Price` (
	`priceNo`	bigint	NOT NULL,
	`seatNo`	bigInt	NOT NULL,
	`type`	varchar(10)	NULL	COMMENT '시간(H)/일(D)/월(M)',
	`price`	varchar(10)	NULL	COMMENT '구분 별 가격',
    PRIMARY KEY (priceNo)
);

CREATE TABLE `Reservation` (
	`resNo`	bigint	NOT NULL AUTO_INCREMENT,
	`userNo`	bigint	NOT NULL	COMMENT '회원가입 시 식별번호 자동 생성(시퀀스)',
	`seatNo`	bigInt	NOT NULL,
	`totalPrice`	varchar(10)	NOT NULL	COMMENT '쿠폰 적용 후 최종 결제비용',
	`resPrice`	varchar(10)	NOT NULL	COMMENT '좌석 가격',
	`dcPrice`	varchar(10)	NULL,
	`userCpNo`	bigint	NULL,
	`resDt`	timestamp	NOT NULL,
	`moDt`	timestamp	NULL	COMMENT '변경/취소',
	`resStatus`	boolean	NULL	COMMENT '예약완료/예약취소',
	`resStart`	timestamp	NOT NULL,
	`resEnd`	timestamp	NOT NULL,
    PRIMARY KEY (resNo)
);

CREATE TABLE `Payment` (
	`paymentKey`	VARCHAR(255)	NOT NULL	COMMENT '토스: paymentKey, 카카오: tid, 네이버: paymentId / 결제, 취소 에 사용',
	`resNo`	bigint	NOT NULL,
	`payPrice`	VARCHAR(255)	NULL,
	`payStatus`	VARCHAR(255)	NULL	COMMENT ''READY', 'IN_PROGRESS', 'DONE', 'CANCELED', 'FAILED'',
	`payApproveDt`	VARCHAR(255)	NULL,
	`payCanclDt`	VARCHAR(255)	NULL,
	`errCode`	VARCHAR(255)	NOT NULL,
	`errMsg`	VARCHAR(255)	NOT NULL,
	`registDt`	VARCHAR(255)	NULL,
	`method`	VARCHAR(255)	NULL	COMMENT ''CARD', 'VIRTUAL_ACCOUNT', 'MOBILE', 'TRANSFER', 'CASH'',
	`payProvider`	VARCHAR(255)	NULL	COMMENT ''TOSS', 'KAKAO', 'NAVER'',
	`updateDt`	VARCHAR(255)	NULL,
    PRIMARY KEY (paymentKey)
);

CREATE TABLE `Coupon` (
	`cpNo`	bigint	NOT NULL AUTO_INCREMENT,
	`cpnNm`	varchar(30)	NOT NULL,
	`discount`	varchar(10)	NOT NULL	COMMENT '할인값',
	`discountAt`	varchar(5)	NOT NULL	COMMENT '원/율 코드로 구분',
	`cpnLimit`	bigint	NULL	COMMENT 'null이면 무제한',
	`createDate`	timestamp	NOT NULL,
	`cpnStartDt`	timestamp	NOT NULL	COMMENT '년월일시분초',
	`cpnEndDt`	timestamp	NOT NULL	COMMENT '년월일시분초',
	`cpnDsc`	varchar(100)	NULL,
	`activeYn`	varchar(1)	NOT NULL	COMMENT 'Y/N',
    PRIMARY KEY (cpNo)
);

CREATE TABLE `UserCoupon` (
	`userCpNo`	bigint	NOT NULL AUTO_INCREMENT,
	`userNo`	bigint	NOT NULL	COMMENT '회원가입 시 식별번호 자동 생성(시퀀스)',
	`cpNo`	varchar(20)	NOT NULL,
	`issueDt`	timestamp	NOT NULL,
	`useAt`	varchar(1)	NOT NULL	COMMENT 'Y/N',
	`useDt`	timestamp	NULL,
    PRIMARY KEY (userCpNo)
);




CREATE TABLE `Menu` (
    `menuNo` bigint NOT NULL AUTO_INCREMENT,
    `upperMenuNo` bigint DEFAULT NULL,
    `menuNm` varchar(255) DEFAULT NULL,
    `menuDsc` varchar(255) DEFAULT NULL,
    `menuUrl` varchar(255) DEFAULT NULL,
    `useAt` varchar(10) DEFAULT NULL,
    `createDt` timestamp NULL DEFAULT NULL,
    `sortOrder` int DEFAULT NULL,
    PRIMARY KEY (menuNo)
);

CREATE TABLE `AuthMenu` (
	`menuNo`	VARCHAR(255)	NOT NULL AUTO_INCREMENT,
	`authCd`	VARCHAR(255)	NOT NULL,
    PRIMARY KEY (menuNo)
);

CREATE TABLE `Banner` (
	`bNo`	bigint	NOT NULL AUTO_INCREMENT,
	`bTitle`	varchar(50)	NOT NULL,
	`bImageUrl`	text	NOT NULL,
	`startDt`	timestamp	NOT NULL,
	`endDt`	timestamp	NOT NULL,
	`register`	varchar(100)	NOT NULL,
	`createDt`	timestamp	NOT NULL,
	`useAt`	boolean	NOT NULL	COMMENT 'Y/N',
    PRIMARY KEY (bNo)
);

CREATE TABLE `Faq` (
	`qNo`	bigint	NOT NULL,
	`question`	varchar(50)	NOT NULL,
	`answer`	text	NOT NULL,
	`createDt`	timestamp	NOT NULL,
    PRIMARY KEY (qNo)
);


INSERT INTO `Menu` (
    `upperMenuNo`,
    `menuNm`,
    `menuDsc`,
    `menuUrl`,
    `useAt`,
    `createDt`,
    `sortOrder`
) VALUES
    (null,
     '로그아웃',
     '로그아웃',
     '/logout',
     'Y',
     now(),
     0);