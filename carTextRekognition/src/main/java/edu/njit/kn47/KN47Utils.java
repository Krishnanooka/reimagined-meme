package edu.njit.kn47;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;
import software.amazon.awssdk.services.sqs.model.SqsException;

public class KN47Utils {

  public static String createQueueIfNotExists(SqsClient sqsClient, String queueName) {
    String queueUrl = "";

    try {
      ListQueuesRequest listQueuesRequest = ListQueuesRequest.builder()
          .queueNamePrefix(queueName)
          .build();

      ListQueuesResponse listQueuesResponse = sqsClient.listQueues(listQueuesRequest);

      if (listQueuesResponse.queueUrls().size() == 0) {
        // No Queue Exists, So create one!
        System.out.println("Queue doesnt exists! Creating a new one!!");

        CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
            .attributesWithStrings(Map.of("FifoQueue", "true",
                "ContentBasedDeduplication", "true"))
            .queueName(queueName)
            .build();
        sqsClient.createQueue(createQueueRequest);

        GetQueueUrlRequest getQueueUrlRequest = GetQueueUrlRequest.builder()
            .queueName(queueName)
            .build();

        queueUrl = sqsClient.getQueueUrl(getQueueUrlRequest).queueUrl();

      } else {
        // Queue already exists, So use it
        System.out.println("Queue already exists! Won't create a new one!!");
        queueUrl = listQueuesResponse.queueUrls().get(0);
      }

      System.out.println("Queue URL: " + queueUrl);

      return queueUrl;
    } catch (SqsException e) {
      // TODO: handle exception
      System.err.println(e.awsErrorDetails().errorMessage());
      System.exit(1);
    }

    // runtime will never reach here!! due to system.exit()
    return queueUrl;
  }

  public static void appendTextToFile(String text) {
    try {
      Files.writeString(
          Path.of(System.getProperty("java.io.tmpdir"), "carTextOutput.txt"),
          text + System.lineSeparator(),
          StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      // Check using `cat /tmp/carTextOutput.txt`
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("Unable to write text to file!!");
    }
  }

  public static String getExistingQueurUrl(String queueName, SqsClient sqsClient) {
    // Poll SQS until the queue is created (by DetectCars)
    boolean queueExists = false;

    String queueUrl = "";

    while (!queueExists) {
      ListQueuesRequest listQueueRequest = ListQueuesRequest.builder()
          .queueNamePrefix(queueName)
          .build();

      ListQueuesResponse listQueueResponse = sqsClient.listQueues(listQueueRequest);

      if (listQueueResponse.queueUrls().size() > 0) {
        queueExists = true;
        System.out.println("Queue found, Proceeding further!");
      } else {
        // Sleep for some time as we dont want flood with the request!
        try {
          int sleepTime = 10;
          System.out.println("Queue not found, waiting for " + sleepTime + "s!!");
          TimeUnit.SECONDS.sleep(sleepTime);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

    }

    try {
      GetQueueUrlRequest getQueueUrlRequest = GetQueueUrlRequest.builder()
          .queueName(queueName)
          .build();
      queueUrl = sqsClient.getQueueUrl(getQueueUrlRequest)
          .queueUrl();

      System.out.println("QueueURL: " + queueUrl);

    } catch (QueueNameExistsException e) {
      System.err.println(e.awsErrorDetails().errorMessage());
      System.exit(1);
    }

    return queueUrl;
  }

  public static StaticCredentialsProvider getAWSCredentials() {
    AwsBasicCredentials credentials = AwsBasicCredentials.create("AKIAVKHAOYI6JI2PLV2I",
        "rTVokbLAIzarN4DjSYr5zhHkyGlTPvBXarS3kofL");

    StaticCredentialsProvider staticCredentials = StaticCredentialsProvider.create(credentials);

    return staticCredentials;
  }

  public static SqsClient getSQSClient() {
    StaticCredentialsProvider staticCredentials = KN47Utils.getAWSCredentials();
    Region region = Region.US_EAST_1;

    SqsClient sqsClient = SqsClient.builder()
        .credentialsProvider(staticCredentials)
        .region(region)
        .build();

    return sqsClient;
  }

  public static RekognitionClient getRekognitionClient() {
    StaticCredentialsProvider staticCredentials = KN47Utils.getAWSCredentials();
    Region region = Region.US_EAST_1;

    RekognitionClient rekognitionClient = RekognitionClient.builder()
        .credentialsProvider(staticCredentials)
        .region(region)
        .build();

    return rekognitionClient;
  }

  public static S3Client getS3Client() {
    StaticCredentialsProvider staticCredentials = KN47Utils.getAWSCredentials();
    Region region = Region.US_EAST_1;

    S3Client s3Client = S3Client.builder()
        .credentialsProvider(staticCredentials)
        .region(region)
        .build();

    return s3Client;
  }
}
