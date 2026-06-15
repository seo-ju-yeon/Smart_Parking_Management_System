CREATE DATABASE IF NOT EXISTS `smart_parking_team2`;

USE `smart_parking_team2`;

# member : 회원, 차량 정보 테이블
CREATE TABLE IF NOT EXISTS `member`
(
    `member_id`      INT AUTO_INCREMENT PRIMARY KEY COMMENT '회원 고유 식별자',
    `car_num`        VARCHAR(20) NOT NULL COMMENT '차량번호(중복불가, 공백제거)',
    `car_type`       TINYINT     NOT NULL COMMENT '차량유형 (1:일반, 2:월정액대상, 3:경차, 4:장애인)',
    `name`           VARCHAR(20) NOT NULL COMMENT '운전자 이름',
    `phone`          VARCHAR(20) NOT NULL COMMENT '연락처',
    `start_date`     DATE        NOT NULL COMMENT '월정액 시작일',
    `end_date`       DATE        NOT NULL COMMENT '월정액 종료일',
    `subscribed`     BOOLEAN     NOT NULL DEFAULT FALSE COMMENT '현재 월정액 구독 중인지 여부',
    `subscribed_fee` INT         NOT NULL DEFAULT 100000 COMMENT '월정액 가격 - 100,000원',
    `create_date`    DATE        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입일',
    INDEX idx_car_num (car_num)
);

-- member dummy -> DAO TEST 3회 실행
INSERT INTO `member` (`car_num`, `car_type`, `name`, `phone`, `start_date`, `end_date`, `subscribed`, `subscribed_fee`)
VALUES ('11가1111', 2, '홍길동', '010-1111-2222', '2026-02-01', '2026-03-01', TRUE, 100000),
       ('22나2222', 2, '김철수', '010-2222-3333', '2026-02-02', '2026-03-02', TRUE, 100000),
       ('33다3333', 2, '이영희', '010-3333-4444', '2026-02-03', '2026-03-03', TRUE, 100000),
       ('44라4444', 2, '박박사', '010-4444-5555', '2026-02-04', '2026-03-04', TRUE, 100000),
       ('55마5555', 2, '테스터1', '010-5555-6666', '2026-02-05', '2026-03-05', TRUE, 100000),
       ('66바6666', 2, '테스터2', '010-6666-7777', '2026-02-06', '2026-03-06', TRUE, 100000),
       ('77사7777', 2, '테스터3', '010-7777-8888', '2026-02-07', '2026-03-07', TRUE, 100000),
       ('88아8888', 2, '테스터4', '010-8888-9999', '2026-02-08', '2026-03-08', TRUE, 100000),
       ('99자9999', 2, '테스터5', '010-9999-1010', '2026-02-09', '2026-03-09', TRUE, 100000),
       ('10차1010', 2, '테스터6', '010-1010-1111', '2026-02-10', '2026-03-10', TRUE, 100000),
       ('11카1111', 2, '테스터7', '010-1111-1212', '2026-02-11', '2026-03-11', TRUE, 100000),
       ('12타1212', 2, '테스터8', '010-1212-1313', '2026-02-12', '2026-03-12', TRUE, 100000),
       ('13파1313', 2, '테스터9', '010-1313-1414', '2026-02-13', '2026-03-13', TRUE, 100000),
       ('14하1414', 2, '테스터10', '010-1414-1515', '2026-02-14', '2026-03-14', TRUE, 100000),
       ('15게1515', 2, '테스터11', '010-1515-1616', '2026-02-15', '2026-03-15', TRUE, 100000),
       ('16네1616', 2, '테스터12', '010-1616-1717', '2026-02-16', '2026-03-16', TRUE, 100000);


# manager : 관리자 정보 테이블
CREATE TABLE IF NOT EXISTS `manager`
(
    `manager_no`   INT AUTO_INCREMENT PRIMARY KEY COMMENT '관리자 시스템 내부 번호',
    `manager_id`   VARCHAR(50)  NOT NULL UNIQUE COMMENT '로그인 아이디 (중복 불가)',
    `manager_name` VARCHAR(20)  NOT NULL COMMENT '관리자 이름',
    `password`     VARCHAR(255) NOT NULL COMMENT '관리자 비밀번호 (해시 암호화)',
    `email`        VARCHAR(100) NOT NULL COMMENT '관리자 이메일(2차 인증)',
    `active`       BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '계정 활성화 여부',
    `role`         VARCHAR(20)  NOT NULL DEFAULT 'NORMAL'
        COMMENT '관리자 권한: ADMIN(최고관리자), NORMAL(일반관리자)'
);
-- 최고 관리자(ADMIN) 초기 데이터 삽입
INSERT INTO `manager` (`manager_id`, `manager_name`, `password`, `email`, `active`, `role`)
VALUES ('admin',
        '최고관리자',
#     비밀번호 : admin1234
        '$2a$12$ZCQ/eJfwieyh19zSm8g15Os9hbtPS4.W6wgtWg2kycba/5x8o6JVS',
        'wndus6110@naver.com',
        TRUE,
        'ADMIN')
