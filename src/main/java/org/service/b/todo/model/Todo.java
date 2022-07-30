package org.service.b.todo.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;
import org.service.b.auth.model.User;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "todos")
@Data
public class Todo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "title")
  @NotNull
  private String title;

  @Column(name = "created_by")
  @NotNull
  private Long createdBy;

  @Column(name = "created_at")
  private LocalDateTime created_at;

  @Column(name = "updated_at")
  private LocalDateTime updated_at;

  @Column(name = "done")
  private boolean done;

  @OneToMany
  @JoinColumn(name = "todo_id")
  private Set<Item> items;

  @ManyToMany(fetch = FetchType.LAZY) // (mappedBy = "todos")
  @JoinTable(name = "todos_users", joinColumns = @JoinColumn(name = "todo_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
  @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
  private Set<User> users;

  @Column(name = "simple")
  private boolean simple;

  public Todo() {}

  public Todo(@NotNull String title) {
    this.title = title;
  }


}
