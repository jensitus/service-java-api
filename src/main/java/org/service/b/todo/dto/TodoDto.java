package org.service.b.todo.dto;

import lombok.Data;
import org.service.b.auth.dto.UserDto;

import java.util.List;
import java.util.Set;

@Data
public class TodoDto {

  private Long id;
  private String title;
  private Long createdBy;
  private boolean done;
  private List<ItemDto> items;
  private Set<UserDto> users;
  private DescriptionDto description;
  private boolean simple;

}
