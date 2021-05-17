package com.spring.servlet;

import com.spring.annotation.XAutowired;
import com.spring.annotation.XController;
import com.spring.annotation.XRequestMapping;
import com.spring.annotation.XService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class XDispatcherServlet extends HttpServlet {

    //与web.xml中的param-name一致
    private static final String LOCATION = "contextConfigLocation";

    //保存所有配置信息
    private Properties properties = new Properties();

    //保存所有扫描到的相关类的类名
    private List<String> classNames = new ArrayList<String>();

    //IOC容器，保存所有初始化的Bean
    private Map<String, Object> iocMaps = new HashMap<String, Object>();

    //保存所有Url和方法的映射关系
    private Map<String, Method> handlerMaps = new HashMap<String, Method>();

    public XDispatcherServlet() {
        super();
    }

    public void init(ServletConfig servletConfig) {
        //1. 加载配置文件
        doLoadConfig(servletConfig.getInitParameter(LOCATION));

        //2. 扫描所有相关类
        doScanner(properties.getProperty("scanPackage"));

        //3. 初始化所有相关类的实例，并保存到IOC容器
        doInstance();

        //4. 依赖注入
        doAutowired();

        //5. 构造HandlerMapping
        initHandlerMapping();

        System.out.println("x-spring-mvc is init.");
    }

    private void doLoadConfig(String contextConfigLocation) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (file.getName().endsWith(".class")) {
                    classNames.add((scanPackage + "." + file.getName()).replace(".class", ""));
                }
            }
        }
    }

    private void doInstance() {
        if (classNames.isEmpty()) return;
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(XController.class)) {
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    iocMaps.put(beanName, clazz.newInstance());
                } else if (clazz.isAnnotationPresent(XService.class)) {
                    XService xService = clazz.getAnnotation(XService.class);
                    // 如果注解包含自定义名称
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    if (!"".equals(xService.value())) {
                        beanName = xService.value();
                    }
                    iocMaps.put(beanName, clazz.newInstance());

                    // 如果没有自定义民称，按照接口类型创建一个实例
                    for (Class<?> i : clazz.getInterfaces()) {
                        iocMaps.put(i.getName(), clazz.newInstance());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doAutowired() {
        if (iocMaps.isEmpty()) return;

        for (Map.Entry<String, Object> entry : iocMaps.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(XAutowired.class)) {
                    continue;
                }
                // 获取注解对应的类
                XAutowired xAutowired = field.getAnnotation(XAutowired.class);
                String beanName = xAutowired.value().trim();
                // 获取 XAutowired 注解的值
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }
                // 设置私有属性的访问权限
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), iocMaps.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void initHandlerMapping() {
        if (iocMaps.isEmpty()) return;

        for (Map.Entry<String, Object> entry : iocMaps.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(XController.class)) {
                continue;
            }

            String baseUrl = "";

            //获取Controller的url配置
            if (clazz.isAnnotationPresent(XRequestMapping.class)) {
                XRequestMapping xRequestMapping = clazz.getAnnotation(XRequestMapping.class);
                baseUrl = xRequestMapping.value();
            }

            //获取Method的url配置
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(XRequestMapping.class)) {
                    continue;
                }
                XRequestMapping xRequestMapping = method.getAnnotation(XRequestMapping.class);
                String url = ("/" + baseUrl + "/" + xRequestMapping.value()).replaceAll("/+", "/");
                handlerMaps.put(url, method);
            }
        }
    }

    private String toLowerFirstCase(String className) {
        char[] charArray = className.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception Detail:\n" + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws InvocationTargetException, IllegalAccessException, IOException {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        if (!this.handlerMaps.containsKey(url)) {
            resp.getWriter().write("404 NOT FOUND!!");
            return;
        }

        Map<String, String[]> params = req.getParameterMap();
        Method method = this.handlerMaps.get(url);

        //获取方法的参数列表
        Class<?>[] paramTypes = method.getParameterTypes();
        //获取请求的参数
        Map<String, String[]> paramMaps = req.getParameterMap();
        Object[] paramValues = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            //根据参数名称，做某些处理
            Class paramType = paramTypes[i];
            if (paramType == HttpServletRequest.class) {
                paramValues[i] = req;
                continue;
            } else if (paramType == HttpServletResponse.class) {
                paramValues[i] = resp;
                continue;
            } else if (paramType == String.class) {
                for (Map.Entry<String, String[]> param : paramMaps.entrySet()) {
                    String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll("\\s", ",");
                    paramValues[i] = value;
                }
            }
        }

        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        // 利用反射机制调用
        method.invoke(iocMaps.get(beanName), req, resp);
    }
}
