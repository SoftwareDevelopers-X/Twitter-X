package com.twitter.social.service.config;

import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default Feign uses Jackson-only encoding, which CANNOT serialize
 * MultipartFile as multipart/form-data. This config plugs in
 * feign-form's SpringFormEncoder, which detects multipart content and
 * encodes it properly, falling back to normal Spring encoding for
 * everything else.
 */
@Configuration
public class FeignMultipartSupportConfig {

    private final ObjectProvider<FeignHttpMessageConverters> messageConverters;

    public FeignMultipartSupportConfig(ObjectProvider<FeignHttpMessageConverters> messageConverters) {
        this.messageConverters = messageConverters;
    }

    @Bean
    public Encoder feignFormEncoder() {
        return new SpringFormEncoder(new SpringEncoder(messageConverters));
    }

    @Bean
    public SpringMvcContract feignContract() {
        return new SpringMvcContract();
    }
}
