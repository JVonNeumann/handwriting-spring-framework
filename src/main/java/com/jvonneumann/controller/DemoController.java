package com.jvonneumann.controller;

import com.jvonneumann.service.DemoService;
import com.spring.annotation.XAutowired;
import com.spring.annotation.XController;
import com.spring.annotation.XRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@XController
public class DemoController {

    @XAutowired
    private DemoService demoService;

    public void query(HttpServletRequest req, HttpServletResponse resp, @XRequestParam("name") String name) {
        String result = demoService.sayHello(name);
        try {
            resp.getWriter().println(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
