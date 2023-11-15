package com.example.dblock.infra.repository;

import com.example.dblock.domain.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NamedLockRepository extends JpaRepository<Ticket,Long> {
    @Query(value = "select get_lock(:key,5)",nativeQuery = true)
    int getLock(String key);
    @Query(value = "select release_lock(:key)",nativeQuery = true)
    int releaseLock(String key);
}
