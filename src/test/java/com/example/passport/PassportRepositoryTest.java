package com.example.passport;

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
class PassportRepositoryTest {

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

    @Test
    @Order(1)
    void creatingPassport() {
        var passport = new Passport();
        passport.setCode("AB125634");
        passport.setExpiresAt(Instant.now().plus(Duration.ofDays(365 * 10)));

        var passportCreated = passportRepository.save(passport);

        assertEquals(1L, passport.getId(), "Passport instance gets flushed after save");
        assertEquals(1L, passportCreated.getId());
        assertEquals(passport.getCode(), passportCreated.getCode());
        assertEquals(passport.getExpiresAt(), passportCreated.getExpiresAt());
    }

    @Test
    @Order(2)
    void renewingPassport() {
        var passport = passportRepository.findById(1L)
                .orElseThrow();
        var expirationBeforeRenewal = passport.getExpiresAt();
        passport.setExpiresAt(expirationBeforeRenewal.plus(Duration.ofDays(3 * 30)));

        var passportRenewed = passportRepository.save(passport);

        assertEquals(passport.getId(), passportRenewed.getId());
        assertEquals(passport.getCode(), passportRenewed.getCode());
        assertNotEquals(passportRenewed.getExpiresAt(), expirationBeforeRenewal);
        assertEquals(passport.getExpiresAt(), passportRenewed.getExpiresAt());
    }

    @Test
    @Order(3)
    void deletingPassport() {
        var passport = passportRepository.findById(1L)
                .orElseThrow();

        passportRepository.delete(passport);

        assertEquals(1L, passport.getId());
        assertFalse(passportRepository.existsById(passport.getId()));
    }
}