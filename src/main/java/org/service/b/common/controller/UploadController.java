package org.service.b.common.controller;

import org.service.b.common.model.Uploaded;
import org.service.b.common.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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

  @GetMapping(value = "/get-images/filelist")
  @ResponseStatus(HttpStatus.OK)
  public List<Uploaded> getFileList() {
    return uploadService.getFileList();
  }

  @GetMapping(value = "/get-images/single-file")
  public ResponseEntity<byte[]> getSingleFile() throws IOException {
    ClassPathResource f = new ClassPathResource("images/1-leiberl.jpg");
    byte[] fBytes = StreamUtils.copyToByteArray(f.getInputStream());
    return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(fBytes);
  }

}
