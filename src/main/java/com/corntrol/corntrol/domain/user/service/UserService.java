package com.corntrol.corntrol.domain.user.service;

import com.corntrol.corntrol.domain.user.dto.*;
import com.corntrol.corntrol.domain.user.entity.User;
import com.corntrol.corntrol.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 회원가입
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
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("비밀번호 틀림");
        }

        return LoginResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .token("mock-token")
                .build();
    }

    // 사용자 조회
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