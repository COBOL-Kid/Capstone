package com.capstone.data;

import com.capstone.models.Owner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OwnerRepositoryTest {

    @Autowired
    private OwnerRepository repository;

    @BeforeEach
    public void setUp() {
        repository.deleteAll();
        repository.save(new Owner("test", "test", "test", "test", "test"));
    }

    @Test
    public void shouldNotFindNonExistentUsername() {
        Optional<Owner> result = repository.getOwnerByUserName("not_existent_username");
        assertFalse(result.isPresent());
    }

    @Test
    public void shouldFindUserByUsername() {
        Owner expected = new Owner("test", "test", "test", "test", "test");
        Owner actual = repository.getOwnerByUserName("test").get();
        assertEquals(expected, actual);
    }

}