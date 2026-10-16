package com.van_uber.famapos.repository;

import com.van_uber.famapos.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    List<Ticket> findByFechaBetween(LocalDate start, LocalDate end);
}
