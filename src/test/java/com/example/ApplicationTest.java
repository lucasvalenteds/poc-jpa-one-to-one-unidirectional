package com.example;

import com.example.document.Document;
import com.example.document.DocumentRepository;
import com.example.person.Person;
import com.example.person.PersonRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApplicationTest {

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

    @Autowired
    private PersonRepository personRepository;

    @Test
    @Order(1)
    void assigningDocumentToPerson() {
        var document = new Document();
        document.setCode("US89234");
        document.setExpiresAt(Instant.now().plus(Duration.ofDays(10 * 365)));
        documentRepository.save(document);

        var person = new Person();
        person.setName("Mary Jane");
        person.setDocument(document);
        personRepository.save(person);

        assertEquals(1L, document.getId());
        assertEquals(1L, person.getId());
        assertEquals(document, person.getDocument());
        assertTrue(documentRepository.existsById(document.getId()));
        assertTrue(personRepository.existsById(person.getId()));
    }

    @Test
    @Order(2)
    void renewingDocumentAssignedToPerson() {
        var documentBeforeRenewal = documentRepository.findById(1L)
                .orElseThrow();
        var expirationBeforeRenewal = documentBeforeRenewal.getExpiresAt();
        documentBeforeRenewal.setExpiresAt(expirationBeforeRenewal.plus(Duration.ofDays(365 * 10)));

        var documentRenewed = documentRepository.save(documentBeforeRenewal);

        assertEquals(documentBeforeRenewal.getId(), documentRenewed.getId());
        assertEquals(documentBeforeRenewal.getCode(), documentRenewed.getCode());
        assertNotEquals(expirationBeforeRenewal, documentRenewed.getExpiresAt());
        var person = personRepository.findById(1L)
                .orElseThrow();
        assertEquals(person.getDocument(), documentRenewed);
    }

    @Test
    @Order(3)
    void documentAssignedToPersonCannotBeDeleted() {
        var document = documentRepository.findById(1L)
                .orElseThrow();

        var exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> documentRepository.delete(document)
        );

        assertTrue(documentRepository.existsById(document.getId()));
        var person = personRepository.findById(1L)
                .orElseThrow();
        assertEquals(document, person.getDocument());

        assertThat(exception)
                .getRootCause()
                .hasMessageContainingAll(
                        "ERROR: update or delete on table \"document\" violates foreign key constraint \"person_document_id_fkey\" on table \"person\"",
                        "Detail: Key (document_id)=(1) is still referenced from table \"person\"."
                );
    }

    @Test
    @Order(4)
    void revokingDocumentAndDeletingIt() {
        var personWithDocument = personRepository.findById(1L)
                .orElseThrow();
        var document = personWithDocument.getDocument();
        personWithDocument.setDocument(null);
        var personWithoutDocument = personRepository.save(personWithDocument);

        documentRepository.deleteById(document.getId());

        assertFalse(documentRepository.existsById(document.getId()));
        assertNull(personWithoutDocument.getDocument());
    }

    @Test
    @Order(5)
    void deletingPersonDoesNotDeleteDocument() {
        var document = new Document();
        document.setCode("XD892342");
        document.setExpiresAt(Instant.now().plus(Duration.ofDays(365 * 10)));
        var documentCreated = documentRepository.save(document);
        var person = personRepository.findById(1L)
                .orElseThrow();
        person.setDocument(documentCreated);

        var personCreated = personRepository.save(person);
        personRepository.delete(personCreated);

        assertFalse(personRepository.existsById(personCreated.getId()));
        assertTrue(documentRepository.existsById(person.getDocument().getId()));
    }

    @Test
    @Order(6)
    void twoPeopleCannotHaveTheSameDocument() {
        // Creating the document and assigning it to John
        final var document = new Document();
        document.setCode("XYZ123456");
        document.setExpiresAt(Instant.now().plus(Duration.ofDays(365 * 10)));
        documentRepository.save(document);

        final var person1 = new Person();
        person1.setName("John Smith");
        person1.setDocument(document);
        personRepository.save(person1);

        // Trying to assign the same document to Mary
        final var person2 = new Person();
        person2.setName("Mary Jane");
        person2.setDocument(document);

        // Asserting the second assignment does not work
        final var exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> personRepository.save(person2)
        );

        assertThat(exception)
                .getRootCause()
                .hasMessageContainingAll(
                        "ERROR: duplicate key value violates unique constraint \"person_document_id_key\"",
                        "Detail: Key (document_id)=(3) already exists."
                );
    }
}
