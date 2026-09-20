package com.web.motoridelab3api.repository;

import com.web.motoridelab3api.model.Technology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TechnologyRepository extends JpaRepository<Technology, Long> {
    List<Technology> findAllByOrderByNameAsc();
}
