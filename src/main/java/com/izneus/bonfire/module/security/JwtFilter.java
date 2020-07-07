package com.izneus.bonfire.module.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Izneus
 * @date 2020/07/03
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;
    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(httpServletRequest);
        if (StringUtils.hasText(token)) {
            // todo jwt保存在redis，扩展实现黑名单等功能
            Authentication authentication = jwtUtils.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // todo jwt续期
        }
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    /**
     * 初步校验Token
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtProperties.getHeader());
        // 判断token类型
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtProperties.getType())) {
            // 去掉令牌前缀
            return bearerToken.replace(jwtProperties.getType(), "");
        } else {
            log.debug("非法Token：{}", bearerToken);
        }
        return null;
    }
}