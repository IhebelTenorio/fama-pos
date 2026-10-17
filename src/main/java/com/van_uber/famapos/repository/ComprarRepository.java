package com.van_uber.famapos.repository;

import com.van_uber.famapos.domain.Comprar;
import com.van_uber.famapos.domain.ComprarId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ComprarRepository extends JpaRepository<Comprar, ComprarId> {
    
    @Query("SELECT c FROM Comprar c JOIN FETCH c.product WHERE c.id.idTicket = :idTicket")
    List<Comprar> findById_IdTicket(@Param("idTicket") Integer idTicket);

    @Query("SELECT c FROM Comprar c JOIN FETCH c.product JOIN FETCH c.ticket WHERE c.ticket.fecha BETWEEN :start AND :end")
    List<Comprar> findByTicketFechaBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
