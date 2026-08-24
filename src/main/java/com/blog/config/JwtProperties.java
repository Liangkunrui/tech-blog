package com.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性（application.yml 中 jwt.* 前缀）
 *
 * @author Liangkunrui
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 签名密钥（HS256 要求至少 32 字节，生产环境务必更换并通过环境变量注入） */
    private String secret;

    /** Token 有效期（秒），默认 7 天 */
    private long expireSeconds = 604800;
}
