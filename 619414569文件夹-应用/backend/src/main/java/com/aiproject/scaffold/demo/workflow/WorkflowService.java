package com.aiproject.scaffold.demo.workflow;

import com.aiproject.scaffold.common.api.ErrorCode;
import com.aiproject.scaffold.common.exception.BusinessException;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowService {
    private static final String PROCESS_DEFINITION_KEY = "demoApproval";

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public WorkflowService(RuntimeService runtimeService, TaskService taskService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    public ProcessStarted start(StartApprovalRequest request) {
        Map<String, Object> variables = Map.of(
                "title", request.title(),
                "approver", request.approver()
        );
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                PROCESS_DEFINITION_KEY,
                request.businessKey(),
                variables
        );
        return new ProcessStarted(instance.getId(), instance.getProcessDefinitionId(), instance.getBusinessKey());
    }

    public List<TaskSummary> tasksFor(String assignee) {
        return taskService.createTaskQuery()
                .taskAssignee(assignee)
                .orderByTaskCreateTime()
                .desc()
                .listPage(0, 100)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public void complete(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Workflow task does not exist");
        }
        taskService.complete(taskId);
    }

    private TaskSummary toSummary(Task task) {
        Instant createdAt = task.getCreateTime() == null
                ? null
                : task.getCreateTime().toInstant();
        return new TaskSummary(
                task.getId(),
                task.getName(),
                task.getAssignee(),
                task.getProcessInstanceId(),
                createdAt
        );
    }

    public record StartApprovalRequest(String title, String businessKey, String approver) {
    }

    public record ProcessStarted(String processInstanceId, String processDefinitionId, String businessKey) {
    }

    public record TaskSummary(
            String id,
            String name,
            String assignee,
            String processInstanceId,
            Instant createdAt
    ) {
    }
}
