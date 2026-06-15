package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dao.ManagerDAO;
import org.example.smart_parking_260219.dto.ManagerDTO;
import org.example.smart_parking_260219.util.MapperUtil;
import org.example.smart_parking_260219.util.PasswordUtil;
import org.example.smart_parking_260219.vo.ManagerVO;
import org.modelmapper.ModelMapper;

import java.util.List;

/**
 * 관리자 관련 비즈니스 로직을 처리하는 서비스입니다.
 *
 * <p>
 * 관리자 로그인 검증, 관리자 목록 조회, 관리자 등록 기능을 담당합니다.
 * </p>
 */
@Log4j2
public enum ManagerService {
    // ManagerService 싱글턴 인스턴스
    INSTANCE;

    private final ManagerDAO managerDAO;
    private final ModelMapper modelMapper;

    ManagerService() {
        this.managerDAO = ManagerDAO.getInstance();
        this.modelMapper = MapperUtil.INSTANCE.getInstance();
    }

    /**
     * 관리자 로그인 정보를 검증합니다.
     *
     * <p>
     * 아이디로 관리자 정보를 조회환 뒤,
     * 입력한 비밀번호와 DB에 저장된 BCrypt 해시값을 비교합니다.
     * 비밀번호가 일치하더라도 계정이 활성화 상태일 때만 로그인에 성공합니다.
     * </p>
     *
     * @param managerId 로그인 아이디
     * @param password  입력한 비밀번호
     * @return 로그인 가능 여부
     */
    public boolean isAuth(String managerId, String password) {
        ManagerVO managerVO = managerDAO.selectOne(managerId);

        if (managerVO != null) {
            // 입력한 비밀번호와 DB에 저장된 해시 비밀번호 비교
            boolean passwordMatch = PasswordUtil.checkPassword(password, managerVO.getPassword());

            if (passwordMatch) {
                log.info("비밀번호 검증 성공 - ID: {}", managerId);
                // 비밀번호가 일치해도 계정이 활성화(active) 상태일 때만 true를 반환하여 로그인 허용
                return managerVO.isActive();
            }

            log.warn("비밀번호 불일치 - ID: {}", managerId);
        }
        // 계정 없음, 비밀번호 불일치, 비활성화 계정은 로그인 실패
        return false;
    }

    /**
     * 모든 관리자 목록을 조회합니다.
     *
     * @return 관리자 DTO 목록
     */
    public List<ManagerDTO> getAllManagers() {
//        log.info("getAllManagers... 호출확인");
        List<ManagerVO> voList = managerDAO.selectAll();

        // VO 목록을 화면 전달용 DTO 목록으로 반환
        return voList.stream()
                .map(vo -> modelMapper.map(vo, ManagerDTO.class))
                .toList();
    }

    /**
     * 관리자 계쩡을 추가합니다.
     *
     * @param managerDTO 추가할 관리자 정보
     */
    public void addManager(ManagerDTO managerDTO) {
        log.info("추가할 관리자 DTO: {}", managerDTO);

        // 화면에서 전달받은 DTO를 DB 저장용 VO로 변환
        ManagerVO managerVO = modelMapper.map(managerDTO, ManagerVO.class);

        managerDAO.insertManager(managerVO);
    }
}