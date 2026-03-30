package cc.felixzoe.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 开往站点
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravellingSites implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    // 站点名称
    private String name;

    // 站点地址
    private String url;

    // 站点描述
    private String description;

    // 标签（技术、生活、设计等）
    private String tag;

    // 是否激活 1-激活 0-未激活
    private Integer isActive;

    // 排序，越小越靠前
    private Integer sort;

    // 创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 更新时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
