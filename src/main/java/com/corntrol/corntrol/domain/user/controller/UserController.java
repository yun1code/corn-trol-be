package com.corntrol.corntrol.domain.user.controller;

import com.corntrol.corntrol.domain.user.dto.UpdateProfileRequest;
import com.corntrol.corntrol.domain.user.dto.UserProfileResponse;
import com.corntrol.corntrol.domain.user.dto.UserResponse;
import com.corntrol.corntrol.domain.user.dto.UserStatsResponse;
import com.corntrol.corntrol.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자 API", description = "사용자 정보 조회 및 프로필 수정을 담당합니다.")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "사용자 조회", description = "특정 사용자의 상세 정보를 가져옵니다.")
    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable Long userId) {
        return userService.getUser(userId);
    }

    @Operation(summary = "프로필 수정", description = "사용자의 닉네임이나 프로필 정보를 업데이트합니다.")
    @PutMapping("/{userId}")
    public UserResponse updateProfile(
            @PathVariable Long userId,
            @RequestBody UpdateProfileRequest request) {

        return userService.updateProfile(userId, request);
    }

    @Operation(summary = "프로필 조회", description = "특정 사용자의 닉네임과 프로필 이미지를 조회합니다.")
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @Operation(summary = "사용자 통계 조회", description = "특정 사용자의 전체 몰입 시간, 기록 개수, 연결 노드 개수 통계를 조회합니다. (현재 더미 데이터 반환)")
    @GetMapping("/{userId}/stats")
    public ResponseEntity<UserStatsResponse> getUserStats(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserStats(userId));
    }
}
