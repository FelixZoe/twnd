package cc.felixzoe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 开往站点DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravellingSiteDTO implements Serializable {

    private Long id;

    // 站点名称
    @NotBlank(message = "站点名称不能为空")
    @Size(max = 100, message = "站点名称不能超过100字")
    private String name;

    // 站点地址
    @NotBlank(message = "站点地址不能为空")
    @Size(max = 255, message = "站点地址不能超过255字")
    private String url;

    // 站点描述
    @Size(max = 255, message = "站点描述不能超过255字")
    private String description;

    // 标签
    @Size(max = 50, message = "标签不能超过50字")
    private String tag;

    // 是否激活
    private Integer isActive;

    // 排序
    private Integer sort;
}
