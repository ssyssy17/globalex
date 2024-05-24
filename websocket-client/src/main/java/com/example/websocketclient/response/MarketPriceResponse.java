package com.example.websocketclient.response;

import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@ToString
public class MarketPriceResponse {
    private String code;
    private String dataType;
    private Data data;

    @ToString
    public static class Data {
        private String e; //Event type
        @Getter
        private Long E; //Event time
        private String s; //Trading pair, e.g., BTC-USDT
        private BigDecimal p; //Latest mark price
    }

    public Long getEventTime() {
        return this.data.getE();
    }
}
