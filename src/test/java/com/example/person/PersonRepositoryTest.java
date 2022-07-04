package com.example.person;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PersonRepositoryTest {

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
    private PersonRepository personRepository;

    @Test
    @Order(1)
    void creatingPerson() {
        var person = new Person();
        person.setName("John Smith");

        var personCreated = personRepository.save(person);

        assertEquals(1L, person.getId(), "Person instance gets flushed after save");
        assertEquals(1L, personCreated.getId());
        assertEquals(person.getName(), personCreated.getName());
        assertNull(person.getDocument(), "Person does not posses a document yet");
    }

    @Test
    @Order(2)
    void fixingPersonName() {
        final var person = personRepository.findById(1L).orElseThrow();
        final var previousName = person.getName();
        person.setName("Mary Jane");

        final var personRenamed = personRepository.save(person);

        assertEquals(person.getId(), personRenamed.getId());
        assertEquals(person.getName(), personRenamed.getName());
        assertNotEquals(previousName, person.getName());
    }

    @Test
    void deletingPerson() {
        var person = personRepository.findById(1L)
                .orElseThrow();

        personRepository.delete(person);

        assertEquals(1L, person.getId());
        assertFalse(personRepository.existsById(person.getId()));
    }
}