package com.example.umc9th.domain.member.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.umc9th.domain.member.dto.MemberRequest;
import com.example.umc9th.domain.member.dto.MemberResponse;
import com.example.umc9th.domain.member.enums.Gender;
import com.example.umc9th.domain.member.service.MemberService;
import com.example.umc9th.global.auth.enums.SocialType;
import com.example.umc9th.global.exception.CustomException;
import com.example.umc9th.global.exception.GlobalExceptionHandler;
import com.example.umc9th.global.response.code.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * MemberController 테스트
 *
 * @WebMvcTest:
 * - Controller 계층만 테스트
 * - MockMvc를 사용해서 실제 HTTP 요청/응답 테스트
 * - Service는 @MockBean으로 Mock 처리
 *
 * MockMvc:
 * - 실제 서버 띄우지 않고 HTTP 요청 시뮬레이션
 * - perform(): 요청 실행
 * - andExpect(): 응답 검증
 * - andDo(): 추가 작업 (로그 출력 등)
 *
 * 왜 Controller 테스트가 중요할까?
 * - API 스펙 검증 (URL, HTTP 메서드, 요청/응답 형식)
 * - Validation 검증 (@Valid 동작 확인)
 * - GlobalExceptionHandler 동작 확인
 * - 프론트엔드 입장에서 API 검증
 */
@WebMvcTest(controllers = {
        MemberController.class,
        GlobalExceptionHandler.class  // 예외 처리 테스트를 위해 포함
})
@DisplayName("MemberController 테스트")
class MemberControllerTest {

    /**
     * MockMvc: HTTP 요청을 시뮬레이션하는 객체
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * ObjectMapper: Java 객체 ↔ JSON 변환
     * - Request DTO를 JSON으로 변환
     * - 응답 JSON을 파싱
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * @MockitoBean: Spring Context에 Mock 객체 주입
     * - @Mock과 비슷하지만 Spring Bean으로 등록됨
     * - Controller가 의존하는 Service를 Mock으로 대체
     *
     * 참고: Spring Boot 3.4.0+부터 @MockBean 대신 @MockitoBean 사용
     */
    @MockitoBean
    private MemberService memberService;


    // ===== GET /api/members - 전체 회원 조회 =====

