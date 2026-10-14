package com.aiproject.scaffold.workspace;

import com.aiproject.scaffold.common.api.ErrorCode;
import com.aiproject.scaffold.common.exception.BusinessException;
import com.aiproject.scaffold.workspace.dto.UpdateLogDocumentResponse;
import com.aiproject.scaffold.workspace.dto.UpdateLogSummaryResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UpdateLogService {
    private static final Logger log = LoggerFactory.getLogger(UpdateLogService.class);
    private static final Pattern SAFE_AUTHOR = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final Pattern SAFE_FILE_NAME = Pattern.compile("[A-Za-z0-9._-]{1,120}\\.md");
    private static final long MAX_DOCUMENT_BYTES = 1024 * 1024;

    private final Path rootDirectory;
    private final ObjectMapper objectMapper;

    public UpdateLogService(
            @Value("${app.update-logs.directory:../提交日志}") String directory,
            ObjectMapper objectMapper
    ) {
        this.rootDirectory = Path.of(directory).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
    }

    public List<UpdateLogSummaryResponse> listUpdateLogs() {
        return readIndex().stream()
                .map(UpdateLogIndexEntry::toResponse)
                .sorted(Comparator.comparing(UpdateLogSummaryResponse::fileName).reversed())
                .toList();
    }

    public UpdateLogDocumentResponse readUpdateLog(String author, String fileName) {
        validatePathSegment(author, fileName);
        UpdateLogIndexEntry entry = readIndex().stream()
                .filter(item -> item.author().equals(author) && item.fileName().equals(fileName))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Update log was not found"));

        Path documentPath = rootDirectory.resolve(author).resolve(fileName).normalize();
        if (!documentPath.startsWith(rootDirectory)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Update log path is invalid");
        }
        try {
            if (!Files.isRegularFile(documentPath) || Files.size(documentPath) > MAX_DOCUMENT_BYTES) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "Update log was not found");
            }
            return entry.toDocument(Files.readString(documentPath, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            log.error("Failed to read update log document: {}", documentPath, exception);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Update log service is unavailable");
        }
    }

    private List<UpdateLogIndexEntry> readIndex() {
        Path indexPath = rootDirectory.resolve("index.json");
        try {
            List<UpdateLogIndexEntry> entries = objectMapper.readValue(
                    indexPath.toFile(),
                    new TypeReference<>() { }
            );
            entries.forEach(entry -> validatePathSegment(entry.author(), entry.fileName()));
            return entries;
        } catch (IOException | IllegalArgumentException exception) {
            log.error("Failed to read update log index: {}", indexPath, exception);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Update log service is unavailable");
        }
    }

    private void validatePathSegment(String author, String fileName) {
        if (!SAFE_AUTHOR.matcher(author).matches() || !SAFE_FILE_NAME.matcher(fileName).matches()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Update log path is invalid");
        }
    }

    private record UpdateLogIndexEntry(
            String author,
            String fileName,
            String version,
            String releaseType,
            String releasedAt,
            String summary
    ) {
        UpdateLogSummaryResponse toResponse() {
            return new UpdateLogSummaryResponse(author, fileName, version, releaseType, releasedAt, summary);
        }

        UpdateLogDocumentResponse toDocument(String markdown) {
            return new UpdateLogDocumentResponse(
                    author, fileName, version, releaseType, releasedAt, summary, markdown
            );
        }
    }
}
