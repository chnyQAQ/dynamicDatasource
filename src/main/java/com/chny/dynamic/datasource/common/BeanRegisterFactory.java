package com.chny.dynamic.datasource.common;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;

public class BeanRegisterFactory {

    public static <T> T registerBean(ConfigurableApplicationContext applicationContext, String beanName, Class<T> clazz, Object... args) {
        if (applicationContext.containsBean(beanName)) {
            Object bean = applicationContext.getBean(beanName);
            if (bean.getClass().isAssignableFrom(clazz)) {
                return (T) bean;
            } else {
                throw new RuntimeException("Error create bean with name '" + beanName +"'.");
            }
        }
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
        for (Object arg : args) {
            beanDefinitionBuilder.addConstructorArgValue(arg);
        }
        BeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
        BeanDefinitionRegistry beanFactory = (BeanDefinitionRegistry) applicationContext.getBeanFactory();
        beanFactory.registerBeanDefinition(beanName, beanDefinition);
        return applicationContext.getBean(beanName, clazz);
    }

}
