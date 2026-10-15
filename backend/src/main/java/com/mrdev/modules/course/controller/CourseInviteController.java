package com.mrdev.modules.course.controller;

import com.mrdev.common.dto.ApiResponse;
import com.mrdev.common.util.SecurityUtils;
import com.mrdev.modules.course.dto.CourseInviteDto;
import com.mrdev.modules.course.dto.RedeemInviteResultDto;
import com.mrdev.modules.course.service.CourseInviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/invites")
@RequiredArgsConstructor
public class CourseInviteController {

    private final CourseInviteService inviteService;

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<CourseInviteDto>> getInviteDetails(@PathVariable String code) {
        CourseInviteDto details = inviteService.getInviteDetails(code);
        return ResponseEntity.ok(ApiResponse.success(details));
    }

    @PostMapping("/{code}/redeem")
    public ResponseEntity<ApiResponse<RedeemInviteResultDto>> redeemInvite(@PathVariable String code) {
        Long userId = SecurityUtils.getCurrentUserId();
        RedeemInviteResultDto result = inviteService.redeemInvite(code, userId);
        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), result));
    }
}
