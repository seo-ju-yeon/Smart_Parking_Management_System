package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.dao.FeePolicyDAO;
import org.example.smart_parking_260219.dto.FeePolicyDTO;
import org.example.smart_parking_260219.vo.FeePolicyVO;
import org.modelmapper.ModelMapper;
import org.example.smart_parking_260219.util.MapperUtil;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class FeePolicyService {
    private final FeePolicyDAO feePolicyDAO = FeePolicyDAO.getInstance();
    private final ModelMapper modelMapper = MapperUtil.INSTANCE.getInstance();

    private static FeePolicyService instance;
    private FeePolicyService() {}
    public static FeePolicyService getInstance() {
        if (instance == null) {
            instance = new FeePolicyService();
        }
        return instance;
    }

    // ModelMapper 사용 변환
    private FeePolicyDTO toDto(FeePolicyVO vo) {
        return modelMapper.map(vo, FeePolicyDTO.class);
    }

    private FeePolicyVO toVo(FeePolicyDTO dto) {
        return modelMapper.map(dto, FeePolicyVO.class);
    }

    // 신규 추가: 월정액 요금 조회
    public Integer getSubscribedFee() throws Exception {
        try (Connection connection = DBConnection.INSTANCE.getConnection()) {
            return feePolicyDAO.getSubscribedFee(connection);
        }
    }

    // [정책 등록]
    public void addPolicy(FeePolicyDTO feePolicyDTO) {
        // 수치 입력시 기본값이 0보다 커야 함.
        if (feePolicyDTO.getDefaultTime() <= 0) {
            throw new IllegalArgumentException("defaultTime은 0보다 커야 합니다.");
        }

        if (feePolicyDTO.getDefaultFee() < 0) {
            throw new IllegalArgumentException("defaultFee는 0 이상이어야 합니다.");
        }

        if (feePolicyDTO.getExtraFee() < 0) {
            throw new IllegalArgumentException("extraFee는 0 이상이어야 합니다.");
        }

        if (feePolicyDTO.getGracePeriod() < 0) {
            throw new IllegalArgumentException("gracePeriod는 0 이상이어야 합니다.");
        }

        feePolicyDAO.deactivateAllPolicies();  // 기존 정책 전부 false

        // Dto -> vo
        FeePolicyVO originalVo = toVo(feePolicyDTO);

        double lightDiscount = originalVo.getLightDiscount() * 0.01;
        double disabledDiscount = originalVo.getDisabledDiscount() * 0.01;

        FeePolicyVO feePolicyVo = FeePolicyVO.builder()
                .policyId(originalVo.getPolicyId())
                .gracePeriod(originalVo.getGracePeriod())
                .defaultTime(originalVo.getDefaultTime())
                .defaultFee(originalVo.getDefaultFee())
                .extraTime(originalVo.getExtraTime())
                .extraFee(originalVo.getExtraFee())
                .lightDiscount(lightDiscount)
                .disabledDiscount(disabledDiscount)
                .subscribedFee(originalVo.getSubscribedFee())
                .maxDailyFee(originalVo.getMaxDailyFee())
                .isActive(true)
                .modifyDate(originalVo.getModifyDate())
                .build();
        feePolicyDAO.insertPolicy(feePolicyVo);
        log.info("요금 정책 등록 완료");
    }

    // [목록 조회]
    public List<FeePolicyDTO> getPolicyList() {
        List<FeePolicyVO> voList = feePolicyDAO.selectAllPolicies();

        List<FeePolicyDTO> dtoList = new ArrayList<>();
        for (FeePolicyVO vo : voList) {
            dtoList.add(toDto(vo));
        }
        return dtoList;
    }

    // [상세 조회]
    public FeePolicyDTO getPolicy() {
        FeePolicyVO feePolicyVO = feePolicyDAO.selectOnePolicy();

        if (feePolicyVO == null) {
            return null; // 또는 throw new IllegalStateException("활성 정책이 없습니다.");
        }
        return toDto(feePolicyVO);
    }

    // [단건 조회] 요금 정책 상세 출력 (ID 기반)
    public FeePolicyDTO getPolicyById(int policyId) throws Exception {
        FeePolicyVO vo = feePolicyDAO.selectPolicyById(policyId);
        if (vo == null) return null;
        return toDto(vo);
    }

    // 특정 ID의 정책만 활성화 나머지 정책은 비활성화
    public void applyPolicy(int id) throws Exception {
        // 1. 현재 활성화된 모든 정책을 꺼버림 (작성하신 메서드 호출)
        feePolicyDAO.deactivateAllPolicies();

        // 2. 선택한 특정 ID의 정책만 켬 (새로 추가한 메서드 호출)
        feePolicyDAO.activatePolicy(id);

        log.info("요금 정책 적용 완료 - 정책 ID: {}", id);
    }
}
