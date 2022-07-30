package org.service.b.todo.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class DescriptionDto {

  private Long id;
  private String text;
  private Long item_id;
  private Long user_id;
  private Long todo_id;

}
