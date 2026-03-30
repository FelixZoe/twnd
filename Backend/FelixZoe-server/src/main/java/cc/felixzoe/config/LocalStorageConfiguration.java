package cc.felixzoe.config;

import cc.felixzoe.properties.LocalStorageProperties;
import cc.felixzoe.utils.LocalStorageUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocalStorageConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public LocalStorageUtil localStorageUtil(LocalStorageProperties props) {
        return new LocalStorageUtil(props.getPath(), props.getAccessUrl());
    }
}
