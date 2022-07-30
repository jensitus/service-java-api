package org.service.b.todo.service;

import org.modelmapper.ModelMapper;
import org.service.b.auth.dto.UserDto;
import org.service.b.auth.service.UserService;
import org.service.b.common.config.ServiceBProcessEnum;
import org.service.b.common.mailer.service.ServiceBOrgMailer;
import org.service.b.common.model.ModelType;
import org.service.b.common.model.NotifyUsers;
import org.service.b.common.model.WhatIsReported;
import org.service.b.common.repository.NotifyUsersRepo;
import org.service.b.todo.dto.ItemDto;
import org.service.b.todo.dto.TodoDto;
import org.service.b.todo.model.Description;
import org.service.b.todo.model.Item;
import org.service.b.todo.model.Todo;
import org.service.b.todo.repository.DescriptionRepo;
import org.service.b.todo.repository.ItemRepo;
import org.service.b.todo.repository.TodoRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ItemService {

  private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

  private static final String NEW_ITEMS_EMAIL_SUBJECT = "new items ";

  private static final String LINE_BREAK_AND_NBSP = "<br>&nbsp; - ";
  // "<br>&nbsp; - "
  private final TodoRepo todoRepo;
  private final ItemRepo itemRepo;
  private final ModelMapper modelMapper;
  private final UserService userService;
  private final DescriptionRepo descriptionRepo;
  private final DescriptionService descriptionService;
  private final NotifyUsersRepo notifyUsersRepo;
  private final ServiceBOrgMailer serviceBOrgMailer;

  public ItemService(TodoRepo todoRepo, ItemRepo itemRepo, ModelMapper modelMapper, UserService userService, DescriptionRepo descriptionRepo, DescriptionService descriptionService, NotifyUsersRepo notifyUsersRepo, ServiceBOrgMailer serviceBOrgMailer) {
    this.todoRepo = todoRepo;
    this.itemRepo = itemRepo;
    this.modelMapper = modelMapper;
    this.userService = userService;
    this.descriptionRepo = descriptionRepo;
    this.descriptionService = descriptionService;
    this.notifyUsersRepo = notifyUsersRepo;
    this.serviceBOrgMailer = serviceBOrgMailer;
  }

  public ItemDto createItem(Long todo_id, String name) {
    Todo todo = todoRepo.getOne(todo_id);
    Item item = new Item();
    item.setName(name);
    item.setDone(false);
    item.setCreatedAt(LocalDateTime.now());
    item.setCreatedBy(userService.getCurrentUser().getId());
    item.setTodoId(todo_id);
    Item newItem = itemRepo.save(item);
    ItemDto itemDto = modelMapper.map(newItem, ItemDto.class);
    NotifyUsers notifyUsers = new NotifyUsers();
    notifyUsers.setWhatIsReported(WhatIsReported.NEW_ITEM);
    notifyUsers.setModelId(itemDto.getId());
    notifyUsers.setModelType(ModelType.ITEM);
    notifyUsers.setNotified(Boolean.FALSE);
    notifyUsers.setStringId(createIdForNotifyingTheUser(itemDto.getId(), WhatIsReported.NEW_ITEM, itemDto.getName()));
    notifyUsersRepo.save(notifyUsers);
    return itemDto;
  }

  public ItemDto updateItem(Long item_id) {
    Item item = itemRepo.getOne(item_id);
    boolean done = !item.isDone();
    item.setDone(done);
    itemRepo.save(item);
//    setNewStringId();
    return modelMapper.map(item, ItemDto.class);
  }

  public void deleteItem(Long item_id) {
    Item item = itemRepo.getOne(item_id);
    logger.info(item.toString());
    List<Description> itemDescriptions = descriptionRepo.findByItemIdOrderByCreatedAt(item.getId());
    descriptionRepo.deleteInBatch(itemDescriptions);
    NotifyUsers notifyUsers = notifyUsersRepo.findByStringId(createIdForNotifyingTheUser(item.getId(), WhatIsReported.NEW_ITEM, item.getName()));
    if (notifyUsers != null) {
      notifyUsersRepo.delete(notifyUsers);
    }
    itemRepo.delete(item);
  }

  public ItemDto setItemDueDate(Long item_id, String dueDate) {
    logger.info(dueDate.toString());
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    LocalDate formattedDueDate = LocalDate.parse(dueDate, formatter);
    Item item = itemRepo.getOne(item_id);
    item.setDueDate(formattedDueDate);
    itemRepo.save(item);
    ItemDto itemDto = modelMapper.map(item, ItemDto.class);
    NotifyUsers notifyUsers;
    notifyUsers = notifyUsersRepo.findByStringId(createIdForNotifyingTheUser(item.getId(), WhatIsReported.DUE_DATE, item.getName()));
    // TODO: We need to find a solution for the Identifier (ID)
    if (notifyUsers == null) {
      notifyUsers = new NotifyUsers();
      notifyUsers.setStringId(createIdForNotifyingTheUser(itemDto.getId(), WhatIsReported.DUE_DATE, itemDto.getName()));
      notifyUsers.setNotified(Boolean.FALSE);
      notifyUsers.setModelId(itemDto.getId());
      notifyUsers.setWhatIsReported(WhatIsReported.DUE_DATE);
      notifyUsers.setModelType(ModelType.ITEM);
    } else {
      notifyUsers.setNotified(Boolean.FALSE);
    }
    notifyUsersRepo.save(notifyUsers);
    logger.info(itemDto.toString());
    return itemDto;
  }

  /**
   * We will need the scheduler below in future versions
   */
  @Scheduled(cron = "0 10 * * * *")
  @Transactional
  public void checkFixedRateTask() {
    LocalDate dueDateStart = LocalDate.now();
    LocalDate dueDateEnd = LocalDate.now().plusDays(2);
    getDueDatedItems(dueDateStart, dueDateEnd);
  }

  private void getDueDatedItems(LocalDate dueDateStart, LocalDate dueDateEnd) {
    List<Item> itemList = itemRepo.findByDueDateBetween(dueDateStart, dueDateEnd);
    for (Item item : itemList) {
      String string_id = createIdForNotifyingTheUser(item.getId(), WhatIsReported.DUE_DATE, item.getName());
      NotifyUsers notifyUsers = notifyUsersRepo.findByStringId(string_id);
      if (notifyUsers != null && !notifyUsers.isNotified()) {
        TodoDto todoDto = getTodoDto(item.getTodoId());
        String subject = ServiceBProcessEnum.SERVICE_B_EMAIL_SUBJECT_PREFIX.getValue() + "Due Date of " + item.getName();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String text = "Due Date of " + item.getName() + " in " + todoDto.getTitle() + ":" + LINE_BREAK_AND_NBSP + item.getDueDate().format(formatter);
        String url = ServiceBProcessEnum.SERVICE_B_BASE_URL.getValue();
        sendTheMail(todoDto, subject, text, url);
        notifyUsers.setNotified(true);
      }
    }
  }

  private String createIdForNotifyingTheUser(Long id, WhatIsReported whatIsReported, String name) {
    String[] donner = name.split(" ");
    String donnerString = "";
    for (int i = 0; i < donner.length; i++) {
      donnerString = donnerString + donner[i];
    }
    return id.toString() + "_" + whatIsReported.toString() + "_" + donnerString;
  }

  @Scheduled(cron = "0 0/5 7-21 * * *")
  @Transactional
  public synchronized void informAboutNewItem() {
    List<ItemDto> itemDtoList = new ArrayList<>();
    Map<Long, List<ItemDto>> itemMap = new HashMap<>();
    List<NotifyUsers> notifyList = notifyUsersRepo.findByWhatIsReportedAndNotified(WhatIsReported.NEW_ITEM, false);
    if (!notifyList.isEmpty()) {
      List<ItemDto> itemsToSetNotified = new ArrayList<>();
      itemMap = notifyList.stream().map(n -> getItemDto(n.getModelId())).collect(Collectors.groupingBy(ItemDto::getTodoId));
      Map<TodoDto, List<ItemDto>> todoItemMap = itemMap.entrySet().stream().collect(Collectors.toMap(e -> getTodoDto(e.getKey()), e -> e.getValue()));
      for (TodoDto todoDto : todoItemMap.keySet()) {
        String subject = ServiceBProcessEnum.SERVICE_B_EMAIL_SUBJECT_PREFIX.getValue() + NEW_ITEMS_EMAIL_SUBJECT + todoDto.getTitle();
        String text = "the following items are new in<br><b>" + todoDto.getTitle() + ":</b>";
        String url = ServiceBProcessEnum.SERVICE_B_BASE_URL.getValue() + "todos/" + todoDto.getId();
        for (ItemDto itemDto : todoItemMap.get(todoDto)) {
          text = text + LINE_BREAK_AND_NBSP + itemDto.getName();
          if (itemDto != null) {
            itemsToSetNotified.add(itemDto);
          }
        }
        sendTheMail(todoDto, subject, text, url);
      }
      setNotifiedTrue(itemsToSetNotified);
    }
  }

  private void sendTheMail(TodoDto todoDto, String subject, String text, String url) {
    Set<UserDto> receivers = todoDto.getUsers();
    if (!receivers.isEmpty()) {
      receivers.forEach(u -> serviceBOrgMailer.getTheMailDetails(u.getEmail(), subject, text, u.getUsername(), url));
    }
  }

  private void setNotifiedTrue(List<ItemDto> itemToSetNotified) {
    for (ItemDto itemDto : itemToSetNotified) {
      NotifyUsers notifyUsers = notifyUsersRepo.findByStringId(createIdForNotifyingTheUser(itemDto.getId(), WhatIsReported.NEW_ITEM, itemDto.getName()));
      if (notifyUsers != null) {
        notifyUsers.setNotified(true);
        notifyUsersRepo.save(notifyUsers);
      }
    }
  }

  private ItemDto getItemDto(Long id) {
    Item item = itemRepo.getOne(id);
    return modelMapper.map(item, ItemDto.class);
  }

  private TodoDto getTodoDto(Long id) {
    Todo todo = todoRepo.getOne(id);
    return modelMapper.map(todo, TodoDto.class);
  }

}
