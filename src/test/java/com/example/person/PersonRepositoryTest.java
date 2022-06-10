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
        person.setFirstName("John");
        person.setLastName("Smith");

        var personCreated = personRepository.save(person);

        assertEquals(1L, person.getId(), "Person instance gets flushed after save");
        assertEquals(1L, personCreated.getId());
        assertEquals(person.getFirstName(), personCreated.getFirstName());
        assertEquals(person.getLastName(), personCreated.getLastName());
        assertNull(person.getPassport(), "Person does not posses a passport yet");
    }

    @Test
    @Order(2)
    void fixingPersonName() {
        var person = personRepository.findById(1L)
                .orElseThrow();
        var lastNameBeforeRenaming = person.getLastName();
        person.setLastName("Rogers");

        var personRenamed = personRepository.save(person);

        assertEquals(person.getId(), personRenamed.getId());
        assertEquals(person.getFirstName(), personRenamed.getFirstName());
        assertEquals(person.getLastName(), personRenamed.getLastName());
        assertNotEquals(lastNameBeforeRenaming, person.getLastName());
        assertNotEquals(lastNameBeforeRenaming, personRenamed.getLastName());
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