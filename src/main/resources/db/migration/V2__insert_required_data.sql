-- 애플리케이션 실행에 필요한 주차 공간과 기본 요금 정책을 생성합니다.

INSERT INTO `parking_spot` (`space_id`)
VALUES ('A1'),
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

INSERT INTO `fee_policy`
(`policy_id`, `grace_period`, `default_time`, `default_fee`, `extra_time`, `extra_fee`, `light_discount`,
 `disabled_discount`, `subscribed_fee`, `max_daily_fee`, `is_active`, `modify_date`)
VALUES (1, 10, 60, 2000, 30, 1000, 0.3, 0.5, 100000, 15000, TRUE, CURRENT_TIMESTAMP);
