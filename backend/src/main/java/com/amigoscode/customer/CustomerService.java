package com.amigoscode.customer;

import com.amigoscode.exception.DuplicateResourceException;
import com.amigoscode.exception.RequestValidationException;
import com.amigoscode.exception.ResourceNotFoundException;
import com.amigoscode.s3.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final static Logger logger = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerDao customerDao;
    private final CustomerDTOMapper customerDTOMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${aws.s3.buckets.bucketName}")
    private String bucketName;

    @Autowired
    private S3Service s3Service;

    public CustomerService(@Qualifier("jdbc") CustomerDao customerDao,    // specify jdbc as implementation and instantiate
                           CustomerDTOMapper customerDTOMapper,
                           PasswordEncoder passwordEncoder) {
        this.customerDao = customerDao;
        this.customerDTOMapper = customerDTOMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<CustomerDTO> getAllCustomers() {
        return customerDao.selectAllCustomers()
                .stream()
                .map(customerDTOMapper)
                .collect(Collectors.toList());
    }

    public CustomerDTO getCustomer(Integer id) {
        return customerDao.selectCustomerById(id)
                .map(customerDTOMapper)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "customer with id [%s] not found".formatted(id)
                ));
    }

    public void addCustomer(CustomerRegistrationRequest customerRegistrationRequest) {
        // check if email exists
        String email = customerRegistrationRequest.email();
        if (customerDao.existsCustomerWithEmail(email)) {
            throw new DuplicateResourceException(
                    "email already taken"
            );
        }

        // add
        Customer customer = new Customer(
                customerRegistrationRequest.name(),
                customerRegistrationRequest.email(),
                passwordEncoder.encode(customerRegistrationRequest.password()),
                customerRegistrationRequest.age(),
                customerRegistrationRequest.gender());

        customerDao.insertCustomer(customer);
    }

    private void checkIfCustomerExist(Integer customerId) {
        if (!customerDao.existsCustomerById(customerId)) {
            throw new ResourceNotFoundException(
                    "customer with id [%s] not found".formatted(customerId)
            );
        }
    }
    public void deleteCustomerById(Integer customerId) {
        checkIfCustomerExist(customerId);

        customerDao.deleteCustomerById(customerId);
    }

    public void updateCustomer(Integer customerId,
                               CustomerUpdateRequest updateRequest) {
        // TODO: for JPA use .getReferenceById(customerId) as it does does not bring object into memory and instead a reference
        Customer customer = customerDao.selectCustomerById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "customer with id [%s] not found".formatted(customerId)
                ));

        boolean changes = false;

        if (updateRequest.name() != null && !updateRequest.name().equals(customer.getName())) {
            customer.setName(updateRequest.name());
            changes = true;
        }

        if (updateRequest.age() != null && !updateRequest.age().equals(customer.getAge())) {
            customer.setAge(updateRequest.age());
            changes = true;
        }

        if (updateRequest.email() != null && !updateRequest.email().equals(customer.getEmail())) {
            if (customerDao.existsCustomerWithEmail(updateRequest.email())) {
                throw new DuplicateResourceException(
                        "email already taken"
                );
            }
            customer.setEmail(updateRequest.email());
            changes = true;
        }

        if (!changes) {
           throw new RequestValidationException("no data changes found");
        }

        customerDao.updateCustomer(customer);
    }

    public void uploadCustomerProfileImage(Integer customerId, MultipartFile file) throws IOException {
        checkIfCustomerExist(customerId);
        String profileImageId = UUID.randomUUID().toString();
        try {
            s3Service.putObject(bucketName,
                    "profile-images/%s/%s".formatted(customerId, profileImageId),
                    file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        uploadImage(customerId, file.getBytes());
//        // TODO: store fileId in database
        customerDao.updateCustomerProfileImageId(profileImageId, customerId);
    }

    public void uploadImage(Integer customerId, byte[] bytes) {
        logger.info("Start upload customer profile image for: " + customerId);

        checkIfCustomerExist(customerId);
        String profileImageId = UUID.randomUUID().toString();
        s3Service.putObject(bucketName,
                "profile-images/%s/%s".formatted(customerId, profileImageId),
                bytes);
        // TODO: store fileId in database
        customerDao.updateCustomerProfileImageId(profileImageId, customerId);
    }

    public byte[] getCustomerProfileImage(Integer customerId) {
        checkIfCustomerExist(customerId);

        var customer = customerDao.selectCustomerById(customerId)
                .map(customerDTOMapper)
                .orElseThrow(() -> new ResourceNotFoundException(
                   "customer with id [%s] not found".formatted(customerId)
                ));

        // TODO: Check if fileid is empty of null
        if (customer.profileImageId().isBlank()) {
            throw new ResourceNotFoundException(
                    "customer with id [%s] profile image not found".formatted(customerId));
        }
        String profileImageId = customer.profileImageId();
        byte[] image = s3Service.getObject(
                bucketName,
                "profile-images/%s/%s".formatted(customerId, profileImageId)
        );
        return image;
    }
}

