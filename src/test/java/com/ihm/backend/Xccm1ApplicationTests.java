package com.ihm.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnableAutoConfiguration(exclude = {
    KafkaAutoConfiguration.class,
    MailSenderAutoConfiguration.class,
    RedisAutoConfiguration.class  // Si vous n'ajoutez pas la dépendance Redis
})class Xccm1ApplicationTests {

	@Test
	void contextLoads() {
	}

}
