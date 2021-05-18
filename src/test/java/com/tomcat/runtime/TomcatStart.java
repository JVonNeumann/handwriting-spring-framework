package com.tomcat.runtime;

import com.spring.servlet.XDispatcherServlet;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import javax.servlet.ServletException;
import java.io.File;

public class TomcatStart {
    public static int TOMCAT_PORT = 8080;
    public static String TOMCAT_HOSTNAME = "127.0.0.1";
    public static String WEBAPP_PATH = "src/main";
    public static String WEBINF_CLASSES = "/WEB-INF/classes";
    public static String CLASS_PATH = "target/classes";
    public static String INTERNAL_PATH = "/";

    public static void main(String[] args) throws ServletException, LifecycleException {
        TomcatStart.run();
    }

    public static void run() throws ServletException, LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(TomcatStart.TOMCAT_PORT);
        tomcat.setHostname(TomcatStart.TOMCAT_HOSTNAME);
        tomcat.setBaseDir("."); // tomcat 信息保存在项目下

        Context context = tomcat.addWebapp("/hsf", new File("src/main/webapp").getAbsolutePath());
        WebResourceRoot resources = new StandardRoot(context);
        resources.addPreResources(
                new DirResourceSet(resources, "/WEB-INF/classes",
                        new File("target/classes").getAbsolutePath(), "/"));
        context.setResources(resources);

        tomcat.start();
        tomcat.getServer().await();
    }
}