ON DUPLICATE KEY UPDATE `role` = 'ADMIN';

-- 슈퍼 관리자(SUPER) 초기 데이터 삽입
INSERT INTO `manager` (`manager_id`, `manager_name`, `password`, `email`, `active`, `role`)
VALUES ('super',
        '슈퍼관리자',
#     비밀번호 : super1234
        '$2a$12$12q5tYhznZe7E6Pt73SpAubFpKJjD/y24xAAFU6W4ghGyXXUacZO6',
        'example@naver.com',
        TRUE,
        'SUPER')
ON DUPLICATE KEY UPDATE `role` = 'SUPER';

-- manager dummy

-- id - test01, password - 1111
insert into manager (manager_id, manager_name, password, email, active, role)
VALUES ('test01', 'tester1', '$2a$12$4ReuaFjNvpNJlf/ZzjTC1u59qKSvH0kZcg0jS1tlPP/K8ubssv8Jq', 'example@naver.com', true,
        'NORMAL');

-- id - test02, password - 2222
insert into manager (manager_id, manager_name, password, email, active, role)
VALUES ('test02', 'tester2', '$2a$12$fH1QjEr/cAZvm87TyiOqluVV04tHx88ojnUbjH6z5HLnoSEJjLeOm', 'example@naver.com', true,
        'NORMAL');

-- id - test03, password - 3333
insert into manager (manager_id, manager_name, password, email, active, role)
VALUES ('test03', 'tester3', '$2a$12$FBBPtsVf4wkRTdlGo64pV.PLDGeJTRBLzrPRl766Z2xLJxudQ/Su6', 'example@naver.com', true,
        'NORMAL');

-- id - test04, password - 4444
insert into manager (manager_id, manager_name, password, email, active, role)
VALUES ('test04', 'tester4', '$2a$12$ikBOSKCvmiQVjG8fi3flwu853ctVAWsFRmym.FlZQS1w2O73Yrhd2', 'example@naver.com', true,
        'NORMAL');

-- id - test05, password - 5555
insert into manager (manager_id, manager_name, password, email, active, role)
VALUES ('test05', 'tester5', '$2a$12$UunwhxzJhMdX6yMaoA7uFOl0CaV/Xaxa08WTmZyAYffwPZRAbSuzi', 'example@naver.com', true,
        'NORMAL');


CREATE TABLE IF NOT EXISTS `validation`
(
    `no`          int auto_increment primary key,
    `string_otp`  char(6)      not null,
    `email`       varchar(100) not null,
    `expiry_time` datetime     not null comment '만료시간'
);

# parking_spot : 주차 공간 상태 테이블
CREATE TABLE IF NOT EXISTS `parking_spot`
(
    `space_id`    VARCHAR(5) PRIMARY KEY COMMENT '주차 공간 번호(A1, A2...)',
    `empty`       BOOLEAN NOT NULL DEFAULT TRUE COMMENT '빈 공간 여부(True:비어있음, False:주차중)',
    `car_num`     VARCHAR(10)      DEFAULT NULL COMMENT '현재 주차된 차량 번호(없으면 NULL)',
    `last_update` DATETIME         DEFAULT CURRENT_TIMESTAMP COMMENT '상태 변경일'
);

insert into parking_spot (space_id)
values ('A1'),
       ('A2'),
       ('A3'),
       ('A4'),
       ('A5'),
       ('A6'),
       ('A7'),
       ('A8'),
       ('A9'),
       ('A10'),
       ('A11'),
       ('A12'),
       ('A13'),
       ('A14'),
       ('A15'),
       ('A16'),
       ('A17'),
       ('A18'),
       ('A19'),
       ('A20');

-- parking spot dummy
-- 회원
update `parking_spot`
set `empty`     = false,
    car_num     = '11가1111',
    last_update = now()
