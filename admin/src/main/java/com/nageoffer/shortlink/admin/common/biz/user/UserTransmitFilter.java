/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nageoffer.shortlink.admin.common.biz.user;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.nageoffer.shortlink.admin.common.convention.exception.ClientException;
import com.nageoffer.shortlink.admin.common.enums.UserErrorCodeEnum;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * 用户信息传输过滤器
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@RequiredArgsConstructor
public class UserTransmitFilter implements Filter {
    private final StringRedisTemplate stringRedisTemplate;
    private static final List<String> IGNORE_URI = Lists.newArrayList(
            "/api/short-link/admin/v1/user/has-username",
            "/api/short-link/admin/v1/user/login"
    );

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String requestURI = request.getRequestURI();
        if (!IGNORE_URI.contains(requestURI)){
            String method = request.getMethod();
            if (!(Objects.equals(requestURI,"/api/short-link/admin/v1/user") && Objects.equals(method,"POST"))){
                String username = request.getHeader("username");
                String token = request.getHeader("token");
                if (!StrUtil.isAllNotBlank(username,token)){
                    throw new ClientException(UserErrorCodeEnum.USER_TOKEN_FAIL);
                }
                Object o;
                try {
                    o =stringRedisTemplate.opsForHash().get("login_" + username, token);
                    if (o == null){
                        throw new ClientException(UserErrorCodeEnum.USER_TOKEN_FAIL);
                    }
                }catch (Exception ex){
                    throw new ClientException(UserErrorCodeEnum.USER_TOKEN_FAIL);
                }
                UserInfoDTO userInfoDTO = JSON.parseObject(o.toString(), UserInfoDTO.class);
                UserContext.setUser(userInfoDTO);
            }
        }
        try{
            filterChain.doFilter(servletRequest,servletResponse);
        }finally {
            UserContext.removeUser();
        }
    }

//    @SneakyThrows
//    @Override
//    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
//        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
//        String username = httpServletRequest.getHeader("username");
//        if (StrUtil.isNotBlank(username)) {
//            String userId = httpServletRequest.getHeader("userId");
//            String realName = httpServletRequest.getHeader("realName");
//            UserInfoDTO userInfoDTO = new UserInfoDTO(userId, username, realName);
//            UserContext.setUser(userInfoDTO);
//        }
//        try {
//            filterChain.doFilter(servletRequest, servletResponse);
//        } finally {
//            UserContext.removeUser();
//        }
//    }
}