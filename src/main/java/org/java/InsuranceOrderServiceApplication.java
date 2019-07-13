package org.java;

import org.activiti.spring.boot.SecurityAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class InsuranceOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsuranceOrderServiceApplication.class, args);
    }

}
