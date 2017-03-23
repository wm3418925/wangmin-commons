package com.test.redis.aspect;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:spring/test-redis/aspect/test-redis-aspect.xml")
public class TestRedisAnnotaionCache {
    @Autowired
    private AnnotationDemo annotationDemo;


    @Test
    public void doTest() {
        annotationDemo.testValue("123", 0);
        annotationDemo.testValue("123", 1);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        annotationDemo.testValue("123", 2);

        annotationDemo.testValue("ASG", 3);
    }

}
