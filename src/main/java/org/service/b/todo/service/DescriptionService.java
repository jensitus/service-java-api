package org.service.b.todo.service;

import org.modelmapper.ModelMapper;
import org.service.b.auth.service.UserService;
import org.service.b.todo.dto.DescriptionDto;
import org.service.b.todo.form.DescriptionForm;
import org.service.b.todo.model.Description;
import org.service.b.todo.model.Item;
import org.service.b.todo.model.Todo;
import org.service.b.todo.repository.DescriptionRepo;
import org.service.b.todo.repository.ItemRepo;
import org.service.b.todo.repository.TodoRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DescriptionService {

  private static final Logger logger = LoggerFactory.getLogger(DescriptionService.class);

  private static final String TODO_ENTITIY_NAME = "todo";

  private static final String ITEM_ENTITIY_NAME = "item";

  private final DescriptionRepo descriptionRepo;
  private final ModelMapper modelMapper;
  private final ItemRepo itemRepo;
  private final TodoRepo todoRepo;
  private final UserService userService;

  public DescriptionService(DescriptionRepo descriptionRepo, ModelMapper modelMapper, ItemRepo itemRepo, TodoRepo todoRepo, UserService userService) {
    this.descriptionRepo = descriptionRepo;
    this.modelMapper = modelMapper;
    this.itemRepo = itemRepo;
    this.todoRepo = todoRepo;
    this.userService = userService;
  }

  public List<DescriptionDto> getDescriptionsByItemId(Long item_id) {
    List<Description> descriptionList = descriptionRepo.findByItemIdOrderByCreatedAt(item_id);
    List<DescriptionDto> descriptionDtoList = new ArrayList<>();
    for (Description description : descriptionList) {
//      logger.info("Description for Item {} found: {}", item_id, description);
      descriptionDtoList.add(modelMapper.map(description, DescriptionDto.class));
    }
    return descriptionDtoList;
  }

  public DescriptionDto createDescription(DescriptionForm descriptionForm, Long entity_id, String entityName) {
    Description description = new Description(descriptionForm.getText());
    description.setCreatedAt(LocalDateTime.now());
    description.setUserId(userService.getCurrentUser().getId());
    if (TODO_ENTITIY_NAME.equals(entityName)) {
      Todo todo = todoRepo.getOne(entity_id);
      description.setTodoId(todo.getId());
    } else if (ITEM_ENTITIY_NAME.equals(entityName)) {
      Item item = itemRepo.getOne(entity_id);
      description.setItem(item);
    }
    descriptionRepo.save(description);
    return modelMapper.map(description, DescriptionDto.class);
  }

  public DescriptionDto updateDescription(DescriptionForm descriptionForm, Long entity_id, String entityName) {
    Description description = descriptionRepo.getOne(descriptionForm.getId());
    description.setText(descriptionForm.getText());
    descriptionRepo.save(description);
    return modelMapper.map(description, DescriptionDto.class);
  }

}
