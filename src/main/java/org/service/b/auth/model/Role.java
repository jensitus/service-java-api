package org.service.b.auth.model;

import org.hibernate.annotations.NaturalId;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private int id;

  @Enumerated(EnumType.STRING)
  @NaturalId
  @Column(name = "name")
  private RoleName name;

  public Role() {
  }

  public Role(RoleName name) {
    this.name = name;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public RoleName getName() {
    return name;
  }

  public void setName(RoleName name) {
    this.name = name;
  }
}
