package org.service.b.todo.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class TodoForm {

  @NotBlank
  @Size(min = 9)
  private String title;

  private boolean simple = true;

}
