package com.tairui.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@MapperScan("com.tairui.server.mapper")
@EnableScheduling
@SpringBootApplication
@CrossOrigin(origins ="*")
@RestController
public class App {

    public static void main(String[] args) {
        System.out.println("当前项目的运行目录 (user.dir) 是: " + System.getProperty("user.dir"));
        SpringApplication.run(App.class, args);
    }

}
