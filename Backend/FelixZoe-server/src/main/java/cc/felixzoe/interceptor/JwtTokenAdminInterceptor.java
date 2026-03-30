package cc.felixzoe.interceptor;

import cc.felixzoe.constant.JwtClaimsConstant;
import cc.felixzoe.constant.MessageConstant;
import cc.felixzoe.constant.StatusConstant;
import cc.felixzoe.context.BaseContext;
import cc.felixzoe.exception.GuestReadOnlyException;
import cc.felixzoe.exception.NotLoginException;
import cc.felixzoe.exception.UnauthorizedException;
import cc.felixzoe.properties.JwtProperties;
import cc.felixzoe.service.TokenService;
import cc.felixzoe.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT令牌校验拦截器
 */
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getTokenName());

        if (StringUtils.isEmpty(token)) {
            throw new NotLoginException(MessageConstant.NOT_LOGIN);
        }

        // 令牌长度基本校验（JWT至少有3个部分）
        if (token.length() < 20 || !token.contains(".")) {
            throw new UnauthorizedException(MessageConstant.NOT_AUTHORIZED);
        }

        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), token);
            Long adminId = Long.valueOf(claims.get(JwtClaimsConstant.ADMIN_ID).toString());
            Integer role = Integer.valueOf(claims.get(JwtClaimsConstant.ADMIN_ROLE).toString());

            if (log.isDebugEnabled()) {
                log.debug("jwt校验,当前管理员id：{}, role: {}", adminId, role);
            }

            // 检测令牌是否在服务端存在
            if (!tokenService.isValidToken(adminId, token)) {
                throw new UnauthorizedException(MessageConstant.NOT_AUTHORIZED);
            }

            // 游客账号(role=0)只允许GET查询操作，禁止增删改
            if (role.equals(StatusConstant.DISABLE) && !"GET".equalsIgnoreCase(request.getMethod())) {
                throw new GuestReadOnlyException(MessageConstant.GUEST_READ_ONLY);
            }

            BaseContext.setCurrentId(adminId);
            BaseContext.setCurrentRole(role);
            return true;
        } catch (GuestReadOnlyException ex) {
            throw ex;
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("JWT解析失败: {}", ex.getMessage());
            throw new UnauthorizedException(MessageConstant.NOT_AUTHORIZED);
        }
    }

    /**
     * 后置处理 - 清理ThreadLocal
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        BaseContext.clear();
    }
}
