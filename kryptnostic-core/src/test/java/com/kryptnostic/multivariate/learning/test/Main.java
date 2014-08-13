package com.kryptnostic.multivariate.learning.test;

public class Main {
	
	public static void main(String[] args) {
		MultivariateLearningTests learningTests = new MultivariateLearningTests();
		
		learningTests.learnFunctionVarySize( true );
		learningTests.learnFunctionVarySize( false );
		
		learningTests.learnFunctionVaryOrder( true );
		learningTests.learnFunctionVaryOrder( false );
	}
}