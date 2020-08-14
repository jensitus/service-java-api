package org.service.b.common.controller;

import org.service.b.common.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@CrossOrigin(origins = {"https://www.service-b.org", "https://service-b.org", "http://localhost:4200", "http://localhost:8080"}, maxAge = 3600)
@RestController
@RequestMapping("/service/app")
public class UploadController {

  @Autowired
  private UploadService uploadService;

  @PostMapping(value = "/files")
  @ResponseStatus(HttpStatus.OK)
  public void handleFileUpload(@RequestParam("file")MultipartFile file) throws IOException {
    uploadService.storeFile(file);
  }

}
