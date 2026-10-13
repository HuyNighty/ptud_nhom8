package com.quanlynhakhoa;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class QuanLyNhaKhoa {
    public static void main(String[] args) {
        new SpringApplicationBuilder(QuanLyNhaKhoa.class)
                .headless(false)
                .run(args);
    }
}
