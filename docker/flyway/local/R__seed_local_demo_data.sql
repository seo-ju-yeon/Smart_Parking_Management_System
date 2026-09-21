-- 로컬 개발 및 포트폴리오 시연에만 사용하는 데이터입니다.
-- 이 파일은 운영 Flyway 경로에 포함하지 않습니다.
-- 고정된 샘플 ID와 UPSERT를 사용해 파일 변경 후 재실행되어도 중복 생성을 방지합니다.

-- 로컬 로그인 계정
-- demo_normal / normal1234 : 등록 이메일 일치 확인 흐름
-- demo_super  / super1234  : Mailpit에서 OTP를 확인하는 전체 기능 시연 흐름
INSERT INTO `manager`
(`manager_no`, `manager_id`, `manager_name`, `password`, `email`, `active`, `role`)
VALUES (1001,
        'demo_normal',
        '일반관리자',
        '$2a$12$pEZRMZsPfvxbQ6CafJb3GePRxPRpO3SFjkMTz.zOFcnvtkXtAL7uu',
        'demo-normal@smartparking.local',
        TRUE,
        'NORMAL'),
       (1002,
        'demo_super',
        '슈퍼관리자',
        '$2a$12$EaDDPZlExsClDZsQMl8c8e4gu8RA/K8uk9SqJTP9CynVIns5YujDq',
        'demo-super@smartparking.local',
        TRUE,
        'SUPER')
ON DUPLICATE KEY UPDATE `manager_name` = VALUES(`manager_name`),
                        `password`     = VALUES(`password`),
                        `email`        = VALUES(`email`),
                        `active`       = VALUES(`active`),
                        `role`         = VALUES(`role`);

-- 차량 유형과 구독 상태를 함께 확인할 수 있는 가상 회원 12명
INSERT INTO `member`
(`member_id`, `car_num`, `car_type`, `name`, `phone`, `start_date`, `end_date`, `subscribed`, `subscribed_fee`,
 `create_date`)
VALUES (1001, '10가1001', 1, '테스트회원01', '010-0000-1001', DATE_SUB(CURRENT_DATE, INTERVAL 40 DAY),
        DATE_ADD(CURRENT_DATE, INTERVAL 20 DAY), FALSE, 100000, DATE_SUB(CURRENT_DATE, INTERVAL 40 DAY)),
       (1002, '20나1002', 2, '테스트회원02', '010-0000-1002', DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY),
        DATE_ADD(CURRENT_DATE, INTERVAL 15 DAY), TRUE, 100000, DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY)),
       (1003, '30다1003', 3, '테스트회원03', '010-0000-1003', DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY),
        DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY), FALSE, 100000, DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY)),
       (1004, '40라1004', 4, '테스트회원04', '010-0000-1004', DATE_SUB(CURRENT_DATE, INTERVAL 25 DAY),
        DATE_ADD(CURRENT_DATE, INTERVAL 35 DAY), FALSE, 100000, DATE_SUB(CURRENT_DATE, INTERVAL 25 DAY)),
       (1005, '50마1005', 1, '테스트회원05', '010-0000-1005', DATE_SUB(CURRENT_DATE, INTERVAL 20 DAY),
        DATE_ADD(CURRENT_DATE, INTERVAL 40 DAY), FALSE, 100000, DATE_SUB(CURRENT_DATE, INTERVAL 20 DAY)),
       (1006, '60바1006', 2, '테스트회원06', '010-0000-1006', DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY),
        DATE_ADD(CURRENT_DATE, INTERVAL 20 DAY), TRUE, 100000, DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY)),
       (1007, '70사1007', 3, '테스트회원07', '010-0000-1007', DATE_SUB(CURRENT_DATE, INTERVAL 60 DAY),
        DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), FALSE, 100000, DATE_SUB(CURRENT_DATE, INTERVAL 60 DAY)),
       (1008, '80아1008', 4, '테스트회원08', '010-0000-1008', DATE_SUB(CURRENT_DATE, INTERVAL 50 DAY),
        DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), FALSE, 100000, DATE_SUB(CURRENT_DATE, INTERVAL 50 DAY)),
       (1009, '90자1009', 1, '테스트회원09', '010-0000-1009', DATE_SUB(CURRENT_DATE, INTERVAL 12 DAY),
        DATE_ADD(CURRENT_DATE, INTERVAL 48 DAY), FALSE, 100000, DATE_SUB(CURRENT_DATE, INTERVAL 12 DAY)),
       (1010, '11차1010', 2, '테스트회원10', '010-0000-1010', DATE_SUB(CURRENT_DATE, INTERVAL 60 DAY),
        DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY), FALSE, 100000, DATE_SUB(CURRENT_DATE, INTERVAL 60 DAY)),
       (1011, '12카1011', 3, '테스트회원11', '010-0000-1011', DATE_SUB(CURRENT_DATE, INTERVAL 18 DAY),
        DATE_ADD(CURRENT_DATE, INTERVAL 42 DAY), FALSE, 100000, DATE_SUB(CURRENT_DATE, INTERVAL 18 DAY)),
       (1012, '13타1012', 4, '테스트회원12', '010-0000-1012', DATE_SUB(CURRENT_DATE, INTERVAL 22 DAY),
        DATE_ADD(CURRENT_DATE, INTERVAL 38 DAY), FALSE, 100000, DATE_SUB(CURRENT_DATE, INTERVAL 22 DAY))
