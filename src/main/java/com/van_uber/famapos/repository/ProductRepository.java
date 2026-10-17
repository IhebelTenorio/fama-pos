package com.van_uber.famapos.repository;

import com.van_uber.famapos.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    @Query("SELECT DISTINCT p.categoria FROM Product p WHERE p.categoria IS NOT NULL AND TRIM(p.categoria) != '' ORDER BY p.categoria ASC")
    List<String> findAllDistinctCategories();

    @Query("SELECT DISTINCT p.laboratorio FROM Product p WHERE p.laboratorio IS NOT NULL AND TRIM(p.laboratorio) != '' ORDER BY p.laboratorio ASC")
    List<String> findAllDistinctLaboratories();
}
