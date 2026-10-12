package com.aiproject.scaffold.workspace;

import com.aiproject.scaffold.common.api.ApiResponse;
import com.aiproject.scaffold.workspace.dto.CreateApplicationRequest;
import com.aiproject.scaffold.workspace.dto.SubmissionLogResponse;
import com.aiproject.scaffold.workspace.dto.UpdateApplicationRequest;
import com.aiproject.scaffold.workspace.dto.WorkspaceApplicationResponse;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspace")
public class WorkspaceController {
    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping("/applications")
    public ApiResponse<List<WorkspaceApplicationResponse>> listApplications(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(workspaceService.listApplications(jwt.getSubject()));
    }

    @PostMapping("/applications")
    public ApiResponse<WorkspaceApplicationResponse> createApplication(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateApplicationRequest request
    ) {
        return ApiResponse.success(workspaceService.createApplication(jwt.getSubject(), request));
    }

    @PatchMapping("/applications/{applicationId}")
    public ApiResponse<WorkspaceApplicationResponse> updateApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String applicationId,
            @Valid @RequestBody UpdateApplicationRequest request
    ) {
        return ApiResponse.success(workspaceService.updateApplication(jwt.getSubject(), applicationId, request));
    }

    @DeleteMapping("/applications/{applicationId}")
    public ApiResponse<Void> deleteApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String applicationId
    ) {
        workspaceService.deleteApplication(jwt.getSubject(), applicationId);
        return ApiResponse.success();
    }

    @GetMapping("/submission-logs")
    public ApiResponse<List<SubmissionLogResponse>> listSubmissionLogs() {
        return ApiResponse.success(workspaceService.listSubmissionLogs());
    }
}
