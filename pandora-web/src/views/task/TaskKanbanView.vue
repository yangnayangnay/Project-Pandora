<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div>
            <h2 style="margin: 0;">潘多拉，任务展示及AI展示</h2>
            <p style="margin: 4px 0 0 0; color: #6c757d; font-size: 13px;">任务总览与智能分析</p>
          </div>
          <div>
            <el-select v-model="filterStatus" placeholder="按状态筛选" clearable size="small" style="width: 140px; margin-right: 8px;" @change="loadTasks">
              <el-option label="未完成" value="PENDING" />
              <el-option label="进行中" value="IN_PROGRESS" />
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="已确认" value="CONFIRMED" />
            </el-select>
            <el-button type="primary" plain size="small" @click="loadTasks" :loading="loading">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tasks" v-loading="loading" border style="width: 100%">
        <el-table-column type="selection" width="40" />
        <el-table-column prop="NAME" label="任务名称" min-width="200" sortable>
          <template #default="{ row }">
            {{ row.NAME || row.name || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="END_TIME" label="截止日期" width="130" sortable>
          <template #default="{ row }">
            {{ formatDate(row.END_TIME || row.end_time) }}
          </template>
        </el-table-column>
        <el-table-column label="紧急程度" width="100" sortable :sort-method="(a, b) => sortUrgency(a, b)">
          <template #default="{ row }">
            <el-tag :type="isUrgent(row) ? 'danger' : 'success'" size="small" effect="dark">
              {{ isUrgent(row) ? '紧急' : '不紧急' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="完成情况" width="100" sortable :sort-method="(a, b) => sortCompletion(a, b)">
          <template #default="{ row }">
            <el-tag :type="isCompleted(row) ? 'primary' : 'warning'" size="small" effect="dark">
              {{ isCompleted(row) ? '已完成' : '未完成' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="PROGRESS_NOTE" label="进度备注" min-width="200" sortable>
          <template #default="{ row }">
            {{ row.PROGRESS_NOTE || row.progress_note || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="负责人" width="100" sortable>
          <template #default="{ row }">
            {{ row.ASSIGNEE_NAME || row.assignee_name || row.ASSIGNEE_ID || row.assignee_id || '-' }}
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tasks.length === 0" description="暂无任务数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import request from '@/utils/request'

const tasks = ref([])
const loading = ref(false)
const filterStatus = ref('')

const toBool = (v) => v === true || v === 1

const isUrgent = (row) => toBool(row.IS_URGENT ?? row.is_urgent)

const isCompleted = (row) => {
  const s = row.STATUS || row.status || ''
  return s === 'COMPLETED' || s === 'CONFIRMED'
}

const formatDate = (d) => {
  if (!d) return '-'
  return d.toString().substring(0, 10)
}

const sortUrgency = (a, b) => {
  return (isUrgent(a) ? 1 : 0) - (isUrgent(b) ? 1 : 0)
}

const sortCompletion = (a, b) => {
  return (isCompleted(a) ? 1 : 0) - (isCompleted(b) ? 1 : 0)
}

const loadTasks = async () => {
  loading.value = true
  try {
    const params = {}
    if (filterStatus.value) params.status = filterStatus.value
    const res = await request.get('/task/list', { params })
    tasks.value = res.data || []
  } catch (e) { /* handled by interceptor */ }
  loading.value = false
}

onMounted(loadTasks)
onActivated(loadTasks)
</script>

<style scoped>
:deep(.el-tag.el-tag--danger.el-tag--dark) {
  background-color: #ff6b6b;
}
:deep(.el-tag.el-tag--success.el-tag--dark) {
  background-color: #69db7c;
}
:deep(.el-tag.el-tag--warning.el-tag--dark) {
  background-color: #ffd43b;
  color: #212529;
}
:deep(.el-tag.el-tag--primary.el-tag--dark) {
  background-color: #339af0;
}
</style>