ON DUPLICATE KEY UPDATE `car_num`        = VALUES(`car_num`),
                        `car_type`       = VALUES(`car_type`),
                        `name`           = VALUES(`name`),
                        `phone`          = VALUES(`phone`),
                        `start_date`     = VALUES(`start_date`),
                        `end_date`       = VALUES(`end_date`),
                        `subscribed`     = VALUES(`subscribed`),
                        `subscribed_fee` = VALUES(`subscribed_fee`),
                        `create_date`    = VALUES(`create_date`);

-- 일별·주별·월별 통계 화면을 확인하기 위한 정산 완료 주차 기록 16건
INSERT INTO `parking`
(`parking_id`, `member_id`, `space_id`, `car_num`, `car_type`, `entry_time`, `exit_time`, `total_time`, `paid`)
VALUES (2001, 1002, 'A1', '20나1002', 2, TIMESTAMP(CURRENT_DATE, '08:00:00'),
        TIMESTAMP(CURRENT_DATE, '09:00:00'), 60, TRUE),
       (2002, 1003, 'A2', '30다1003', 3, TIMESTAMP(CURRENT_DATE, '09:00:00'),
        TIMESTAMP(CURRENT_DATE, '11:00:00'), 120, TRUE),
       (2003, 1004, 'A3', '40라1004', 4, TIMESTAMP(CURRENT_DATE, '10:00:00'),
        TIMESTAMP(CURRENT_DATE, '12:00:00'), 120, TRUE),
       (2004, NULL, 'A4', '91가2004', 1, TIMESTAMP(CURRENT_DATE, '11:00:00'),
        TIMESTAMP(CURRENT_DATE, '12:30:00'), 90, TRUE),
       (2005, 1001, 'A5', '10가1001', 1, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), '08:30:00'),
        TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), '10:30:00'), 120, TRUE),
       (2006, NULL, 'A6', '92나2006', 1, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), '12:00:00'),
        TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), '12:45:00'), 45, TRUE),
       (2007, 1006, 'A7', '60바1006', 2, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), '14:00:00'),
        TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), '18:00:00'), 240, TRUE),
       (2008, 1007, 'A8', '70사1007', 3, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), '09:00:00'),
        TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), '10:30:00'), 90, TRUE),
       (2009, 1008, 'A9', '80아1008', 4, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), '11:00:00'),
        TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), '14:00:00'), 180, TRUE),
       (2010, NULL, 'A10', '93다2010', 1, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), '15:00:00'),
        TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), '18:30:00'), 210, TRUE),
       (2011, 1005, 'A11', '50마1005', 1, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH), '08:00:00'),
        TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH), '09:00:00'), 60, TRUE),
       (2012, 1011, 'A12', '12카1011', 3, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH), '10:00:00'),
        TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH), '12:00:00'), 120, TRUE),
       (2013, NULL, 'A13', '94라2013', 1, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH), '13:00:00'),
        TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH), '17:00:00'), 240, TRUE),
       (2014, 1009, 'A14', '90자1009', 1, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 3 MONTH), '09:00:00'),
        TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 3 MONTH), '10:30:00'), 90, TRUE),
       (2015, 1012, 'A15', '13타1012', 4, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 3 MONTH), '11:00:00'),
        TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 3 MONTH), '13:00:00'), 120, TRUE),
       (2016, NULL, 'A16', '95마2016', 1, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 3 MONTH), '14:00:00'),
        TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 3 MONTH), '15:00:00'), 60, TRUE)
ON DUPLICATE KEY UPDATE `member_id`  = VALUES(`member_id`),
                        `space_id`   = VALUES(`space_id`),
                        `car_num`    = VALUES(`car_num`),
                        `car_type`   = VALUES(`car_type`),
                        `entry_time` = VALUES(`entry_time`),
                        `exit_time`  = VALUES(`exit_time`),
                        `total_time` = VALUES(`total_time`),
                        `paid`       = VALUES(`paid`);

