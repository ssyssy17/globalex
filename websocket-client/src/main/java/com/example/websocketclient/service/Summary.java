package com.example.websocketclient.service;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Slf4j
@ToString
public class Summary {
    private long startTimeMillis;
    private long count;
    private long gap;
    private long min;
    private long max;
    private long avg;
    private long sum;
    private long lose;

    public Summary() {
        this.init();
        Flux.interval(Duration.ofSeconds(1), Schedulers.newSingle("summary"))
                .map(d -> {
                    log.info(this.toString());
                    return d;
                })
                .subscribe();
    }

    public void init() {
        this.startTimeMillis = System.currentTimeMillis();
        this.count = 0;
        this.gap = 0;
        this.min = 9999;
        this.max = 0;
        this.avg = 0;
        this.sum = 0;
        this.lose = 0;
    }

    public void increment(long eventTime) {
        this.count++;
        this.gap = System.currentTimeMillis() - eventTime;
        this.min = Math.min(this.min, this.gap);
        this.max = Math.max(this.max, this.gap);
        this.sum += gap;
        this.avg = Math.round((float) this.sum / this.count);
    }

    public void incrementLose() {
        this.lose++;
    }
}
