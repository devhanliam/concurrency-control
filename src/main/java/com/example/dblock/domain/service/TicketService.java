package com.example.dblock.domain.service;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface TicketService {
    void sellTicket() throws InterruptedException;
}
