-- 인증 우회 제거 후 기능이 중복된 SUPER 역할을 ADMIN으로 통합합니다.
UPDATE `manager`
SET `role` = 'ADMIN'
WHERE `role` = 'SUPER';

-- 현재 사용하는 역할과 컬럼 설명을 일치시킵니다.
ALTER TABLE `manager`
    MODIFY COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'NORMAL'
        COMMENT '관리자 권한 (NORMAL, ADMIN)';

-- 정의되지 않은 역할이 저장되어 권한 검사에서 누락되지 않도록 허용값을 제한합니다.
ALTER TABLE `manager`
    ADD CONSTRAINT `chk_manager_role`
        CHECK (`role` IN ('NORMAL', 'ADMIN'));
