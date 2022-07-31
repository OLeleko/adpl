package com.smiddle.adpl.core.service;

import java.util.Map;

public interface RequestSendService {
    void sendRequest(Map<String, String> content, String urlEndPart);
}
