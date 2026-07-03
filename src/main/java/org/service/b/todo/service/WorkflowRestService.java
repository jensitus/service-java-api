package org.service.b.todo.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class WorkflowRestService {

    private final RestClient restClient;

    @Value("${cibseven.base-url}")
    private String cibsevenBaseUrl;

    public WorkflowRestService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public void startCib7Process(String processDefinitionKey, StartProcessBody body) {
        ResponseEntity<Void> bodilessEntity =
                restClient.post()
                          .uri(cibsevenBaseUrl + "/engine-rest/process-definition/key/" + processDefinitionKey + "/start")
                          .body(body)
                          .retrieve()
                          .toBodilessEntity();
    }

    @Data
    @RequiredArgsConstructor
    public static class StartProcessBody {
        private Map<String, Object> variables;
        private String businessKey;
    }

}
