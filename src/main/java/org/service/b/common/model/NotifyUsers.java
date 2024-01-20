package org.service.b.common.model;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Table(name = "notify_users")
public class NotifyUsers {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(name = "id", length = 16, unique = true, nullable = false)
  private UUID id;

  @Column(name = "string_id")
  private String stringId;

  @Column(name = "notified")
  private boolean notified;

  @Column(name = "model_id")
  private Long modelId;

  @Enumerated(EnumType.STRING)
  @Column(name = "model_type")
  private ModelType modelType;

  @Enumerated(EnumType.STRING)
  @Column(name = "what_is_reported")
  private WhatIsReported whatIsReported;

  public NotifyUsers() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getStringId() {
    return stringId;
  }

  public void setStringId(String stringId) {
    this.stringId = stringId;
  }

  public boolean isNotified() {
    return notified;
  }

  public void setNotified(boolean notified) {
    this.notified = notified;
  }

  public Long getModelId() {
    return modelId;
  }

  public void setModelId(Long modelId) {
    this.modelId = modelId;
  }

  public ModelType getModelType() {
    return modelType;
  }

  public void setModelType(ModelType modelType) {
    this.modelType = modelType;
  }

  public WhatIsReported getWhatIsReported() {
    return whatIsReported;
  }

  public void setWhatIsReported(WhatIsReported whatIsReported) {
    this.whatIsReported = whatIsReported;
  }

  @Override
  public String toString() {
    return "NotifyUsers{" +
            "id=" + id +
            ", stringId='" + stringId + '\'' +
            ", notified=" + notified +
            ", modelId=" + modelId +
            ", modelType=" + modelType +
            ", whatIsReported=" + whatIsReported +
            '}';
  }
}
