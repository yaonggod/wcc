package com.ssafy.wcc.domain.member.presentation;


import com.ssafy.wcc.domain.collection.application.service.CollectionItemService;
import com.ssafy.wcc.domain.member.application.dto.request.EmailVerifyRequest;
import com.ssafy.wcc.domain.member.application.dto.request.MemberRequest;
import com.ssafy.wcc.domain.member.application.dto.request.MemberloginRequest;
import com.ssafy.wcc.domain.member.application.dto.response.MemberInfoResponse;
import com.ssafy.wcc.domain.member.application.dto.response.MemberLoginResponse;
import com.ssafy.wcc.domain.member.application.service.EmailService;
import com.ssafy.wcc.domain.member.application.service.MemberService;
import com.ssafy.wcc.domain.jwt.application.service.TokenService;
import com.ssafy.wcc.domain.notice.presentation.NoticeController;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Api(tags = "Member 컨트롤러")
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
@CrossOrigin("*")
public class MemberController{

    Logger logger = LoggerFactory.getLogger(MemberController.class);

    private final TokenService tokenService;

    private final MemberService memberService;

    private final EmailService emailService;

    @PostMapping("/join")
    @ApiOperation(value = "회원 가입")
    @ApiResponses({
            @ApiResponse(code = 200, message = "회원가입 성공"),
            @ApiResponse(code = 404, message = "회원가입 실패"),
    })
    public ResponseEntity<?> signUp (
            @RequestBody @ApiParam(value = "회원가입 정보", required = true) MemberRequest signupInfo
    ) {
        logger.info("signUp controller 진입");
        Map<String, Object> resultMap = new HashMap<>();
        memberService.memberSignUp(signupInfo);
        resultMap.put("isSuccess", true);

        return new ResponseEntity<>(resultMap, HttpStatus.OK);
    }

    @PostMapping("/email")
    @ApiOperation(value = "email 인증 중복 검사 및 인증번호 전송")
    @ApiResponses({
            @ApiResponse(code = 200, message = "이메일 인증 메일 전송 성공"),
            @ApiResponse(code = 404, message = "사용 불가능한 이메일"),
    })
    public ResponseEntity<?> confirmEmail(@RequestBody EmailVerifyRequest email) throws MessagingException, UnsupportedEncodingException {
        Map<String, Object> resultMap = new HashMap<>();

        // 이메일 중복 검사
        if (memberService.checkEmail(email.getEmail())) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

        // 인증 메일 전송
        try {
            emailService.sendMessage(email.getEmail());
        } catch (IllegalArgumentException e) {
            resultMap.put("isSuccess", false);
            return new ResponseEntity<>(resultMap, HttpStatus.NOT_FOUND);
        }

        resultMap.put("isSuccess", true);
        return new ResponseEntity<>(resultMap, HttpStatus.OK);
    }

    @PostMapping("/code")
    @ApiOperation(value = "email 인증 번호 확인")
    @ApiResponses({
            @ApiResponse(code = 200, message = "이메일 인증 성공"),
            @ApiResponse(code = 404, message = "이메일 인증 실패"),
    })
    public ResponseEntity<?> verifyEmail(
            @RequestBody @ApiParam(value = "이메일 인증 정보", required = true) EmailVerifyRequest emailVerifyRequest
    ) {
        logger.info("verifyEmail controller 진입");
        Map<String, Object> resultMap = new HashMap<>();

        try {
            emailService.verifyEmail(emailVerifyRequest);
        } catch (ChangeSetPersister.NotFoundException e) {
            resultMap.put("isSuccess", false);
            return new ResponseEntity<>(resultMap, HttpStatus.NOT_FOUND);
        }
        resultMap.put("isSuccess", true);

        return new ResponseEntity<>(resultMap, HttpStatus.OK);
    }

