package org.service.b.todo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "items")
public class Item {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "name")
  @NotNull
  private String name;

  @Column(name = "done")
  @NotNull
  private boolean done;

  @Column(name = "todo_id", nullable = false)
  private Long todoId;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "created_by")
  private Long createdBy;

  @OneToMany(mappedBy = "item")
  private Set<Description> descriptions;

  @Column(name = "due_date")
  private LocalDate dueDate;

  public Item() {}

  public Item(@NotNull String name, boolean done, Long todo_id) {
    this.name = name;
  }

  public Item(@NotNull String name, @NotNull boolean done, Long todoId, LocalDate dueDate) {
    this.name = name;
    this.dueDate = dueDate;
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

  public Long getTodoId() {
    return todoId;
  }

  public void setTodoId(Long todoId) {
    this.todoId = todoId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(Long createdBy) {
    this.createdBy = createdBy;
  }

  public Set<Description> getDescriptions() {
    return descriptions;
  }

  public void setDescriptions(Set<Description> descriptions) {
    this.descriptions = descriptions;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }

  @Override
  public String toString() {
    return "Item{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", done=" + done +
            ", todoId=" + todoId +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            ", createdBy=" + createdBy +
            ", dueDate=" + dueDate +
            '}';
  }
}
