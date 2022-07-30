package org.service.b.todo.service;

import org.modelmapper.ModelMapper;
import org.service.b.auth.dto.UserDto;
import org.service.b.auth.message.Message;
import org.service.b.auth.model.User;
import org.service.b.auth.repository.UserRepo;
import org.service.b.auth.service.UserService;
import org.service.b.common.config.ServiceBProcessEnum;
import org.service.b.common.mailer.service.ServiceBOrgMailer;
import org.service.b.todo.dto.ItemDto;
import org.service.b.todo.dto.TodoDto;
import org.service.b.todo.form.TodoForm;
import org.service.b.todo.model.Item;
import org.service.b.todo.model.Todo;
import org.service.b.todo.repository.ItemRepo;
import org.service.b.todo.repository.TodoRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class TodoService {

  private static final Logger logger = LoggerFactory.getLogger(TodoService.class);

  private static final String ADD_USER_EMAIL_SUBJECT = " someone added you to ";

  private static final String ADD_USER_EMAIL_TEXT = "Hi there, you are added to ";

  private final TodoRepo todoRepo;
  private final ItemRepo itemRepo;
  private final ModelMapper modelMapper;
  private final UserRepo userRepo;
  private final UserService userService;
  private final ServiceBOrgMailer serviceBOrgMailer;

  public TodoService(TodoRepo todoRepo, ItemRepo itemRepo, ModelMapper modelMapper, UserRepo userRepo, UserService userService, ServiceBOrgMailer serviceBOrgMailer) {
    this.todoRepo = todoRepo;
    this.itemRepo = itemRepo;
    this.modelMapper = modelMapper;
    this.userRepo = userRepo;
    this.userService = userService;
    this.serviceBOrgMailer = serviceBOrgMailer;
  }

  public TodoDto createTodo(TodoForm todoForm) {
    Todo todo = new Todo(todoForm.getTitle());
    todo.setSimple(todoForm.isSimple());
    UserDto userDto = userService.getCurrentUser();
    todo.setCreatedBy(userDto.getId());
    todo.setCreated_at(LocalDateTime.now());
    Set<User> users = new HashSet<>();
    users.add(modelMapper.map(userDto, User.class));
    todo.setUsers(users);
    Todo newTodo = todoRepo.save(todo);
    return modelMapper.map(newTodo, TodoDto.class);
  }

  public List<TodoDto> getTodos() {
    UserDto userDto = userService.getCurrentUser();
    User user = userRepo.findByEmail(userDto.getEmail());
    Set<Todo> todoSet = user.getTodos();
    ArrayList<TodoDto> todoDtoList = new ArrayList<>();
    for (Todo todo : todoSet) {
      TodoDto todoDto = modelMapper.map(todo, TodoDto.class);
      todoDtoList.add(todoDto);
    }
    Collections.sort(todoDtoList, (TodoDto a, TodoDto b) -> a.getId().compareTo(b.getId()));
    return todoDtoList;
  }

  public TodoDto getTodoById(Long todo_id) {
    Todo todo = todoRepo.getOne(todo_id);
    Set<User> userSet = todo.getUsers();
    Set<UserDto> userDtoSet = new HashSet<>();
    for (User user : userSet) {
      userDtoSet.add(modelMapper.map(user, UserDto.class));
    }
    Set<Item> itemSet = todo.getItems();
    List<Item> itemList = itemRepo.findByTodoIdOrderByCreatedAt(todo_id);
    List<ItemDto> itemDtoList = new ArrayList<>();
    for (Item item : itemList) {
      itemDtoList.add(modelMapper.map(item, ItemDto.class));
    }
    TodoDto todoDto = modelMapper.map(todo, TodoDto.class);
    todoDto.setUsers(userDtoSet);
    todoDto.setItems(itemDtoList);
    return todoDto;
  }

  public List getTodoItems(Long todo_id) {
    // Todo todo = todoRepo.getOne(todo_id);
    List<Item> itemList = itemRepo.findByTodoIdOrderByCreatedAt(todo_id);
    List<ItemDto> itemDtos = new ArrayList();
    // Set<Item> items = todo.getItems();
    for (Item item : itemList) {
      itemDtos.add(modelMapper.map(item, ItemDto.class));
    }
    return itemDtos;
  }

  public List<UserDto> getTodoUsers(Long todo_id) {
    Todo todo = todoRepo.getOne(todo_id);
    Set<User> userSet = todo.getUsers();
    List<UserDto> userDtoList = new ArrayList<>();
    for (User user : userSet) {
      userDtoList.add(modelMapper.map(user, UserDto.class));
    }
    return userDtoList;
  }

  public TodoDto addUserToTodo(Long todo_id, Long user_id) {
    Todo todo = todoRepo.getOne(todo_id);
    User user = userRepo.getOne(user_id);
    Set<User> userSet = new HashSet<>();
    userSet = todo.getUsers();
    userSet.add(user);
    todo.setUsers(userSet);
    todoRepo.save(todo);
    String subject = ServiceBProcessEnum.SERVICE_B_EMAIL_SUBJECT_PREFIX.value + ADD_USER_EMAIL_SUBJECT + todo.getTitle();
    String text = ADD_USER_EMAIL_TEXT + todo.getTitle();
    String salutation = user.getUsername();
    String url = ServiceBProcessEnum.SERVICE_B_BASE_URL.value;
    serviceBOrgMailer.getTheMailDetails(user.getEmail(),subject,text, salutation, url);
    return modelMapper.map(todo, TodoDto.class);
  }

  public TodoDto updateTodo(Long todo_id) {
    Todo todo = todoRepo.getOne(todo_id);
    todo.setDone(!todo.isDone());
    todoRepo.save(todo);
    return modelMapper.map(todo, TodoDto.class);
  }

  public Message deleteTodo(Long todo_id) {
    Todo todo = todoRepo.getOne(todo_id);
    todoRepo.delete(todo);
    return new Message("yepp", false);
  }

  public boolean checkOpenItems(Long todo_id) {
    List todoItems = getTodoItems(todo_id);
    boolean openItems;
    if (todoItems.size() > 1) {
      openItems = true;
    } else if (todoItems.size() == 1) {
      openItems = true;
    } else {
      openItems = false;
    }
    return openItems;
  }
}
