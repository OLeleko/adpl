package com.smiddle.adpl.core.service.impl;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Month;

@Service
public class ShutdownServiceImpl implements ApplicationContextAware {
    LocalDateTime endDate = LocalDateTime.of(2023, Month.AUGUST, 9, 12, 10);
    private ApplicationContext context;

    public void shutdownContext(){
        ((ConfigurableApplicationContext)context).close();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Scheduled(initialDelay = 60 * 60 * 1000, fixedDelay = 60 * 60 * 1000)
    private void licensCheck(){
        LocalDateTime currentDate = LocalDateTime.now();
        if(currentDate.isAfter(endDate)){
            shutdownContext();
        }
    }
}
