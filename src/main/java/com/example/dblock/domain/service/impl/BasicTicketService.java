package com.example.dblock.domain.service.impl;

import com.example.dblock.domain.entity.Ticket;
import com.example.dblock.domain.service.TicketService;
import com.example.dblock.infra.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BasicTicketService implements TicketService {
    private final TicketRepository ticketRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void sellTicket() {

        Ticket ticket = ticketRepository.findById(1L).orElseThrow(()->new RuntimeException("No Data"));
        ticket.sellOneTicket();
        ticketRepository.saveAndFlush(ticket);
    }
}
