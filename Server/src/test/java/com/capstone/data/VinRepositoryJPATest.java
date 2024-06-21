package com.capstone.data;

import com.capstone.models.Vin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VinRepositoryJPATest {

    @Autowired
    private VinRepositoryJPA repository;

    @BeforeEach
    public void setUp() {
        repository.deleteAll();
    }

    @Test
    void getVinsByOwnerId() {
        Vin expected = new Vin(1, 1, "test", 123);
        repository.save(expected);
        List<Vin> actual = repository.getVinsByOwnerId(1);
        assertEquals(actual.get(0), expected);
        assertEquals(1, actual.size());
    }

    @Test
    void shouldNotFindNonExistentOwnerId() {
        List<Vin> actual = repository.getVinsByOwnerId(99);
        assertTrue(actual.isEmpty());
    }
}