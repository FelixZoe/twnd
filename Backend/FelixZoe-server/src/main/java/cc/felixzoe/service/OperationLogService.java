package cc.felixzoe.service;

import cc.felixzoe.dto.OperationLogPageQueryDTO;
import cc.felixzoe.entity.OperationLogs;
import cc.felixzoe.result.PageResult;

import java.util.List;

public interface OperationLogService {
    /**
     * 保存操作日志
     * @param operationLogs
     */
    void save(OperationLogs operationLogs);

    /**
     * 分页查询操作日志
     * @param operationLogPageQueryDTO
     * @return
     */
    PageResult pageQuery(OperationLogPageQueryDTO operationLogPageQueryDTO);

    /**
     * 批量删除操作日志
     * @param ids
     */
    void batchDelete(List<Long> ids);
}
