package org.example.smart_parking_260219.controller.member;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dto.MemberDTO;
import org.example.smart_parking_260219.service.MemberService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@WebServlet(name = "memberController", value = "/member/member_list")
public class MemberListController extends HttpServlet {
    private final MemberService memberService = MemberService.INSTANCE;

    @SneakyThrows
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("=== member list 시작 ===");

        // 만료된 회원 자동 처리
        memberService.expireSubscriptions();

        // 페이징 파라미터
        String pageParam = req.getParameter("page");
        int currentPage = 1;
        if (pageParam != null) {
            try { currentPage = Integer.parseInt(pageParam); }
            catch (NumberFormatException e) { currentPage = 1; }
        }

        // 정렬 파라미터
        String sortColumn = req.getParameter("sort");
        String sortOrder = req.getParameter("order");
        if (sortColumn == null) sortColumn = "memberId";
        if (sortOrder == null) sortOrder = "desc";

        // 전체 회원 조회
        List<MemberDTO> allMembers = memberService.getAllMember();

        // 정렬 적용
        allMembers = sortMembers(allMembers, sortColumn, sortOrder);

        // 페이징 처리
        int itemsPerPage = 10;
        int totalItems = allMembers.size();
        int totalPages = (totalItems > 0) ? (int) Math.ceil((double) totalItems / itemsPerPage) : 1;

        if (currentPage < 1) currentPage = 1;
        if (currentPage > totalPages) currentPage = totalPages;

        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

        List<MemberDTO> pagedMembers = (startIndex < totalItems)
                ? allMembers.subList(startIndex, endIndex)
                : new ArrayList<>();

        int startNo = totalItems - (currentPage - 1) * itemsPerPage;

        req.setAttribute("dtoList", pagedMembers);
        req.setAttribute("currentPage", currentPage);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("totalItems", totalItems);
        req.setAttribute("startNo", startNo);
        req.setAttribute("sortColumn", sortColumn);
        req.setAttribute("sortOrder", sortOrder);

        req.getRequestDispatcher("/WEB-INF/views/member/member_list.jsp").forward(req, resp);
    }

    /* 회원 목록 정렬 */
    private List<MemberDTO> sortMembers(List<MemberDTO> members, String column, String order) {
        boolean asc = "asc".equals(order);

        Comparator<MemberDTO> comparator = switch (column) {
            case "carNum" -> Comparator.comparing(MemberDTO::getCarNum);
            case "carType" -> Comparator.comparing(MemberDTO::CarTypeText);
            case "name" -> Comparator.comparing(MemberDTO::getName);
            case "phone" -> Comparator.comparing(MemberDTO::getPhone);
            case "startDate" -> Comparator.comparing(m ->
                    m.getStartDate() != null ? m.getStartDate() : LocalDate.MIN);
            case "endDate" -> Comparator.comparing(m ->
                    m.getEndDate() != null ? m.getEndDate() : LocalDate.MIN);
            case "status" -> Comparator.comparing(MemberDTO::isSubscribed);
            case "createDate" -> Comparator.comparing(m ->
                    m.getCreateDate() != null ? m.getCreateDate() : LocalDate.MIN);
            default -> Comparator.comparing(MemberDTO::getMemberId);
        };

        if (!asc) comparator = comparator.reversed();

        return members.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @SneakyThrows
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("=== member list POST - 갱신 처리 ===");

        String action = req.getParameter("action");
        String carNum = req.getParameter("carNum");

        try {
            if ("renew".equals(action)) {
                memberService.renewSubscription(carNum);
                log.info("갱신 완료: {}", carNum);
                resp.sendRedirect("/member/member_list?success=renew");
            } else {
                resp.sendRedirect("/member/member_list");
            }
        } catch (Exception e) {
            log.error("갱신 처리 오류", e);
            resp.sendRedirect("/member/member_list?error=renewFail");
        }
    }
}

