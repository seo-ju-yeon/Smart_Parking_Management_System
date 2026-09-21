-- 애플리케이션이 사용하는 테이블과 관계만 정의합니다.
-- 데이터베이스와 접속 계정은 Docker Compose의 MariaDB 환경변수로 생성합니다.

CREATE TABLE `member`
(
    `member_id`      INT AUTO_INCREMENT PRIMARY KEY COMMENT '회원 고유 식별자',
    `car_num`        VARCHAR(20) NOT NULL COMMENT '차량번호(공백 제거)',
    `car_type`       TINYINT     NOT NULL COMMENT '차량유형 (1:일반, 2:월정액대상, 3:경차, 4:장애인)',
    `name`           VARCHAR(20) NOT NULL COMMENT '운전자 이름',
    `phone`          VARCHAR(20) NOT NULL COMMENT '연락처',
    `start_date`     DATE        NOT NULL COMMENT '월정액 시작일',
    `end_date`       DATE        NOT NULL COMMENT '월정액 종료일',
    `subscribed`     BOOLEAN     NOT NULL DEFAULT FALSE COMMENT '현재 월정액 구독 중인지 여부',
    `subscribed_fee` INT         NOT NULL DEFAULT 100000 COMMENT '월정액 가격',
    `create_date`    DATE        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입일',
    INDEX `idx_member_car_num` (`car_num`)
);

CREATE TABLE `manager`
(
    `manager_no`   INT AUTO_INCREMENT PRIMARY KEY COMMENT '관리자 시스템 내부 번호',
    `manager_id`   VARCHAR(50)  NOT NULL UNIQUE COMMENT '로그인 아이디',
    `manager_name` VARCHAR(20)  NOT NULL COMMENT '관리자 이름',
    `password`     VARCHAR(255) NOT NULL COMMENT 'BCrypt로 해시한 비밀번호',
    `email`        VARCHAR(100) NOT NULL COMMENT '추가 인증에 사용하는 이메일',
    `active`       BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '계정 활성화 여부',
    `role`         VARCHAR(20)  NOT NULL DEFAULT 'NORMAL'
        COMMENT '관리자 권한 (NORMAL, ADMIN, SUPER)'
);

CREATE TABLE `validation`
(
    `no`          INT AUTO_INCREMENT PRIMARY KEY,
    `string_otp`  CHAR(6)      NOT NULL,
    `email`       VARCHAR(100) NOT NULL,
    `expiry_time` DATETIME     NOT NULL COMMENT '인증번호 만료 시간'
);

CREATE TABLE `parking_spot`
(
    `space_id`    VARCHAR(5) PRIMARY KEY COMMENT '주차 공간 번호',
    `empty`       BOOLEAN     NOT NULL DEFAULT TRUE COMMENT '빈 공간 여부',
    `car_num`     VARCHAR(10)          DEFAULT NULL COMMENT '현재 주차된 차량 번호',
    `last_update` DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '상태 변경일'
);

CREATE TABLE `parking`
(
    `parking_id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '주차 기록 ID',
    `member_id`  INT                  DEFAULT NULL COMMENT '회원 ID (비회원은 NULL)',
    `space_id`   VARCHAR(20) NOT NULL COMMENT '주차 공간 ID',
    `car_num`    VARCHAR(20) NOT NULL COMMENT '입차 시점 차량번호',
    `car_type`   TINYINT              DEFAULT NULL COMMENT '입차 시점 차량유형',
    `entry_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입차 시간',
    `exit_time`  DATETIME             DEFAULT NULL COMMENT '출차 시간',
    `total_time` INT                  DEFAULT 0 COMMENT '주차 시간(분)',
    `paid`       BOOLEAN              DEFAULT FALSE COMMENT '정산 완료 여부',

    CONSTRAINT `fk_parking_member` FOREIGN KEY (`member_id`)
        REFERENCES `member` (`member_id`) ON DELETE SET NULL,
    CONSTRAINT `fk_parking_spot` FOREIGN KEY (`space_id`)
        REFERENCES `parking_spot` (`space_id`) ON UPDATE CASCADE
);

CREATE TABLE `fee_policy`
(
    `policy_id`         INT AUTO_INCREMENT PRIMARY KEY COMMENT '요금 정책 고유 ID',
    `grace_period`      INT     NOT NULL DEFAULT 10 COMMENT '무료 회차 시간(분)',
    `default_time`      INT     NOT NULL DEFAULT 60 COMMENT '기본 요금 적용 시간(분)',
    `default_fee`       INT     NOT NULL DEFAULT 2000 COMMENT '기본 요금',
    `extra_time`        INT     NOT NULL DEFAULT 30 COMMENT '추가 요금 단위 시간(분)',
    `extra_fee`         INT     NOT NULL DEFAULT 1000 COMMENT '추가 요금',
    `light_discount`    DOUBLE  NOT NULL DEFAULT 0.3 COMMENT '경차 할인율',
    `disabled_discount` DOUBLE  NOT NULL DEFAULT 0.5 COMMENT '장애인 할인율',
    `subscribed_fee`    INT     NOT NULL DEFAULT 100000 COMMENT '월정액 가격',
    `max_daily_fee`     INT     NOT NULL DEFAULT 15000 COMMENT '일일 최대 요금',
    `is_active`         BOOLEAN NOT NULL DEFAULT TRUE COMMENT '현재 정책 활성화 여부',
    `modify_date`       DATETIME         DEFAULT CURRENT_TIMESTAMP COMMENT '정책 수정일'
);

CREATE TABLE `payment`
(
    `payment_id`      INT AUTO_INCREMENT PRIMARY KEY COMMENT '결제 ID',
    `parking_id`      INT      NOT NULL COMMENT '주차 기록 ID',
    `policy_id`       INT               DEFAULT NULL COMMENT '적용된 요금 정책 ID',
    `payment_type`    TINYINT  NOT NULL COMMENT '결제 수단 (1:카드, 2:현금, 3:월정액)',
    `calculated_fee`  INT      NOT NULL DEFAULT 0 COMMENT '할인 전 요금',
    `discount_amount` INT      NOT NULL DEFAULT 0 COMMENT '총 할인 금액',
    `final_fee`       INT      NOT NULL DEFAULT 0 COMMENT '최종 결제 금액',
    `payment_date`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '실제 결제 일시',

    CONSTRAINT `fk_payment_parking` FOREIGN KEY (`parking_id`)
        REFERENCES `parking` (`parking_id`),
    CONSTRAINT `fk_payment_policy` FOREIGN KEY (`policy_id`)
        REFERENCES `fee_policy` (`policy_id`)
);
