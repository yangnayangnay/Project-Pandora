<template>
  <div class="all-tasks-view">
    <div class="filter-bar">
      <el-select v-model="filters.status" placeholder="任务状态" clearable style="width: 140px">
        <el-option label="待处理" value="PENDING" />
        <el-option label="进行中" value="IN_PROGRESS" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="已确认" value="CONFIRMED" />
      </el-select>
      <el-select v-model="filters.assigneeId" placeholder="负责人" clearable filterable style="width: 180px">
        <el-option v-for="u in users" :key="u.id" :label="u.real_name || u.username" :value="u.id" />
      </el-select>
      <el-button type="primary" @click="loadTasks">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-table :data="tasks" border v-loading="loading" style="margin-top: 16px">
      <el-table-column prop="name" label="任务名称" show-overflow-tooltip />
      <el-table-column label="负责人" width="120">
        <template #default="{ row }">
          {{ row.assignee_real_name || row.assignee_name || row.assignee_id }}
        </template>
      </el-table-column>
      <el-table-column label="派发人" width="120">
        <template #default="{ row }">
          {{ row.dispatcher_real_name || row.dispatcher_name || row.dispatcher_id }}
        </template>
      </el-table-column>
      <el-table-column prop="priority" label="优先级" width="100" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column label="紧急" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.is_urgent" type="danger" size="small">紧急</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="重要" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.is_important" type="warning" size="small">重要</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="end_time" label="截止日期" width="120" />
      <el-table-column prop="progress_note" label="进度备注" show-overflow-tooltip />
    </el-table>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listTasks } from '@/api/task'
import { listUsers } from '@/api/user'

const tasks = ref([])
const users = ref([])
const loading = ref(false)
const filters = reactive({
  status: '',
  assigneeId: ''
})

function buildParams() {
  const params = {}
  if (filters.status) params.status = filters.status
  if (filters.assigneeId) params.assigneeId = filters.assigneeId
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
  loadTasks()
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
</style>