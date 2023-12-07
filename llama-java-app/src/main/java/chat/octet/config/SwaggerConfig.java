package chat.octet.config;


import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Value("${swagger.enabled}")
    Boolean swaggerEnabled;

    @Bean
    public Docket docket() {
        Contact contact = new Contact("william", "https://github.com/eoctet/llama-cpp-java", StringUtils.EMPTY);

        return new Docket(DocumentationType.OAS_30)
                .apiInfo(new ApiInfoBuilder().title("Java API fro llama.cpp").contact(contact).build())
                .enable(swaggerEnabled)
                .select()
                .apis(RequestHandlerSelectors.basePackage("chat.octet.api"))
                .paths(PathSelectors.any())
                .build();
    }

}
