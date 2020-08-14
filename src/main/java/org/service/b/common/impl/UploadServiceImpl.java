package org.service.b.common.impl;

import org.service.b.auth.service.UserService;
import org.service.b.common.model.Uploaded;
import org.service.b.common.repository.UploadedRepo;
import org.service.b.common.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class UploadServiceImpl implements UploadService {

  private static final Logger logger = LoggerFactory.getLogger(UploadServiceImpl.class);

  private static final String FILE_DIRECTORY = "/home/jensitus/Documents/filefolder";

  @Autowired
  private UploadedRepo uploadedRepo;

  @Autowired
  private UserService userService;

  @Override
  public void storeFile(MultipartFile file) throws IOException {
    logger.info("file: " + file.getOriginalFilename() + " " + file.getSize());
    Long userId = userService.getCurrentUser().getId();
    Uploaded uploaded = new Uploaded();
    uploaded.setOriginalFilename(file.getOriginalFilename());
    uploaded.setUserId(userId);
    uploaded.setGeneratedName(userId + "-" + file.getOriginalFilename());
    Uploaded u = saveUploaded(uploaded);
    Path filePath = Paths.get(FILE_DIRECTORY + "/" + u.getGeneratedName());
    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
  }

  @Override
  public Uploaded saveUploaded(Uploaded uploaded) {
    return uploadedRepo.save(uploaded);
  }
}
