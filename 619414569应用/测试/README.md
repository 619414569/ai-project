# 619414569 测试资料

本目录集中存放工作台 v1.0 的测试用例、执行记录和错题本。

- [工作台功能测试](./工作台功能测试.md)
- [验收状态](./验收状态.md)
- [错题本](./错题本/README.md)
- `check-error-notebook.mjs`：校验错题本结构；加 `--release` 后执行发布门禁。

从仓库根目录执行：

```bash
node 619414569应用/测试/check-error-notebook.mjs
node 619414569应用/测试/check-error-notebook.mjs --release
```

普通检查允许验收状态为 `pending`；`--release` 除要求错题本无未解决项外，还要求项目负责人已经单独输入 `本次验收通过`，并将验收状态记录为 `accepted`。
