package org.finos.symphony.toolkit.koreai.response;

import java.io.IOException;

import org.finos.springbot.symphony.data.SymphonyDataHandlerCofig;
import org.finos.springbot.workflow.data.DataHandlerConfig;
import org.finos.springbot.workflow.data.EntityJsonConverter;
import org.finos.symphony.toolkit.koreai.spring.KoreAIConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.base.Charsets;

@SpringBootTest(classes = {
		DataHandlerConfig.class,
		SymphonyDataHandlerCofig.class,
})
public class KoreAIResponseBuilderImplTest {

	private KoreAIResponseBuilderImpl processor = 
		new KoreAIResponseBuilderImpl(new ObjectMapper(), JsonNodeFactory.instance);
	
	@Autowired
	EntityJsonConverter ejc;
	
	@Test
	public void test1() throws IOException {
		cannedTest("1.json");
	}
	
	@Test
	public void test2() throws IOException {
		cannedTest("2.json");
	}
	
	@Test
	public void test3() throws IOException {
		cannedTest("3.json");
	}
	
	@Test
	public void test4() throws IOException {
		cannedTest("4.json");
	}
	
	@Test
	public void test5() throws IOException {
		cannedTest("5.json");
	}
	
	@Test
	public void test6() throws IOException {
		cannedTest("6.json");
	}
	
	@Test
	public void test7() throws IOException {
		cannedTest("7.json");
	}
	
	@Test
	public void test8() throws IOException {
		cannedTest("8.json");
	}
	
	@Test
	public void test9() throws IOException {
		cannedTest("9.json");
	}

	public void cannedTest(String input) throws IOException, JsonProcessingException, JsonMappingException {
		KoreAIConfig.addVersionSpaces(ejc);
		
		// output
		String json = load(input);
		KoreAIResponse response = processor.formatResponse(json);
		String out = ejc.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(response);
		System.out.println(out);
		
		// expected
		String expectedStr = load("expected-"+input);
		JsonNode expectedTree = ejc.getObjectMapper().readTree(expectedStr);
		String expected = ejc.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(expectedTree);
		System.out.println(expected);
				
		Assertions.assertEquals(expected, out);
	}

	public String load(String name) throws IOException {
		return StreamUtils.copyToString(KoreAIResponseBuilderImpl.class.getResourceAsStream(name), Charsets.UTF_8);
	}
}
