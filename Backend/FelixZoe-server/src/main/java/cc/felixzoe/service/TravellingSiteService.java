package cc.felixzoe.service;

import cc.felixzoe.dto.TravellingSiteDTO;
import cc.felixzoe.entity.TravellingSites;
import cc.felixzoe.vo.TravellingSiteVO;

import java.util.List;

public interface TravellingSiteService {

    /**
     * 管理端获取所有站点
     */
    List<TravellingSites> getAll();

    /**
     * 管理端添加站点
     */
    void addSite(TravellingSiteDTO dto);

    /**
     * 管理端修改站点
     */
    void updateSite(TravellingSiteDTO dto);

    /**
     * 批量删除站点
     */
    void batchDelete(List<Long> ids);

    /**
     * 博客端获取激活的站点列表
     */
    List<TravellingSiteVO> getActiveSites();

    /**
     * 博客端随机获取一个站点
     */
    TravellingSiteVO getRandomSite(String tag);
}
