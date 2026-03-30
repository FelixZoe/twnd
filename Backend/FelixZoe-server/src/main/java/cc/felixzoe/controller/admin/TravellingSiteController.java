package cc.felixzoe.controller.admin;

import cc.felixzoe.annotation.OperationLog;
import cc.felixzoe.dto.TravellingSiteDTO;
import cc.felixzoe.entity.TravellingSites;
import cc.felixzoe.enumeration.OperationType;
import cc.felixzoe.result.Result;
import cc.felixzoe.service.TravellingSiteService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端开往站点接口
 */
@RestController("adminTravellingSiteController")
@RequestMapping("/admin/travelling")
@Slf4j
public class TravellingSiteController {

    @Autowired
    private TravellingSiteService travellingSiteService;

    /**
     * 获取所有站点
     */
    @GetMapping
    public Result<List<TravellingSites>> getAll() {
        List<TravellingSites> list = travellingSiteService.getAll();
        return Result.success(list);
    }

    /**
     * 添加站点
     */
    @PostMapping
    @OperationLog(value = OperationType.INSERT, target = "travellingSite")
    public Result addSite(@Valid @RequestBody TravellingSiteDTO dto) {
        log.info("添加开往站点:{}", dto);
        travellingSiteService.addSite(dto);
        return Result.success();
    }

    /**
     * 修改站点
     */
    @PutMapping
    @OperationLog(value = OperationType.UPDATE, target = "travellingSite", targetId = "#dto.id")
    public Result updateSite(@Valid @RequestBody TravellingSiteDTO dto) {
        log.info("修改开往站点:{}", dto);
        travellingSiteService.updateSite(dto);
        return Result.success();
    }

    /**
     * 批量删除站点
     */
    @DeleteMapping
    @OperationLog(value = OperationType.DELETE, target = "travellingSite", targetId = "#ids")
    public Result deleteSites(@RequestParam List<Long> ids) {
        log.info("批量删除开往站点:{}", ids);
        travellingSiteService.batchDelete(ids);
        return Result.success();
    }
}
