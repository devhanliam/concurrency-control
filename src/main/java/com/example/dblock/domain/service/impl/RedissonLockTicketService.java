package com.example.dblock.domain.service.impl;

import com.example.dblock.domain.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedissonLockTicketService implements TicketService {
    private final static String LOCK_FIX = ":lock";
    private final RedissonClient redissonClient;
    private final BasicTicketService basicTicketService;
    @Override
    public void sellTicket() throws InterruptedException {
        final RLock lock = redissonClient.getLock(1L + LOCK_FIX);
        try {
            boolean lockResult = lock.tryLock(5L, 3L, TimeUnit.SECONDS);
            if(!lockResult){
                return;
            }
            basicTicketService.sellTicket();
        }finally {
            lock.unlock();
        }


    }
}
