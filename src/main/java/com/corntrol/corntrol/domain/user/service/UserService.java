package com.corntrol.corntrol.domain.user.service;

import com.corntrol.corntrol.domain.user.dto.*;
import com.corntrol.corntrol.domain.user.entity.User;
import com.corntrol.corntrol.domain.user.repository.UserRepository;
import com.corntrol.corntrol.global.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // 회원가입
    @Transactional
    public UserResponse signup(SignupRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("이미 존재하는 이메일");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPassword()) // 🔥 나중에 암호화
                .nickname(request.getNickname())
                .build();

        userRepository.save(user);

        return UserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    // 로그인
    @Transactional
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("비밀번호 틀림");
        }

        String accessToken = jwtUtil.createAccessToken(user.getEmail());
        String refreshToken = jwtUtil.createRefreshToken(user.getEmail());

        user.updateRefreshToken(refreshToken);

        return LoginResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // 토큰 재발급
    @Transactional(readOnly = true)
    public String refreshAccessToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("리프레시 토큰 만료");
        }

        String email = jwtUtil.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰");
        }

        return jwtUtil.createAccessToken(email);
    }

    // 사용자 조회
    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        return UserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    // 프로필 수정
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        user.updateProfile(request.getNickname(), request.getProfileImage());

        return UserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }
}