package org.service.b.todo.repository;

import org.service.b.todo.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ItemRepo extends JpaRepository<Item, Long> {

  List findByTodoIdOrderByCreatedAt(Long todo_id);

  List findByDueDateBetween(LocalDate dueDateStart, LocalDate dueDateEnd);

  @Query(value = "SELECT i.* from USERS u, ITEMS i, TODOS_USERS tu where u.id = tu.user_id and tu.todo_id = i.todo_id and u.id = ?1 order by i.created_at asc", nativeQuery = true)
  List<Item> findItemsByUserIdOrderByCreatedAt(Long user_id);

}
