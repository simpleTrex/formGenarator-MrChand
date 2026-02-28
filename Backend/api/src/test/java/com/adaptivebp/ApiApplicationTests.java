package com.adaptivebp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.data.mongodb.uri=mongodb://localhost:27017/adaptivebp"
})
class ApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
