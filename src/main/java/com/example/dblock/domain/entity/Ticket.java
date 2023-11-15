package com.example.dblock.domain.entity;

import lombok.Builder;
import lombok.Getter;

import javax.persistence.*;

@Entity
@Getter
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long quantity;

    @Version
    private Long version;

    private Ticket(Long id,Long quantity){
        this.id = id;
        this.quantity = quantity;
    }

    public Ticket() {

    }

    public void sellOneTicket() {
        if (quantity < 1) {
            throw new RuntimeException("남은 수량이 없습니다");
        }
        this.quantity --;
    }

    public static Ticket getTestInstance(Long id, Long quantity){
        return new Ticket(id,quantity);
    }
}
