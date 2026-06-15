//package org.example.smart_parking_260219.controller.login;
//
//import jakarta.servlet.ServletException;
//import jakarta.servlet.annotation.WebServlet;
//import jakarta.servlet.http.HttpServlet;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import jakarta.servlet.http.HttpSession;
//import lombok.extern.log4j.Log4j2;
//import org.example.smart_parking_260219.dao.ManagerDAO;
//import org.example.smart_parking_260219.dto.ManagerDTO;
//import org.example.smart_parking_260219.service.ManagerService;
//import org.example.smart_parking_260219.vo.ManagerVO;
//
//import java.io.IOException;
//import java.util.List;
//
//@Log4j2
//@WebServlet(name = "managerController", value = {"/mgr/*"})
//public class ManagerController extends HttpServlet {
//
//    // 관리자 DB 작업을 처리하는 DAO
//    private final ManagerDAO managerDAO = ManagerDAO.getInstance();
//
//    @Override
//    protected void doGet(HttpServletRequest request, HttpServletResponse response)
//            throws ServletException, IOException {
//
//        log.info("=== ManagerController doGet() 호출 ===");
//        log.info("Request URI: {}", request.getRequestURI());
//        log.info("Context Path: {}", request.getContextPath());
//        log.info("Servlet Path: {}", request.getServletPath());
//        log.info("Path Info: {}", request.getPathInfo());
//
//        // 로그인 세션 확인
//        HttpSession session = request.getSession(false);
//
//        if (session != null) {
//            log.info("세션 ID: {}", session.getId());
//            Object loginManager = session.getAttribute("loginManager");
//            log.info("loginManager: {}", loginManager);
//        } else {
//            log.warn("세션이 없음");
//        }
//
//        if (session == null || session.getAttribute("loginManager") == null) {
//            log.warn("미인증 - 로그인 페이지로 리다이렉트");
//            response.sendRedirect(request.getContextPath() + "/login");
//            return;
//        }
//
//        // 요청 경로 확인
//        String pathInfo = request.getPathInfo();
//        log.info("처리할 pathInfo: {}", pathInfo);
//
//        // 경로가 없으면 관리자 목록으로 이동
//        if (pathInfo == null || pathInfo.equals("/")) {
//            pathInfo = "/list";
//        }
//
//        switch (pathInfo) {
//            case "/add":
//                // 관리자 추가 화면으로 이동
//                log.info("관리자 추가 화면으로 이동");
//                request.getRequestDispatcher("/WEB-INF/views/mgr_add.jsp").forward(request, response);
//                break;
//
//            case "/view":
//                // 관리자 상세 조회 화면 처리
//                log.info("관리자 상세 조회 화면 처리");
//
//                // URL에서 ?id=admin 처럼 넘겨온 ID 값을 읽음
//                String viewId = request.getParameter("id");
//                log.info("조회할 관리자 ID: {}", viewId);
//
//                // id 파라미터가 없으면 현재 로그인한 관리자 정보 조회
//                if (viewId == null || viewId.trim().isEmpty()) {
//                    log.info("ID 파라미터가 없어 세션에서 정보를 찾습니다.");
//                    ManagerVO loginManager = (ManagerVO) session.getAttribute("loginManager");
//                    if (loginManager != null) {
//                        viewId = loginManager.getManagerId();
//                    }
//                }
//                log.info("최종 조회할 관리자 ID: {}", viewId);
//
//                if (viewId != null && !viewId.isEmpty()) {
//                    try {
//                        // DAO를 통해 DB에서 관리자 정보 조회 후 읽어옴
//                        ManagerVO manager = managerDAO.selectOne(viewId);
//
//                        if (manager != null) {
//
//                            // ★ 추가: ADMIN 계정이면 view 접근 차단 후 목록으로 리다이렉트
//                            if ("ADMIN".equals(manager.getRole())) {
//                                log.warn("최고관리자 계정({}) view 접근 차단 → 목록으로 리다이렉트", viewId);
//                                HttpSession sess = request.getSession(false);
//                                if (sess != null) {
//                                    sess.setAttribute("error",
//                                            "최고 관리자 계정은 '최고 관리자 정보 수정' 메뉴를 이용해 주세요.");
//                                }
//                                response.sendRedirect(request.getContextPath() + "/mgr/list");
//                                return;   // ← 반드시 return
//                            }
//                            // ★ 추가 끝
//
//                            // 조회한 정보 'manager'를 가져옴
//                            request.setAttribute("manager", manager);
//                            log.info("관리자 데이터 조회 성공: {}", manager.getManagerName());
//                        } else {
//                            log.warn("ID가 {}인 관리자를 찾을 수 없음", viewId);
//                            request.setAttribute("error", "존재하지 않는 관리자입니다.");
//                        }
//                    } catch (Exception e) {
//                        log.error("관리자 조회 중 DB 오류", e);
//                        request.setAttribute("error", "데이터를 가져오는 중 오류가 발생했습니다.");
//                    }
//                } else {
//                    log.warn("조회할 ID를 찾을 수 없음 (파라미터X, 세션X)");
//                    request.setAttribute("error", "조회할 관리자 정보를 특정할 수 없습니다.");
//                }
//
//                // 조회한 정보 'manager'를 JSP에 전달(setAttribute)
//                request.getRequestDispatcher("/WEB-INF/views/mgr_view.jsp").forward(request, response);
//                break;
//
//            /* 수정 */
//            case "/modify":
//                log.info("관리자 수정 페이지 처리");
//
//                // URL에서 ?id=admin 처럼 넘겨온 ID 값을 읽음
//                String modifyId = request.getParameter("id");
//                log.info("수정할 관리자 ID: {}", modifyId);
//
//                // 만약 id 파라미터가 없다면, 현재 로그인한 '나'의 정보를 보여줌.
//                if (modifyId == null || modifyId.trim().isEmpty()) {
//                    log.info("ID 파라미터가 없어 세션에서 정보를 찾습니다.");
//                    ManagerVO loginManager = (ManagerVO) session.getAttribute("loginManager");
//                    if (loginManager != null) {
//                        modifyId = loginManager.getManagerId();
//                    }
//                }
//                log.info("최종 수정할 관리자 ID: {}", modifyId);
//
//                if (modifyId != null && !modifyId.isEmpty()) {
//                    try {
//                        // DAO를 통해 DB에서 관리자 정보 조회 후 읽어옴
//                        ManagerVO manager = managerDAO.selectOne(modifyId);
//
//                        if (manager != null) {
//                            // 조회한 정보 'manager'를 가져옴
//                            request.setAttribute("manager", manager);
//                            log.info("관리자 데이터 조회 성공: {}", manager.getManagerName());
//                        } else {
//                            log.warn("ID가 {}인 관리자를 찾을 수 없음", modifyId);
//                            request.setAttribute("error", "존재하지 않는 관리자입니다.");
//                        }
//                    } catch (Exception e) {
//                        log.error("관리자 조회 중 DB 오류", e);
//                        request.setAttribute("error", "데이터를 가져오는 중 오류가 발생했습니다.");
//                    }
//                } else {
//                    log.warn("수정할 ID를 찾을 수 없음 (파라미터X, 세션X)");
//                    request.setAttribute("error", "수정할 관리자 정보를 특정할 수 없습니다.");
//                }
//
//                // 조회한 정보 'manager'를 JSP에 전달(setAttribute)
//                request.getRequestDispatcher("/WEB-INF/views/mgr_modify.jsp").forward(request, response);
//                break;
//
//            /* 관리자 목록 조회 */
//            case "/list":
//                log.info("관리자 목록 조회 요청 처리 중..");
//                try {
//                    // Service계층 호출하여 전체 관리자 리스트를 DTO형태로 가져옴
//                    List<ManagerDTO> dtoList = ManagerService.INSTANCE.getAllManagers();
//                    // JSP 파일에서 반복문을 돌릴 수 있도록 리스트로 전달
//                    request.setAttribute("managerList", dtoList);
//                    log.info("목록 조회 완료: {}명", dtoList.size());
//
//                } catch (Exception e) {
//                    log.error("목록 조회 중 오류 발생", e);
//                    request.setAttribute("error", "목록을 불러오는 중 오류가 발생했습니다.");
//                }
//                // 목록 페이지로 포워딩
//                request.getRequestDispatcher("/WEB-INF/views/mgr_list.jsp").forward(request, response);
//                break;
//
//            /* ★ 일반 관리자 본인 정보 수정 페이지 (로그인한 본인만 접근) */
//            case "/my_modify":
//                log.info("일반 관리자 본인 정보 수정 페이지 요청");
//
//                // 세션에서 로그인한 관리자 정보를 가져와 그대로 JSP로 전달
//                // (아이디를 URL 파라미터로 노출하지 않고 세션 기준으로 처리)
//                ManagerVO myManager = (ManagerVO) session.getAttribute("loginManager");
//
//                if (myManager == null) {
//                    log.warn("세션에 로그인 정보 없음 - 로그인 페이지로 리다이렉트");
//                    response.sendRedirect(request.getContextPath() + "/login");
//                    return;
//                }
//
//                // 보안: 순수 ADMIN은 전용 수정 페이지 사용 / SUPER는 양쪽 모두 접근 허용
//                if ("ADMIN".equals(myManager.getRole()) && !SuperKeyConfig.SUPER_ROLE.equals(myManager.getRole())) {
//                    log.warn("ADMIN 계정의 /my_modify 접근 차단 → /mgr/modify 로 리다이렉트");
//                    response.sendRedirect(request.getContextPath() + "/mgr/modify");
//                    return;
//                }
//
//                // DB에서 최신 정보를 다시 조회 (세션 정보는 오래될 수 있음)
//                try {
//                    ManagerVO freshManager = managerDAO.selectOne(myManager.getManagerId());
//                    if (freshManager != null) {
//                        request.setAttribute("manager", freshManager);
//                        log.info("본인 정보 조회 성공 - ID: {}", freshManager.getManagerId());
//                    } else {
//                        log.warn("DB에서 본인 정보를 찾을 수 없음 - ID: {}", myManager.getManagerId());
//                        request.setAttribute("error", "계정 정보를 불러올 수 없습니다.");
//                    }
//                } catch (Exception e) {
//                    log.error("본인 정보 조회 중 오류 발생", e);
//                    request.setAttribute("error", "데이터 조회 중 오류가 발생했습니다.");
//                }
//
//                request.getRequestDispatcher("/WEB-INF/views/mgr_my_modify.jsp").forward(request, response);
//                break;
//
//            /* 일반 관리자 정보 수정 페이지 호출 (ADMIN 전용) */
//            case "/modify_normal":
//                log.info("일반 관리자 수정 페이지 처리");
//
//                // 1. URL 파라미터에서 수정할 대상 ID를 가져옴
//                String targetId = request.getParameter("id");
//                log.info("수정 대상 ID: {}", targetId);
//
//                // 2. 파라미터가 없으면 목록으로 리다이렉트
//                if (targetId == null || targetId.trim().isEmpty()) {
//                    log.warn("수정할 ID가 없어 목록으로 돌아갑니다.");
//                    response.sendRedirect(request.getContextPath() + "/mgr/list");
//                    return;
//                }
//
//                try {
//                    // 3. DAO를 통해 수정할 관리자의 상세 정보를 가져옴
//                    ManagerVO targetManager = managerDAO.selectOne(targetId);
//
//                    if (targetManager != null) {
//                        // JSP에서 사용할 수 있도록 "manager"라는 이름으로 객체 바인딩
//                        request.setAttribute("manager", targetManager);
//                        log.info("수정 대상 조회 성공: {}", targetManager.getManagerName());
//                    } else {
//                        log.warn("ID가 {}인 관리자를 찾을 수 없음", targetId);
//                        request.setAttribute("error", "존재하지 않는 관리자입니다.");
//                    }
//                } catch (Exception e) {
//                    log.error("관리자 조회 중 DB 오류", e);
//                    request.setAttribute("error", "데이터 조회 중 오류 발생");
//                }
//
//                // 4. 작성하신 일반 관리자 수정 전용 JSP로 포워딩
//                request.getRequestDispatcher("/WEB-INF/views/mgr_modify_normal.jsp").forward(request, response);
//                break;
//
//
//            /* 정의되지 않은 경로는 404 에러 */
//            default:
//                log.warn("알 수 없는 경로: {}", pathInfo);
//                response.sendError(HttpServletResponse.SC_NOT_FOUND);
//                break;
//        }
//    }
//
//    @Override
//    protected void doPost(HttpServletRequest request, HttpServletResponse response)
//            throws ServletException, IOException {
//
//        log.info("=== ManagerController doPost() 호출 ===");
//        log.info("Path Info: {}", request.getPathInfo());
//
//        request.setCharacterEncoding("UTF-8");  //한글 깨짐 방지
//
//        // 세션 체크
//        HttpSession session = request.getSession(false);
//        // 세션이 없거나, 세션 안에 로그인 정보(loginManager) 없으면 가짜로 요청으로 간주
//        if (session == null || session.getAttribute("loginManager") == null) {
//            log.warn("미인증 POST 요청 - 로그인 페이지로 리다이렉트");
//            response.sendRedirect(request.getContextPath() + "/login");
//            return;
//        }
//
//        // 경로 추출(pathInfo)
//        String pathInfo = request.getPathInfo();
//
//        // 경로(URL)에 따른 분기 로직
//        if ("/add".equals(pathInfo)) {
//            log.info("관리자 추가 처리 시작");
//            addManager(request, response);  //관리자 등록
//        } else if ("/modify".equals(pathInfo)) {
//            log.info("관리자 수정 처리 시작");
//            modifyManager(request, response);
//        } else if ("/my_modify".equals(pathInfo)) {
//            log.info("일반 관리자 본인 정보 수정 처리 시작");
//            modifyMyInfo(request, response);
//        } else if ("/modify_normal".equals(pathInfo)) { // <--- 이 부분 추가
//            log.info("일반 관리자 수정 처리 시작");
//            modifyManagerNormal(request, response);
//        } else if ("/toggleActive".equals(pathInfo)) {
//            log.info("관리자 활성화 토글 처리 시작");
//            toggleManagerActive(request, response);
//        } else {
//            log.warn("알 수 없는 POST 경로: {}", pathInfo);
//            // 엉뚱 URL 요청 시, 404 Not Found
//            response.sendError(HttpServletResponse.SC_NOT_FOUND);
//        }
//    }
//
//    /* 관리자 수정 처리 */
//    private void modifyManager(HttpServletRequest request, HttpServletResponse response)
//            throws ServletException, IOException {
//
//        // 세션 체크
//        HttpSession session = request.getSession(false);
//
//        // JSP 파일에서 사용자가 입력한 값을 변수에 담음
//        String managerId = request.getParameter("id");  // 누구를 수정할 지 결정하는 키 값
//        String managerName = request.getParameter("name");
//        String password = request.getParameter("pw");  // 변경할 새 비밀번호 (비어있을 수 있음)
//        String passwordConfirm = request.getParameter("passwordConfirm");  // 확인용 비밀번호
//        String email = request.getParameter("email");
//
//        log.info("관리자 정보 수정 요청 - ID: {}, 이름: {}, 이메일: {}", managerId, managerName, email);
//        log.info("비밀번호 변경 여부: {}", (password != null && !password.trim().isEmpty() ? "Yes" : "No"));
//
//        // 필수 입력 항목이 비어있는지 확인
//        if (managerId == null || managerId.trim().isEmpty() ||
//                managerName == null || managerName.trim().isEmpty() ||
//                email == null || email.trim().isEmpty()) {
//
////            log.info("1, {}", managerId);
////            log.info("2, {}",managerName);
////            log.info("3, {}",email);
//            log.warn("필수 입력값 누락");
//            request.setAttribute("error", "ID, 이름, 이메일은 필수 입력값입니다.");
//
//            try {
//                // 에러 발생 시, 기존 정보를 다시 DB에서 읽어와 화면에 뿌려줘야 입력폼이 유지됨
//                ManagerVO manager = managerDAO.selectOne(managerId);
//                request.setAttribute("manager", manager);
//            } catch (Exception e) {
//                log.error("관리자 정보 재조회 실패", e);
//            }
//            request.getRequestDispatcher("/WEB-INF/views/mgr_modify.jsp").forward(request, response);
//            return;
//        }
//
//        // 비밀번호 변경 시 일치 확인
//        // 비밀번호 칸에 무언가 적었을 때만 실행
//        if (password != null && !password.trim().isEmpty()) {
//            // 새 비밀번호 & 확인용 비밀번호가 동일한지 확인
//            if (!password.equals(passwordConfirm)) {
//                log.warn("비밀번호 불일치");
//                request.setAttribute("error", "비밀번호가 일치하지 않습니다.");
//
//                try {
//                    // 기존 정보를 다시 DB에서 읽어와 화면에 뿌려줘야 입력폼이 유지됨
//                    ManagerVO manager = managerDAO.selectOne(managerId);
//                    request.setAttribute("manager", manager);
//                } catch (Exception e) {
//                    log.error("관리자 정보 재조회 실패", e);
//                }
//
//                request.getRequestDispatcher("/WEB-INF/views/mgr_modify.jsp").forward(request, response);
//                return;
//            }
//
//            // 비밀번호 길이 검증
//            if (password.length() < 4) {
//                log.warn("비밀번호 길이 부족: {}", password.length());
//                request.setAttribute("error", "비밀번호는 최소 4자 이상이어야 합니다.");
//
//                try {
//                    // 기존 정보를 다시 DB에서 읽어와 화면에 뿌려줘야 입력폼이 유지됨
//                    ManagerVO manager = managerDAO.selectOne(managerId);
//                    request.setAttribute("manager", manager);
//                } catch (Exception e) {
//                    log.error("관리자 정보 재조회 실패", e);
//                }
//                request.getRequestDispatcher("/WEB-INF/views/mgr_modify.jsp").forward(request, response);
//                return;
//            }
//        }
//
//        // 이메일 형식 검증
//        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
//            log.warn("잘못된 이메일 형식: {}", email);
//            request.setAttribute("error", "올바른 이메일 형식이 아닙니다.");
//
//            try {
//                ManagerVO manager = managerDAO.selectOne(managerId);
//                request.setAttribute("manager", manager);
//            } catch (Exception e) {
//                log.error("관리자 정보 재조회 실패", e);
//            }
//
//            request.getRequestDispatcher("/WEB-INF/views/mgr_modify.jsp").forward(request, response);
//            return;
//        }
//
//        try {
//            // 빌더 패턴으로 수정할 정보를 담은 객체(VO)를 만듬
//            // DAO의 updateManager()에서 BCrypt 해싱 처리
//            ManagerVO managerVO = ManagerVO.builder()
//                    .managerId(managerId)
//                    .managerName(managerName)
//                    .password(password)  // 평문으로 넘기면 DAO 내부에서 암호화(BCrypt)수행
//                    .email(email)
//                    .build();
//
//            // DB 업데이트 (DAO 내부에서 BCrypt 해싱 수행)
//            managerDAO.updateManager(managerVO);
//            log.info("관리자 정보 수정 완료 - ID: {}", managerId);
//
//            // 본인 정보 수정 시 재로그인 요구
//            ManagerVO loginManager = (ManagerVO) session.getAttribute("loginManager");
//
//            // 수정된 아이디가 현재 로그인 중인 ID와 같을 경우 (본인 정보 수정한 경우)
//            if (loginManager != null && managerId.equals(loginManager.getManagerId())) {
//                log.info("최고 관리자 정보 수정 - 재로그인 필요");
//
//                // 성공 메시지 설정
//                session.setAttribute("logoutMessage", "관리자 정보가 변경되었으니 다시 로그인해주세요.");
//                // 모든 세션 정보 삭제 (로그아웃)
//                session.invalidate();
//                // 로그인 페이지로 리다이렉트
//                response.sendRedirect(request.getContextPath() + "/login");
//            } else {
//                // 다른 관리자의 정보를 수정한 경우
//                log.info("일반 관리자 정보 수정 완료 - ID: {}", managerId);
//                // 성공 메시지와 함께 조회 페이지로 리다이렉트
//                session.setAttribute("successMessage", "관리자 정보가 성공적으로 수정되었습니다.");
//                // 관리자 목록 페이지로 리다이렉트
//                response.sendRedirect(request.getContextPath() + "/mgr/view?id=" + managerId);
//            }
//
//        } catch (Exception e) {
//            log.error("관리자 정보 수정 중 오류 발생", e);
//            request.setAttribute("error", "정보 수정 중 오류가 발생했습니다: " + e.getMessage());
//
//            try {
//                ManagerVO manager = managerDAO.selectOne(managerId);
//                request.setAttribute("manager", manager);
//            } catch (Exception ex) {
//                log.error("관리자 정보 재조회 실패", ex);
//            }
//
//            request.getRequestDispatcher("/WEB-INF/views/mgr_modify.jsp").forward(request, response);
//        }
//    }
//
//    /* 일반 관리자 정보 수정 처리 (ADMIN이 타 관리자 수정 시) */
//    private void modifyManagerNormal(HttpServletRequest request, HttpServletResponse response)
//            throws ServletException, IOException {
//
//        // 1. 폼 데이터 수신 (JSP의 name 속성과 일치해야 함)
//        String managerId = request.getParameter("managerId"); // hidden 필드에서 온 ID (기존 ID)
//        String managerName = request.getParameter("name");
//        String password = request.getParameter("pw");        // JSP의 name="pw"
//        String email = request.getParameter("email");
//
//        log.info("수정 요청 수신 - ID: {}, Name: {}, PW입력여부: {}, Email: {}",
//                managerId, managerName, (password != null && !password.isEmpty()), email);
//
//        try {
//            ManagerVO existing = managerDAO.selectOne(managerId);
//            if (existing == null) {
//                request.setAttribute("error", "존재하지 않는 관리자입니다.");
//                request.getRequestDispatcher("/WEB-INF/views/mgr_modify_normal.jsp").forward(request, response);
//                return;
//            }
//
//            // ★ 추가: POST 위조 요청으로 ADMIN 계정 수정 시도 차단
//            if ("ADMIN".equals(existing.getRole())) {
//                log.warn("최고관리자 계정({}) POST 수정 시도 차단", managerId);
//                HttpSession sess = request.getSession(false);
//                if (sess != null) {
//                    sess.setAttribute("error",
//                            "최고 관리자 계정은 '최고 관리자 정보 수정' 메뉴를 이용해 주세요.");
//                }
//                response.sendRedirect(request.getContextPath() + "/mgr/list");
//                return;
//            }
//            // ★ 추가 끝
//
//            // 2. 빌더로 업데이트 객체 생성
//            ManagerVO.ManagerVOBuilder builder = ManagerVO.builder()
//                    .managerNo(existing.getManagerNo())
//                    .managerId(existing.getManagerId())
//                    .managerName(managerName)
//                    .email(email)
//                    .active(existing.isActive())
//                    .role(existing.getRole());
//
//            // 3. 비밀번호 처리 (매우 중요)
//            if (password != null && !password.trim().isEmpty()) {
//                // 사용자가 비밀번호를 입력했을 때만 암호화해서 교체
//                log.info("비밀번호 변경을 수행합니다.");
////                builder.password(managerDAO.passEncode(password));
//                builder.password(password);
//            } else {
//                // 입력하지 않았다면 DB에 있던 기존 암호화된 비밀번호 유지
//                log.info("기존 비밀번호를 유지합니다.");
//                builder.password(existing.getPassword());
//            }
//
//            // 4. DB 업데이트 실행
//            managerDAO.updateManager(builder.build());
//
//            log.info("관리자 수정 완료 - ID: {}", managerId);
//
//            // ★ 요청자 역할에 따른 분기 처리
//            HttpSession sess = request.getSession(false);
//            ManagerVO currentLogin = (sess != null) ? (ManagerVO) sess.getAttribute("loginManager") : null;
//
//            if (currentLogin != null && "NORMAL".equals(currentLogin.getRole())) {
//                // 일반 관리자가 본인 정보를 수정한 경우 → 세션 무효화 후 재로그인 유도
//                log.info("일반 관리자 본인 수정 완료 - 세션 무효화 후 로그인 페이지로 이동");
//                sess.setAttribute("logoutMessage", "정보가 수정되었습니다. 변경된 정보로 다시 로그인해주세요.");
//                sess.invalidate();
//                response.sendRedirect(request.getContextPath() + "/login");
//            } else {
//                // ADMIN이 타 관리자 수정한 경우 → 관리자 목록으로 이동
//                log.info("ADMIN에 의한 관리자 수정 완료 - 목록으로 이동");
//                request.getSession().setAttribute("successMessage", "정보가 성공적으로 수정되었습니다.");
//                response.sendRedirect(request.getContextPath() + "/mgr/list");
//            }
//
//        } catch (Exception e) {
//            log.error("수정 중 오류 발생", e);
//            request.setAttribute("error", "오류가 발생했습니다: " + e.getMessage());
//            request.getRequestDispatcher("/WEB-INF/views/mgr_modify_normal.jsp").forward(request, response);
//        }
//    }
//
//    /**
//     * 일반 관리자 본인 정보 수정 처리 (POST /mgr/my_modify)
//     * <p>
//     * - 아이디는 수정 불가 (세션/DB 일치 보장)
//     * - 이름, 비밀번호(선택), 이메일 수정 가능
//     * - 수정 완료 후 세션 갱신 및 재로그인 유도
//     */
//    private void modifyMyInfo(HttpServletRequest request, HttpServletResponse response)
//            throws ServletException, IOException {
//
//        HttpSession session = request.getSession(false);
//
//        // 세션에서 현재 로그인한 관리자 정보 취득
//        ManagerVO loginManager = (ManagerVO) session.getAttribute("loginManager");
//        if (loginManager == null) {
//            log.warn("세션 없음 - 로그인 페이지로 리다이렉트");
//            response.sendRedirect(request.getContextPath() + "/login");
//            return;
//        }
//
//        // 보안: 순수 ADMIN은 이 엔드포인트 사용 불가 / SUPER는 허용
//        if ("ADMIN".equals(loginManager.getRole()) && !SuperKeyConfig.SUPER_ROLE.equals(loginManager.getRole())) {
//            log.warn("ADMIN이 /my_modify POST 시도 - 차단");
//            response.sendRedirect(request.getContextPath() + "/mgr/modify");
//            return;
//        }
//
//        // ★ 아이디는 hidden 필드로 오지만, 세션의 ID와 반드시 일치해야 함 (위조 요청 방어)
//        String sessionId = loginManager.getManagerId();
//        String requestedId = request.getParameter("managerId");
//
//        if (!sessionId.equals(requestedId)) {
//            log.warn("세션 ID({})와 요청 ID({}) 불일치 - 위조 요청 차단", sessionId, requestedId);
//            response.sendError(HttpServletResponse.SC_FORBIDDEN, "잘못된 요청입니다.");
//            return;
//        }
//
//        String managerName = request.getParameter("name");
//        String password = request.getParameter("pw");
//        String email = request.getParameter("email");
//
//        log.info("본인 정보 수정 요청 - ID: {}, 이름: {}, 이메일: {}, 비밀번호 변경: {}",
//                sessionId, managerName, email,
//                (password != null && !password.trim().isEmpty() ? "Yes" : "No"));
//
//        // 필수값 검증
//        if (managerName == null || managerName.trim().isEmpty() ||
//                email == null || email.trim().isEmpty()) {
//            log.warn("필수 입력값 누락");
//            request.setAttribute("error", "이름과 이메일은 필수 입력값입니다.");
//            ManagerVO fresh = managerDAO.selectOne(sessionId);
//            request.setAttribute("manager", fresh);
//            request.getRequestDispatcher("/WEB-INF/views/mgr_my_modify.jsp").forward(request, response);
//            return;
//        }
//
//        // 비밀번호 길이 검증 (입력한 경우만)
//        if (password != null && !password.trim().isEmpty() && password.length() < 4) {
//            log.warn("비밀번호 길이 부족: {}", password.length());
//            request.setAttribute("error", "비밀번호는 최소 4자 이상이어야 합니다.");
//            ManagerVO fresh = managerDAO.selectOne(sessionId);
//            request.setAttribute("manager", fresh);
//            request.getRequestDispatcher("/WEB-INF/views/mgr_my_modify.jsp").forward(request, response);
//            return;
//        }
//
//        // 이메일 형식 검증
//        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
//            log.warn("잘못된 이메일 형식: {}", email);
//            request.setAttribute("error", "올바른 이메일 형식이 아닙니다.");
//            ManagerVO fresh = managerDAO.selectOne(sessionId);
//            request.setAttribute("manager", fresh);
//            request.getRequestDispatcher("/WEB-INF/views/mgr_my_modify.jsp").forward(request, response);
//            return;
//        }
//
//        try {
//            // 기존 정보 조회
//            ManagerVO existing = managerDAO.selectOne(sessionId);
//            if (existing == null) {
//                request.setAttribute("error", "존재하지 않는 계정입니다.");
//                request.getRequestDispatcher("/WEB-INF/views/mgr_my_modify.jsp").forward(request, response);
//                return;
//            }
//
//            // 수정할 정보 빌드
//            ManagerVO.ManagerVOBuilder builder = ManagerVO.builder()
//                    .managerNo(existing.getManagerNo())
//                    .managerId(sessionId)          // ★ 아이디는 절대 변경 안 함
//                    .managerName(managerName.trim())
//                    .email(email.trim())
//                    .active(existing.isActive())
//                    .role(existing.getRole());
//
//            // 비밀번호: 입력이 없으면 기존 해시 유지
//            if (password != null && !password.trim().isEmpty()) {
//                log.info("비밀번호 변경 수행 - ID: {}", sessionId);
//                builder.password(password);        // DAO의 updateManager()에서 BCrypt 해싱
//            } else {
//                log.info("비밀번호 유지 - ID: {}", sessionId);
//                builder.password(existing.getPassword());  // 기존 해시값 유지
//            }
//
//            managerDAO.updateManager(builder.build());
//            log.info("본인 정보 수정 완료 - ID: {}", sessionId);
//
//            // ★ SUPER 계정: 세션 유지 + 대시보드 이동 (재로그인 불필요)
//            if (SuperKeyConfig.SUPER_ROLE.equals(loginManager.getRole())) {
//                session.setAttribute("successMessage", "정보가 수정되었습니다.");
//                response.sendRedirect(request.getContextPath() + "/dashboard");
//                return;
//            }
//
//            // 일반 관리자: 수정 완료 후 세션 무효화 → 재로그인 유도
//            session.setAttribute("logoutMessage", "정보가 수정되었습니다. 변경된 정보로 다시 로그인해주세요.");
//            session.invalidate();
//            response.sendRedirect(request.getContextPath() + "/login");
//
//        } catch (Exception e) {
//            log.error("본인 정보 수정 중 오류 발생 - ID: {}", sessionId, e);
//            request.setAttribute("error", "정보 수정 중 오류가 발생했습니다: " + e.getMessage());
//            try {
//                ManagerVO fresh = managerDAO.selectOne(sessionId);
//                request.setAttribute("manager", fresh);
//            } catch (Exception ex) {
//                log.error("재조회 실패", ex);
//            }
//            request.getRequestDispatcher("/WEB-INF/views/mgr_my_modify.jsp").forward(request, response);
//        }
//    }
//
//    /* 관리자 추가 처리 */
//    private void addManager(HttpServletRequest request, HttpServletResponse response)
//            throws ServletException, IOException {
//
//        // JSP 파일에서 사용자가 입력한 값을 변수에 담음
//        String managerId = request.getParameter("id");
//        String managerName = request.getParameter("name");
//        String password = request.getParameter("pw");
//        String passwordConfirm = request.getParameter("passwordConfirm");
//        String email = request.getParameter("email");
//
//        log.info("관리자 추가 요청 - ID: {}, 이름: {}, 이메일: {}", managerId, managerName, email);
//
//        // 필수 입력값 검증
//        if (managerId == null || managerId.trim().isEmpty() ||
//                managerName == null || managerName.trim().isEmpty() ||
//                password == null || password.trim().isEmpty() ||
//                email == null || email.trim().isEmpty()) {
//
//            log.warn("필수 입력값 누락");
//            request.setAttribute("error", "모든 필드를 입력해주세요.");
//            // 에러가 나서 돌아가더라도 사용자가 이미 입력했던 값들은 그대로 유지
//            request.setAttribute("managerId", managerId);
//            request.setAttribute("managerName", managerName);
//            request.setAttribute("email", email);
//
//            // 다시 입력 폼으로 이동
//            request.getRequestDispatcher("/WEB-INF/views/mgr_add.jsp").forward(request, response);
//            return;
//        }
//
//        // 비밀번호 일치 확인
//        if (!password.equals(passwordConfirm)) {
//            log.warn("비밀번호 불일치");
//            request.setAttribute("error", "비밀번호가 일치하지 않습니다.");
//            request.setAttribute("managerId", managerId);
//            request.setAttribute("managerName", managerName);
//            request.setAttribute("email", email);
//            request.getRequestDispatcher("/WEB-INF/views/mgr_add.jsp").forward(request, response);
//            return;
//        }
//
//        // 비밀번호 길이 검증
//        if (password.length() < 4) {
//            log.warn("비밀번호 길이 부족: {}", password.length());
//            request.setAttribute("error", "비밀번호는 최소 4자 이상이어야 합니다.");
//            request.setAttribute("managerId", managerId);
//            request.setAttribute("managerName", managerName);
//            request.setAttribute("email", email);
//            request.getRequestDispatcher("/WEB-INF/views/mgr_add.jsp").forward(request, response);
//            return;
//        }
//
//        // 이메일 형식 검증
//        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
//            log.warn("잘못된 이메일 형식: {}", email);
//            request.setAttribute("error", "올바른 이메일 형식이 아닙니다.");
//            request.setAttribute("managerId", managerId);
//            request.setAttribute("managerName", managerName);
//            request.setAttribute("email", email);
//            request.getRequestDispatcher("/WEB-INF/views/mgr_add.jsp").forward(request, response);
//            return;
//        }
//
//        try {
//            // 중복 ID 체크
//            ManagerVO existingManager = managerDAO.selectOne(managerId);
//            if (existingManager != null) {
//                log.warn("중복된 관리자 ID: {}", managerId);
//                request.setAttribute("error", "이미 사용 중인 아이디입니다.");
//                request.setAttribute("managerId", managerId);
//                request.setAttribute("managerName", managerName);
//                request.setAttribute("email", email);
//                request.getRequestDispatcher("/WEB-INF/views/mgr_add.jsp").forward(request, response);
//                return;
//            }
//
//            // 빌더 패턴으로 수정할 정보를 담은 객체(VO)를 만듬
//            // DAO의 insertManager()에서 BCrypt 해싱 처리
//            ManagerVO newManager = ManagerVO.builder()
//                    .managerId(managerId)
//                    .managerName(managerName)
//                    .password(password)  // 평문으로 넘기면 DAO 내부에서 암호화(BCrypt)수행
//                    .email(email)
//                    .active(true)  // 새로 생성된 계정은 바로 사용할 수 있게 활성화 상태로 생성
//                    .build();
//
//            // DB에 저장
//            managerDAO.insertManager(newManager);
//            log.info("관리자 추가 성공 - ID: {}, 이름: {}", managerId, managerName);
//
//            // 성공 메시지와 함께 대시보드로 리다이렉트
//            HttpSession session = request.getSession();
//            session.setAttribute("successMessage", "관리자가 성공적으로 추가되었습니다.");
//
//            log.info("대시보드로 리다이렉트: {}/dashboard", request.getContextPath());
//            response.sendRedirect(request.getContextPath() + "/dashboard");
//
//        } catch (Exception e) {
//            log.error("관리자 추가 중 오류 발생", e);
//            request.setAttribute("error", "관리자 추가 중 오류가 발생했습니다: " + e.getMessage());
//            request.setAttribute("managerId", managerId);
//            request.setAttribute("managerName", managerName);
//            request.setAttribute("email", email);
//            request.getRequestDispatcher("/WEB-INF/views/mgr_add.jsp").forward(request, response);
//        }
//    }
//
//    /* 관리자 계정 활성화/비활성화 */
//    private void toggleManagerActive(HttpServletRequest request, HttpServletResponse response)
//            throws ServletException, IOException {
//
//        // 현재 세션에 로그인된 관리자 ID 가져오기 (본인 차단 방지용)
//        HttpSession session = request.getSession(false);
//        String currentLoginId = null;
//
//        if (session != null) {
//            ManagerVO loginVO = (ManagerVO) session.getAttribute("loginManager");
//            if (loginVO != null) {
//                currentLoginId = loginVO.getManagerId();  // 현재 로그인한 ID
//            }
//        }
//
//        // 파라미터 수신
//        String targetId = request.getParameter("managerId");  // 변경할 대상 ID
//        String activeStr = request.getParameter("active");  // 변경할 상태값
//        log.info("관리자 상태 변경 요청 - ID: {}, active: {}", targetId, activeStr);
//
//        // 값이 하나라도 없으면 400 에러 처리
//        if (targetId == null || activeStr == null) {
//            log.warn("필수 파라미터 누락- ID: {}, active: {}", targetId, activeStr);
//            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
//            return;
//        }
//
//        // 문자열 "true"를 자바의 논리값 true로 변경 (활성화 : string->boolean 형변환)
//        boolean active = Boolean.parseBoolean(activeStr);  //"true"이면 true, 그외 모두 false
//
//        // 본인 계정(currentLoginId)은 스스로 비활성화(false) 할 수 없게 방어
//        if (!active && targetId.equals(currentLoginId)) {
//            log.warn("본인 계정 비활성화 시도 차단 - ID: {}", targetId);
//            // JSP에서 에러를 보여주기 위해 세션에 에러 메시지 저장
//            session.setAttribute("error", "본인 계정은 비활성화할 수 없습니다.");
//            response.sendRedirect(request.getContextPath() + "/mgr/view?id=" + targetId);
//            return;
//        }
//
//        try {
//            // DB 업데이트 수행 (DAO를 호출해 해당 ID의 'active' 컬럼 값을 변경)
//            managerDAO.updateActive(active, targetId);
//            log.info("관리자 계정 상태 변경 성공 - ID: {}, 활성화: {}", targetId, active);
//
//            // 성공 메시지 저장 및 리다이렉트 경로
//            String statusText = active ? "활성화" : "비활성화";
//            session.setAttribute("successMessage", targetId + " 계정이 " + statusText + " 되었습니다.");
//
//            response.sendRedirect(request.getContextPath() + "/mgr/view?id=" + targetId);
//
//        } catch (Exception e) {
//            log.error("관리자 계정 상태 변경 중 오류 발생", e);
//            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//        }
//    }
//}