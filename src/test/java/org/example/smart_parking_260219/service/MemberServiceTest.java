package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dto.MemberDTO;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

// 설정된 DB의 회원 정보를 추가·수정·삭제하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class MemberServiceTest {

    private MemberService memberService;

    @BeforeEach
    public void ready() {
        memberService = MemberService.INSTANCE;
    }

    @Test
    @Disabled("전용 테스트 DB와 검증 코드가 준비될 때까지 실행하지 않음")
    public void addTest() throws SQLException {
        MemberDTO memberDTO = MemberDTO.builder()
                .carNum("12341234")
                .carType(1)
                .name("service")
                .phone("010-1111-2222")
                .subscribed(true)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(2))
                .build();
        memberService.addMember(memberDTO);
    }

    @Test
    public void getAllTest() throws SQLException {
        var members = memberService.getAllMember();

        members.forEach(it -> log.info(it));
    }

    @Test
    public void getOneTest() throws SQLException{
        String carNum = "12341234";

        MemberDTO memberDTO = memberService.getOneMember(carNum);
        log.info(memberDTO);
        Assertions.assertNotNull(memberDTO);
    }

    @Test
    @Disabled("전용 테스트 DB와 검증 코드가 준비될 때까지 실행하지 않음")
    public void modify() throws SQLException {
        MemberDTO memberDTO = MemberDTO.builder()
                .carType(3)
                .name("update")
                .phone("010-3333-4444")
                .subscribed(false)
                .carNum("12341234")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusMonths(2))
                .build();
        memberService.modifyMember(memberDTO);

    }

    @Test
    @Disabled("전용 테스트 DB와 검증 코드가 준비될 때까지 실행하지 않음")
    public void remove() throws SQLException {
        String carNum = "12341234";

        memberService.removeMember(carNum);
    }

}
