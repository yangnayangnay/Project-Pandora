<template>
  <div class="all-logs-view">
    <div class="filter-bar">
      <el-select v-model="filters.userId" placeholder="选择用户" clearable filterable style="width: 180px">
        <el-option v-for="u in users" :key="u.id" :label="u.real_name || u.username" :value="u.id" />
      </el-select>
      <el-date-picker v-model="filters.dateRange" type="daterange" range-separator="至"
        start-placeholder="开始日期" end-placeholder="结束日期" format="YYYY-MM-DD"
        value-format="YYYY-MM-DD" style="width: 280px" />
      <el-input v-model="filters.keyword" placeholder="工作事项关键字" clearable style="width: 200px" />
      <el-button type="primary" @click="loadLogs">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-table :data="logs" border v-loading="loading" style="margin-top: 16px">
      <el-table-column label="用户" width="140">
        <template #default="{ row }">
          {{ row.real_name || row.username || row.user_id }}
        </template>
      </el-table-column>
      <el-table-column prop="work_item" label="工作事项" show-overflow-tooltip />
      <el-table-column prop="time_cost" label="时间花费(h)" width="120" />
      <el-table-column prop="log_date" label="日志日期" width="120" />
      <el-table-column prop="created_at" label="创建时间" width="180" />
    </el-table>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listAllLogs } from '@/api/log'
import { listUsers } from '@/api/user'

const logs = ref([])
const users = ref([])
const loading = ref(false)
const filters = reactive({
  userId: '',
  dateRange: null,
  keyword: ''
})

function buildParams() {
  const params = {}
  if (filters.userId) params.userId = filters.userId
  if (filters.dateRange && filters.dateRange.length === 2) {
    params.startDate = filters.dateRange[0]
    params.endDate = filters.dateRange[1]
  }
  if (filters.keyword) params.keyword = filters.keyword
  return params
}

async function loadLogs() {
  loading.value = true
  try {
    const res = await listAllLogs(buildParams())
    logs.value = res.data || []
  } catch (e) {
    console.error('加载日志失败', e)
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
  filters.userId = ''
  filters.dateRange = null
  filters.keyword = ''
  loadLogs()
}

onMounted(() => {
  loadUsers()
  loadLogs()
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}
</style>