package com.example.document;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PassportRepository extends CrudRepository<Passport, Long> {
}
