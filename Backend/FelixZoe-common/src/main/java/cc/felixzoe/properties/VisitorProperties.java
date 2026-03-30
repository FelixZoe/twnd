package cc.felixzoe.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "felixzoe.visitor")
@Data
public class VisitorProperties {
    private String verifyCode;
}
