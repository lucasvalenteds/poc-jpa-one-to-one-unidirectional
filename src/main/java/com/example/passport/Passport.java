package com.example.passport;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "PASSPORT")
@Data
@NoArgsConstructor
public class Passport {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PASSPORT_ID_SEQUENCE_GENERATOR")
    @SequenceGenerator(name = "PASSPORT_ID_SEQUENCE_GENERATOR", sequenceName = "PASSPORT_ID_SEQUENCE", allocationSize = 1)
    @Column(name = "PASSPORT_ID")
    private Long id;

    @Column(name = "CODE")
    private String code;

    @Column(name = "EXPIRES_AT")
    private Instant expiresAt;
}
