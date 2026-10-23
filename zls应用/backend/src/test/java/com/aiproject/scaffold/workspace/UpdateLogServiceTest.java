package com.aiproject.scaffold.workspace;

import com.aiproject.scaffold.common.exception.BusinessException;
import com.aiproject.scaffold.workspace.dto.UpdateLogDocumentResponse;
import com.aiproject.scaffold.workspace.dto.UpdateLogSummaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateLogServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void listsFilesInDescendingFileNameOrderAndReadsMarkdown() throws IOException {
        Files.createDirectories(tempDirectory.resolve("619414569"));
        Files.writeString(tempDirectory.resolve("619414569/2026-10-14_V1.1.md"), "# V1.1\n\n内容");
        Files.writeString(tempDirectory.resolve("619414569/2026-10-12_V1.0.md"), "# V1.0\n");
        Files.writeString(tempDirectory.resolve("index.json"), """
                [
                  {"author":"619414569","fileName":"2026-10-12_V1.0.md","version":"V1.0","releaseType":"major","releasedAt":"2026-10-12","summary":"初始版本"},
                  {"author":"619414569","fileName":"2026-10-14_V1.1.md","version":"V1.1","releaseType":"minor","releasedAt":"2026-10-14","summary":"日志预览"}
                ]
                """);
        UpdateLogService service = new UpdateLogService(tempDirectory.toString(), new ObjectMapper());

        List<UpdateLogSummaryResponse> logs = service.listUpdateLogs();
        UpdateLogDocumentResponse document = service.readUpdateLog("619414569", logs.get(0).fileName());

        assertEquals(List.of("V1.1", "V1.0"), logs.stream().map(UpdateLogSummaryResponse::version).toList());
        assertEquals("# V1.1\n\n内容", document.markdown());
    }

    @Test
    void rejectsUnsafeDocumentPaths() throws IOException {
        Files.writeString(tempDirectory.resolve("index.json"), "[]");
        UpdateLogService service = new UpdateLogService(tempDirectory.toString(), new ObjectMapper());

        assertThrows(BusinessException.class, () -> service.readUpdateLog("..", "secret.md"));
        assertThrows(BusinessException.class, () -> service.readUpdateLog("619414569", "../secret.md"));
    }
}
