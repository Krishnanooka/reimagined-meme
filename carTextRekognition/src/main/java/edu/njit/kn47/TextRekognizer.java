package edu.njit.kn47;

import java.util.List;
import java.util.concurrent.TimeUnit;

import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectTextRequest;
import software.amazon.awssdk.services.rekognition.model.DetectTextResponse;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.S3Object;
import software.amazon.awssdk.services.rekognition.model.TextDetection;
import software.amazon.awssdk.services.rekognition.model.TextTypes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

public class TextRekognizer {
  S3Client s3Client;
  RekognitionClient rekognitionClient;
  SqsClient sqsClient;

  String queueGroup = "queue-group";
  String bucketName;
  String queueName = "car-images-queue.fifo";
  Boolean inProgress = false;
  String queueUrl = "";

  public static void main(String[] args) {
    System.out.println("Trying to rekognize the text from the images coming in from the queue !!");

    new TextRekognizer().startProcessingImagesFromQueue();
  }

  // Constructor
  public TextRekognizer() {

    String bucketName = "njit-cs-643";
    this.s3Client = KN47Utils.getS3Client();
    this.rekognitionClient = KN47Utils.getRekognitionClient();
    this.sqsClient = KN47Utils.getSQSClient();

    this.bucketName = bucketName;
  }

  private void startProcessingImagesFromQueue() {
    this.queueUrl = KN47Utils.getExistingQueurUrl(this.queueName, this.sqsClient);

    int runningCount = 0;
    while (runningCount < 100) {

      if (inProgress) {
        System.out.println("Processing image in progress, So skipping this check!");
      } else {
        runningCount++;

        System.out.println("Checking for messages in the queue! - " + runningCount);

        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder().queueUrl(queueUrl)
            .maxNumberOfMessages(1).build();
        List<Message> messages = this.sqsClient.receiveMessage(receiveMessageRequest).messages();

        if (messages.size() > 0) {
          Message message = messages.get(0);
          String imageKey = message.body();
          System.out
              .println("Getting the text out of car image from \"" + this.bucketName + "\" S3 bucket: " + imageKey);

          inProgress = true;

          Image img = Image.builder().s3Object(S3Object.builder().bucket(this.bucketName).name(imageKey).build())
              .build();
          DetectTextRequest textDetectionRequest = DetectTextRequest.builder()
              .image(img)
              .build();

          DetectTextResponse textDetectionResponse = this.rekognitionClient.detectText(textDetectionRequest);
          List<TextDetection> textDetections = textDetectionResponse.textDetections();
          if (textDetections.size() != 0) {
            String text = "";

            for (TextDetection detectedText : textDetections) {
              if (detectedText.type().equals(TextTypes.WORD))
                text = text.concat(" " + detectedText.detectedText());
            }

            KN47Utils.appendTextToFile(imageKey + ": " + text);
          }

          inProgress = false;

          this.sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl)
              .receiptHandle(message.receiptHandle())
              .build());
        }
      }

      try {
        TimeUnit.SECONDS.sleep(5);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    // Close the clients
    this.s3Client.close();
    this.rekognitionClient.close();
    this.sqsClient.close();
    System.exit(0);
  }

}
