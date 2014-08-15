package com.kryptnostic.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfiguration {
	@Bean
	public SomeInstrumentedClass sic() {
		return new SomeInstrumentedClass();
	}
 }
