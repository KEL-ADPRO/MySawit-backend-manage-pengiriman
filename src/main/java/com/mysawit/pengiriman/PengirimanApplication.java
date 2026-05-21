package com.mysawit.pengiriman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class PengirimanApplication {

    public static void main(String[] args) {
        SpringApplication.run(PengirimanApplication.class, args);
    }
}
