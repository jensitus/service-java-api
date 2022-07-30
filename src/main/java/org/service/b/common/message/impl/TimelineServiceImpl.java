package org.service.b.common.message.impl;

import org.modelmapper.ModelMapper;
import org.service.b.common.dto.TimelineItemDto;
import org.service.b.common.message.service.TimelineService;
import org.service.b.todo.dto.ItemDto;
import org.service.b.todo.dto.TodoDto;
import org.service.b.todo.model.Item;
import org.service.b.todo.model.Todo;
import org.service.b.todo.repository.ItemRepo;
import org.service.b.todo.repository.TodoRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TimelineServiceImpl implements TimelineService {

  private static final Logger logger = LoggerFactory.getLogger(TimelineServiceImpl.class);

  @Autowired
  private ItemRepo itemRepo;

  @Autowired
  private ModelMapper modelMapper;

  @Autowired
  private TodoRepo todoRepo;

//  @Autowired
//  private ServiceBTaskService serviceBTaskService;

  @Override
  public List<TimelineItemDto> getItemsForTimeline(Long user_id) {
    List<TimelineItemDto> timelineItemDtoList = getTimelineDtos(user_id);
    List<TimelineItemDto> dueDateFirstList = timelineItemDtoList.stream().filter(x -> x.getDueDate() != null).collect(Collectors.toList());
    for (TimelineItemDto t : timelineItemDtoList) {
      if (t.getDueDate() == null) {
        dueDateFirstList.add(t);
      }
    }
    return dueDateFirstList;
  }

  @Override
  public Map<String, ItemDto> getItemsGroupedByMonth(Long user_id) {
    List<TimelineItemDto> timelineItemDtos = getTimelineDtos(user_id);
    Map timelineItemMap = timelineItemDtos.stream().collect(Collectors.groupingBy(i -> i.getCreatedAt().getMonth()));
    for (TimelineItemDto dto : timelineItemDtos) {
      dto.getCreatedAt().getMonth();
    }
    return timelineItemMap;
  }

  private TodoDto getTodoDto(Long id) {
    Todo todo = todoRepo.getOne(id);
    return modelMapper.map(todo, TodoDto.class);
  }

  private List<TimelineItemDto> getTimelineDtos(Long user_id) {
    List<TimelineItemDto> timelineItemDtos = new ArrayList<>();
    List<Item> oldItems = itemRepo.findItemsByUserIdOrderByCreatedAt(user_id);
    // oldItems.stream().filter(oldItem -> oldItemDtos.add(modelMapper.map(oldItem, ItemDto.class)));
    for (Item item : oldItems) {
      TodoDto todoDto = getTodoDto(item.getTodoId());
      TimelineItemDto timelineItemDto = modelMapper.map(item, TimelineItemDto.class);
      timelineItemDto.setTodoTitle(todoDto.getTitle());
      timelineItemDtos.add(timelineItemDto);
    }
    return timelineItemDtos;
  }



}
