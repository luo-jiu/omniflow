package com.loyce.omniflow;

import com.loyce.omniflow.service.TagService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "search.engine=mysql")
class OmniflowApplicationTests {

    @Resource
    private TagService tagService;

    @Test
    void contextLoads() {
//       tagService.print();
        System.out.println("test");
    }

}
