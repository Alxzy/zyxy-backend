package com.zy.zyxy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * @author Administrator
 * @version 1.0
 * @date 2024-03-11 20:23
 * 自定义 Swagger 接口文档配置
 */
@Configuration // 配置类
@EnableSwagger2WebMvc // Swagger接口文档注解
@Profile({"dev","test"}) // 版本控制访问
public class SwaggerConfig {
    @Bean(value = "defaultApi2")
    public Docket defaultApi2() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                // 这里一定要标注你控制器的位置
                .apis(RequestHandlerSelectors.basePackage("com.zy.zyxy.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * api信息
     * @return
     */
    private ApiInfo apiInfo(){
        return new ApiInfoBuilder()
                .title("伙伴匹配系统")
                .description("伙伴匹配接口文档")
                .termsOfServiceUrl("https://github.com/Alxzy")
                .contact(new Contact("zzzyy","https://github.com/Alxzy","1571441255@qq.com"))
                .version("1.0")
                .build();
    }
}
