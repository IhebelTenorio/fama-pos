package com.van_uber.famapos.repository;

import com.van_uber.famapos.domain.CompuestoGramaje;
import com.van_uber.famapos.domain.CompuestoGramajeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import java.util.List;

@Repository
public interface CompuestoGramajeRepository extends JpaRepository<CompuestoGramaje, CompuestoGramajeId> {
    
    @Query("SELECT DISTINCT c.id.compuestoGramaje FROM CompuestoGramaje c ORDER BY c.id.compuestoGramaje ASC")
    List<String> findAllDistinctCompuestos();
}
