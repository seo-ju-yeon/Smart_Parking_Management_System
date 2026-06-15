package org.example.smart_parking_260219.util;

import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;
import org.modelmapper.convention.MatchingStrategies;

/**
 * DTO와 VO를 변환할 때 사용하는 ModelMapper 설정 클래스입니다.
 *
 * <p>
 * 프로젝트에서 같은 ModelMapper 설정을 재사용하기 위해
 * enum 싱글턴 방식으로 작성했습니다.
 * </p>
 */
public enum MapperUtil {
    // MapperUtil의 싱글턴 인스턴스
    INSTANCE;

    // DTO와 VO 변환에 사용할 ModelMapper 객체
    private final ModelMapper modelMapper;

    MapperUtil() {
        modelMapper = new ModelMapper();

        // private 필드도 매핑할 수 있도록 설정
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(Configuration.AccessLevel.PRIVATE)
                .setMatchingStrategy(MatchingStrategies.STRICT);
    }

    /**
     * 설정이 적용된 ModelMapper 객체를 반환합니다.
     *
     * @return DTO와 VO 변환에 사용할 ModelMapper 객체
     */
    public ModelMapper getInstance() {
        return modelMapper;
    }
}
