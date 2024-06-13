package com.amigoscode;

import com.amigoscode.customer.CustomerService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { Main.class })
public class S3Test {

    @Autowired
    CustomerService customerService;
    public static byte[] importFile() throws Exception {
        File file = new File("/Users/wendyhu/Projects/SpringBoot + React + S3/full-stack-professional/backend/src/main/resources/download1.jpeg");
        byte[] data = new byte[(int) file.length()];
        return data;
    }
    @Test
    public void testUploadFile() throws Exception {
        byte[] testBinaryData = importFile();
        customerService.uploadImage(2, testBinaryData);
    }
}
