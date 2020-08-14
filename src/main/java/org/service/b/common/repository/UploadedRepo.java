package org.service.b.common.repository;

import org.service.b.common.model.Uploaded;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UploadedRepo extends JpaRepository<Uploaded, UUID> {
}
