package org.service.b.common.processservice;

import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.service.b.auth.dto.UserDto;
import org.service.b.common.message.service.ServiceBCamundaUserService;
import org.service.b.common.message.service.MessageService;
import org.service.b.todo.dto.TodoDto;
import org.service.b.todo.service.ItemService;
import org.service.b.todo.service.TodoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Named("todoProcessService")
public class TodoProcessService {

  private static final Logger logger = LoggerFactory.getLogger(TodoProcessService.class);

  @Autowired
  private RuntimeService runtimeService;

  @Autowired
  private MessageService messageService;

  @Autowired
  private ItemService itemService;

  @Autowired
  private TodoService todoService;

  @Autowired
  private FormService formService;

  @Autowired
  private IdentityService identityService;

  @Autowired
  private ServiceBCamundaUserService serviceBCamundaUserService;

  public void startTodo(Long todo_id, UserDto createUser) {
    Map variables = new HashMap();
    variables.put("entityId", todo_id);
    ProcessInstance todoProcessInstance = runtimeService.startProcessInstanceByKey("service-b-todo", "service-b-todo-" + todo_id.toString(), variables);
    List<String> stringList = new ArrayList<>();
    String theFinalGroupId = serviceBCamundaUserService.getTheCamundaGroupId("todo", todo_id);
    Group todoGroup = identityService.newGroup(theFinalGroupId);
    todoGroup.setName("Todo " + theFinalGroupId);
    todoGroup.setType("service-b-todo");
    identityService.saveGroup(todoGroup);
    logger.info(todoGroup.toString());
    logger.info(todoGroup.getId());
    logger.info("createUser: " + createUser);
    logger.info(createUser.toString());
    /**
     * @TODO the Camunda User has to be created in der auth section when a new user registered
     * DONE!!
     */

    String userId = createUser.getId().toString();
    logger.info(userId);
    String groupId = todoGroup.getId();
    logger.info(groupId);
    identityService.createMembership(userId, groupId);
  }

  private void setTodoTaskName(Execution execution, String todoTitle) {
    runtimeService.setVariable(execution.getId(), "todoTaskName", todoTitle);
  }

  public void setTheTodoProcess(Execution execution, Long entityId) {
    TodoDto todoDto = todoService.getTodoById(entityId);
    setTodoTaskName(execution, todoDto.getTitle());
    logger.info("The Process Service is working well God Damn Hell Yeah!");
  }

  public void startSubTodoServiceItem(Long todo_id, Long item_id) {
    Map variables = new HashMap();
    variables.put("entityId", item_id);
    variables.put("item", "item-" + item_id.toString());
    variables.put("todo", todo_id);
    runtimeService.startProcessInstanceByKey("sub-todo-service-item", "service-b-todo-" + todo_id.toString(), variables);
  }

  public void deleteTodoItem(Long item_id) {
    itemService.deleteItem(item_id);
  }

  public void checkIfItemsOpen(Execution execution, Long todo_id) {
    TodoDto todoDto = todoService.getTodoById(todo_id);
    if (todoDto.getItems().isEmpty()) {
      runtimeService.setVariable(execution.getId(), "itemsOpen", false);
    } else {
      runtimeService.setVariable(execution.getId(), "itemsOpen", true);
    }
  }

  public void sendSubItemDone(String messageName, String processDefinitionKey, Long todoId) {
    messageService.sendMessageToCatchEvent(messageName, processDefinitionKey, todoId);
  }

  public void checkIfTodoFinished(Long todo_id) {
    TodoDto todoDto = todoService.getTodoById(todo_id);
  }

  public void deleteTodo(Execution execution, Long todo_id) {
    TodoDto todoDto = todoService.getTodoById(todo_id);
    if (todoDto.getItems().isEmpty()) {
      runtimeService.setVariable(execution.getId(), "todoFinished", true);
      todoService.deleteTodo(todo_id);
    } else {
      runtimeService.setVariable(execution.getId(), "todoFinished", false);
    }
  }

  public List<String> getUsers(Long todoId) {
    List<UserDto> userDtoList = todoService.getTodoUsers(todoId);
    List<String> userPerTodo = new ArrayList<>();
    for (UserDto user : userDtoList) {
      userPerTodo.add(user.getId().toString());
    }
    List<User> userList = identityService.createUserQuery().memberOfGroup(serviceBCamundaUserService.getTheCamundaGroupId("todo", todoId)).orderByUserId().asc().list();

    return userPerTodo;
  }

  public String getGroup(Long todoId) {
    return serviceBCamundaUserService.getTheCamundaGroupId("todo", todoId);
  }

}
