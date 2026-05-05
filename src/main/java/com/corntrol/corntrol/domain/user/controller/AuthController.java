package com.corntrol.corntrol.domain.user.controller;

import com.corntrol.corntrol.domain.user.dto.LoginRequest;
import com.corntrol.corntrol.domain.user.dto.LoginResponse;
import com.corntrol.corntrol.domain.user.dto.SignupRequest;
import com.corntrol.corntrol.domain.user.dto.UserResponse;
import com.corntrol.corntrol.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증 API", description = "회원가입 및 로그인을 담당합니다.")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "새로운 사용자 정보를 등록합니다.")
    @PostMapping("/signup")
    public UserResponse signup(@RequestBody SignupRequest request) {
        return userService.signup(request);
    }

    @Operation(summary = "로그인", description = "아이디와 비밀번호로 로그인을 시도합니다.")
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return userService.login(request);
    }
}
