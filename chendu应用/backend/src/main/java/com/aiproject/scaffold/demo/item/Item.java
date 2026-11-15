package com.aiproject.scaffold.demo.item;

import java.time.Instant;

/**
 * 演示用的数据对象，仅存放在内存中，进程重启即丢失。
 * {@code deleted} 为 true 表示在回收站中，只有彻底删除才会从存储移除。
 */
public record Item(String id, String title, String content, Instant createdAt, Instant updatedAt, boolean deleted) {
}
