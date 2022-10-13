CS643-852
Programming Assignment 3
Author: Krishna theja reddy Nooka
Date: 10/13/2022


## Initialization
For our project, we will need two instances of EC2, as one will be used to recognize automobile images and the other to extract the text from them.

## Use cases that this project might have
1. This project could be used to address traffic signal infractions
2. Face recognition might be utilized at the checkout counters of supermarkets in place of the car-based system.

The possibilities are endless.

For the program to function, the AWS credentials must be set up.
Since educate credits are not yet available for the aws, we shall be using the *free tier* option that it offers.

1. Open an AWS account using your NIT email.
2. Following login, look for the IAM option.

Let's make a new user with only programmatic access, sometimes known as a service account. This user will have access to the services S3, SQS, and Rekognition.

3. Make an arbitrarily named user (eg: crazycoder)

a. From the left-hand menu, select "Access Management" and then "Users."

b. Use the "Add User" button to create a new user (e.g. crazycoder)

c. Enter the username, select "Access key - Programmatic access," and then press "next."

d. Click "Attach existing policies directly," then pick the policies listed below.

"' AmazonRekognitionFullAccess "' "' "' "' AmazonS3FullAccess "'
Having tight policies in place would help us prevent the keys from suffering significant damage.

g. There will be a download option for the credentials in ".csv" format after the final step. Be sure to save it in a secure location. Later, we'll make use of it.


## EC2 Instance Deployment

1. In the search bar, type "EC2," and then select the first result.
2. Select "Instances" from the navigation menu on the left.
2. Select "Launch Instances." 3.
4. Choose the "Amazon linux 2 AMI - free tier eligible" option.

6. Make a login "key pair"
Once the "Key pair" has been formed, the browser will launch a download window. Save it in a recognizable location so you won't lose it.

7. Leave the other default settings in place and click "Launch instance" to start the instance.

### Configuring an EC2 instance to run our code

To change the permissions of the.pem file downloaded in the previous step, issue the following command:

key name>.pem $ chmod 400

The "Public IPv4 DNS" property of either EC2 instance may be substituted for "YOUR EC2 INSTANCE PUBLIC DNS" in the following command to connect to the newly formed instance:

"ssh ssh -i" "The key name.

pem "ec2-user@<YOUR EC2 INSTANCE PUBLIC IPV4 ADDRESS>
```

Installing Java, Maven, and Git is necessary for our project to function. Using the following instructions
"'sh sudo yum install java-1.8.0-openjdk git sudo amazon-linux-extras sudo yum update sudo wget https://downloads.apache.org/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz install java-openjdk11 -y /opt sudo ln -s /opt/apache-maven-3.8.6 /opt/maven "' -O /tmp/apache-maven-3.8.6-bin.tar.gz sudo tar xf /tmp/apache-maven-3.8.6-bin.tar.gz

For it to function properly, add the following lines to "maven.sh"

the command "sh sudo nano /etc/profile.d/maven.sh"
"sh export" Exporting JAVA HOME=/usr/lib/jvm/jre-11-openjdk Export M2 HOME=/opt/maven Export MAVEN HOME=/opt/maven PATH = "$M2 HOME/bin:$PATH" "
"'Shift + sudo chmod +x /etc/profile.d/maven.sh source "'
Starting an EC2 instance with the application

Automobile Image Recognition

'CarImageRekognizer.java' & 'TextRekognizer.java' should both have the credentials copied in.

On the local system, open a terminal window inside the project directory, and then copy the folder using scp.

"sh scp -r -i" "key name; end.

pem "ec2-user@YOUR EC2 INSTANCE PUBLIC DNS:/home/ec2-user/ "' carTextRekognition

Run the command "sh cd carTextRekognition/ mvn install mvn exec:java@carRekognizer" on ec2-a.

Text Extractor ####

'CarImageRekognizer.java' & 'TextRekognizer.java' should both have the credentials copied in.

On the local system, open a terminal window inside the project directory, and then copy the folder using scp.

"sh scp -r -i" "key name; end.

pem "ec2-user@YOUR EC2 INSTANCE PUBLIC DNS:/home/ec2-user/ "' carTextRekognition

Run the command "sh cd carTextRekognition/ mvn install mvn exec:java@textRekognizer" on ec2-a.``sh cd carTextRekognition/ mvn install mvn exec:java@textRekognizer ```