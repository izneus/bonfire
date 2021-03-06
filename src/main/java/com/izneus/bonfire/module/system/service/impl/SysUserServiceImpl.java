package com.izneus.bonfire.module.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.izneus.bonfire.common.util.RedisUtil;
import com.izneus.bonfire.config.BonfireConfig;
import com.izneus.bonfire.module.system.controller.v1.query.ListUserQuery;
import com.izneus.bonfire.module.system.controller.v1.query.UserQuery;
import com.izneus.bonfire.module.system.controller.v1.vo.UserVO;
import com.izneus.bonfire.module.system.entity.SysFileEntity;
import com.izneus.bonfire.module.system.entity.SysUserEntity;
import com.izneus.bonfire.module.system.entity.SysUserRoleEntity;
import com.izneus.bonfire.module.system.mapper.SysUserMapper;
import com.izneus.bonfire.module.system.service.SysFileService;
import com.izneus.bonfire.module.system.service.SysUserRoleService;
import com.izneus.bonfire.module.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.izneus.bonfire.common.constant.Constant.REDIS_KEY_AUTHS;
import static com.izneus.bonfire.common.constant.Constant.REDIS_KEY_LOGIN_RETRY;

/**
 * <p>
 * 系统_用户 服务实现类
 * </p>
 *
 * @author Izneus
 * @since 2020-06-28
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUserEntity> implements SysUserService {

    private final BonfireConfig bonfireConfig;
    private final SysUserRoleService userRoleService;
    private final SysFileService fileService;
    private final RedisUtil redisUtil;

    @Override
    public Page<SysUserEntity> listUsers(ListUserQuery query) {
        return page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysUserEntity>()
                        .like(StrUtil.isNotBlank(query.getUsername()), SysUserEntity::getUsername, query.getUsername())
                        .orderByDesc(SysUserEntity::getCreateTime)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createUser(UserQuery userQuery) {
        // 新增用户
        SysUserEntity userEntity = BeanUtil.copyProperties(userQuery, SysUserEntity.class);
        userEntity.setPassword(new BCryptPasswordEncoder().encode(bonfireConfig.getDefaultPassword()));
        String userId = save(userEntity) ? userEntity.getId() : null;
        // 新增用户角色关联
        saveUserRoles(userId, userQuery.getRoleIds());
        return userId;
    }

    @Override
    public UserVO getUserById(String userId) {
        // 查询用户表
        SysUserEntity userEntity = getById(userId);
        if (userEntity == null) {
            return null;
        }
        UserVO userVO = BeanUtil.copyProperties(userEntity, UserVO.class);
        // 查询用户角色
        List<SysUserRoleEntity> roles = userRoleService.list(
                new LambdaQueryWrapper<SysUserRoleEntity>().eq(SysUserRoleEntity::getUserId, userId)
        );
        List<String> roleIds = roles.stream().map(SysUserRoleEntity::getRoleId).collect(Collectors.toList());
        userVO.setRoleIds(roleIds);
        return userVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserById(String userId, UserQuery userQuery) {
        // 更新用户表
        SysUserEntity userEntity = BeanUtil.copyProperties(userQuery, SysUserEntity.class);
        userEntity.setId(userId);
        updateById(userEntity);
        // 更新用户角色表，先删除现有角色信息，再重新插入新角色
        userRoleService.remove(new LambdaQueryWrapper<SysUserRoleEntity>().eq(SysUserRoleEntity::getUserId, userId));
        saveUserRoles(userId, userQuery.getRoleIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserById(String userId) {
        // 删除用户表
        removeById(userId);
        // 删除用户的角色
        userRoleService.remove(new LambdaQueryWrapper<SysUserRoleEntity>().eq(SysUserRoleEntity::getUserId, userId));
    }

    @Override
    public String exportUsers(ListUserQuery query) {
        String filename = IdUtil.fastSimpleUUID() + ".xlsx";
        String filePath = bonfireConfig.getPath().getTempPath() + File.separator + filename;
        // 创建excel writer
        BigExcelWriter writer = ExcelUtil.getBigWriter(filePath);
        List<Map<String, Object>> exportData = new ArrayList<>();
        List<SysUserEntity> users = listUsers(query).getRecords();
        // 填充数据
        for (SysUserEntity user : users) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("用户名", user.getUsername());
            map.put("昵称", user.getNickname());
            map.put("全名", user.getFullname());
            map.put("email", user.getEmail());
            map.put("手机", user.getMobile());
            map.put("创建时间", user.getCreateTime());
            map.put("备注", user.getRemark());
            map.put("账号状态", user.getStatus());
            exportData.add(map);
        }
        // 写文件
        writer.write(exportData, true);
        SXSSFSheet sheet = (SXSSFSheet) writer.getSheet();
        sheet.trackAllColumnsForAutoSizing();
        writer.autoSizeColumnAll();
        writer.close();
        return filename;
    }

    @Override
    public boolean resetPassword(String userId) {
        SysUserEntity userEntity = new SysUserEntity();
        userEntity.setId(userId);
        userEntity.setPassword(new BCryptPasswordEncoder().encode(bonfireConfig.getDefaultPassword()));
        return updateById(userEntity);
    }

    @Override
    public void importUsers(String fileId) {
        // 获得提前上传的导入文件
        SysFileEntity fileEntity = fileService.getById(fileId);
        String filePath = bonfireConfig.getPath().getUploadPath() + File.separator + fileEntity.getUniqueFilename();
        // 解析excel写用户表
        ExcelReader reader = ExcelUtil.getReader(filePath);
        List<Map<String, Object>> users = reader.readAll();
        List<SysUserEntity> userEntities = users.stream().map(user -> {
            SysUserEntity userEntity = new SysUserEntity();
            userEntity.setUsername((String) user.get("用户名"));
            userEntity.setNickname((String) user.get("昵称"));
            userEntity.setFullname((String) user.get("全名"));
            userEntity.setEmail((String) user.get("email"));
            userEntity.setMobile((String) user.get("手机"));
            userEntity.setRemark((String) user.get("备注"));
            return userEntity;
        }).collect(Collectors.toList());
        saveBatch(userEntities);
    }

    @Override
    public void unlockUser(String username) {
        String retryKey = StrUtil.format(REDIS_KEY_LOGIN_RETRY, username);
        redisUtil.del(retryKey);
    }

    @Override
    public void kickOut(String userId) {
        // 删除白名单
        String key = StrUtil.format(REDIS_KEY_AUTHS, userId);
        redisUtil.del(key);
    }

    private void saveUserRoles(String userId, List<String> roleIds) {
        if (userId == null) {
            return;
        }
        if (roleIds != null && roleIds.size() > 0) {
            List<SysUserRoleEntity> userRoles = new ArrayList<>();
            for (String roleId : roleIds) {
                SysUserRoleEntity userRoleEntity = new SysUserRoleEntity();
                userRoleEntity.setUserId(userId);
                userRoleEntity.setRoleId(roleId);
                userRoles.add(userRoleEntity);
            }
            userRoleService.saveBatch(userRoles);
        }
    }

}
