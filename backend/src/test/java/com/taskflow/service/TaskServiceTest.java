package com.taskflow.service;

import com.taskflow.dto.AuthResponse;
import com.taskflow.dto.RegisterRequest;
import com.taskflow.dto.TaskRequest;
import com.taskflow.dto.TaskResponse;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.enums.TaskPriority;
import com.taskflow.enums.TaskStatus;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    private String userAEmail = "userA@example.com";
    private String userBEmail = "userB@example.com";

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        registerUser(userAEmail, "User A");
        registerUser(userBEmail, "User B");
    }

    private void registerUser(String email, String name) {
        RegisterRequest request = new RegisterRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword("Password123");
        request.setConfirmPassword("Password123");
        authService.register(request);
    }

    private TaskRequest sampleTaskRequest(String title) {
        TaskRequest request = new TaskRequest();
        request.setTitle(title);
        request.setDescription("Sample description");
        request.setStatus(TaskStatus.PENDING);
        request.setPriority(TaskPriority.MEDIUM);
        request.setDueDate(LocalDate.now().plusDays(7));
        return request;
    }

    @Test
    void createTask_savesTaskForCorrectUser() {
        TaskResponse response = taskService.createTask(userAEmail, sampleTaskRequest("Write report"));

        assertNotNull(response.getId());
        assertEquals("Write report", response.getTitle());
        assertEquals(TaskStatus.PENDING, response.getStatus());

        User userA = userRepository.findByEmail(userAEmail).orElseThrow();
        assertEquals(userA.getId(), response.getUserId());
    }

    @Test
    void getTasks_onlyReturnsOwnTasks() {
        taskService.createTask(userAEmail, sampleTaskRequest("Task A1"));
        taskService.createTask(userAEmail, sampleTaskRequest("Task A2"));
        taskService.createTask(userBEmail, sampleTaskRequest("Task B1"));

        List<TaskResponse> userATasks = taskService.getTasks(userAEmail, null, null, null, "createdAt", "asc");

        assertEquals(2, userATasks.size());
        assertTrue(userATasks.stream().allMatch(t -> t.getTitle().startsWith("Task A")));
    }

    @Test
    void getTaskById_forAnotherUsersTask_throwsNotFound() {
        TaskResponse taskB = taskService.createTask(userBEmail, sampleTaskRequest("Private task"));

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.getTaskById(userAEmail, taskB.getId()));
    }

    @Test
    void updateTask_forAnotherUsersTask_throwsNotFound() {
        TaskResponse taskB = taskService.createTask(userBEmail, sampleTaskRequest("Private task"));
        TaskRequest update = sampleTaskRequest("Hacked title");

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.updateTask(userAEmail, taskB.getId(), update));
    }

    @Test
    void deleteTask_forAnotherUsersTask_throwsNotFound() {
        TaskResponse taskB = taskService.createTask(userBEmail, sampleTaskRequest("Private task"));

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.deleteTask(userAEmail, taskB.getId()));

        assertTrue(taskRepository.findById(taskB.getId()).isPresent());
    }

    @Test
    void updateTaskStatus_updatesOnlyStatus() {
        TaskResponse created = taskService.createTask(userAEmail, sampleTaskRequest("Task to update"));

        TaskResponse updated = taskService.updateTaskStatus(userAEmail, created.getId(), TaskStatus.COMPLETED);

        assertEquals(TaskStatus.COMPLETED, updated.getStatus());
        assertEquals("Task to update", updated.getTitle());
    }

    @Test
    void searchTasks_filtersByTitleOrDescription() {
        taskService.createTask(userAEmail, sampleTaskRequest("Buy groceries"));
        taskService.createTask(userAEmail, sampleTaskRequest("Finish homework"));

        List<TaskResponse> results = taskService.getTasks(userAEmail, "grocer", null, null, "createdAt", "asc");

        assertEquals(1, results.size());
        assertEquals("Buy groceries", results.get(0).getTitle());
    }

    @Test
    void filterTasks_byStatusAndPriority() {
        TaskRequest highPriority = sampleTaskRequest("Urgent task");
        highPriority.setPriority(TaskPriority.HIGH);
        highPriority.setStatus(TaskStatus.IN_PROGRESS);
        taskService.createTask(userAEmail, highPriority);

        taskService.createTask(userAEmail, sampleTaskRequest("Regular task"));

        List<TaskResponse> results = taskService.getTasks(
                userAEmail, null, TaskStatus.IN_PROGRESS, TaskPriority.HIGH, "createdAt", "asc"
        );

        assertEquals(1, results.size());
        assertEquals("Urgent task", results.get(0).getTitle());
    }

    @Test
    void getSummary_returnsCorrectCounts() {
        taskService.createTask(userAEmail, sampleTaskRequest("Task 1"));

        TaskRequest inProgress = sampleTaskRequest("Task 2");
        inProgress.setStatus(TaskStatus.IN_PROGRESS);
        taskService.createTask(userAEmail, inProgress);

        TaskRequest completed = sampleTaskRequest("Task 3");
        completed.setStatus(TaskStatus.COMPLETED);
        taskService.createTask(userAEmail, completed);

        var summary = taskService.getSummary(userAEmail);

        assertEquals(3, summary.getTotal());
        assertEquals(1, summary.getPending());
        assertEquals(1, summary.getInProgress());
        assertEquals(1, summary.getCompleted());
    }
}
