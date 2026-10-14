package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.dao.MemberDAO;
import org.example.smart_parking_260219.dto.MemberDTO;
import org.example.smart_parking_260219.util.AppConfig;
import org.example.smart_parking_260219.vo.MemberVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 테스트 DB에 회원을 추가, 조회, 수정, 삭제하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class MemberServiceTest {

    private final MemberService memberService = MemberService.INSTANCE;
    private final MemberDAO memberDAO = MemberDAO.getInstance();
    private String testCarNum;

    @BeforeEach
    void setUp() throws SQLException {
        // IDE에서 직접 실행해도 개발 DB의 회원 정보를 변경하지 않도록 확인함
        assertEquals("jdbc:mariadb://localhost:3308/smart_parking_test", AppConfig.get("db.url"));
        assertEquals("parking_test", AppConfig.get("db.username"));

        String newCarNum = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        // 차량번호는 중복 가능하므로 기존 회원의 번호와 겹치지 않는지 확인함
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM member WHERE car_num = ? LIMIT 1")) {
            statement.setString(1, newCarNum);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertFalse(resultSet.next());
            }
        }

        testCarNum = newCarNum;
        log.info("--- 회원 서비스 테스트 준비: carNum={} ---", testCarNum);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testCarNum == null) {
            return;
        }

        // 같은 차량번호의 갱신 이력까지 이번 테스트 데이터만 정리함
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM member WHERE car_num = ?")) {
            statement.setString(1, testCarNum);
            int deletedCount = statement.executeUpdate();
            log.info("--- 회원 서비스 테스트 정리: carNum={}, 삭제 {}건 ---", testCarNum, deletedCount);
        }
    }

    @Test
    void addMemberTest() throws SQLException {
        LocalDate startDate = LocalDate.now();
        MemberDTO input = MemberDTO.builder()
                .carNum(testCarNum)
                .carType(2)
                .name("테스트회원")
                .phone("010-1111-2222")
                .subscribed(true)
                .startDate(startDate)
                .endDate(startDate.plusMonths(2))
                .subscribedFee(100000)
                .build();

        memberService.addMember(input);
        MemberVO saved = memberDAO.selectOneMember(testCarNum);

        log.info("--- 회원 등록 결과: carNum={}, 회원={} ---", testCarNum, saved);
        assertNotNull(saved);
        assertEquals(testCarNum, saved.getCarNum());
        assertEquals(2, saved.getCarType());
        assertEquals("테스트회원", saved.getName());
        assertEquals("010-1111-2222", saved.getPhone());
        assertTrue(saved.isSubscribed());
        assertEquals(startDate, saved.getStartDate());
        assertEquals(startDate.plusMonths(2), saved.getEndDate());
        assertEquals(100000, saved.getSubscribedFee());
        assertNotNull(saved.getCreateDate());
    }

    @Test
    void getAllMemberTest() throws SQLException {
        insertTestMember();

        List<MemberDTO> members = memberService.getAllMember();
        MemberDTO found = members.stream()
                .filter(member -> testCarNum.equals(member.getCarNum()))
                .findFirst()
                .orElse(null);

        log.info("--- 회원 목록 조회 결과: 전체 {}명, 테스트 회원={} ---", members.size(), found);
        assertNotNull(found);
        assertEquals(testCarNum, found.getCarNum());
        assertEquals("테스트회원", found.getName());
        assertFalse(found.isSubscribed());
    }

    @Test
    void getOneMemberTest() throws SQLException {
        insertTestMember();

        MemberDTO found = memberService.getOneMember(testCarNum);

        log.info("--- 차량번호 회원 조회 결과: carNum={}, 회원={} ---", testCarNum, found);
        assertNotNull(found);
        assertEquals(testCarNum, found.getCarNum());
        assertEquals(1, found.getCarType());
        assertEquals("테스트회원", found.getName());
        assertNull(memberService.getOneMember(testCarNum + "-missing"));
    }

    @Test
    void modifyMemberTest() throws SQLException {
        insertTestMember();
        LocalDate startDate = LocalDate.now().plusDays(5);
        MemberDTO changes = MemberDTO.builder()
                .carNum(testCarNum)
                .carType(3)
                .name("수정된회원")
                .phone("010-3333-4444")
                .subscribed(true)
                .startDate(startDate)
                .endDate(startDate.plusMonths(3))
                .build();

        memberService.modifyMember(changes);
        MemberVO updated = memberDAO.selectOneMember(testCarNum);

        log.info("--- 회원 정보 수정 결과: carNum={}, 회원={} ---", testCarNum, updated);
        assertNotNull(updated);
        assertEquals(3, updated.getCarType());
        assertEquals("수정된회원", updated.getName());
        assertEquals("010-3333-4444", updated.getPhone());
        assertTrue(updated.isSubscribed());
        assertEquals(startDate, updated.getStartDate());
        assertEquals(startDate.plusMonths(3), updated.getEndDate());
    }

    @Test
    void removeMemberTest() throws SQLException {
        insertTestMember();

        memberService.removeMember(testCarNum);

        MemberVO deleted = memberDAO.selectOneMember(testCarNum);
        log.info("--- 회원 삭제 결과: carNum={}, 조회 결과={} ---", testCarNum, deleted);
        assertNull(deleted);
    }

    // 조회, 수정, 삭제 테스트가 사용할 회원은 서비스 등록 메서드와 별개로 직접 준비함
    private void insertTestMember() throws SQLException {
        LocalDate startDate = LocalDate.now();
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO member "
                             + "(car_num, car_type, name, phone, subscribed, start_date, end_date, subscribed_fee) "
                             + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, testCarNum);
            statement.setInt(2, 1);
            statement.setString(3, "테스트회원");
            statement.setString(4, "010-1111-2222");
            statement.setBoolean(5, false);
            statement.setObject(6, startDate);
            statement.setObject(7, startDate.plusMonths(1));
            statement.setInt(8, 100000);
            assertEquals(1, statement.executeUpdate());
        }

        log.info("--- 회원 서비스 테스트 데이터 생성: carNum={} ---", testCarNum);
    }
}
