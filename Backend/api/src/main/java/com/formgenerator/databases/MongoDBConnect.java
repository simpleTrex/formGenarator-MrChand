package com.formgenerator.databases;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@Configuration
public class MongoDBConnect {

	@Value("${spring.data.mongodb.uri}")
	private String _database_url;

	public @Bean MongoClient mongoClient() {
		return MongoClients.create(_database_url);
	}

	public @Bean MongoTemplate mongoTemplate() {
		return new MongoTemplate(mongoClient(), "app");
	}
}
