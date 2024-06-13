package com.amigoscode.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;

@Service
public class S3Service {     // upload/store object, download object

    private final static Logger logger = LoggerFactory.getLogger(S3Service.class);

    @Autowired
    private S3Client s3ClientCust;

    public S3Service(S3Client s3ClientCust) {
        this.s3ClientCust = s3ClientCust;
    }

    public void putObject(String bucketName, String key, byte[] file) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3ClientCust.putObject(objectRequest, RequestBody.fromBytes(file));
        logger.info("Finish uploading file to S3: " + key);
    }

    public byte[] getObject(String bucketName, String key) {
        try {
            GetObjectRequest objectRequest = GetObjectRequest
                    .builder()
                    .key(key)
                    .bucket(bucketName)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3ClientCust.getObjectAsBytes(objectRequest);
            byte[] data = objectBytes.asByteArray();
            return data;
//            // Write the data to a local file.
//            File myFile = new File(path );
//            OutputStream os = new FileOutputStream(myFile);
//            os.write(data);
//            System.out.println("Successfully obtained bytes from an S3 object");
//            os.close();

//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return null;
    }
}
