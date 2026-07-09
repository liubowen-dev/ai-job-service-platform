package interview.guide;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 智能求职服务平台 - Application Tests
 */
class AppTest {
    
    @Test 
    void contextLoads() {
        // 验证应用主类存在
        assertNotNull(App.class);
    }
}
