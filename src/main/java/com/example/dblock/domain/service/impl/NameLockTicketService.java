package com.example.dblock.domain.service.impl;

import com.example.dblock.domain.entity.Ticket;
import com.example.dblock.domain.service.TicketService;
import com.example.dblock.infra.repository.NamedLockRepository;
import com.example.dblock.infra.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NameLockTicketService implements TicketService {
    private final TicketRepository ticketRepository;
    private final NamedLockRepository namedLockRepository;
    private final BasicTicketService basicTicketService;

//    @Transactional
    @Override
    public void sellTicket() {
        try {
            int getLockNum = getLock();

            if(getLockNum == 1){
                basicTicketService.sellTicket();
            }
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            releaseLock();
        }
    }

    public int getLock() {
        return namedLockRepository.getLock("lock" + 1L);
    }

    public int releaseLock() {
        return namedLockRepository.releaseLock("lock" + 1L);
    }
}