where space_id = 'A2';
update `parking_spot`
set `empty`     = false,
    car_num     = '22나2222',
    last_update = now()
where space_id = 'A17';
update `parking_spot`
set `empty`     = false,
    car_num     = '33다3333',
    last_update = now()
where space_id = 'A14';
-- 비회원
update `parking_spot`
set `empty`     = false,
    car_num     = '13다1234',
    last_update = now()
where space_id = 'A5';
update `parking_spot`
set `empty`     = false,
    car_num     = '23다1234',
    last_update = now()
where space_id = 'A16';
update `parking_spot`
set `empty`     = false,
    car_num     = '33다1234',
    last_update = now()
where space_id = 'A9';

# parking : 입차, 출차, 요금 정보 테이블
CREATE TABLE IF NOT EXISTS `parking`
(
    `parking_id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '주차 기록 ID',
    `member_id`  INT                  DEFAULT NULL COMMENT '회원 ID (비회원일 경우 NULL)',
    `space_id`   VARCHAR(20) NOT NULL COMMENT '주차 공간 ID (FK)',
    `car_num`    VARCHAR(20) NOT NULL COMMENT '차량 번호 (스냅샷)',
    `car_type`   TINYINT     NULL COMMENT '차량 유형 (스냅샷)',
    `entry_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입차 시간',
    `exit_time`  DATETIME             DEFAULT NULL COMMENT '출차 시간 (주차중이면 NULL)',
    `total_time` INT                  DEFAULT 0 COMMENT '주차 시간(분)',
    `paid`       BOOLEAN              DEFAULT FALSE COMMENT '정산 완료 여부',

    CONSTRAINT `fk_parking_member` FOREIGN KEY (`member_id`)
        REFERENCES `member` (`member_id`) ON DELETE SET NULL,
    CONSTRAINT `fk_parking_spot` FOREIGN KEY (`space_id`)
        REFERENCES `parking_spot` (`space_id`) ON UPDATE CASCADE
);

# fee_policy : 요금 부과 정책 테이블
CREATE TABLE IF NOT EXISTS `fee_policy`
(
    `policy_id`         INT AUTO_INCREMENT PRIMARY KEY COMMENT '요금 정책 고유 ID',
    `grace_period`      INT     NOT NULL DEFAULT 10 COMMENT '무료 회차 시간(분) - 이 시간 내 출차 시 0원',
    `default_time`      INT     NOT NULL DEFAULT 60 COMMENT '기본 요금 적용 시간(분) - 60분',
    `default_fee`       INT     NOT NULL DEFAULT 2000 COMMENT '기본 요금 - 2,000원',
    `extra_time`        INT     NOT NULL DEFAULT 30 COMMENT '추가 요금 단위 시간(분) - 30분',
    `extra_fee`         INT     NOT NULL DEFAULT 1000 COMMENT '추가 요금 - 1,000원',
    `light_discount`    DOUBLE  NOT NULL DEFAULT 0.3 COMMENT '경차 할인율(0.3 = 30%)',
    `disabled_discount` DOUBLE  NOT NULL DEFAULT 0.5 COMMENT '장애인 할인율(0.5 = 50%)',
    `subscribed_fee`    INT     NOT NULL DEFAULT 100000 COMMENT '월정액 가격 - 100,000원',
    `max_daily_fee`     INT     NOT NULL DEFAULT 15000 COMMENT '일일 최대 요금 - 15,000원',
    `is_active`         BOOLEAN NOT NULL DEFAULT TRUE COMMENT '현재 정책 활성화 여부',
    `modify_date`       DATETIME         DEFAULT CURRENT_TIMESTAMP COMMENT '정책 수정일'
);

-- fee_policy dummy
insert into fee_policy
(grace_period, default_time, default_fee, extra_time, extra_fee, light_discount, disabled_discount, subscribed_fee,
 max_daily_fee, is_active, modify_date)
VALUES (10, 10, 2000, 30, 1000, 0.3, 0.5, 100000, 15000, true, now());

