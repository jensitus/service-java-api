package org.service.b.todo.dto;

import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@ToString
public class ItemDto {

  @NotNull
  private Long id;
  private String name;
  private boolean done;
  private Long todoId;
  private LocalDate dueDate;
  private Long createdBy;
  private LocalDateTime createdAt;

}
