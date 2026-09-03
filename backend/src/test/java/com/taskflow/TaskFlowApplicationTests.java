package com.taskflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TaskFlowApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the full Spring application context starts up correctly
        // with all beans (security, JPA, controllers) wired together.
    }
}
