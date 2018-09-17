package org.jacoco.examples.maven.java;

public class HelloWorld2 {
	//NEW COMMENT
	public String getMessage(boolean bigger) {
		if (bigger) {
			return "Hello Universe!";
		} else {
			return "Hello World!";
		}
	}

}
