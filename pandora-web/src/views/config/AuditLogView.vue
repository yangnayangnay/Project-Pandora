<template>
  <div class="audit-log-view">
    <el-table :data="logs" border>
      <el-table-column prop="operator_id" label="操作人ID" width="100" />
      <el-table-column prop="action" label="动作" width="100" />
      <el-table-column prop="target_type" label="目标类型" width="120" />
      <el-table-column prop="target_id" label="目标ID" width="100" />
      <el-table-column prop="detail" label="详情" />
      <el-table-column prop="operated_at" label="操作时间" width="180" />
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAuditLogs } from '@/api/audit'

const logs = ref([])

onMounted(async () => {
  try {
    const res = await getAuditLogs()
    logs.value = res.data.data
  } catch (e) {
    console.error('加载审计日志失败', e)
  }
})
</script>