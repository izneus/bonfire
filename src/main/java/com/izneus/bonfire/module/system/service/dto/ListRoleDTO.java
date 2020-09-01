package com.izneus.bonfire.module.system.service.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Izneus
 * @date 2020/08/14
 */
@ApiModel("角色列表dto")
@Data
public class ListRoleDTO {

    @ApiModelProperty("角色名称")
    private String roleName;

    @ApiModelProperty("创建时间")
    private String createTime;

    @ApiModelProperty("备注")
    private String remark;
}
