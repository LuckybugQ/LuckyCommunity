package com.uestc.community;


import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class LoggerTests {

	private static final Logger log = LoggerFactory.getLogger(LoggerTests.class);

	@Test
	public void loggerTest() {
		//test
		log.info("heihei");
	}


}

