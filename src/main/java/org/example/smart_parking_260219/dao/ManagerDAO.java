package org.example.smart_parking_260219.dao;

import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.vo.ManagerVO;
import org.example.smart_parking_260219.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * manager 테이블의 DB 작업을 처리하는 DAO입니다.
 *
 * <p>
 * 관리자 등록, 조회, 수정, 활성화 상태 변경,
 * 비밀번호 변경 기능을 담당합니다.
 * </p>
 */
@Log4j2
public class ManagerDAO {

    // ManagerDAO 싱글턴 인스턴스
    private static ManagerDAO instance;

    // 외부에서 직접 객체 생성을 막기 위한 private 생성자
    private ManagerDAO() {
    }

    /**
     * ManagerDAO 싱글턴 인스턴스를 반환합니다.
     *
     * @return ManagerDAO 인스턴스
     */
    public static ManagerDAO getInstance() {
        if (instance == null) {
            instance = new ManagerDAO();
        }
        return instance;
    }

    /**
     * 관리자 계정을 등록합니다.
     *
     * <p>
     * 입력받은 비밀번호는 BCrypt로 해싱된 뒤 DB에 저장합니다.
     * </p>
     *
     * @param managerVO 등록할 관리자 정보
     */
    public void insertManager(ManagerVO managerVO) {
        String sql = "INSERT INTO manager (manager_id, manager_name, password, email) VALUES (?, ?, ?, ?)";
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, managerVO.getManagerId());
            preparedStatement.setString(2, managerVO.getManagerName());

            // 평문 비밀번호를 BCrypt로 해싱
            String hashedPassword = PasswordUtil.hashPassword(managerVO.getPassword());
            preparedStatement.setString(3, hashedPassword);
            preparedStatement.setString(4, managerVO.getEmail());
            preparedStatement.executeUpdate();  //INSERT 실행

