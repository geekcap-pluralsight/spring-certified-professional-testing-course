package com.pluralsight.coffeeservice.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.pluralsight.coffeeservice.model.Coffee;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestEntityManager
@Transactional
@ActiveProfiles("test")
public class CoffeeIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestEntityManager entityManager;

    /**
     * Maintains a list of the generated IDs for each coffee we preload in the database
     */
    private final List<Long> coffeeIds = new ArrayList<>();

    /**
     * The test coffees that we persist to the database before every test
     */
    private final List<Coffee> testCoffees = Arrays.asList(
            new Coffee("Coffee 1", 1),
            new Coffee("Coffee 2", 1),
            new Coffee("Coffee 3", 1));

    @BeforeEach
    void setUp() {
        // Load three coffees into the database
        testCoffees.forEach(coffee -> {
            // Save the coffee to the database
            entityManager.persist(coffee);

            // Add the generated ID to our list of coffee IDs
            coffeeIds.add((Long)entityManager.getId(coffee));
        });

        // Flush the persisted entities to the database
        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        // Remove all coffees from the database
        coffeeIds.forEach(id -> {
            Coffee coffee = entityManager.find(Coffee.class, id);
            if (coffee != null) {
                entityManager.remove(coffee);
            }
        });

        // Clear the list of generated Ids
        coffeeIds.clear();
    }

    @Test
    public void testGetCoffeeById() throws Exception {
        // Invoke GET /coffee/{firstID}
        mockMvc.perform(get("/coffee/{id}", coffeeIds.getFirst()))

                // Validate that we get a 200 OK HTTP Response
                .andExpect(status().isOk())

                // Validate the headers
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.LOCATION, "/coffee/" + coffeeIds.getFirst()))
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""))

                // Validate the contents of the response
                .andExpect(jsonPath("$.id").value(coffeeIds.getFirst()))
                .andExpect(jsonPath("$.name").value("Coffee 1"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    public void testGetCoffeeByIdNotFound() throws Exception {
        // Invoke GET /coffee/{badCoffeId}
        mockMvc.perform(get("/coffee/" + coffeeIds.getFirst() + 3))
                // Validate that we get a 404 Not Found HTTP Response
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetCoffees() throws Exception {
        // Invoke the GET /coffees endpoint
        mockMvc.perform(get("/coffees"))
                // Validate that we get a 200 OK HTTP Response
                .andExpect(status().isOk())

                // Validate that the response has three elements
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Coffee 1"));
    }

    @Test
    public void testCreateCoffee() throws Exception {
        mockMvc.perform(post("/coffee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":  \"Coffee 4\"}"))

                // Validate that we get a 201 Created HTTP Response
                .andExpect(status().isCreated())

                // Validate the headers
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.LOCATION, "/coffee/" + (coffeeIds.getFirst() + 3)))
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""))

                // Validate the contents of the response
                .andExpect(jsonPath("$.id").value(coffeeIds.getFirst() + 3))
                .andExpect(jsonPath("$.name").value("Coffee 4"))
                .andExpect(jsonPath("$.version").value(1));

        // Add the new coffee's ID to our list of coffees so the tearDown() method will delete it
        coffeeIds.add(coffeeIds.getFirst() + 3);
    }

    @Test
    public void testSuccessfulUpdate() throws Exception {
        // Execute a PUT /coffee/{firstId} with a matching version: 1
        Long firstId = coffeeIds.getFirst();
        mockMvc.perform(put("/coffee/{id}", firstId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.IF_MATCH, 1)
                        .content(String.format("{\"id\": %d,  \"name\":  \"Updated Coffee 1\"}",
                                firstId)))

                // Validate that we get a 200 OK HTTP Response
                .andExpect(status().isOk())

                // Validate the headers
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.LOCATION, "/coffee/" + firstId))
                .andExpect(header().string(HttpHeaders.ETAG, "\"2\""))

                // Validate the contents of the response
                .andExpect(jsonPath("$.id").value(firstId))
                .andExpect(jsonPath("$.name").value("Updated Coffee 1"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    public void testUpdateConflict() throws Exception {
        // Execute a PUT /coffee/1 with a mismatched version number: 5
        Long firstId = coffeeIds.getFirst();
        mockMvc.perform(put("/coffee/{id}", firstId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.IF_MATCH, 5)
                        .content(String.format("{\"id\": %d,  \"name\":  \"Updated Coffee 1\"}",
                                firstId)))

                // Validate that we get a 409 Conflict HTTP Response
                .andExpect(status().isConflict());
    }

    @Test
    public void testUpdateNotFound() throws Exception {
        // Execute a PUT /coffee/{firstId + 4} with an invalid ID
        Long firstId = coffeeIds.getFirst();
        mockMvc.perform(put("/coffee/{id}", firstId + 3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.IF_MATCH, 2)
                        .content(String.format("{\"id\": %d,  \"name\":  \"Updated Coffee 1\"}",
                                firstId)))

                // Validate that we get a 404 Not Found HTTP Response Code
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteCoffeeSuccess() throws Exception {
        // Execute our DELETE request
        Long firstId = coffeeIds.getFirst();
        mockMvc.perform(delete("/coffee/{id}", firstId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/coffee/{id}", firstId))
                // Validate that we get a 404 Not Found HTTP Response
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteCoffeeNotFound() throws Exception {
        // Execute our DELETE request
        mockMvc.perform(delete("/coffee/{id}", coffeeIds.getFirst() + 3))
                .andExpect(status().isNotFound());
    }
}
