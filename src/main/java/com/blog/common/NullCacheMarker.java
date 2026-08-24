package com.blog.common;

import lombok.NoArgsConstructor;

/**
 * 空值缓存标记：用于缓存不存在的资源（防缓存穿透）
 *
 * @author Liangkunrui
 */
@NoArgsConstructor
public class NullCacheMarker {

    public static final NullCacheMarker INSTANCE = new NullCacheMarker();
}
