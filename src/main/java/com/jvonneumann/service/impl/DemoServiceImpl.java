package com.jvonneumann.service.impl;

import com.jvonneumann.service.DemoService;
import com.spring.annotation.XService;

@XService
public class DemoServiceImpl implements DemoService {
    @Override
    public String sayHello(String name) {
        return "Hello, " + name;
    }
}
