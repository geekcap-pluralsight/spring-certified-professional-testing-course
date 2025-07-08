package com.pluralsight.coffeeservice.repository;

import java.util.List;

import com.pluralsight.coffeeservice.model.Coffee;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoffeeRepository extends JpaRepository<Coffee, Long> {
    List<Coffee> findByName(String name);
}
