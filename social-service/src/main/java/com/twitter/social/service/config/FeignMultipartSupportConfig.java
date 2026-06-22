package com.twitter.social.service.config;

import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default Feign uses Jackson-only encoding, which CANNOT serialize
 * MultipartFile as multipart/form-data — it'll try to JSON-encode it and
 * media-service will reject the request (400/415). This config plugs in
 * feign-form's SpringFormEncoder, which detects multipart content and
 * encodes it properly, falling back to normal Spring encoding for
 * everything else (e.g. JSON bodies on the same client, if any).
 *
 * Requires in social-service's pom.xml:
 *
 *   <dependency>
 *       <groupId>io.github.openfeign.form</groupId>
 *       <artifactId>feign-form</artifactId>
 *       <version>3.8.0</version>
 *   </dependency>
 *   <dependency>
 *       <groupId>io.github.openfeign.form</groupId>
 *       <artifactId>feign-form-spring</artifactId>
 *       <version>3.8.0</version>
 *   </dependency>
 *
 * Also requires SpringMvcContract bean so @RequestPart / @RequestParam on
 * Feign interfaces resolve the same way they do on normal @RestController.
 */
@Configuration
public class FeignMultipartSupportConfig {

    @Bean
    public Encoder feignFormEncoder() {
        return new SpringFormEncoder();
    }

    @Bean
    public SpringMvcContract feignContract() {
        return new SpringMvcContract();
    }
}
