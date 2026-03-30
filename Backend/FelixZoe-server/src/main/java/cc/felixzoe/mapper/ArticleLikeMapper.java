package cc.felixzoe.mapper;

import cc.felixzoe.annotation.AutoFill;
import cc.felixzoe.entity.ArticleLikes;
import cc.felixzoe.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArticleLikeMapper {

    void insert(ArticleLikes articleLikes);

    @Delete("delete from article_likes where article_id = #{articleId} and visitor_id = #{visitorId}")
    void deleteByArticleIdAndVisitorId(Long articleId, Long visitorId);

    @Select("select count(*) from article_likes where article_id = #{articleId} and visitor_id = #{visitorId}")
    int countByArticleIdAndVisitorId(Long articleId, Long visitorId);
}
