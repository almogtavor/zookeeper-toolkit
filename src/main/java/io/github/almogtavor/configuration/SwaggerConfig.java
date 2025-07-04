package io.github.almogtavor.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ZKT - Zookeeper Toolkit")
                        .description(
                                """
                                        Zookeeper Toolkit.
                                        """));
    }

//    @Bean
//    public GroupedOpenApi customApi() {
//        return GroupedOpenApi.builder().group("api").pathsToMatch("/api/**").build();
//    }
//
//    @Bean
//    public GroupedOpenApi actuatorApi() {
//        return GroupedOpenApi.builder().group("actuator").pathsToMatch("/actuator/**").build();
//    }
}
