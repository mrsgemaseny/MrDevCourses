package com.mrdev.modules.course.controller;

import com.mrdev.common.dto.ApiResponse;
import com.mrdev.common.util.SecurityUtils;
import com.mrdev.modules.course.dto.CourseInviteDto;
import com.mrdev.modules.course.dto.CreateCourseInviteRequest;
import com.mrdev.modules.course.service.CourseInviteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/invites")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInviteController {

    private final CourseInviteService inviteService;

    @PostMapping
    public ResponseEntity<ApiResponse<CourseInviteDto>> createInvite(
            @Valid @RequestBody CreateCourseInviteRequest request
    ) {
        Long adminId = SecurityUtils.getCurrentUserId();
        CourseInviteDto invite = inviteService.createInvite(request, adminId);
        return ResponseEntity.ok(ApiResponse.success("Инвайт-ссылка успешно создана", invite));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseInviteDto>>> getAllInvites() {
        List<CourseInviteDto> invites = inviteService.getAllInvites();
        return ResponseEntity.ok(ApiResponse.success(invites));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInvite(@PathVariable Long id) {
        Long adminId = SecurityUtils.getCurrentUserId();
        inviteService.deleteInvite(id, adminId);
        return ResponseEntity.ok(ApiResponse.success("Инвайт-ссылка удалена", null));
    }
}
