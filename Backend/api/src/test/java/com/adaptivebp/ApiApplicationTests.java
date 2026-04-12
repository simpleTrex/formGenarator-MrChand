package com.adaptivebp;

import com.adaptivebp.modules.formbuilder.repository.ModelTemplateRepository;
import com.adaptivebp.modules.process.repository.ProcessTemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
		"spring.data.mongodb.uri=mongodb://localhost:27017/adaptivebp"
})
class ApiApplicationTests {

	@MockBean
	private ModelTemplateRepository modelTemplateRepository;

	@MockBean
	private ProcessTemplateRepository processTemplateRepository;

	@Test
	void contextLoads() {
	}

}
