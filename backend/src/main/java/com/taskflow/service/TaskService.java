package com.taskflow.service;

import com.taskflow.dto.TaskRequest;
import com.taskflow.dto.TaskResponse;
import com.taskflow.dto.TaskSummaryResponse;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.enums.TaskPriority;
import com.taskflow.enums.TaskStatus;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    // Manual constructor - replaces @RequiredArgsConstructor
    public TaskService(
            TaskRepository taskRepository,
            UserRepository userRepository
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(
            String userEmail,
            String search,
            TaskStatus status,
            TaskPriority priority,
            String sortBy,
            String order
    ) {
        User user = getUserByEmail(userEmail);

        String normalizedSearch =
                (search == null || search.isBlank())
                        ? null
                        : search.trim();

        List<Task> tasks =
                taskRepository.search(user, normalizedSearch, status, priority);

        Comparator<Task> comparator = buildComparator(sortBy);

        if ("desc".equalsIgnoreCase(order)) {
            comparator = comparator.reversed();
        }

        tasks.sort(comparator);

        return tasks.stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private Comparator<Task> buildComparator(String sortBy) {

        if (sortBy == null) {
            return Comparator.comparing(Task::getCreatedAt);
        }

        return switch (sortBy) {

            case "title" ->
                    Comparator.comparing(
                            t -> t.getTitle().toLowerCase()
                    );

            case "dueDate" ->
                    Comparator.comparing(
                            Task::getDueDate,
                            Comparator.nullsLast(
                                    Comparator.naturalOrder()
                            )
                    );

            case "priority" ->
                    Comparator.comparing(Task::getPriority);

            case "status" ->
                    Comparator.comparing(Task::getStatus);

            case "updatedAt" ->
                    Comparator.comparing(Task::getUpdatedAt);

            default ->
                    Comparator.comparing(Task::getCreatedAt);
        };
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(
            String userEmail,
            Long taskId
    ) {
        User user = getUserByEmail(userEmail);

        Task task = findOwnedTask(user, taskId);

        return TaskResponse.fromEntity(task);
    }

    @Transactional
    public TaskResponse createTask(
            String userEmail,
            TaskRequest request
    ) {
        User user = getUserByEmail(userEmail);

        Task task = new Task();

        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setUser(user);

        Task saved = taskRepository.save(task);

        return TaskResponse.fromEntity(saved);
    }

    @Transactional
    public TaskResponse updateTask(
            String userEmail,
            Long taskId,
            TaskRequest request
    ) {
        User user = getUserByEmail(userEmail);

        Task task = findOwnedTask(user, taskId);

        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());

        Task saved = taskRepository.save(task);

        return TaskResponse.fromEntity(saved);
    }

    @Transactional
    public TaskResponse updateTaskStatus(
            String userEmail,
            Long taskId,
            TaskStatus status
    ) {
        User user = getUserByEmail(userEmail);

        Task task = findOwnedTask(user, taskId);

        task.setStatus(status);

        Task saved = taskRepository.save(task);

        return TaskResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteTask(
            String userEmail,
            Long taskId
    ) {
        User user = getUserByEmail(userEmail);

        Task task = findOwnedTask(user, taskId);

        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public TaskSummaryResponse getSummary(String userEmail) {

        User user = getUserByEmail(userEmail);

        long total =
                taskRepository.countByUser(user);

        long pending =
                taskRepository.countByUserAndStatus(
                        user,
                        TaskStatus.PENDING
                );

        long inProgress =
                taskRepository.countByUserAndStatus(
                        user,
                        TaskStatus.IN_PROGRESS
                );

        long completed =
                taskRepository.countByUserAndStatus(
                        user,
                        TaskStatus.COMPLETED
                );

        return new TaskSummaryResponse(
                total,
                pending,
                inProgress,
                completed
        );
    }

    /**
     * Fetches a task and verifies that it belongs
     * to the requesting user.
     */
    private Task findOwnedTask(
            User user,
            Long taskId
    ) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Task not found"
                        )
                );

        if (!task.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException(
                    "Task not found"
            );
        }

        return task;
    }
}