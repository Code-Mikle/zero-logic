package com.mikle.zerologic.config;

import jakarta.validation.Validation;
import org.hibernate.validator.HibernateValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ValidationProviderTest {

    @Test
    void loadsHibernateValidatorImplementation() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertNotNull(factory.unwrap(HibernateValidatorFactory.class));
        }
    }
}
