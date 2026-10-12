package com.aiproject.scaffold.workspace;

import com.aiproject.scaffold.common.exception.BusinessException;
import com.aiproject.scaffold.user.DatabaseUserDetailsService;
import com.aiproject.scaffold.workspace.dto.CreateApplicationRequest;
import com.aiproject.scaffold.workspace.dto.UpdateApplicationRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkspaceServiceTest {

    @Test
    void createsCollaborationApplicationWithNormalizedCollaboratorsAndUrl() {
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        DatabaseUserDetailsService users = mock(DatabaseUserDetailsService.class);
        when(users.listActiveUsernames()).thenReturn(List.of("admin", "chyong", "liyan"));
        WorkspaceService service = new WorkspaceService(repository, users);

        service.createApplication(
                "admin",
                new CreateApplicationRequest(
                        "协作助手",
                        "用于多人协同处理事项。",
                        "流程自动化",
                        "collaboration",
                        "shared",
                        List.of("admin", "chyong", "chyong"),
                        "https://example.com/apps/1"
                )
        );

        ArgumentCaptor<WorkspaceApplication> captor = ArgumentCaptor.forClass(WorkspaceApplication.class);
        verify(repository).insert(captor.capture());
        assertEquals(List.of("chyong"), captor.getValue().collaborators());
        assertEquals("https://example.com/apps/1", captor.getValue().applicationUrl());
    }

    @Test
    void rejectsUpdateFromNonOwner() {
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        DatabaseUserDetailsService users = mock(DatabaseUserDetailsService.class);
        WorkspaceApplication application = new WorkspaceApplication(
                "app-1", "应用", "描述", "智能问答", "collaboration", "shared", "running",
                "admin", List.of("chyong"), null, OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(repository.findById("app-1")).thenReturn(Optional.of(application));
        WorkspaceService service = new WorkspaceService(repository, users);

        assertThrows(
                BusinessException.class,
                () -> service.updateApplication(
                        "chyong",
                        "app-1",
                        new UpdateApplicationRequest(null, "paused", null, null)
                )
        );
    }

    @Test
    void rejectsUnsafeApplicationUrl() {
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        DatabaseUserDetailsService users = mock(DatabaseUserDetailsService.class);
        WorkspaceService service = new WorkspaceService(repository, users);

        assertThrows(
                BusinessException.class,
                () -> service.createApplication(
                        "admin",
                        new CreateApplicationRequest(
                                "问答助手", "描述内容", "智能问答", "personal", "exclusive", List.of(),
                                "javascript:alert(1)"
                        )
                )
        );
    }
}
