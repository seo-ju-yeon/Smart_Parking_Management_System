-- 하나의 주차 기록에는 하나의 결제 내역만 허용함
ALTER TABLE `payment`
    ADD CONSTRAINT `uk_payment_parking_id`
        UNIQUE (`parking_id`);
