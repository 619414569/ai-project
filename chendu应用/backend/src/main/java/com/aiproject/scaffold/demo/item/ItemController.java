package com.aiproject.scaffold.demo.item;

import com.aiproject.scaffold.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 内存存储的增删改查示例接口，数据仅保存在 HashMap 中。
 * 删除为两段式：DELETE 移入回收站，/permanent 彻底删除，/restore 恢复。
 */
@Validated
@RestController
@RequestMapping("/api/v1/items")
public class ItemController {
    private static final String ID_PATTERN = "[A-Za-z0-9-]{1,64}";

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * @param deleted true 时返回回收站内容，默认返回正常条目
     */
    @GetMapping
    public ApiResponse<List<Item>> list(@RequestParam(required = false, defaultValue = "false") boolean deleted) {
        return ApiResponse.success(itemService.list(deleted));
    }

    @GetMapping("/{id}")
    public ApiResponse<Item> get(@PathVariable @Pattern(regexp = ID_PATTERN) String id) {
        return ApiResponse.success(itemService.get(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Item>> create(@Valid @RequestBody ItemRequest request) {
        Item item = itemService.create(request.title(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(item));
    }

    @PutMapping("/{id}")
    public ApiResponse<Item> update(
            @PathVariable @Pattern(regexp = ID_PATTERN) String id,
            @Valid @RequestBody ItemRequest request
    ) {
        return ApiResponse.success(itemService.update(id, request.title(), request.content()));
    }

    /**
     * 移入回收站。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Item> trash(@PathVariable @Pattern(regexp = ID_PATTERN) String id) {
        return ApiResponse.success(itemService.trash(id));
    }

    @PostMapping("/{id}/restore")
    public ApiResponse<Item> restore(@PathVariable @Pattern(regexp = ID_PATTERN) String id) {
        return ApiResponse.success(itemService.restore(id));
    }

    /**
     * 从回收站彻底删除。
     */
    @DeleteMapping("/{id}/permanent")
    public ApiResponse<Void> purge(@PathVariable @Pattern(regexp = ID_PATTERN) String id) {
        itemService.purge(id);
        return ApiResponse.success();
    }

    public record ItemRequest(
            @NotBlank(message = "title is required") @Size(max = 100) String title,
            @Size(max = 2000) String content
    ) {
    }
}