-- 정산 완료 주차 기록과 일대일로 대응하는 결제 데이터 16건
INSERT INTO `payment`
(`payment_id`, `parking_id`, `policy_id`, `payment_type`, `calculated_fee`, `discount_amount`, `final_fee`,
 `payment_date`)
VALUES (3001, 2001, 1, 3, 2000, 2000, 0, TIMESTAMP(CURRENT_DATE, '09:05:00')),
       (3002, 2002, 1, 1, 4000, 1200, 2800, TIMESTAMP(CURRENT_DATE, '11:05:00')),
       (3003, 2003, 1, 1, 4000, 2000, 2000, TIMESTAMP(CURRENT_DATE, '12:05:00')),
       (3004, 2004, 1, 2, 3000, 0, 3000, TIMESTAMP(CURRENT_DATE, '12:35:00')),
       (3005, 2005, 1, 1, 4000, 0, 4000, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), '10:35:00')),
       (3006, 2006, 1, 2, 2000, 0, 2000, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), '12:50:00')),
       (3007, 2007, 1, 3, 8000, 8000, 0, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), '18:05:00')),
       (3008, 2008, 1, 1, 3000, 900, 2100, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), '10:35:00')),
       (3009, 2009, 1, 1, 6000, 3000, 3000, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), '14:05:00')),
       (3010, 2010, 1, 2, 7000, 0, 7000, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), '18:35:00')),
       (3011, 2011, 1, 1, 2000, 0, 2000, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH), '09:05:00')),
       (3012, 2012, 1, 1, 4000, 1200, 2800, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH), '12:05:00')),
       (3013, 2013, 1, 2, 8000, 0, 8000, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH), '17:05:00')),
       (3014, 2014, 1, 1, 3000, 0, 3000, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 3 MONTH), '10:35:00')),
       (3015, 2015, 1, 1, 4000, 2000, 2000, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 3 MONTH), '13:05:00')),
       (3016, 2016, 1, 2, 2000, 0, 2000, TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 3 MONTH), '15:05:00'))
ON DUPLICATE KEY UPDATE `parking_id`      = VALUES(`parking_id`),
                        `policy_id`       = VALUES(`policy_id`),
                        `payment_type`    = VALUES(`payment_type`),
                        `calculated_fee`  = VALUES(`calculated_fee`),
                        `discount_amount` = VALUES(`discount_amount`),
                        `final_fee`       = VALUES(`final_fee`),
                        `payment_date`    = VALUES(`payment_date`);

-- 현재 주차 중인 차량 6대(회원 3대, 비회원 3대)
INSERT INTO `parking`
(`parking_id`, `member_id`, `space_id`, `car_num`, `car_type`, `entry_time`, `exit_time`, `total_time`, `paid`)
VALUES (2101, 1002, 'A2', '20나1002', 2, DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, 0, FALSE),
       (2102, 1003, 'A14', '30다1003', 3, DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL, 0, FALSE),
       (2103, 1004, 'A17', '40라1004', 4, DATE_SUB(NOW(), INTERVAL 3 HOUR), NULL, 0, FALSE),
       (2104, NULL, 'A5', '96바2104', 1, DATE_SUB(NOW(), INTERVAL 30 MINUTE), NULL, 0, FALSE),
       (2105, NULL, 'A9', '97사2105', 1, DATE_SUB(NOW(), INTERVAL 90 MINUTE), NULL, 0, FALSE),
       (2106, NULL, 'A16', '98아2106', 1, DATE_SUB(NOW(), INTERVAL 4 HOUR), NULL, 0, FALSE)
ON DUPLICATE KEY UPDATE `member_id`  = VALUES(`member_id`),
                        `space_id`   = VALUES(`space_id`),
                        `car_num`    = VALUES(`car_num`),
                        `car_type`   = VALUES(`car_type`),
                        `entry_time` = VALUES(`entry_time`),
                        `exit_time`  = VALUES(`exit_time`),
                        `total_time` = VALUES(`total_time`),
                        `paid`       = VALUES(`paid`);

UPDATE `parking_spot`
SET `empty`       = FALSE,
    `car_num`     = CASE `space_id`
                        WHEN 'A2' THEN '20나1002'
                        WHEN 'A5' THEN '96바2104'
                        WHEN 'A9' THEN '97사2105'
                        WHEN 'A14' THEN '30다1003'
                        WHEN 'A16' THEN '98아2106'
                        WHEN 'A17' THEN '40라1004'
        END,
    `last_update` = CURRENT_TIMESTAMP
WHERE `space_id` IN ('A2', 'A5', 'A9', 'A14', 'A16', 'A17');
