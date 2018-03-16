package com.baeldung.annotation;


import com.baeldung.annotation.processor.BuilderProperty;
import com.clover.autotest.annotation.AutoTest;

public class Person {

    private int age;

    private String name;

    @AutoTest
    public int getAge() {
        return age;
    }

    @BuilderProperty
    public void setAge(int age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    @BuilderProperty
    public void setName(String name) {
        this.name = name;
    }

}
