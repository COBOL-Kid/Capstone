package com.capstone.data;

import com.capstone.models.Vin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VinRepositoryTest {

    @Autowired
    private VinRepository repository;

    @Test
    void getVinsByOwnerId() {
        Vin expected = new Vin(1, 1, "test", 123, "image");
        repository.save(expected);
        List<Vin> actual = repository.getVinsByOwnerId(1).get();
        assertEquals(actual.get(0), expected);
    }

    @Test
    void shouldNotFindNonExistentOwnerId() {
        Vin test = new Vin(1, 1, "test", 123, "image");
        repository.save(test);
        List<Vin> actual = repository.getVinsByOwnerId(99).get();
        assertEquals(0, actual.size());
    }
}