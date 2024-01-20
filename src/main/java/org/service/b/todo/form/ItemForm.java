package org.service.b.todo.form;

import jakarta.validation.constraints.NotBlank;

public class ItemForm {

  private Long id;

  @NotBlank
  private String name;

  private boolean done;

  public ItemForm() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isDone() {
    return done;
  }

  public void setDone(boolean done) {
    this.done = done;
  }
}
