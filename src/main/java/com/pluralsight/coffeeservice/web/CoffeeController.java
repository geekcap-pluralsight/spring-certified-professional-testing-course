package com.pluralsight.coffeeservice.web;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import com.pluralsight.coffeeservice.model.Coffee;
import com.pluralsight.coffeeservice.service.CoffeeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CoffeeController {
    @Autowired
    private CoffeeService coffeeService;

    @GetMapping("/coffee/{id}")
    public ResponseEntity<?> getCoffee(@PathVariable Long id) {
        return coffeeService.findById(id)
                .map(coffee -> {
                    try {
                        return ResponseEntity
                                .ok()
                                .location(new URI("/coffee/" + id))
                                .eTag(Integer.toString(coffee.getVersion()))
                                .body(coffee);
                    } catch (URISyntaxException e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/coffees")
    public List<Coffee> getCoffees() {
        return coffeeService.findAll();
    }

    @PostMapping("/coffee")
    public ResponseEntity<Coffee> createCoffee(@RequestBody Coffee coffee) {
        Coffee newCoffee = coffeeService.create(coffee);
        try {
            return ResponseEntity
                    .created(new URI("/coffee/" + newCoffee.getId()))
                    .eTag(Integer.toString(newCoffee.getVersion()))
                    .body(newCoffee);
        } catch (URISyntaxException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/coffee/{id}")
    public ResponseEntity<?> updateCoffee(@RequestBody Coffee coffee,
                                          @PathVariable Long id,
                                          @RequestHeader("If-Match") Integer ifMatch) {
        Optional<Coffee> existingCoffee = coffeeService.findById(id);
        return existingCoffee.map(c -> {
            if (c.getVersion() != ifMatch) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            // Update the coffee
            c.setName(coffee.getName());
            c.setVersion(c.getVersion() + 1);

            Coffee updatedCoffee = coffeeService.save(c);
            try {
                return ResponseEntity.ok()
                        .location(new URI("/coffee/" + updatedCoffee.getId()))
                        .eTag(Integer.toString(updatedCoffee.getVersion()))
                        .body(c);
            } catch (URISyntaxException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/coffee/{id}")
    public ResponseEntity<?> deleteCoffee(@PathVariable Long id) {
        // Get the existing product
        Optional<Coffee> existingCoffee = coffeeService.findById(id);

        return existingCoffee.map(coffee -> {
            coffeeService.deleteById(coffee.getId());
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
