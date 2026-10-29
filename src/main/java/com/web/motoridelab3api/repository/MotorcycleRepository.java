package com.web.motoridelab3api.repository;

import com.web.motoridelab3api.model.Motorcycle;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MotorcycleRepository
        extends JpaRepository<Motorcycle, Long>, JpaSpecificationExecutor<Motorcycle> {
    @EntityGraph(attributePaths = "category")
    List<Motorcycle> findTop3ByOrderByHorsepowerDesc();

    @Query(
            """
            SELECT m FROM Motorcycle m
            LEFT JOIN m.category c
            WHERE (:categorySlug IS NULL OR c.slug = :categorySlug)
              AND (:search IS NULL
                   OR LOWER(m.brand) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(m.model) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    List<Motorcycle> search(
            @Param("categorySlug") String categorySlug, @Param("search") String search);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByTechnologiesId(Long technologyId);

    @EntityGraph(attributePaths = "category")
    Page<Motorcycle> findAll(Specification<Motorcycle> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "technologies"})
    Optional<Motorcycle> findById(Long id);

    boolean existsByCategoryIdAndId(Long categoryId, Long id);
}
