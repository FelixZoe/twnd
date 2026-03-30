package cc.felixzoe.mapper;

import cc.felixzoe.annotation.AutoFill;
import cc.felixzoe.entity.TravellingSites;
import cc.felixzoe.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TravellingSiteMapper {

    /**
     * 获取所有开往站点
     */
    @Select("select * from travelling_sites order by sort asc, id asc")
    List<TravellingSites> getAll();

    /**
     * 获取激活的开往站点
     */
    @Select("select * from travelling_sites where is_active = 1 order by sort asc, id asc")
    List<TravellingSites> getActiveSites();

    /**
     * 添加站点
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(TravellingSites site);

    /**
     * 修改站点
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(TravellingSites site);

    /**
     * 删除站点
     */
    @Delete("delete from travelling_sites where id = #{id}")
    void delete(Long id);

    /**
     * 批量删除站点
     */
    void batchDelete(List<Long> ids);
}
