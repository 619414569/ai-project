<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { itemsApi } from '@/api/modules/items'
import type { Item } from '@/types/api'

type Filter = 'all' | 'today' | 'week' | 'trash'

const items = ref<Item[]>([])
const trashed = ref<Item[]>([])
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const filter = ref<Filter>('all')
const selected = ref<Item | null>(null)
const isNew = ref(false)
const draft = reactive({ id: '', title: '', content: '' })

const filterOptions: Array<{ key: Filter; label: string }> = [
  { key: 'all', label: '所有日记' },
  { key: 'today', label: '今天' },
  { key: 'week', label: '本周' },
  { key: 'trash', label: '回收站' },
]

const counts = computed(() => ({
  all: items.value.length,
  today: items.value.filter((item) => isToday(item.createdAt)).length,
  week: items.value.filter((item) => isThisWeek(item.createdAt)).length,
  trash: trashed.value.length,
}))

const source = computed(() => (filter.value === 'trash' ? trashed.value : items.value))

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return source.value.filter((item) => {
    if (filter.value === 'today' && !isToday(item.createdAt)) return false
    if (filter.value === 'week' && !isThisWeek(item.createdAt)) return false
    if (!kw) return true
    return item.title.toLowerCase().includes(kw) || (item.content ?? '').toLowerCase().includes(kw)
  })
})

const currentTitle = computed(() => filterOptions.find((option) => option.key === filter.value)?.label ?? '所有日记')
const editing = computed(() => isNew.value || Boolean(draft.id))
const inTrash = computed(() => !isNew.value && Boolean(selected.value?.deleted))

const checked = ref<Set<string>>(new Set())
const checkedCount = computed(() => checked.value.size)
const allChecked = computed(() => filtered.value.length > 0 && filtered.value.every((item) => checked.value.has(item.id)))

function isChecked(id: string) {
  return checked.value.has(id)
}

