package com.aiproject.scaffold.demo.item;

import com.aiproject.scaffold.common.api.ErrorCode;
import com.aiproject.scaffold.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemServiceTest {
    private final ItemService service = new ItemService();

    @Test
    void createStoresItemInMemory() {
        Item item = service.create("标题", "内容");

        assertNotNull(item.id());
        assertFalse(item.deleted());
        assertEquals("标题", item.title());
        assertEquals("内容", service.get(item.id()).content());
        assertEquals(1, service.size(false));
    }

    @Test
    void updateKeepsIdAndCreatedAt() {
        Item created = service.create("旧标题", "旧内容");

        Item updated = service.update(created.id(), "新标题", "新内容");

        assertEquals(created.id(), updated.id());
        assertEquals(created.createdAt(), updated.createdAt());
        assertEquals("新标题", updated.title());
        assertFalse(updated.updatedAt().isBefore(created.updatedAt()));
    }

    @Test
    void trashMovesItemOutOfActiveList() {
        Item item = service.create("待删除", null);

        service.trash(item.id());

        assertEquals(0, service.size(false));
        assertEquals(1, service.size(true));
        assertTrue(service.get(item.id()).deleted());
        assertEquals(0, service.list(false).size());
        assertEquals(1, service.list(true).size());
    }

    @Test
    void restoreBringsItemBackToActiveList() {
        Item item = service.create("待删除", null);
        service.trash(item.id());

        Item restored = service.restore(item.id());

        assertFalse(restored.deleted());
        assertEquals(1, service.size(false));
        assertEquals(0, service.size(true));
    }

    @Test
    void restoreRejectsItemThatIsNotInTrash() {
        Item item = service.create("正常条目", null);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.restore(item.id()));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void purgeRequiresItemInTrash() {
        Item item = service.create("正常条目", null);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.purge(item.id()));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        assertEquals(1, service.size(false));
    }

    @Test
    void purgeRemovesTrashedItemCompletely() {
        Item item = service.create("待删除", null);
        service.trash(item.id());

        service.purge(item.id());

        assertEquals(0, service.size(false));
        assertEquals(0, service.size(true));
        assertThrows(BusinessException.class, () -> service.get(item.id()));
    }

    @Test
    void updateRejectsTrashedItem() {
        Item item = service.create("待删除", null);
        service.trash(item.id());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.update(item.id(), "新标题", null));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void missingItemReportsNotFound() {
        BusinessException exception = assertThrows(BusinessException.class, () -> service.get("missing-id"));

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void listReturnsActiveItemsNewestFirst() {
        Item first = service.create("A", null);
        Item second = service.create("B", null);

        List<Item> items = service.list(false);

        assertEquals(2, items.size());
        assertTrue(items.stream().anyMatch(item -> item.id().equals(first.id())));
        assertTrue(items.stream().anyMatch(item -> item.id().equals(second.id())));
    }
}
