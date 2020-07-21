package com.izneus.bonfire.module.system.service.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Izneus
 * @date 2020/07/17
 */
@ApiModel("验证码")
@Data
public class CaptchaDTO {
    @ApiModelProperty("验证码id")
    private final String id;
    @ApiModelProperty("验证码图片，base64")
    private final String captcha;
}
