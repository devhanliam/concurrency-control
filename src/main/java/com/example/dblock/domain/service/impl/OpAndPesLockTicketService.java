package com.example.dblock.domain.service.impl;

import com.example.dblock.domain.entity.Ticket;
import com.example.dblock.domain.service.TicketService;
import com.example.dblock.infra.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OpAndPesLockTicketService implements TicketService {
    private final TicketRepository ticketRepository;
    public static final String MODE_PESSIMISTIC = "p_lock";
    public static final String MODE_OPTIMISTIC = "o_lock";
    private String mode;

    @Transactional
    @Override
    public void sellTicket() throws InterruptedException {
        try {
            Ticket ticket = getTicket();
            sellTicketByMode(ticket);
            ticketRepository.save(ticket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setMode(final String mode) {
        this.mode = mode;
    }

    private void sellTicketByMode(final Ticket ticket) throws InterruptedException {
        if (mode.equals(MODE_PESSIMISTIC)) {
            ticket.sellOneTicket();
            return;
        }

        if (mode.equals(MODE_OPTIMISTIC)) { //TODO : 낙관적락 동시성 제어 왜 안되는지 확인하기
            while (true) {
                try {
                    ticket.sellOneTicket();
                    break;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Thread.sleep(1);
                }
            }
        }
    }
    private Ticket getTicket() {
        if (mode.equals(MODE_OPTIMISTIC)) {
            return ticketRepository.findByIdWithOptimisticLock(1L).orElseThrow();
        }
        if (mode.equals(MODE_PESSIMISTIC)) {
            return ticketRepository.findByIdWithPessimisticLock(1L).orElseThrow();
        }
        throw new RuntimeException("맞는 모드가 없습니다");
    }
}
