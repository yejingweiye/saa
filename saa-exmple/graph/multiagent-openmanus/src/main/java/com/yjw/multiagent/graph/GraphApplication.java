
package com.yjw.multiagent.graph;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.yjw.multiagent.graph","com.alibaba.cloud.ai" })
public class GraphApplication {

	public static void main(String[] args) {

		SpringApplication.run(GraphApplication.class, args);
	}

}
