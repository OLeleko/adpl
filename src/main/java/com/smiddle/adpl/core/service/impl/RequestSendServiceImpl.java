package com.smiddle.adpl.core.service.impl;

import com.smiddle.adpl.core.service.RequestSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class RequestSendServiceImpl implements RequestSendService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final RestTemplate restTemplate;
    @Value("${iviva.url}")
    private String ivivaUrl;
    @Value("${iviva.token}")
    private String ivivaToken;
    @Override
    public void sendRequest(String content, String urlEndPart) {
        String finalIvivaUrl = ivivaUrl + urlEndPart;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(ivivaToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(content, headers);
        logger.info("Send request " + requestEntity);
        try {
            ResponseEntity<String> entity = restTemplate.postForEntity(finalIvivaUrl, requestEntity, String.class);
            logger.info("Responese status code: " + entity.getStatusCode());
        } catch (RestClientException e) {
            logger.error("Request error: " + e);
        }
    }
}
