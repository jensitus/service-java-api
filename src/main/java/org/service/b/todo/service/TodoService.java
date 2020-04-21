package org.service.b.todo.service;

import org.service.b.auth.dto.UserDto;
import org.service.b.auth.message.Message;
import org.service.b.todo.dto.ItemDto;
import org.service.b.todo.dto.TodoDto;

import java.util.List;

public interface TodoService {

  TodoDto createTodo(String title);

  List<TodoDto> getTodos();

  TodoDto getTodoById(Long todo_id);

  List<ItemDto> getTodoItems(Long todo_id);

  List<UserDto> getTodoUsers(Long todo_id);

  TodoDto addUserToTodo(Long todo_id, Long user_id);

  TodoDto updateTodo(Long todo_id);

  Message todoFinished(Long todo_id);

  Message deleteTodo(Long todo_id);

  boolean checkOpenItems(String task_id);

}