            log.info("관리자 계정 등록 완료 - ID: {}, 이름: {}",
                    managerVO.getManagerId(), managerVO.getManagerName());
        } catch (SQLException e) {
            log.error("관리자 계정 등록 중 오류 발생", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 관리자 아이디로 관리자 정보를 조회합니다.
     *
     * @param managerId 조회할 관리자 아이디
     * @return 조회된 관리자 정보, 없으면 null
     */
    public ManagerVO selectOne(String managerId) {
        String sql = "SELECT * FROM manager WHERE manager_id = ?";
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, managerId);
            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                ManagerVO managerVO = ManagerVO.builder()
                        .managerNo(resultSet.getInt("manager_no"))
                        .managerId(resultSet.getString("manager_id"))
                        .managerName(resultSet.getString("manager_name"))
                        .password(resultSet.getString("password"))  // DB에 저장된 해시값이 담김
                        .email(resultSet.getString("email"))
                        .active(resultSet.getBoolean("active"))
                        .role(resultSet.getString("role"))
                        .build();
                return managerVO;
            }
        } catch (SQLException e) {
            log.error("관리자 조회 중 오류 발생 - ID: {}", managerId, e);
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * 모든 관리자 목록을 조회합니다.
     *
     * @return 관리자 목록
     */
    public List<ManagerVO> selectAll() {
        // 최신 등록 순으로 관리자 목록 조회
        String sql = "SELECT * FROM manager ORDER BY manager_no DESC";
        List<ManagerVO> list = new ArrayList<>();

        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                ManagerVO managerVO = ManagerVO.builder()
                        .managerNo(resultSet.getInt("manager_no"))
                        .managerId(resultSet.getString("manager_id"))
                        .managerName(resultSet.getString("manager_name"))
                        .password(resultSet.getString("password"))
                        .email(resultSet.getString("email"))
                        .active(resultSet.getBoolean("active"))
                        .role(resultSet.getString("role"))
                        .build();
                list.add(managerVO);
            }
        } catch (SQLException e) {
            log.error("관리자 목록 조회 중 오류 발생", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    /**
     * 관리자 계정의 활성화 상태를 변경합니다.
     *
     * @param active    변경할 활성화 상태
     * @param managerId 상태를 변경할 관리자 아이디
     */
    public void updateActive(boolean active, String managerId) {
        String sql = "UPDATE manager set active = ? WHERE manager_id = ?";
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);

            // active 값으로 계정 사용 여부 변경
            preparedStatement.setBoolean(1, active);
            preparedStatement.setString(2, managerId);
            preparedStatement.executeUpdate();

            log.info("관리자 활성화 상태 변경 - ID: {}, Active: {}", managerId, active);

        } catch (SQLException e) {
            log.error("관리자 활성화 상태 변경 중 오류 발생", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 관리자 정보를 수정합니다.
     *
     * <p>
     * 비밀번호가 입력된 경우에는 비밀번호도 함께 변경하고,
     * 비밀번호가 비어 있으면 이름과 이메일만 수정합니다.
     * </p>
     *
     * @param managerVO 수정할 관리자 정보
     */
    public void updateManager(ManagerVO managerVO) {
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();

            // 변경할 비밀번호 값이 입력되었는지 확인
            String password = managerVO.getPassword();
            // 비밀번호 입력 여부에 따라 수정 쿼리 분리
            boolean updatePassword = (password != null && !password.trim().isEmpty());

            String sql;
            PreparedStatement preparedStatement;

            if (updatePassword) {
                // 새 비밀번호가 입력된 경우 비밀번호까지 함께 수정
                sql = "UPDATE manager SET manager_name = ?, password = ?, email = ? WHERE manager_id = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, managerVO.getManagerName());

                String hashedPassword;

                // 기존 BCrypt 해시값이면 다시 해싱하지 않음
                if (password.length() == 60 && password.startsWith("$2a$")) {
                    hashedPassword = password;
                    log.debug("기존 BCrypt 해시 유지 - ID: {}", managerVO.getManagerId());
                } else {
                    // 평문 비밀번호인 경우에만 새로 해싱
                    hashedPassword = PasswordUtil.hashPassword(password);
                    log.info("새 비밀번호로 변경 - ID: {}", managerVO.getManagerId());
                }
                preparedStatement.setString(2, hashedPassword);
                preparedStatement.setString(3, managerVO.getEmail());
                preparedStatement.setString(4, managerVO.getManagerId());

            } else {
                // 비밀번호 입력이 없으면 이름과 이메일만 수정
                sql = "UPDATE manager SET manager_name = ?, email = ? WHERE manager_id = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, managerVO.getManagerName());
                preparedStatement.setString(2, managerVO.getEmail());
                preparedStatement.setString(3, managerVO.getManagerId());

                log.info("비밀번호 제외하고 정보 수정 - ID: {}", managerVO.getManagerId());
            }

            int affectedRows = preparedStatement.executeUpdate();
            preparedStatement.close();

            if (affectedRows > 0) {
                log.info("관리자 정보 업데이트 성공 - ID: {}, 이름: {}, 비밀번호 변경: {}",
                        managerVO.getManagerId(), managerVO.getManagerName(), updatePassword);
            } else {
                log.warn("업데이트된 행이 없음 - ID: {}", managerVO.getManagerId());
            }

        } catch (SQLException e) {
            log.error("관리자 정보 업데이트 중 오류 발생 - ID: {}", managerVO.getManagerId(), e);
            throw new RuntimeException("관리자 정보 업데이트 실패", e);
        }
    }

    /**
     * 관리자 비밀번호만 변경합니다.
     *
     * @param managerId   비밀번호를 변경할 관리자 아이디
     * @param newPassword 새 비밀번호
     */
    public void updatePassword(String managerId, String newPassword) {
        String sql = "UPDATE manager SET password = ? WHERE manager_id = ?";
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);

            // 새 BCrypt를 BCrypt로 해싱
            String hashedPassword = PasswordUtil.hashPassword(newPassword);
            preparedStatement.setString(1, hashedPassword);
            preparedStatement.setString(2, managerId);

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows > 0) {
                log.info("비밀번호 변경 성공 - ID: {}", managerId);
            } else {
                log.warn("비밀번호 변경 실패 - 존재하지 않는 ID: {}", managerId);
            }

        } catch (SQLException e) {
            log.error("비밀번호 변경 중 오류 발생 - ID: {}", managerId, e);
            throw new RuntimeException("비밀번호 변경 실패", e);
        }
    }
}