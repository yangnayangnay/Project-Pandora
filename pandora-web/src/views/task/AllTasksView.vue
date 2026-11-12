<template>
  <div class="all-tasks-view">
    <div class="filter-bar" aria-label="任务筛选">
      <div class="filter-block"><span>搜索</span><el-input v-model="filters.keyword" placeholder="任务名称或内容" clearable /></div>
      <div class="filter-block"><span>完成情况</span>
      <el-select v-model="filters.status" placeholder="任务状态" clearable style="width: 140px">
        <el-option label="进行中" value="IN_PROGRESS" />
        <el-option label="已完成" value="COMPLETED" />
      </el-select>
      </div>
      <div class="filter-block"><span>负责人</span>
      <el-select v-model="filters.assigneeId" placeholder="负责人" clearable filterable style="width: 180px">
        <el-option v-for="u in users" :key="u.id" :label="u.real_name || u.username" :value="u.id" />
      </el-select>
      </div>
      <div class="filter-block"><span>日期</span><el-date-picker v-model="filters.dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" /></div>
      <el-button type="primary" @click="loadTasks">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
      <el-button class="landscape-button" circle :title="landscape ? '退出横屏' : '横屏展示'" @click="landscape = !landscape">{{ landscape ? '↙' : '↗' }}</el-button>
    </div>

    <div :class="['table-shell', { landscape }]">
    <el-table :data="tasks" border v-loading="loading">
      <el-table-column prop="name" label="任务名称" show-overflow-tooltip />
      <el-table-column prop="end_time" label="截止日期" width="120" />
      <el-table-column label="任务标签" min-width="180"><template #default="{ row }"><div class="tag-list"><el-tag v-for="tag in row.tags || []" :key="tag.id" size="small" :class="`task-tag color-${tag.color_index ?? 0}`">{{ tag.name }}</el-tag><span v-if="!(row.tags || []).length">-</span></div></template></el-table-column>
      <el-table-column label="紧急程度" width="100"><template #default="{ row }"><el-tag :type="row.is_urgent ? 'danger' : 'info'">{{ row.is_urgent ? '紧急' : '不紧急' }}</el-tag></template></el-table-column>
      <el-table-column label="完成情况" width="100"><template #default="{ row }"><el-tag :type="row.status === 'COMPLETED' ? 'success' : 'warning'">{{ row.status === 'COMPLETED' ? '已完成' : '进行中' }}</el-tag></template></el-table-column>
      <el-table-column label="负责人" min-width="160"><template #default="{ row }">{{ assigneeNames(row) }}</template></el-table-column>
    </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listTasks } from '@/api/task'
import { listUsers } from '@/api/user'

const tasks = ref([])
const users = ref([])
const loading = ref(false)
const landscape = ref(false)
const filters = reactive({
  status: '',
  assigneeId: '',
  keyword: '',
  dateRange: null
})

function buildParams() {
  const params = {}
  if (filters.status) params.status = filters.status
  if (filters.assigneeId) params.assigneeId = filters.assigneeId
  if (filters.keyword) params.keyword = filters.keyword
  if (filters.dateRange?.length === 2) { params.startDate = filters.dateRange[0]; params.endDate = filters.dateRange[1] }
  return params
}

async function loadTasks() {
  loading.value = true
  try {
    const res = await listTasks(buildParams())
    tasks.value = res.data || []
  } catch (e) {
    console.error('加载任务失败', e)
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  try {
    const res = await listUsers()
    users.value = res.data || []
  } catch (e) {
    console.error('加载用户列表失败', e)
  }
}

function resetFilters() {
  filters.status = ''
  filters.assigneeId = ''
  filters.keyword = ''
  filters.dateRange = null
  loadTasks()
}

function assigneeNames(row) {
  const names = (row.assignees || []).map(u => u.real_name || u.username).filter(Boolean)
  return names.length ? names.join('、') : row.assignee_real_name || row.assignee_name || '-'
}

onMounted(() => {
  loadUsers()
  loadTasks()
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}
.filter-block { display:flex; gap:8px; align-items:center; padding:8px 10px; border-radius:10px; background:var(--el-fill-color-light); }
.filter-block > span { color:var(--el-text-color-secondary); white-space:nowrap; font-size:13px; }
.landscape-button { margin-left:auto; }
.table-shell { margin-top:16px; background:var(--el-bg-color); }
.table-shell.landscape { position:fixed; inset:16px; z-index:3000; padding:16px; overflow:auto; box-shadow:var(--el-box-shadow-dark); border-radius:12px; }
.tag-list { display:flex; gap:6px; flex-wrap:wrap; }
.task-tag { border-width:1px; }
.color-0 { background:#e7f5ff; border-color:#74c0fc; color:#1864ab; }
.color-1 { background:#fff3bf; border-color:#ffd43b; color:#8a5d00; }
.color-2 { background:#ebfbee; border-color:#8ce99a; color:#2b8a3e; }
.color-3 { background:#fff0f6; border-color:#faa2c1; color:#a61e4d; }
</style>
