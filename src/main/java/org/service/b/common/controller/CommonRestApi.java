package org.service.b.common.controller;

import org.service.b.todo.service.TodoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = {"https://www.service-b.org", "https://service-b.org", "http://localhost:4200", "http://localhost:8080"}, maxAge = 3600)
@RestController
@RequestMapping("/service/app")
public class CommonRestApi {

    private static final Logger logger = LoggerFactory.getLogger(CommonRestApi.class);

    private final TodoService todoService;

    public CommonRestApi(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/start-todo/{todo-name}/")
    public ResponseEntity<Void> startProcess(@PathVariable("todo-name") String todoName) {
        return ResponseEntity.status(203).build();
    }

}
