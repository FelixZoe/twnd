package cc.felixzoe.controller.blog;

import cc.felixzoe.result.Result;
import cc.felixzoe.service.TravellingSiteService;
import cc.felixzoe.vo.TravellingSiteVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 博客端开往站点接口
 */
@Slf4j
@RestController("blogTravellingSiteController")
@RequestMapping("/blog/travelling")
public class TravellingSiteController {

    @Autowired
    private TravellingSiteService travellingSiteService;

    /**
     * 随机获取一个站点（用于 /go/ 跳转）
     */
    @GetMapping("/random")
    public Result<TravellingSiteVO> random(@RequestParam(required = false) String tag) {
        TravellingSiteVO site = travellingSiteService.getRandomSite(tag);
        if (site == null) {
            return Result.error("暂无可用站点");
        }
        return Result.success(site);
    }

    /**
     * 获取全部激活站点列表
     */
    @GetMapping("/list")
    public Result<List<TravellingSiteVO>> list() {
        return Result.success(travellingSiteService.getActiveSites());
    }
}
