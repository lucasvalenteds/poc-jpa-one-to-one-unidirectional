package com.example;

import com.example.passport.Passport;
import com.example.passport.PassportRepository;
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
    private PassportRepository passportRepository;

    @Autowired
    private PersonRepository personRepository;

    @Test
    @Order(1)
    void assigningPassportToPerson() {
        var passport = new Passport();
        passport.setCode("US89234");
        passport.setExpiresAt(Instant.now().plus(Duration.ofDays(10 * 365)));
        passportRepository.save(passport);

        var person = new Person();
        person.setFirstName("Mary");
        person.setLastName("Jane");
        person.setPassport(passport);
        personRepository.save(person);

        assertEquals(1L, passport.getId());
        assertEquals(1L, person.getId());
        assertEquals(passport, person.getPassport());
        assertTrue(passportRepository.existsById(passport.getId()));
        assertTrue(personRepository.existsById(person.getId()));
    }

    @Test
    @Order(2)
    void renewingPassportAssignedToPerson() {
        var passportBeforeRenewal = passportRepository.findById(1L)
                .orElseThrow();
        var expirationBeforeRenewal = passportBeforeRenewal.getExpiresAt();
        passportBeforeRenewal.setExpiresAt(expirationBeforeRenewal.plus(Duration.ofDays(365 * 10)));

        var passportRenewed = passportRepository.save(passportBeforeRenewal);

        assertEquals(passportBeforeRenewal.getId(), passportRenewed.getId());
        assertEquals(passportBeforeRenewal.getCode(), passportRenewed.getCode());
        assertNotEquals(expirationBeforeRenewal, passportRenewed.getExpiresAt());
        var person = personRepository.findById(1L)
                .orElseThrow();
        assertEquals(person.getPassport(), passportRenewed);
    }

    @Test
    @Order(3)
    void passportAssignedToPersonCannotBeDeleted() {
        var passport = passportRepository.findById(1L)
                .orElseThrow();

        var exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> passportRepository.delete(passport)
        );

        assertTrue(passportRepository.existsById(passport.getId()));
        var person = personRepository.findById(1L)
                .orElseThrow();
        assertEquals(passport, person.getPassport());

        assertThat(exception)
                .getRootCause()
                .hasMessageContainingAll(
                        "ERROR: update or delete on table \"passport\" violates foreign key constraint \"person_passport_id_fkey\" on table \"person\"",
                        "Detail: Key (passport_id)=(1) is still referenced from table \"person\"."
                );
    }

    @Test
    @Order(4)
    void revokingPassportAndDeletingIt() {
        var personWithPassport = personRepository.findById(1L)
                .orElseThrow();
        var passport = personWithPassport.getPassport();
        personWithPassport.setPassport(null);
        var personWithoutPassword = personRepository.save(personWithPassport);

        passportRepository.deleteById(passport.getId());

        assertFalse(passportRepository.existsById(passport.getId()));
    }

    @Test
    @Order(5)
    void deletingPersonDoesNotDeletePassport() {
        var passport = new Passport();
        passport.setCode("XD892342");
        passport.setExpiresAt(Instant.now().plus(Duration.ofDays(365 * 10)));
        var passportCreated = passportRepository.save(passport);
        var person = personRepository.findById(1L)
                .orElseThrow();
        person.setPassport(passportCreated);

        var personCreated = personRepository.save(person);
        personRepository.delete(personCreated);

        assertFalse(personRepository.existsById(personCreated.getId()));
        assertTrue(passportRepository.existsById(person.getPassport().getId()));
    }
}
