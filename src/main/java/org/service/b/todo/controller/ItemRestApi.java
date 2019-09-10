package org.service.b.todo.controller;

import org.service.b.common.message.Message;
import org.service.b.common.message.service.MessageService;
import org.service.b.todo.dto.ItemDto;
import org.service.b.todo.form.ItemForm;
import org.service.b.todo.model.Item;
import org.service.b.todo.service.ItemService;
import org.service.b.todo.service.TodoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.Path;
import java.time.LocalDate;

@RestController
@CrossOrigin(origins = {"https://www.service-b.org", "https://service-b.org", "http://localhost:4200", "http://localhost:8080"}, maxAge = 3600)
@RequestMapping("/service/todos")
public class ItemRestApi {

  private static final Logger logger = LoggerFactory.getLogger(ItemRestApi.class);

  @Autowired
  private TodoService todoService;

  @Autowired
  private ItemService itemService;

  @Autowired
  private MessageService messageService;

  @GetMapping("/{todo_id}/items")
  public ResponseEntity getTodoItems(@PathVariable("todo_id") Long todo_id) {
    return new ResponseEntity(todoService.getTodoItems(todo_id), HttpStatus.OK);
  }

  @PostMapping("/{todo_id}/items")
  public ResponseEntity createTodoItem(@PathVariable("todo_id") Long todo_id, @RequestBody ItemForm itemForm) {
    Item jepp = itemService.createItem(todo_id, itemForm.getName());
    return new ResponseEntity(jepp, HttpStatus.OK);
  }

  @PutMapping("/{todo_id}/items/{item_id}")
  public ResponseEntity updateTodoItem(@PathVariable("todo_id") Long todo_id, @PathVariable("item_id") Long item_id, @RequestBody ItemForm itemForm) {
    if (item_id.equals(itemForm.getId())) {
      ItemDto itemDto = itemService.updateItem(item_id);
      return new ResponseEntity(itemDto, HttpStatus.OK);
    } else {
      return new ResponseEntity(new Message("ach bitte"), HttpStatus.I_AM_A_TEAPOT);
    }
  }

  @DeleteMapping("/{todo_id}/items/{item_id}")
  public ResponseEntity deleteTodoItem(@PathVariable("todo_id") Long todo_id, @PathVariable("item_id") Long item_id) {
    messageService.sendMessageToCatchEvent("sub-item-done", "sub-todo-service-item", item_id);
    itemService.deleteItem(item_id);
    Message message = new Message("successfully deleted");
    return new ResponseEntity(message, HttpStatus.OK);
  }

  @PutMapping("/{todo_id}/items/{item_id}/due_date")
  public ResponseEntity setItemDuedate(@PathVariable("todo_id") Long todo_id, @PathVariable("item_id") Long item_id, @RequestBody String dueDate) {
    logger.info(dueDate.toString());
    ItemDto itemDto = itemService.setItemDueDate(item_id, dueDate);
    return new ResponseEntity(itemDto, HttpStatus.OK);
  }

}
