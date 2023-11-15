package com.example.dblock.infra.repository;

import com.example.dblock.domain.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket,Long> {
    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "select t from Ticket t where t.id = :id ")
    Optional<Ticket> findByIdWithPessimisticLock(Long id);
    @Lock(value = LockModeType.NONE)
    @Query(value = "select t from Ticket t where t.id = :id ")
    Optional<Ticket> findByIdWithOptimisticLock(Long id);
}
