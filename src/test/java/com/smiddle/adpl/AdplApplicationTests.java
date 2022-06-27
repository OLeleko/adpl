package com.smiddle.adpl;

import com.smiddle.adpl.core.service.impl.RequestSendServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.springframework.http.HttpMethod.POST;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.mock.http.server.reactive.MockServerHttpRequest.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

@SpringBootTest
class AdplApplicationTests {

    @Autowired
    private RequestSendServiceImpl requestSendService;

    /*@Autowired
    private MockRestServiceServer mockRestServiceServer;
*/
   @Test
    void contextLoads() throws Exception{
       assertThat(requestSendService).isNotNull();
    }


    /*private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp(){
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void exceptionTest(){
        mockServer.expect(requestTo("http://10.100.80.54"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND));
        assertThrows(HttpClientErrorException.class, () -> requestSendService.sendRequest("test"));
    }*/





}
