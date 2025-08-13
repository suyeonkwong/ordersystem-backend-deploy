package com.example.ordersystem.member.controller;

import com.example.ordersystem.common.auth.JwtTokenProvider;
import com.example.ordersystem.common.dto.CommonDto;
import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.member.dto.*;
import com.example.ordersystem.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/create")
    public ResponseEntity<?> join(@RequestBody @Valid MemberCreateRequest memberCreateRequest) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
//                .body(new CommonDto(memberService.saveMember(memberCreateRequest), HttpStatus.OK.value(), "회원가입이 완료 되었습니다."));
                .body(CommonDto.builder()
                        .result(memberService.saveMember(memberCreateRequest))
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("회원가입이 완료 되었습니다.").build());
    }

    @PostMapping("/doLogin")
    public ResponseEntity<?> login(@RequestBody MemberLoginRequest memberLoginRequest) {
        Member member = memberService.login(memberLoginRequest);
        /// at 토큰 생성
        String accessToken = jwtTokenProvider.createAtToken(member);
        /// at 토큰 생성
        String refreshToken = jwtTokenProvider.createRtToken(member);
        return new ResponseEntity<>(
                new CommonDto(new MemberLoginResponse(accessToken, refreshToken), HttpStatus.OK.value(), "로그인성공"),
                HttpStatus.OK);
    }

    // rt를 통한 at 갱신 요청
    @PostMapping("/refresh-at")
    public ResponseEntity<?> generateNewAt(@RequestBody RefreshTokenDto refreshTokenDto){
        /// rt 검증 로직
        Member member = jwtTokenProvider.validateRt(refreshTokenDto.getRefreshToken());

        /// at 신규 생성
        String accessToken = jwtTokenProvider.createAtToken(member);
        MemberLoginResponse loginResDto = MemberLoginResponse.builder()
                .accessToken(accessToken)
                .build();

        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(loginResDto)
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("AT 재발급 완료")
                        .build()
                , HttpStatus.OK);
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getMemberList() {
        return ResponseEntity.status(HttpStatus.OK).body(memberService.getMemberList());
    }

    @GetMapping("/detail/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getMemberDetail(@PathVariable long id) {
        return ResponseEntity.status(HttpStatus.OK).body(memberService.getMemberDetail(id));
    }

    @GetMapping("/myinfo")
    public ResponseEntity<?> getMyInfo() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.status(HttpStatus.OK)
                .body(CommonDto.builder().result(memberService.getMemberDetail(email)).statusCode(HttpStatus.OK.value()).statusMessage("마이페이지 조회 성공").build());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete() {
        memberService.delete();
        return new ResponseEntity<>(CommonDto.builder().result("ok").statusCode(HttpStatus.OK.value()).statusMessage("회원탈퇴완료").build(), HttpStatus.OK);
    }

}
