package com.harshil.ragqa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RagDocumentQaApplication {
    public static void main(String[] args) {
        SpringApplication.run(RagDocumentQaApplication.class, args);
    }
}
