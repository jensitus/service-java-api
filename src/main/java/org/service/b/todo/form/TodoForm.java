package org.service.b.todo.form;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class TodoForm {

  @NotBlank
  @Size(min = 9)
  private String title;

  private boolean simple = true;

}
