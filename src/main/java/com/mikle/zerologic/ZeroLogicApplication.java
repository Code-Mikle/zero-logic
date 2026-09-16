package com.mikle.zerologic;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan({
        "com.mikle.zerologic.user.mapper",
        "com.mikle.zerologic.app.mapper",
        "com.mikle.zerologic.app.deployment.mapper",
        "com.mikle.zerologic.app.version.mapper",
        "com.mikle.zerologic.conversation.mapper",
        "com.mikle.zerologic.generation.build.mapper",
        "com.mikle.zerologic.generation.repair.mapper",
        "com.mikle.zerologic.generation.task.mapper",
        "com.mikle.zerologic.generation.tool.mapper",
        "com.mikle.zerologic.knowledge.attachment.mapper",
        "com.mikle.zerologic.knowledge.document.mapper",
        "com.mikle.zerologic.knowledge.embedding.mapper",
        "com.mikle.zerologic.knowledge.retrieval.mapper"
})
public class ZeroLogicApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZeroLogicApplication.class, args);
    }

}
