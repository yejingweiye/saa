

package com.yjw.vector.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan(basePackages = {"com.yjw.vector.management", "com.alibaba.cloud.ai"})
public class NVMApplication {

    public static void main(String[] args) {
        SpringApplication.run(NVMApplication.class, args);
    }

}
