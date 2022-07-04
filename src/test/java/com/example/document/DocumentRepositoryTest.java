package com.example.document;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocumentRepositoryTest {

    @Container
    private static final PostgreSQLContainer<?> CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres"));

    @DynamicPropertySource
    private static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", CONTAINER::getUsername);
        registry.add("spring.datasource.password", CONTAINER::getPassword);
    }

    @Autowired
    private DocumentRepository documentRepository;

    @Test
    @Order(1)
    void creatingDocument() {
        var document = new Document();
        document.setCode("AB125634");
        document.setExpiresAt(Instant.now().plus(Duration.ofDays(365 * 10)));

        var documentCreated = documentRepository.save(document);

        assertEquals(1L, document.getId(), "Document instance gets flushed after save");
        assertEquals(1L, documentCreated.getId());
        assertEquals(document.getCode(), documentCreated.getCode());
        assertEquals(document.getExpiresAt(), documentCreated.getExpiresAt());
    }

    @Test
    @Order(2)
    void renewingDocument() {
        var document = documentRepository.findById(1L)
                .orElseThrow();
        var expirationBeforeRenewal = document.getExpiresAt();
        document.setExpiresAt(expirationBeforeRenewal.plus(Duration.ofDays(3 * 30)));

        var documentRenewed = documentRepository.save(document);

        assertEquals(document.getId(), documentRenewed.getId());
        assertEquals(document.getCode(), documentRenewed.getCode());
        assertNotEquals(documentRenewed.getExpiresAt(), expirationBeforeRenewal);
        assertEquals(document.getExpiresAt(), documentRenewed.getExpiresAt());
    }

    @Test
    @Order(3)
    void deletingDocument() {
        var document = documentRepository.findById(1L)
                .orElseThrow();

        documentRepository.delete(document);

        assertEquals(1L, document.getId());
        assertFalse(documentRepository.existsById(document.getId()));
    }
}