function toggleCheck(id: string) {
  const next = new Set(checked.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  checked.value = next
}

function toggleAll() {
  checked.value = allChecked.value ? new Set() : new Set(filtered.value.map((item) => item.id))
}

function clearChecked() {
  checked.value = new Set()
}

/** 多选删除：正常视图移入回收站，回收站视图彻底删除（与原型一致） */
async function deleteChecked() {
  const ids = Array.from(checked.value)
  if (!ids.length) return
  const fromTrash = filter.value === 'trash'
  try {
    await Promise.all(ids.map((id) => (fromTrash ? itemsApi.purge(id) : itemsApi.trash(id))))
    ElMessage.success(fromTrash ? `已彻底删除 ${ids.length} 篇日记` : `已将 ${ids.length} 篇日记移入回收站`)
    clearChecked()
    if (draft.id && ids.includes(draft.id)) resetEditor()
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
    await load()
  }
}

function isToday(iso: string) {
  const date = new Date(iso)
  const now = new Date()
  return date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate()
}

function isThisWeek(iso: string) {
  const date = new Date(iso)
  const now = new Date()
  const start = new Date(now)
  const offset = (now.getDay() + 6) % 7
  start.setDate(now.getDate() - offset)
  start.setHours(0, 0, 0, 0)
  return date.getTime() >= start.getTime()
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function snippet(text: string | null) {
  if (!text) return '暂无正文'
  return text.replace(/\s+/g, ' ').slice(0, 46)
}

async function load() {
  loading.value = true
  try {
    const [active, deleted] = await Promise.all([itemsApi.list(false), itemsApi.list(true)])
    items.value = active
    trashed.value = deleted
    const aliveIds = new Set([...active, ...deleted].map((item) => item.id))
    checked.value = new Set(Array.from(checked.value).filter((id) => aliveIds.has(id)))
    if (draft.id) {
      const found = [...active, ...deleted].find((item) => item.id === draft.id)
      if (found) {
        selected.value = found
      } else {
        resetEditor()
      }
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function select(item: Item) {
  isNew.value = false
  selected.value = item
  draft.id = item.id
  draft.title = item.title
  draft.content = item.content ?? ''
}

function startNew() {
  isNew.value = true
  selected.value = null
  draft.id = ''
  draft.title = ''
  draft.content = ''
}

function resetEditor() {
  isNew.value = false
  selected.value = null
  draft.id = ''
  draft.title = ''
  draft.content = ''
}

async function save() {
  if (!draft.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  saving.value = true
  try {
    const payload = { title: draft.title.trim(), content: draft.content }
    if (draft.id) {
      const updated = await itemsApi.update(draft.id, payload)
      draft.title = updated.title
      draft.content = updated.content ?? ''
      ElMessage.success('已保存')
    } else {
      const created = await itemsApi.create(payload)
      draft.id = created.id
      isNew.value = false
      selected.value = created
      ElMessage.success('已创建')
    }
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

/** 服务端两段式删除：先移入回收站，再从回收站彻底删除 */
async function moveToTrash(item: Item) {
  try {
    await itemsApi.trash(item.id)
    ElMessage.success(`已将「${item.title}」移入回收站`)
    if (draft.id === item.id) resetEditor()
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

async function restore(item: Item) {
  try {
    await itemsApi.restore(item.id)
    ElMessage.success(`已恢复「${item.title}」`)
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '恢复失败')
  }
}

async function purge(item: Item) {
  try {
    await itemsApi.purge(item.id)
    ElMessage.success(`已彻底删除「${item.title}」`)
    if (draft.id === item.id) resetEditor()
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '彻底删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="nb">
    <nav class="rail">
      <div class="rail-logo">N</div>
      <button
        v-for="option in filterOptions.filter((item) => item.key !== 'trash')"
        :key="option.key"
        class="rail-btn"
        :class="{ on: filter === option.key }"
        :title="option.label"
        @click="filter = option.key"
      >
        {{ option.label.slice(0, 1) }}
      </button>
      <button
        class="rail-btn"
        :class="{ on: filter === 'trash' }"
        title="回收站"
        @click="filter = 'trash'"
      >
        残
      </button>
      <div class="rail-spacer" />
      <div class="rail-avatar">记</div>
    </nav>

    <aside class="side">
      <div class="side-title">手账</div>
      <button
        v-for="option in filterOptions"
        :key="option.key"
        class="s-item"
        :class="{ on: filter === option.key }"
        @click="filter = option.key"
      >
        <span>{{ option.label }}</span>
        <span class="cnt">{{ counts[option.key] }}</span>
      </button>
      <div class="side-foot">
        <span class="dot" />
        数据来自后端内存存储
      </div>
    </aside>

    <section class="list">
      <header class="list-head">
        <div class="list-title">{{ currentTitle }}</div>
        <button class="ghost-btn" :disabled="loading" @click="load">刷新</button>
      </header>

      <div class="search-bar">
        <input v-model="keyword" placeholder="搜索标题或正文" />
      </div>

      <div v-if="checkedCount" class="sel-bar">
        <button class="sel-all" @click="toggleAll">
          <span class="cbx" :class="{ on: allChecked }">✓</span>
          全选
        </button>
        <span>已选 <b>{{ checkedCount }}</b> 篇</span>
        <span class="sel-actions">
          <button class="link-btn danger" @click="deleteChecked">
            {{ filter === 'trash' ? '彻底删除' : '删除' }}
          </button>
          <button class="link-btn" @click="clearChecked">取消</button>
        </span>
      </div>

      <button v-if="filter !== 'trash'" class="add-row" @click="startNew">
        <span class="plus">+</span>
        写点什么
      </button>

      <div class="list-scroll">
        <div v-if="!filtered.length" class="empty-tip">
          <template v-if="filter === 'trash'">
            回收站是空的<br />删除的日记会先放在这里
          </template>
          <template v-else>
            {{ loading ? '加载中…' : '这里还没有日记，点上方「写点什么」开始第一篇' }}
          </template>
        </div>
        <div
          v-for="item in filtered"
          :key="item.id"
          class="entry"
          :class="{ on: item.id === draft.id, checked: isChecked(item.id) }"
          role="button"
          tabindex="0"
          @click="select(item)"
          @keydown.enter="select(item)"
        >
          <button
            class="cbx"
            :class="{ on: isChecked(item.id) }"
            :aria-label="isChecked(item.id) ? '取消选择' : '选择'"
            @click.stop="toggleCheck(item.id)"
          >
            ✓
          </button>
          <div class="entry-body">
            <div class="entry-title">{{ item.title }}</div>
            <div class="entry-snippet">{{ snippet(item.content) }}</div>
            <div class="entry-time">
              <span>{{ formatTime(item.createdAt) }}</span>
              <span
                v-if="filter === 'trash'"
                class="restore"
                @click.stop="restore(item)"
              >恢复</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="reader">
      <div v-if="!editing" class="reader-empty">
        <p>选择左侧条目查看内容</p>
        <p class="muted">或点「写点什么」新建一条</p>
      </div>

      <div v-else class="editor">
        <input
          v-model="draft.title"
          class="title-input"
          maxlength="100"
          placeholder="标题"
          :disabled="inTrash"
        />
        <div class="meta">
          <span>{{ inTrash ? '回收站中' : draft.id ? '编辑中' : '新建' }}</span>
          <span>{{ draft.content.length }} / 2000</span>
        </div>
        <textarea
          v-model="draft.content"
          class="content-input"
          maxlength="2000"
          placeholder="今天发生了什么…"
          :disabled="inTrash"
        />
        <div class="actions">
          <template v-if="inTrash">
            <button class="primary" @click="selected && restore(selected)">恢复</button>
            <button class="danger" @click="selected && purge(selected)">彻底删除</button>
          </template>
          <template v-else>
            <button class="primary" :disabled="saving" @click="save">
              {{ saving ? '保存中…' : '保存' }}
            </button>
            <button v-if="draft.id" class="danger" @click="selected && moveToTrash(selected)">
              移入回收站
            </button>
          </template>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.nb {
  --accent: #e07b39;
  --accent-deep: #b85f24;
  --accent-soft: #fbe7d5;
  --line: #f0e7de;
  --text: #2f2b27;
  --text-2: #5f574f;
  --muted: #a49a90;
  display: flex;
  height: 100vh;
  color: var(--text);
  background: #fff;
  font-size: 13px;
}

.rail {
  width: 54px;
  flex: 0 0 54px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 0;
  background: linear-gradient(180deg, #fefaf6, #fdf4ea 62%, #fbe9d8);
}

.rail-logo {
  width: 28px;
  height: 28px;
  margin-bottom: 14px;
  border-radius: 9px;
  background: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  box-shadow: 0 2px 6px rgba(224, 123, 57, 0.35);
}

.rail-btn {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--text-2);
  cursor: pointer;
  font-size: 12px;
}

.rail-btn:hover,
.rail-btn.on {
  background: #fff;
  color: var(--accent-deep);
  box-shadow: 0 1px 4px rgba(184, 95, 36, 0.16);
}

.rail-spacer {
  flex: 1;
}

.rail-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--accent-soft);
  color: var(--accent-deep);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
}

.side {
  width: 186px;
  flex: 0 0 186px;
  display: flex;
  flex-direction: column;
  padding: 16px 10px;
  border-right: 1px solid var(--line);
  background: #fcf7f2;
}

.side-title {
  padding: 0 8px 12px;
  font-size: 15px;
  font-weight: 600;
}

.s-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 32px;
  padding: 0 10px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: var(--text-2);
  font-size: 13px;
  cursor: pointer;
}

.s-item:hover {
  background: #fdf1e5;
}

.s-item.on {
  background: #fdeee1;
  color: var(--accent-deep);
  font-weight: 600;
}

.cnt {
  font-size: 11px;
  color: var(--muted);
}

.side-foot {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: auto;
  padding: 10px 8px 0;
  border-top: 1px solid var(--line);
  color: var(--muted);
  font-size: 11px;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #58b368;
}

.list {
  width: 300px;
  flex: 0 0 300px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--line);
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 10px;
}

.list-title {
  font-size: 15px;
  font-weight: 600;
}

.ghost-btn {
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 4px 10px;
  background: #fff;
  color: var(--text-2);
  font-size: 12px;
  cursor: pointer;
}

.search-bar {
  padding: 0 16px 10px;
}

.search-bar input {
  width: 100%;
  height: 30px;
  padding: 0 10px;
  border: 1px solid var(--line);
  border-radius: 7px;
  background: #fdfaf7;
  color: var(--text);
  font-size: 12px;
  outline: none;
}

.search-bar input:focus {
  border-color: var(--accent-soft);
  background: #fff;
}

.add-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 16px 8px;
  height: 32px;
  border: 1px dashed #ecd9c8;
  border-radius: 7px;
  background: #fffaf5;
  color: var(--accent-deep);
  font-size: 12px;
  cursor: pointer;
}

.add-row:hover {
  background: #fdf1e5;
}

.plus {
  font-size: 15px;
  line-height: 1;
}

.list-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 0 10px 16px;
}

.empty-tip {
  padding: 26px 12px;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.8;
  text-align: center;
}

.entry {
  display: flex;
  gap: 10px;
  width: 100%;
  margin-bottom: 6px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.entry:hover {
  background: #fdf7f1;
}

.entry.on {
  border-color: #f4dcc7;
  background: #fdeee1;
}

.entry.checked {
  background: #fdf3e9;
}

.cbx {
  width: 15px;
  height: 15px;
  flex: 0 0 15px;
  margin-top: 2px;
  padding: 0;
  border: 1.5px solid #dccfc0;
  border-radius: 3px;
  background: #fff;
  color: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  line-height: 1;
  cursor: pointer;
  transition: 0.12s;
}

.cbx.on {
  border-color: var(--accent);
  background: var(--accent);
  color: #fff;
}

.entry-body {
  min-width: 0;
  flex: 1;
}

.sel-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 2px 16px 8px;
  height: 38px;
  padding: 0 10px;
  border: 1px solid #f1e0cd;
  border-radius: 6px;
  background: #fdf6ec;
  color: #8a7c6d;
  font-size: 12px;
}

.sel-bar b {
  color: var(--accent-deep);
  font-weight: 600;
}

.sel-all {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #83776b;
  font-size: 12px;
  cursor: pointer;
  user-select: none;
}

.sel-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.link-btn {
  padding: 0;
  border: 0;
  background: transparent;
  color: #8a7f74;
  font-size: 12px;
  cursor: pointer;
}

.link-btn:hover {
  color: var(--accent-deep);
}

.link-btn.danger {
  color: #c8503c;
}

.link-btn.danger:hover {
  color: #a83926;
}

.entry-title {
  font-size: 13px;
  font-weight: 600;
}

.entry-snippet {
  margin-top: 4px;
  color: var(--text-2);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry-time {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
  color: var(--muted);
  font-size: 11px;
}

.restore {
  color: var(--accent-deep);
  cursor: pointer;
}

.restore:hover {
  text-decoration: underline;
}

.reader {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.reader-empty {
  margin: auto;
  color: var(--muted);
  text-align: center;
}

.reader-empty .muted {
  margin-top: 6px;
  font-size: 12px;
}

.editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 24px 32px;
}

.title-input {
  border: 0;
  border-bottom: 1px solid var(--line);
  padding: 6px 0 10px;
  color: var(--text);
  font-size: 20px;
  font-weight: 600;
  outline: none;
}

.title-input::placeholder {
  color: #cfc4b9;
}

.title-input:disabled,
.content-input:disabled {
  color: var(--muted);
  background: #fbf8f5;
}

.meta {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  color: var(--muted);
  font-size: 11px;
}

.content-input {
  flex: 1;
  margin-top: 8px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fdfaf7;
  color: var(--text);
  font-family: inherit;
  font-size: 13px;
  line-height: 1.8;
  resize: none;
  outline: none;
}

.content-input:focus {
  background: #fff;
  border-color: var(--accent-soft);
}

.actions {
  display: flex;
  gap: 10px;
  padding-top: 14px;
}

.primary,
.danger {
  height: 32px;
  padding: 0 18px;
  border: 0;
  border-radius: 7px;
  font-size: 13px;
  cursor: pointer;
}

.primary {
  background: var(--accent);
  color: #fff;
}

.primary:hover {
  background: var(--accent-deep);
}

.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.danger {
  background: #fdeceb;
  color: #c0392b;
}

.danger:hover {
  background: #fadbd8;
}

@media (max-width: 900px) {
  .side {
    display: none;
  }

  .list {
    width: 240px;
    flex-basis: 240px;
  }

  .editor {
    padding: 18px;
  }
}
</style>
