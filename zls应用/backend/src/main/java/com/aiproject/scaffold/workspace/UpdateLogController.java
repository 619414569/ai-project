package com.aiproject.scaffold.workspace;

import com.aiproject.scaffold.common.api.ApiResponse;
import com.aiproject.scaffold.workspace.dto.UpdateLogDocumentResponse;
import com.aiproject.scaffold.workspace.dto.UpdateLogSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspace/update-logs")
public class UpdateLogController {
    private final UpdateLogService updateLogService;

    public UpdateLogController(UpdateLogService updateLogService) {
        this.updateLogService = updateLogService;
    }

    @GetMapping
    public ApiResponse<List<UpdateLogSummaryResponse>> listUpdateLogs() {
        return ApiResponse.success(updateLogService.listUpdateLogs());
    }

    @GetMapping("/{author}/{fileName}")
    public ApiResponse<UpdateLogDocumentResponse> readUpdateLog(
            @PathVariable String author,
            @PathVariable String fileName
    ) {
        return ApiResponse.success(updateLogService.readUpdateLog(author, fileName));
    }
}
