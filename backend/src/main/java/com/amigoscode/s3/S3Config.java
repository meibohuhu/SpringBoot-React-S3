package com.amigoscode.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {   // server as a means fo us to create the client, put local env in .aws

    @Value("${aws.region}")
    private String region;
    @Bean
    public S3Client s3ClientCust() {
        S3Client client = S3Client.builder()
                .region(Region.of(region))
//                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        return client;
    }

}
