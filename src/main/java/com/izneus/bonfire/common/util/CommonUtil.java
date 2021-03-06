package com.izneus.bonfire.common.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.EnumUtil;
import com.izneus.bonfire.common.constant.Dict;

import java.util.Map;

/**
 * 其他通用工具类
 *
 * @author Izneus
 * @date 2020/08/03
 */
public class CommonUtil {

    private static final int MAX_FILENAME_LENGTH = 255;
    private static final String PARENT_DIR = "..";

    /**
     * 判断是否是合法的文件名
     *
     * @param filename 文件名
     * @return boolean
     */
    public static boolean isValidFilename(String filename) {
        if (filename == null || filename.length() > MAX_FILENAME_LENGTH) {
            return false;
        }
        // 是否含有相对路径，只是个简单的安全性检查
        if (filename.contains(PARENT_DIR)) {
            return false;
        }
        return !FileUtil.containsInvalid(filename);
    }

    /**
     * 通过枚举中自定义字段的值，获得枚举实例
     *
     * @param enumClass  枚举类
     * @param fieldName  字段名
     * @param fieldValue 字段值
     * @param <E>        枚举类
     * @return 枚举实例
     */
    public static <E extends Enum<E>> E getEnum(Class<E> enumClass, String fieldName, Object fieldValue) {
        // 先获得name:value的map，再互换key:value的位置，通过value获得name，然后获得枚举实例
        Map<String, Object> enumMap = EnumUtil.getNameFieldMap(enumClass, fieldName);
        Map<Object, String> inverseMap = MapUtil.inverse(enumMap);
        String enumName = MapUtil.getStr(inverseMap, fieldValue);
        return EnumUtil.fromString(enumClass, enumName);
    }

}
