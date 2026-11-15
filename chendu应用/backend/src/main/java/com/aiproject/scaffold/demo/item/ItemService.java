package com.aiproject.scaffold.demo.item;

import com.aiproject.scaffold.common.api.ErrorCode;
import com.aiproject.scaffold.common.exception.BusinessException;
import com.aiproject.scaffold.common.util.IdUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 以 ConcurrentHashMap 作为内存存储的演示服务：不依赖数据库，重启后数据清空。
 * 删除分两步：先移入回收站（{@code deleted = true}），再从回收站彻底删除。
 */
@Service
public class ItemService {
    private final Map<String, Item> store = new ConcurrentHashMap<>();

    public List<Item> list(boolean deleted) {
        return store.values().stream()
                .filter(item -> item.deleted() == deleted)
                .sorted(Comparator.comparing(Item::createdAt).reversed())
                .toList();
    }

    public Item get(String id) {
        Item item = store.get(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Item does not exist: " + id);
        }
        return item;
    }

    public Item create(String title, String content) {
        Instant now = Instant.now();
        Item item = new Item(IdUtils.compactUuid(), title, content, now, now, false);
        store.put(item.id(), item);
        return item;
    }

    public Item update(String id, String title, String content) {
        Item existing = requireActive(id);
        Item updated = new Item(existing.id(), title, content, existing.createdAt(), Instant.now(), false);
        store.put(id, updated);
        return updated;
    }

    /**
     * 移入回收站；已在回收站时抛冲突异常，避免重复操作。
     */
    public Item trash(String id) {
        Item existing = requireActive(id);
        Item trashed = new Item(existing.id(), existing.title(), existing.content(),
                existing.createdAt(), Instant.now(), true);
        store.put(id, trashed);
        return trashed;
    }

    /**
     * 从回收站恢复；不在回收站时抛冲突异常。
     */
    public Item restore(String id) {
        Item existing = get(id);
        if (!existing.deleted()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Item is not in trash: " + id);
        }
        Item restored = new Item(existing.id(), existing.title(), existing.content(),
                existing.createdAt(), Instant.now(), false);
        store.put(id, restored);
        return restored;
    }

    /**
     * 彻底删除；只能是回收站中的条目，避免误删正常数据。
     */
    public void purge(String id) {
        Item existing = get(id);
        if (!existing.deleted()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Move item to trash before deleting permanently: " + id);
        }
        store.remove(id);
    }

    public int size(boolean deleted) {
        return (int) store.values().stream().filter(item -> item.deleted() == deleted).count();
    }

    private Item requireActive(String id) {
        Item existing = get(id);
        if (existing.deleted()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Item is in trash: " + id);
        }
        return existing;
    }
}
