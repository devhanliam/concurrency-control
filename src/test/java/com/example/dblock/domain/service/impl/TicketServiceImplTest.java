package com.example.dblock.domain.service.impl;

import com.example.dblock.domain.entity.Ticket;
import com.example.dblock.domain.facade.NamedLockFacade;
import com.example.dblock.domain.service.TicketService;
import com.example.dblock.infra.repository.TicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TicketServiceImplTest {
    @Autowired
    BasicTicketService ticketService;
    @Autowired
    NameLockTicketService nameLockTicketService;
    @Autowired
    OpAndPesLockTicketService opAndPesLockTicketService;
    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    RedissonLockTicketService redissonLockTicketService;

    @Autowired
    NamedLockFacade namedLockFacade;
    @BeforeEach
    public void initTicket() {
        Ticket ticket = Ticket.getTestInstance(1L,100L);
        ticketRepository.saveAndFlush(ticket);
    }

    @AfterEach
    public void flushTicket() {
        ticketRepository.deleteAll();
    }

    @Test
    @DisplayName("일반 JPA 테스트")
    public void dbLockTest() throws InterruptedException {
        executeService(ticketService);
        compareResult();
    }

    @Test
    @DisplayName("네임드락 테스트")
    public void namedLockTest() throws InterruptedException {
        executeService(nameLockTicketService);
        compareResult();
    }
    @Test
    @DisplayName("레디슨 분산락 테스트")
    public void redissonLockTest() throws InterruptedException {
        executeService(redissonLockTicketService);
        compareResult();
    }


    @Test
    @DisplayName("낙관적/비관적 락 테스트")
    public void pOrOLockTest() throws InterruptedException {
//        opAndPesLockTicketService.setMode(OpAndPesLockTicketService.MODE_OPTIMISTIC);
        opAndPesLockTicketService.setMode(OpAndPesLockTicketService.MODE_PESSIMISTIC);
        executeService(opAndPesLockTicketService);
        compareResult();

    }

    private void executeService(TicketService ticketService) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(30);
        CountDownLatch countDownLatch = new CountDownLatch(100);
        for (int i = 0; i < 100; i++) {
            executorService.submit(()->{
                try {
                    ticketService.sellTicket();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
    }

    private void compareResult() {
        Ticket ticket = ticketRepository.findById(1L).orElseThrow();
        assertEquals(0L,ticket.getQuantity());
    }

}