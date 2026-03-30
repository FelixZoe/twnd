package cc.felixzoe.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "felixzoe.local-storage")
@Data
public class LocalStorageProperties {
    private String path = "/app/uploads";
    private String accessUrl = "http://localhost/uploads";
}
