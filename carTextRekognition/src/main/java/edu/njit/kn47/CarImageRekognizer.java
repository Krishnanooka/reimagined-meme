package edu.njit.kn47;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.Label;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class CarImageRekognizer {
  // Clients
  S3Client s3Client;
  RekognitionClient rekognitionClient;
  SqsClient sqsClient;

  // Variables
  String labelToBeMatched;
  String bucketName;
  Float passingConfidence;

  String queueGroup = "queue-group";
  String queueName = "car-images-queue.fifo";
  String queueUrl = ""; // Created on the fly if it doesnt exist

  public static void main(String[] args) {

    String bucketName = "njit-cs-643";
    String labelToBeMatched = "Car";
    Float passingConfidence = 90.00f;

    System.out.println("Trying to rekognize the cars in from the image present in the " + bucketName + " bucket!!");
    System.out.println(
        "The label to be matched is " + labelToBeMatched + " and the passing confidence is " + passingConfidence);

    new CarImageRekognizer(bucketName, labelToBeMatched,
        passingConfidence).startProcessImages();
  }

  public CarImageRekognizer(String bucketName,
      String labelName, Float passingConfidence) {
    this.s3Client = KN47Utils.getS3Client();
    this.rekognitionClient = KN47Utils.getRekognitionClient();
    this.sqsClient = KN47Utils.getSQSClient();

    this.bucketName = bucketName;
    this.labelToBeMatched = labelName;
    this.passingConfidence = passingConfidence;
  }

  private void startProcessImages() {
    System.out.println("Get all the images from S3");
    List<String> imageKeys = new ArrayList<>();

    ListObjectsRequest listObjects = ListObjectsRequest
        .builder()
        .bucket(this.bucketName)
        .build();

    try {

      ListObjectsResponse res = s3Client.listObjects(listObjects);
      List<S3Object> objects = res.contents();

      for (ListIterator<S3Object> iterVals = objects.listIterator(); iterVals.hasNext();) {
        S3Object s3Object = (S3Object) iterVals.next();
        System.out.println("Image found in njit-cs-643 S3 bucket: " + s3Object.key());

        imageKeys.add(s3Object.key());
      }

      System.out.println("---Fetched the list of images from S3----");

    } catch (S3Exception e) {
      System.err.println(e.awsErrorDetails().errorMessage());
      System.exit(1);
    }

    System.out.println(
        "Starting to rekognize images and searching for images with label \"" + this.labelToBeMatched + "\" "
            + this.passingConfidence + " !!!");

    for (String imageKey : imageKeys) {
      Image img = Image.builder().s3Object(software.amazon.awssdk.services.rekognition.model.S3Object
          .builder().bucket(this.bucketName).name(imageKey).build())
          .build();

      DetectLabelsRequest detectLabelRequest = DetectLabelsRequest.builder().image(img)
          .minConfidence(this.passingConfidence)
          .build();

      DetectLabelsResponse detectionResult = this.rekognitionClient.detectLabels(detectLabelRequest);
      List<Label> detectedLabels = detectionResult.labels();

      for (Label detectedLabel : detectedLabels) {

        if (detectedLabel.name().toLowerCase().equals(this.labelToBeMatched.toLowerCase())) {
          System.out.println("----forwaring image \"" + imageKey + "\" to queue for text rekognition!!---");

          if (this.queueUrl.length() == 0)
            this.queueUrl = KN47Utils.createQueueIfNotExists(this.sqsClient, this.queueName);

          this.sqsClient
              .sendMessage(SendMessageRequest.builder().messageGroupId(this.queueGroup).queueUrl(this.queueUrl)
                  .messageBody(imageKey).build());

          break;
        }
      }
    }

    this.s3Client.close();
    this.rekognitionClient.close();
    this.sqsClient.close();
    System.exit(0);
  }
}
