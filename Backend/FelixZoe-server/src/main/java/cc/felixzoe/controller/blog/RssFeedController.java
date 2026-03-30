package cc.felixzoe.controller.blog;

import cc.felixzoe.properties.WebsiteProperties;
import cc.felixzoe.result.Result;
import cc.felixzoe.service.ArticleService;
import cc.felixzoe.service.PersonalInfoService;
import cc.felixzoe.service.RssFeedService;
import cc.felixzoe.vo.BlogArticleVO;
import cc.felixzoe.vo.PersonalInfoVO;
import cc.felixzoe.result.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 博客端 RSS Feed 接口
 */
@Slf4j
@RestController("blogRssFeedController")
@RequestMapping("/blog")
public class RssFeedController {

    @Autowired
    private RssFeedService rssFeedService;

    /**
     * 生成 RSS 2.0 Feed XML
     */
    @GetMapping(value = "/rss", produces = "application/xml; charset=UTF-8")
    @Cacheable(value = "rssFeed", key = "'xml'")
    public String rssFeed() {
        String xml = rssFeedService.generateRssFeed();
        return xml;
    }
}
