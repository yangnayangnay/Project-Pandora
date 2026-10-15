<template>
  <div class="audit-log-view">
    <div class="filter-bar">
      <el-input v-model="filters.operatorId" placeholder="操作人ID" clearable style="width: 120px" />
      <el-date-picker v-model="filters.dateRange" type="datetimerange" range-separator="至"
        start-placeholder="开始时间" end-placeholder="结束时间" format="YYYY-MM-DD HH:mm:ss"
        value-format="YYYY-MM-DD HH:mm:ss" style="width: 380px" />
      <el-select v-model="filters.operationType" placeholder="操作类型" clearable style="width: 180px">
        <el-option v-for="t in operationTypes" :key="t" :label="t" :value="t" />
      </el-select>
      <el-input v-model="filters.keyword" placeholder="关键字" clearable style="width: 180px" />
      <el-button type="primary" @click="loadLogs">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
      <el-button type="success" @click="handleExport" :loading="exporting">导出CSV</el-button>
    </div>

    <el-table :data="logs" border v-loading="loading" style="margin-top: 16px">
      <el-table-column label="操作人" width="120">
        <template #default="{ row }">
          {{ row.operator_real_name || row.operator_name || row.operator_id }}
        </template>
      </el-table-column>
      <el-table-column prop="operation_type" label="操作类型" width="160" />
      <el-table-column prop="target_type" label="目标类型" width="120" />
      <el-table-column prop="target_id" label="目标ID" width="100" />
      <el-table-column prop="operation_content" label="操作内容" show-overflow-tooltip />
      <el-table-column prop="detail" label="详情" width="200" show-overflow-tooltip />
      <el-table-column prop="operated_at" label="操作时间" width="180" />
    </el-table>

    <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
      :total="pagination.total" layout="total, prev, pager, next" @current-change="loadLogs"
      style="margin-top: 16px; justify-content: flex-end" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAuditLogs, exportAuditLogs } from '@/api/audit'

const logs = ref([])
const loading = ref(false)
const exporting = ref(false)
const filters = reactive({
  operatorId: '',
  dateRange: null,
  operationType: '',
  keyword: ''
})
const pagination = reactive({ page: 1, size: 20, total: 0 })

const operationTypes = [
  'LOGIN', 'LOGOUT',
  'TASK_CREATE', 'TASK_PROGRESS_UPDATE', 'TASK_TRANSITION',
  'LOG_CREATE', 'LOG_UPDATE', 'LOG_DELETE', 'LOG_AUTO_GENERATE', 'TOP10_UPDATE',
  'TEST_DATA_CLEANUP',
  'USER_REGISTER', 'USER_APPROVE', 'USER_REJECT', 'USER_PROFILE_UPDATE', 'USER_PASSWORD_CHANGE',
  'PASSWORD_RESET_REQUEST', 'PASSWORD_RESET_APPROVE', 'PASSWORD_RESET_REJECT', 'PASSWORD_RESET_EXECUTE',
  'EMPLOYEE_RECORD_CREATE', 'EMPLOYEE_RECORD_UPDATE', 'EMPLOYEE_RECORD_DELETE',
  'PERM_INVITATION_CREATE', 'PERM_INVITATION_RESPOND', 'PERM_AUDIT_RULE_CREATE',
  'AI_MBTI_SUBMIT', 'AI_KEYWORD_ANALYSIS', 'DATA_SYNC'
]

function buildParams() {
  const params = {
    page: pagination.page,
    size: pagination.size
  }
  if (filters.operatorId) params.operatorId = filters.operatorId
  if (filters.dateRange && filters.dateRange.length === 2) {
    params.startTime = filters.dateRange[0]
    params.endTime = filters.dateRange[1]
  }
  if (filters.operationType) params.operationType = filters.operationType
  if (filters.keyword) params.keyword = filters.keyword
  return params
}

async function loadLogs() {
  loading.value = true
  try {
    const res = await getAuditLogs(buildParams())
    const data = res.data.data
    logs.value = data.records || []
    pagination.total = data.total || 0
  } catch (e) {
    console.error('加载审计日志失败', e)
  } finally {
    loading.value = false
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const params = buildParams()
    delete params.page
    delete params.size
    const res = await exportAuditLogs(params)
    const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'audit_logs.csv'
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    console.error('导出失败', e)
  } finally {
    exporting.value = false
  }
}

function resetFilters() {
  filters.operatorId = ''
  filters.dateRange = null
  filters.operationType = ''
  filters.keyword = ''
  pagination.page = 1
  loadLogs()
}

onMounted(() => loadLogs())
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}
</style>