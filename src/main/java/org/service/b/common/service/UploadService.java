package org.service.b.common.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


public interface UploadService {

  public void storeFile(MultipartFile file) throws IOException;

}