    @PostMapping("/login")
    @ApiOperation(value = "로그인")
    @ApiResponses({
            @ApiResponse(code = 200, message = "로그인 성공"),
            @ApiResponse(code = 401, message = "잘못된 비밀번호"),
            @ApiResponse(code = 404, message = "존재하지 않는 사용자")
    })
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody @ApiParam(value = "로그인 정보") MemberloginRequest loginInfo
    ) {
        logger.info("login controller 진입");
        Map<String, Object> res = new HashMap<>();

        long id = memberService.memberLogin(loginInfo);
        MemberLoginResponse token = tokenService.makeMemberLoginResponse(String.valueOf(id));
        res.put("isSuccess", true);
        res.put("access_token", token.getAccess_token());
        res.put("refresh_token", token.getRefresh_token());

        return new ResponseEntity<>(res, HttpStatus.OK);

    }

    @PostMapping("/logout")
    @ApiOperation(value = "로그아웃")
    @ApiResponses({
            @ApiResponse(code = 200, message = "로그아웃 성공"),
            @ApiResponse(code = 404, message = "로그아웃 실패")
    })
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader("access_token") @ApiParam(value = "access_token", required = true) String accessToken,
            @RequestHeader("refresh_token") @ApiParam(value = "refresh_token", required = true) String refreshToken
    ) {
        logger.info("logout controller 진입");
        Map<String, Object> res = new HashMap<>();
        tokenService.saveLogoutToken(accessToken);
        tokenService.deleteRefreshToken(refreshToken);
        res.put("isSuccess", true);
        return new ResponseEntity<>(res, HttpStatus.OK);

    }

    @PostMapping()
    @ApiOperation(value = "회원정보 조회")
    @ApiResponses({
            @ApiResponse(code = 200, message = "조회 성공"),
            @ApiResponse(code = 404, message = "조회 실패")
    })
    public ResponseEntity<Map<String, Object>> memberInfo(@RequestHeader("access_token") @ApiParam(value = "access_token", required = true) String accessToken) {
        logger.info("memberInfo controller 진입");
        Map<String, Object> res = new HashMap<>();
        MemberInfoResponse memberInfoResponse = memberService.memberInfoResponse(Long.parseLong(tokenService.getAccessTokenId(accessToken)));
        res.put("isSuccess", true);
        res.put("data", memberInfoResponse);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @PutMapping()
    @ApiOperation(value = "회원정보 수정")
    @ApiResponses({
            @ApiResponse(code = 200, message = "수정 성공"),
            @ApiResponse(code = 404, message = "수정 실패")
    })
    public ResponseEntity<Map<String, Object>> memberUpdate(@RequestBody MemberRequest memberRequest, @RequestHeader("access_token") @ApiParam(value = "access_token", required = true) String accessToken) {
        logger.info("memberUpdate controller 진입");
        Map<String, Object> res = new HashMap<>();
        String id = tokenService.getAccessTokenId(accessToken);
        memberService.memberUpdate(memberRequest, id);
        res.put("isSuccess", true);
        return new ResponseEntity<>(res, HttpStatus.OK);

    }

    @DeleteMapping()
    @ApiOperation(value = "회원 탈퇴")
    @ApiResponses({
            @ApiResponse(code = 200, message = "탈퇴 성공"),
            @ApiResponse(code = 404, message = "탈퇴 실패")
    })
    public ResponseEntity<Map<String, Object>> memberDelete(@RequestHeader("access_token") @ApiParam(value = "access_token", required = true) String accessToken) {
        logger.info("memberDelete controller 진입");
        Map<String, Object> res = new HashMap<>();
        String id = tokenService.getAccessTokenId(accessToken);
        memberService.memberDelete(id);
        res.put("isSuccess", true);
        return new ResponseEntity<>(res, HttpStatus.OK);

    }

    @PostMapping("/nickname")
    @ApiOperation(value = "닉네임 중복 검사")
    @ApiResponses({
            @ApiResponse(code = 200, message = "중복 아님"),
            @ApiResponse(code = 404, message = "중복")
    })
    public ResponseEntity<Map<String, Object>> nickNameCheck(@RequestBody MemberRequest loginInfo) {
        logger.info("nickNameCheck controller 진입");
        Map<String, Object> res = new HashMap<>();

        String nickName = loginInfo.getNickname();

        if (memberService.checkNickname(nickName)) {
            res.put("unique", true);
            return new ResponseEntity<>(res, HttpStatus.OK);
        } else {
            res.put("unique", false);
            return new ResponseEntity<>(res, HttpStatus.NOT_FOUND);
        }
    }

}