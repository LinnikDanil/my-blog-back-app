package ru.practicum.blog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.practicum.blog.config.TestContainersConfig;

@Import(TestContainersConfig.class)
@SpringBootTest
class BlogApplicationTests {

	@Test
	void contextLoads() {
	}

}
