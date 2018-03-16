package com.clover.annotation;

import com.baeldung.annotation.Person;
import com.baeldung.annotation.processor.BuilderProperty;
import com.clover.autotest.annotation.AutoTest;

public class A {

	@AutoTest
    public double add(int a, int b) {
    		return a + b;
    }
	
	public static void main(String[] args) {
		System.out.println(new A().add(1, 2));
	}

	@AutoTest
    public Person getPerson() {
		return new Person();
    }
}