    /**
     * 전체 회원 목록 조회 성공 테스트
     */
    @Test
    @DisplayName("GET /api/members - 모든 회원 목록을 조회할 수 있다")
    void getAllMembers_Success() throws Exception {
        // Given: Service가 회원 목록을 반환한다고 가정
        List<MemberResponse.Summary> mockResponse = Arrays.asList(
                MemberResponse.Summary.builder()
                        .id(1L)
                        .name("회원1")
                        .email("member1@example.com")
                        .createdAt(LocalDateTime.now())
                        .build(),
                MemberResponse.Summary.builder()
                        .id(2L)
                        .name("회원2")
                        .email("member2@example.com")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        given(memberService.getAllMembers())
                .willReturn(mockResponse);

        // When & Then: GET 요청 실행 및 검증
        mockMvc.perform(
                        get("/api/members")  // GET /api/members 요청
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())  // 요청/응답 로그 출력 (디버깅용)
                .andExpect(status().isOk())  // HTTP 200 OK
                .andExpect(jsonPath("$.isSuccess").value(true))  // ApiResponse.isSuccess
                .andExpect(jsonPath("$.code").value("MEMBER_LIST_200"))
                .andExpect(jsonPath("$.message").value("회원 목록 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())  // data는 배열
                .andExpect(jsonPath("$.data", hasSize(2)))  // 2개
                .andExpect(jsonPath("$.data[0].name").value("회원1"))
                .andExpect(jsonPath("$.data[1].name").value("회원2"));

        // Service 메서드 호출 확인
        then(memberService).should().getAllMembers();
    }


    // ===== GET /api/members/{id} - 특정 회원 조회 =====

    /**
     * 특정 회원 조회 성공 테스트
     */
    @Test
    @DisplayName("GET /api/members/{id} - ID로 회원을 조회할 수 있다")
    void getMemberById_Success() throws Exception {
        // Given: Service가 회원 정보를 반환
        Long memberId = 1L;
        MemberResponse.Basic mockResponse = MemberResponse.Basic.builder()
                .id(memberId)
                .name("홍길동")
                .gender(Gender.MALE)
                .birth(LocalDate.of(1990, 1, 1))
                .address("서울시")
                .detailAddress("강남구")
                .email("hong@example.com")
                .phoneNumber("01012345678")
                .point(100)
                .socialType(SocialType.GOOGLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(memberService.getMemberById(memberId))
                .willReturn(mockResponse);

        // When & Then: GET 요청
        mockMvc.perform(
                        get("/api/members/{id}", memberId)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("MEMBER_200"))
                .andExpect(jsonPath("$.data.id").value(memberId))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.email").value("hong@example.com"))
                .andExpect(jsonPath("$.data.point").value(100));

        then(memberService).should().getMemberById(memberId);
    }

    /**
     * 존재하지 않는 회원 조회 테스트 (404 에러)
     *
     * GlobalExceptionHandler가 CustomException을 잡아서
     * 통일된 에러 응답을 반환하는지 확인
     */
    @Test
    @DisplayName("GET /api/members/{id} - 존재하지 않는 회원 조회 시 404 에러")
    void getMemberById_NotFound() throws Exception {
        // Given: Service가 CustomException 던짐
        Long nonExistentId = 999L;
        given(memberService.getMemberById(nonExistentId))
                .willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // When & Then: 404 에러 응답 확인
        mockMvc.perform(
                        get("/api/members/{id}", nonExistentId)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())  // HTTP 404
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER_404"))
                .andExpect(jsonPath("$.message").value("회원을 찾을 수 없습니다"));

        then(memberService).should().getMemberById(nonExistentId);
    }


    // ===== POST /api/members - 회원 생성 =====

    /**
     * 회원 생성 성공 테스트
     */
    @Test
    @DisplayName("POST /api/members - 새로운 회원을 생성할 수 있다")
    void createMember_Success() throws Exception {
        // Given: 요청 DTO 준비
        MemberRequest.Create request = MemberRequest.Create.builder()
                .name("신규회원")
                .gender(Gender.MALE)
                .birth(LocalDate.of(1995, 5, 5))
                .address("서울시")
                .detailAddress("강남구")
                .socialUid("google_new")
                .socialType(SocialType.GOOGLE)
                .email("new@example.com")
                .phoneNumber("01099999999")
                .build();

        // Service가 반환할 응답
        MemberResponse.Basic mockResponse = MemberResponse.Basic.builder()
                .id(1L)
                .name("신규회원")
                .gender(Gender.MALE)
                .birth(LocalDate.of(1995, 5, 5))
                .address("서울시")
                .detailAddress("강남구")
                .email("new@example.com")
                .phoneNumber("01099999999")
                .point(0)
                .socialType(SocialType.GOOGLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(memberService.createMember(any(MemberRequest.Create.class)))
                .willReturn(mockResponse);

        // When & Then: POST 요청
        mockMvc.perform(
                        post("/api/members")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))  // DTO → JSON
                )
                .andDo(print())
                .andExpect(status().isOk())  // HTTP 200 (또는 201 Created로 변경 가능)
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("MEMBER_201"))
                .andExpect(jsonPath("$.message").value("회원 생성 성공"))
                .andExpect(jsonPath("$.data.name").value("신규회원"))
                .andExpect(jsonPath("$.data.email").value("new@example.com"))
                .andExpect(jsonPath("$.data.point").value(0));

        then(memberService).should().createMember(any(MemberRequest.Create.class));
    }

    /**
     * Validation 실패 테스트 (@Valid 검증)
     *
     * 이름이 빈 문자열일 때 400 에러 발생
     */
    @Test
    @DisplayName("POST /api/members - 이름이 비어있으면 400 에러")
    void createMember_ValidationFailed_EmptyName() throws Exception {
        // Given: 이름이 빈 요청
        MemberRequest.Create invalidRequest = MemberRequest.Create.builder()
                .name("")  // ❌ 빈 문자열 - @NotBlank 위반
                .gender(Gender.MALE)
                .birth(LocalDate.of(1995, 5, 5))
                .address("서울시")
                .detailAddress("강남구")
                .socialUid("google_new")
                .socialType(SocialType.GOOGLE)
                .email("new@example.com")
                .phoneNumber("01099999999")
                .build();

        // When & Then: 400 에러 발생
        mockMvc.perform(
                        post("/api/members")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest))
                )
                .andDo(print())
                .andExpect(status().isBadRequest())  // HTTP 400
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").exists());  // 에러 메시지 존재

        // Service는 호출되지 않아야 함 (Validation에서 걸림)
        then(memberService).should(never()).createMember(any());
    }

