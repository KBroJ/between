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
	`phoneNo` varchar(11) UNIQUE NOT NULL COMMENT '숫자만 저장',
	`userStts` varchar(10) NOT NULL COMMENT '일반, 휴면, 탈퇴',
	`authCd` VARCHAR(10) NOT NULL COMMENT '일반, 임직원, 관리자 등',
	`createDt` timestamp NOT NULL COMMENT '회원가입 생성일자',
	`updateDt` timestamp NULL COMMENT '회원 정보 수정 일자',
	`updateRs` varchar(500) NULL COMMENT '관리자에 의한 변경 사유',
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
	`planType` varchar(30) NULL,
	`userCpNo`	bigint	NULL,
	`resDt`	timestamp	NOT NULL,
	`moDt`	timestamp	NULL	COMMENT '변경/취소',
	`moReason` varchar(500) NULL COMMENT '관리자에 의한 예약 변경 또는 취소 사유',
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
	`discount`	integer unsigned	NOT NULL	COMMENT '할인값',
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

-- wb_between.`role` definition

CREATE TABLE `role` (
    `roleId` bigint NOT NULL AUTO_INCREMENT COMMENT '역할고유번호',
    `roleCode` varchar(50) NOT NULL COMMENT '역할코드 (ROLE_ADMIN, ROLE_USER..)',
    `roleName` varchar(100) NOT NULL COMMENT '역할명(관리자, 사용자..)',
    `description` varchar(255) DEFAULT NULL COMMENT '설명',
    `createDt` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    `updateDt` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (`roleId`),
    UNIQUE KEY `roleCode` (`roleCode`)
)

CREATE TABLE `UserRole` (
    `userRoleId` BIGINT NOT NULL AUTO_INCREMENT,
    `userNo`     BIGINT NOT NULL COMMENT '회원가입 시 식별번호 자동 생성(시퀀스)',
    `roleId`     BIGINT NOT NULL,
    `createDt`   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시 자동 입력',
    PRIMARY KEY (`userRoleId`),
    CONSTRAINT `FK_UserRole_User`
        FOREIGN KEY (`userNo`)
            REFERENCES `User` (`userNo`)
            ON UPDATE CASCADE
            ON DELETE CASCADE,
    CONSTRAINT `FK_UserRole_Role`
        FOREIGN KEY (`roleId`)
            REFERENCES `Role` (`roleId`)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT
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


-- 1. 관리자 역할 추가
INSERT INTO `role` (`roleCode`, `roleName`, `description`, `createDt`)
VALUES
    ('ROLE_ADMIN', '관리자', '시스템 전체 관리 및 모든 기능에 접근 가능한 최상위 역할', NOW());

-- 2. 일반 사용자 역할 추가
INSERT INTO `role` (`roleCode`, `roleName`, `description`, `createDt`)
VALUES
    ('ROLE_USER', '사용자', '사이트의 일반적인 서비스를 이용할 수 있는 기본 역할', NOW());

-- 3. 임직원 역할 추가
INSERT INTO `role` (`roleCode`, `roleName`, `description`, `createDt`)
VALUES
    ('ROLE_STAFF', '임직원', '회사 내부 직원으로, 특정 관리 기능 또는 내부 서비스에 접근 가능한 역할', NOW());

-- 권한(Permission) 테이블 샘플 데이터 삽입

-- 사용자(User) 관련 권한
INSERT INTO `permission` (`permissionCode`, `permissionName`, `description`) VALUES
                                                                                 ('USER:READ_LIST', '사용자 목록 조회', '관리자가 사용자 목록을 조회할 수 있는 권한'),
                                                                                 ('USER:READ_DETAIL', '사용자 상세 정보 조회', '관리자가 개별 사용자의 상세 정보를 조회할 수 있는 권한'),
                                                                                 ('USER:UPDATE', '사용자 정보 수정', '관리자가 사용자의 특정 정보(예: 상태, 역할)를 수정할 수 있는 권한'),
                                                                                 ('USER:DELETE', '사용자 삭제', '관리자가 사용자를 시스템에서 삭제할 수 있는 권한'),
                                                                                 ('USER:ASSIGN_ROLE', '사용자 역할 할당', '관리자가 사용자에게 역할을 부여하거나 변경할 수 있는 권한');

-- 쿠폰(Coupon) 관련 권한
INSERT INTO `permission` (`permissionCode`, `permissionName`, `description`) VALUES
                                                                                 ('COUPON:READ_LIST', '쿠폰 목록 조회', '관리자 또는 임직원이 쿠폰 목록을 조회할 수 있는 권한'),
                                                                                 ('COUPON:READ_DETAIL', '쿠폰 상세 정보 조회', '관리자 또는 임직원이 쿠폰 상세 정보를 조회할 수 있는 권한'),
                                                                                 ('COUPON:CREATE', '쿠폰 생성', '관리자 또는 임직원이 새로운 쿠폰을 생성할 수 있는 권한'),
                                                                                 ('COUPON:UPDATE', '쿠폰 수정', '관리자 또는 임직원이 기존 쿠폰 정보를 수정할 수 있는 권한'),
                                                                                 ('COUPON:DELETE', '쿠폰 삭제', '관리자 또는 임직원이 쿠폰을 삭제할 수 있는 권한');

-- 관리 기능 관련 권한
INSERT INTO `permission` (`permissionCode`, `permissionName`, `description`) VALUES
                                                                                 ('ADMIN:ACCESS_PANEL', '관리자 패널 접근', '관리자 페이지 또는 대시보드에 접근할 수 있는 기본 권한'),
                                                                                 ('SYSTEM:MANAGE_SETTINGS', '시스템 설정 관리', '관리자가 주요 시스템 설정을 변경할 수 있는 권한');

-- (예시) 마이페이지 관련 권한 (일반 사용자용)
INSERT INTO `permission` (`permissionCode`, `permissionName`, `description`) VALUES
                                                                                 ('MYPAGE:READ', '마이페이지 조회', '로그인한 사용자가 자신의 마이페이지 정보를 조회할 수 있는 권한'),
                                                                                 ('MYPAGE:UPDATE_PROFILE', '개인 정보 수정', '로그인한 사용자가 자신의 프로필 정보를 수정할 수 있는 권한');
INSERT INTO `UserRole` (userNo, roleId)
VALUES
    (17, 1)  -- 유저번호, 역할번호 (1-관리자, 2-사용자, 3-임직원)
;