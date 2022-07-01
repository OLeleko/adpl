package com.smiddle.adpl;

import com.smiddle.adpl.core.service.impl.RequestSendServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AdplApplicationTests {

    @Autowired
    private RequestSendServiceImpl requestSendService;

    @Test
    void contextLoads() throws Exception {
        assertThat(requestSendService).isNotNull();
    }
}
