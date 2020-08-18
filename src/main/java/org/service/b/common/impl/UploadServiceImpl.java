package org.service.b.common.impl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.service.b.auth.service.UserService;
import org.service.b.common.model.Uploaded;
import org.service.b.common.repository.UploadedRepo;
import org.service.b.common.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class UploadServiceImpl implements UploadService {

  private static final Logger logger = LoggerFactory.getLogger(UploadServiceImpl.class);

  private static final String FILE_DIRECTORY = "/home/jensitus/Documents/filefolder";

  @Autowired
  private UploadedRepo uploadedRepo;

  @Autowired
  private UserService userService;

  private AmazonS3 s3client;

  @Value("${awsProperties.access_key}")
  private String accessKey;

  @Value("${awsProperties.secret_key}")
  private String secretKey;

  @Value("${awsProperties.bucket}")
  private String bucket;

  @PostConstruct
  private void initializeAws() {
    AWSCredentials credentials = new BasicAWSCredentials(this.accessKey, this.secretKey);
    this.s3client = AmazonS3ClientBuilder.standard().withRegion(Regions.fromName(Regions.EU_CENTRAL_1.getName())).withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
  }

  @Override
  public void storeFile(MultipartFile file) throws IOException {
    logger.info("file: " + file.getOriginalFilename() + " " + file.getSize());
    Uploaded u = createUploaded(file);
//    Path filePath = Paths.get(FILE_DIRECTORY + "/" + u.getGeneratedName());
//    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
    Region region = null;
    // s3:// + this.bucket + / + file;
    List<Bucket> buckets = s3client.listBuckets();
    for (Bucket b : buckets) {
      logger.info(b.getName());
    }
    s3client.putObject(new PutObjectRequest(this.bucket, file.getOriginalFilename(), convertMultiPartToFile(file)));
    logger.info("accessKey: " + this.accessKey + " bucket: " + this.bucket + " uploadId: ");
  }

  @Override
  public Uploaded saveUploaded(Uploaded uploaded) {
    return uploadedRepo.save(uploaded);
  }

  @Override
  public List<Uploaded> getFileList() {
    return uploadedRepo.findAll();
  }

  private Uploaded createUploaded(MultipartFile file) {
    Long userId = userService.getCurrentUser().getId();
    Uploaded uploaded = new Uploaded();
    uploaded.setOriginalFilename(file.getOriginalFilename());
    uploaded.setUserId(userId);
    uploaded.setGeneratedName(userId + "-" + file.getOriginalFilename());
    return saveUploaded(uploaded);
  }

  private File convertMultiPartToFile(MultipartFile file) throws IOException {
    File convFile = new File(file.getOriginalFilename());
    FileOutputStream fos = new FileOutputStream(convFile);
    fos.write(file.getBytes());
    fos.close();
    return convFile;
  }

}
