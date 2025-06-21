package com.pluralsight.coffeeservice.web;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.pluralsight.coffeeservice.model.Coffee;
import com.pluralsight.coffeeservice.service.CoffeeService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CoffeeController.class)
public class CoffeeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CoffeeService coffeeService;

    @Test
    public void testGetCoffeeById() throws Exception {
        // Create a Coffee object
        Coffee coffee = new Coffee("My Coffee");
        coffee.setId(1L);
        coffee.setVersion(1);

        // Setup our mock service to return the Coffee object
        when(coffeeService.findById(1L)).thenReturn(Optional.of(coffee));

        // Invoke GET /coffee/1
        mockMvc.perform(get("/coffee/{id}", 1))

                // Validate that we get a 200 OK HTTP Response
                .andExpect(status().isOk())

                // Validate the headers
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.LOCATION, "/coffee/1"))
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""))

                // Validate the contents of the response
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("My Coffee"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    public void testGetCoffeeByIdNotFound() throws Exception {
        // Setup our mock service to return an Optional empty
        when(coffeeService.findById(1L)).thenReturn(Optional.empty());

        // Invoke GET /coffee/1
        mockMvc.perform(get("/coffee/1"))
                // Validate that we get a 404 Not Found HTTP Response
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetCoffees() throws Exception {
        // Create a list of 3 coffees
        List<Coffee> coffeeList = new ArrayList<>();
        coffeeList.add(new Coffee("Coffee 1"));
        coffeeList.add(new Coffee("Coffee 2"));
        coffeeList.add(new Coffee("Coffee 3"));

        // Setup our mock service to return the list
        when(coffeeService.findAll()).thenReturn(coffeeList);

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
        Coffee coffee = new Coffee("Coffee 1");
        coffee.setId(1L);
        coffee.setVersion(1);
        when(coffeeService.create(any())).thenReturn(coffee);

        mockMvc.perform(post("/coffee")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":  \"Coffee 1\"}"))

                // Validate that we get a 201 Created HTTP Response
                .andExpect(status().isCreated())

                // Validate the headers
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.LOCATION, "/coffee/1"))
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""))

                // Validate the contents of the response
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Coffee 1"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    public void testSuccessfulUpdate() throws Exception {
        // Create a mock Coffee when the CoffeeService's findById(1L) is called
        Coffee mockCoffee = new Coffee("Coffee 1");
        mockCoffee.setId(1L);
        mockCoffee.setVersion(5);
        when(coffeeService.findById(1L)).thenReturn(Optional.of(mockCoffee));

        // Create a mock Coffee that is returned when the CoffeeController saves the Coffee to the database
        Coffee savedCoffee = new Coffee("Updated Coffee 1");
        savedCoffee.setId(1L);
        savedCoffee.setVersion(6);
        when(coffeeService.save(any())).thenReturn(savedCoffee);

        // Execute a PUT /coffee/1 with a matching version: 5
        mockMvc.perform(put("/coffee/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.IF_MATCH, 5)
                .content("{\"id\": 1, \"name\":  \"Updated Coffee 1\"}"))

                // Validate that we get a 200 OK HTTP Response
                .andExpect(status().isOk())

                // Validate the headers
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.LOCATION, "/coffee/1"))
                .andExpect(header().string(HttpHeaders.ETAG, "\"6\""))

                // Validate the contents of the response
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Updated Coffee 1"))
                .andExpect(jsonPath("$.version").value(6));
    }

    @Test
    public void testUpdateConflict() throws Exception {
        // Create a mock coffee with a version set to 5
        Coffee mockCoffee = new Coffee("Coffee 1");
        mockCoffee.setId(1L);
        mockCoffee.setVersion(5);

        // Return the mock Coffee when the CoffeeService's findById(1L) is called
        when(coffeeService.findById(1L)).thenReturn(Optional.of(mockCoffee));

        // Execute a PUT /coffee/1 with a mismatched version number: 2
        mockMvc.perform(put("/coffee/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.IF_MATCH, 2)
                        .content("{\"id\": 1, \"name\":  \"Updated Coffee 1\"}"))

                // Validate that we get a 409 Conflict HTTP Response
                .andExpect(status().isConflict());
    }

    @Test
    public void testUpdateNotFound() throws Exception {
        // Return the mock Coffee when the CoffeeService's findById(1L) is called
        when(coffeeService.findById(1L)).thenReturn(Optional.empty());

        // Execute a PUT /coffee/1 with a mismatched version number: 2
        mockMvc.perform(put("/coffee/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.IF_MATCH, 2)
                        .content("{\"id\": 1, \"name\":  \"Updated Coffee 1\"}"))

                // Validate that we get a 404 Not Found HTTP Response Code
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteCoffeeSuccess() throws Exception {
        // Setup mocked product
        Coffee mockCoffee = new Coffee("Cold Coffee");

        // Setup the mocked service
        when(coffeeService.findById(1L)).thenReturn(Optional.of(mockCoffee));
        doNothing().when(coffeeService).deleteById(1L);

        // Execute our DELETE request
        mockMvc.perform(delete("/coffee/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteCoffeeNotFound() throws Exception {
        // Setup the mocked service
        when(coffeeService.findById(1L)).thenReturn(Optional.empty());

        // Execute our DELETE request
        mockMvc.perform(delete("/coffee/{id}", 1L))
                .andExpect(status().isNotFound());
    }
}
