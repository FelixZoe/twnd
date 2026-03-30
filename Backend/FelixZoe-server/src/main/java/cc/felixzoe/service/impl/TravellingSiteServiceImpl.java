package cc.felixzoe.service.impl;

import cc.felixzoe.dto.TravellingSiteDTO;
import cc.felixzoe.entity.TravellingSites;
import cc.felixzoe.mapper.TravellingSiteMapper;
import cc.felixzoe.service.TravellingSiteService;
import cc.felixzoe.vo.TravellingSiteVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TravellingSiteServiceImpl implements TravellingSiteService {

    @Autowired
    private TravellingSiteMapper travellingSiteMapper;

    private static final Random random = new Random();

    /**
     * 管理端获取所有站点
     */
    @Cacheable(value = "travellingSites", key = "'all'")
    @Override
    public List<TravellingSites> getAll() {
        List<TravellingSites> list = travellingSiteMapper.getAll();
        if (list != null && !list.isEmpty()) {
            return list;
        }
        return Collections.emptyList();
    }

    /**
     * 管理端添加站点
     */
    @CacheEvict(value = "travellingSites", allEntries = true)
    @Override
    public void addSite(TravellingSiteDTO dto) {
        TravellingSites site = new TravellingSites();
        BeanUtils.copyProperties(dto, site);
        // Auto-complete URL protocol
        if (site.getUrl() != null && !site.getUrl().isEmpty()
                && !site.getUrl().startsWith("http://") && !site.getUrl().startsWith("https://")) {
            site.setUrl("https://" + site.getUrl());
        }
        if (site.getIsActive() == null) {
            site.setIsActive(1);
        }
        if (site.getSort() == null) {
            site.setSort(0);
        }
        travellingSiteMapper.insert(site);
    }

    /**
     * 管理端修改站点
     */
    @CacheEvict(value = "travellingSites", allEntries = true)
    @Override
    public void updateSite(TravellingSiteDTO dto) {
        TravellingSites site = new TravellingSites();
        BeanUtils.copyProperties(dto, site);
        // Auto-complete URL protocol
        if (site.getUrl() != null && !site.getUrl().isEmpty()
                && !site.getUrl().startsWith("http://") && !site.getUrl().startsWith("https://")) {
            site.setUrl("https://" + site.getUrl());
        }
        travellingSiteMapper.update(site);
    }

    /**
     * 批量删除站点
     */
    @CacheEvict(value = "travellingSites", allEntries = true)
    @Override
    public void batchDelete(List<Long> ids) {
        travellingSiteMapper.batchDelete(ids);
    }

    /**
     * 博客端获取激活的站点列表
     */
    @Cacheable(value = "travellingSites", key = "'active'")
    @Override
    public List<TravellingSiteVO> getActiveSites() {
        List<TravellingSites> list = travellingSiteMapper.getActiveSites();
        if (list != null && !list.isEmpty()) {
            return list.stream().map(site -> TravellingSiteVO.builder()
                    .id(site.getId())
                    .name(site.getName())
                    .url(site.getUrl())
                    .description(site.getDescription())
                    .tag(site.getTag())
                    .build()).toList();
        }
        return Collections.emptyList();
    }

    /**
     * 博客端随机获取一个站点
     */
    @Override
    public TravellingSiteVO getRandomSite(String tag) {
        List<TravellingSiteVO> sites = getActiveSites();
        if (tag != null && !tag.isEmpty()) {
            sites = sites.stream().filter(s -> tag.equals(s.getTag())).collect(Collectors.toList());
        }
        if (sites.isEmpty()) {
            return null;
        }
        return sites.get(random.nextInt(sites.size()));
    }
}
