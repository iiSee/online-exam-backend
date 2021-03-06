package edu.sandau.security;

import edu.sandau.rest.model.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpSession;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;

@Auth
@Slf4j
public class RequestFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        //获取客户端Header中提交的token
        String token = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        //判断用户是否已登录
        boolean access = false;
        if ( !StringUtils.isEmpty(token) ) {
            try {
                User user = sessionWrapper.getCurrentUser(token);
                //是登录用户
                if (user != null && user.getId() > -1) {
                    int userId = user.getId();
                    session.setAttribute("key", token);
                    session.setAttribute("userId", userId);
                    session.setAttribute("user", user);
                    sessionWrapper.refresh(token);
                    final SecurityContext currentSecurityContext = requestContext.getSecurityContext();
                    requestContext.setSecurityContext(new SecurityContext() {
                        //重写当前请求的安全信息
                        //获取用户id: 通过 getUserPrincipal().getName() 方法
                        //获取用户redis的key: 通过 getAuthenticationScheme() 方法
                        @Override
                        public Principal getUserPrincipal() {
                            return () -> String.valueOf(userId);
                        }
                        @Override
                        public boolean isUserInRole(String role) {
                            return true;
                        }
                        @Override
                        public boolean isSecure() {
                            return currentSecurityContext.isSecure();
                        }
                        @Override
                        public String getAuthenticationScheme() {
                            return token;
                        }
                    });
                    access = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //拦截
        if ( !access ) {
            requestContext.abortWith(Response
                    .status(Response.Status.UNAUTHORIZED)
                    .entity("请先登录哦~")
                    .build());
        }
    }

    @Autowired
    private SessionWrapper sessionWrapper;
    @Autowired
    private HttpSession session;
}
