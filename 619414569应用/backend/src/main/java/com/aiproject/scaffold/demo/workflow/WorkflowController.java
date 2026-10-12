package com.aiproject.scaffold.demo.workflow;

import com.aiproject.scaffold.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/demo/workflows")
public class WorkflowController {
    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/approval")
    public ApiResponse<WorkflowService.ProcessStarted> startApproval(
            @Valid @RequestBody StartApprovalRequest request
    ) {
        return ApiResponse.success(workflowService.start(new WorkflowService.StartApprovalRequest(
                request.title(),
                request.businessKey(),
                request.approver()
        )));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<WorkflowService.TaskSummary>> tasks(
            @RequestParam @Pattern(regexp = "[A-Za-z0-9._-]{1,64}") String assignee
    ) {
        return ApiResponse.success(workflowService.tasksFor(assignee));
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ApiResponse<Void> complete(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9-]{1,128}") String taskId
    ) {
        workflowService.complete(taskId);
        return ApiResponse.success();
    }

    public record StartApprovalRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 100) String businessKey,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,64}") String approver
    ) {
    }
}
