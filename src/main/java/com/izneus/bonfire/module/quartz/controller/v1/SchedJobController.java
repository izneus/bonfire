package com.izneus.bonfire.module.quartz.controller.v1;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izneus.bonfire.common.annotation.AccessLog;
import com.izneus.bonfire.common.base.BasePageVO;
import com.izneus.bonfire.module.quartz.controller.v1.query.JobQuery;
import com.izneus.bonfire.module.quartz.controller.v1.query.ListJobQuery;
import com.izneus.bonfire.module.quartz.controller.v1.query.ListLogQuery;
import com.izneus.bonfire.module.quartz.controller.v1.vo.ListJobVO;
import com.izneus.bonfire.module.quartz.controller.v1.vo.ListLogVO;
import com.izneus.bonfire.module.quartz.entity.SchedJobEntity;
import com.izneus.bonfire.module.quartz.entity.SchedJobLogEntity;
import com.izneus.bonfire.module.quartz.service.SchedJobLogService;
import com.izneus.bonfire.module.quartz.service.SchedJobService;
import com.izneus.bonfire.module.system.controller.v1.vo.IdVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 调度任务表 前端控制器
 * </p>
 *
 * @author Izneus
 * @since 2020-11-05
 */
@Api(tags = "系统:调度任务")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SchedJobController {

    private final SchedJobService jobService;
    private final SchedJobLogService jobLogService;

    @AccessLog("任务列表")
    @ApiOperation("任务列表")
    @GetMapping("/jobs")
    @PreAuthorize("hasAuthority('sched:jobs:list')")
    public BasePageVO<ListJobVO> listJobs(@Validated ListJobQuery query) {
        Page<SchedJobEntity> page = jobService.listJobs(query);
        List<ListJobVO> rows = page.getRecords().stream()
                .map(job -> BeanUtil.copyProperties(job, ListJobVO.class))
                .collect(Collectors.toList());
        return new BasePageVO<>(page, rows);
    }

    @AccessLog("新增任务")
    @ApiOperation("新增任务")
    @PostMapping("/jobs")
    @PreAuthorize("hasAuthority('sched:jobs:create')")
    @ResponseStatus(HttpStatus.CREATED)
    public IdVO createJob(@Validated @RequestBody JobQuery query) {
        String id = jobService.createJob(query);
        return new IdVO(id);
    }

    /// job没那么多详情,暂时注释了
    /*@AccessLog("任务详情")
    @ApiOperation("任务详情")
    @GetMapping("/jobs/{id}")
    @PreAuthorize("hasAuthority('sched:jobs:get')")
    public JobVO getJobById(@NotBlank @PathVariable String id) {
        SchedJobEntity job = jobService.getById(id);
        return null
    }*/

    @AccessLog("更新任务")
    @ApiOperation("更新任务")
    @PreAuthorize("hasAuthority('sys:jobs:update')")
    @PutMapping("/jobs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateJob(@NotBlank @PathVariable String id,
                          @Validated @RequestBody JobQuery query) {
        jobService.updateJob(id, query);
    }

    @AccessLog("批量暂停任务")
    @ApiOperation("批量暂停任务")
    @PostMapping("/jobs:batchPause")
    @PreAuthorize("hasAuthority('sched:jobs:pause')")
    public void pauseJobs(@RequestBody List<String> ids) {
        jobService.batchPauseJobs(ids);
    }

    @AccessLog("批量恢复任务")
    @ApiOperation("批量恢复任务")
    @PostMapping("/jobs:batchResume")
    @PreAuthorize("hasAuthority('sched:jobs:resume')")
    public void resumeJobs(@RequestBody List<String> ids) {
        jobService.batchResumeJobs(ids);
    }

    @AccessLog("批量删除任务")
    @ApiOperation("批量删除任务")
    @PostMapping("/jobs:batchDelete")
    @PreAuthorize("hasAuthority('sched:jobs:delete')")
    public void deleteJobs(@RequestBody List<String> ids) {
        jobService.batchDeleteJobs(ids);
    }

    @AccessLog("删除任务")
    @ApiOperation("删除任务")
    @DeleteMapping("/jobs/{id}")
    @PreAuthorize("hasAuthority('sched:jobs:delete')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJob(@NotBlank @PathVariable String id) {
        jobService.deleteJob(id);
    }

    @AccessLog("任务日志列表")
    @ApiOperation("任务日志列表")
    @GetMapping("/jobs/{jobId}/logs")
    @PreAuthorize("hasAuthority('sched:logs:list')")
    public BasePageVO<ListLogVO> listLogs(@NotBlank @PathVariable String jobId, @Validated ListLogQuery query) {
        Page<SchedJobLogEntity> page = jobLogService.listLogsByJobId(jobId, query);
        // 查询结果转换成vo
        List<ListLogVO> rows = page.getRecords().stream()
                .map(log -> BeanUtil.copyProperties(log, ListLogVO.class))
                .collect(Collectors.toList());
        return new BasePageVO<>(page, rows);
    }


}
