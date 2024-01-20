package org.service.b.common.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TimelineItemDto {

  @NotNull
  private Long id;

  private String name;

  private boolean done;

  private Long todoId;

  private LocalDate dueDate;

  private Long createdBy;

  private LocalDateTime createdAt;

  private String taskId;

  private String todoTitle;

}