# payment : 결제, 매출 정보 테이블
CREATE TABLE IF NOT EXISTS `payment`
(
    `payment_id`      INT AUTO_INCREMENT PRIMARY KEY COMMENT '결제 ID',
    `parking_id`      INT      NOT NULL COMMENT '주차 기록 ID (FK)',
    `policy_id`       INT               DEFAULT NULL COMMENT '적용된 요금 정책 ID (FK)',
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

-- 통계용 payment, parking dummy
-- 2월 11일 데이터 (매출이 높은 날)
INSERT INTO `parking` (`member_id`, `space_id`, `car_num`, `car_type`, `entry_time`, `exit_time`, `total_time`, `paid`)
VALUES (1, 'A1', '11가1111', 2, '2026-02-11 09:00:00', '2026-02-11 18:00:00', 540, TRUE),
       (2, 'A2', '22나2222', 1, '2026-02-11 10:30:00', '2026-02-11 12:30:00', 120, TRUE),
       (NULL, 'A3', '99허9999', 1, '2026-02-11 14:00:00', '2026-02-11 16:00:00', 120, TRUE);

INSERT INTO `payment` (`parking_id`, `policy_id`, `payment_type`, `calculated_fee`, `discount_amount`, `final_fee`,
                       `payment_date`)
VALUES (1, 1, 3, 15000, 15000, 0, '2026-02-11 18:05:00'),
       (2, 1, 1, 4000, 0, 4000, '2026-02-11 12:35:00'),
       (3, 1, 1, 4000, 0, 4000, '2026-02-11 16:05:00');

-- 2월 12일 데이터
INSERT INTO `parking` (`member_id`, `space_id`, `car_num`, `car_type`, `entry_time`, `exit_time`, `total_time`, `paid`)
VALUES (3, 'A4', '33다3333', 3, '2026-02-12 08:00:00', '2026-02-12 10:00:00', 120, TRUE),
       (NULL, 'A5', '88호8888', 1, '2026-02-12 13:00:00', '2026-02-12 15:30:00', 150, TRUE);

INSERT INTO `payment` (`parking_id`, `policy_id`, `payment_type`, `calculated_fee`, `discount_amount`, `final_fee`,
                       `payment_date`)
VALUES (4, 1, 1, 4000, 1200, 2800, '2026-02-12 10:05:00'),
       (5, 1, 2, 5000, 0, 5000, '2026-02-12 15:35:00');

-- 2월 19일 데이터 (조회 기준일 테스트용)
INSERT INTO `parking` (`member_id`, `space_id`, `car_num`, `car_type`, `entry_time`, `exit_time`, `total_time`, `paid`)
VALUES (4, 'A6', '44라4444', 4, '2026-02-19 09:30:00', '2026-02-19 11:30:00', 120, TRUE),
       (NULL, 'A7', '77가7777', 1, '2026-02-19 10:00:00', '2026-02-19 10:40:00', 40, TRUE),
       (NULL, 'A8', '66나6666', 1, '2026-02-19 10:15:00', '2026-02-19 12:15:00', 120, TRUE);

INSERT INTO `payment` (`parking_id`, `policy_id`, `payment_type`, `calculated_fee`, `discount_amount`, `final_fee`,
                       `payment_date`)
VALUES (6, 1, 1, 4000, 2000, 2000, '2026-02-19 11:35:00'),
       (7, 1, 1, 2000, 0, 2000, '2026-02-19 10:45:00'),
       (8, 1, 1, 4000, 0, 4000, '2026-02-19 12:20:00');

-- 현재 주차 중인 더미 (통계용 데이터 이후에 INSERT → parking_id 9~14)
-- 회원 (car_type은 member 테이블의 car_type 스냅샷)
insert into parking (member_id, space_id, car_num, car_type, entry_time)
VALUES (1, 'A2', '11가1111', 2, now());
insert into parking (member_id, space_id, car_num, car_type, entry_time)
VALUES (2, 'A17', '22나2222', 2, '2026-02-18');
insert into parking (member_id, space_id, car_num, car_type, entry_time)
VALUES (3, 'A14', '33다3333', 2, now());

-- 비회원 (car_type = 1: 일반)
insert into parking (space_id, car_num, car_type, entry_time)
VALUES ('A5', '13다1234', 1, now());
insert into parking (space_id, car_num, car_type, entry_time)
VALUES ('A16', '23다1234', 1, now());
insert into parking (space_id, car_num, car_type, entry_time)
VALUES ('A9', '33다1234', 1, now());

-- 전용 사용자 생성
CREATE USER `admin`@`localhost` IDENTIFIED BY '0219';

-- 사용자에게 DB 권한 부여
GRANT ALL PRIVILEGES ON `smart_parking_team2`.* TO `admin`@`localhost`;