    /**
     * 이메일 형식 검증 실패 테스트
     */
    @Test
    @DisplayName("POST /api/members - 잘못된 이메일 형식이면 400 에러")
    void createMember_ValidationFailed_InvalidEmail() throws Exception {
        // Given: 잘못된 이메일
        MemberRequest.Create invalidRequest = MemberRequest.Create.builder()
                .name("신규회원")
                .gender(Gender.MALE)
                .birth(LocalDate.of(1995, 5, 5))
                .address("서울시")
                .detailAddress("강남구")
                .socialUid("google_new")
                .socialType(SocialType.GOOGLE)
                .email("invalid-email")  // ❌ 이메일 형식 아님
                .phoneNumber("01099999999")
                .build();

        // When & Then: 400 에러
        mockMvc.perform(
                        post("/api/members")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest))
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        then(memberService).should(never()).createMember(any());
    }


    // ===== DELETE /api/members/{id} - 회원 삭제 =====

    /**
     * 회원 삭제 성공 테스트
     */
    @Test
    @DisplayName("DELETE /api/members/{id} - 회원을 삭제할 수 있다")
    void deleteMember_Success() throws Exception {
        // Given: 삭제할 회원 ID
        Long memberId = 1L;

        // Service의 deleteMember()는 void이므로 willDoNothing() 사용
        willDoNothing().given(memberService).deleteMember(memberId);

        // When & Then: DELETE 요청
        mockMvc.perform(
                        delete("/api/members/{id}", memberId)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("MEMBER_200"))
                .andExpect(jsonPath("$.message").value("회원 삭제 성공"))
                .andExpect(jsonPath("$.data").doesNotExist());  // data는 null이므로 JSON에 없음

        then(memberService).should().deleteMember(memberId);
    }

    /**
     * 존재하지 않는 회원 삭제 시도 테스트
     */
    @Test
    @DisplayName("DELETE /api/members/{id} - 존재하지 않는 회원 삭제 시 404 에러")
    void deleteMember_NotFound() throws Exception {
        // Given: 존재하지 않는 회원
        Long nonExistentId = 999L;
        willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND))
                .given(memberService).deleteMember(nonExistentId);

        // When & Then: 404 에러
        mockMvc.perform(
                        delete("/api/members/{id}", nonExistentId)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER_404"));

        then(memberService).should().deleteMember(nonExistentId);
    }


    // ===== GET /api/members/search?name=홍길동 - 이름 검색 =====

    /**
     * 이름으로 회원 검색 테스트
     */
    @Test
    @DisplayName("GET /api/members/search - 이름으로 회원을 검색할 수 있다")
    void searchMembersByName_Success() throws Exception {
        // Given: 검색 결과
        String searchName = "홍길동";
        List<MemberResponse.Detail> mockResponse = Arrays.asList(
                MemberResponse.Detail.builder()
                        .id(1L)
                        .name("홍길동")
                        .email("hong1@example.com")
                        .createdAt(LocalDateTime.now())
                        .build(),
                MemberResponse.Detail.builder()
                        .id(2L)
                        .name("홍길동")
                        .email("hong2@example.com")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        given(memberService.searchMembersByName(searchName))
                .willReturn(mockResponse);

        // When & Then: 쿼리 파라미터로 검색
        mockMvc.perform(
                        get("/api/members/search")
                                .param("name", searchName)  // ?name=홍길동
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").value("홍길동"))
                .andExpect(jsonPath("$.data[1].name").value("홍길동"));

        then(memberService).should().searchMembersByName(searchName);
    }

    /**
     * 필수 파라미터 누락 테스트
     */
    @Test
    @DisplayName("GET /api/members/search - name 파라미터 없으면 400 에러")
    void searchMembersByName_MissingParameter() throws Exception {
        // When & Then: name 파라미터 없이 요청
        mockMvc.perform(
                        get("/api/members/search")  // name 파라미터 없음!
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())  // HTTP 400
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        // Service는 호출되지 않음
        then(memberService).should(never()).searchMembersByName(anyString());
    }
}
