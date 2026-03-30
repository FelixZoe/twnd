package cc.felixzoe.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 开往站点VO - 博客端展示
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TravellingSiteVO implements Serializable {

    private Long id;

    // 站点名称
    private String name;

    // 站点地址
    private String url;

    // 站点描述
    private String description;

    // 标签
    private String tag;
}
