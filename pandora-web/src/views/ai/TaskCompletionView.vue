<template>
  <div>
    <h2>任务完成分析</h2>
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">总任务数</div>
            <div class="stat-value">{{ stats.total || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">已完成</div>
            <div class="stat-value" style="color: #67C23A">{{ stats.completed || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">进行中</div>
            <div class="stat-value" style="color: #E6A23C">{{ stats.inProgress || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">待开始</div>
            <div class="stat-value" style="color: #909399">{{ stats.pending || 0 }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card header="状态分布">
          <div v-for="item in statusList" :key="item.name" style="margin-bottom: 12px">
            <span style="display:inline-block;width:80px">{{ item.name }}</span>
            <el-progress :percentage="item.percentage" :color="item.color" style="display:inline-block;width:calc(100% - 90px)" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="优先级分布">
          <div v-for="item in priorityList" :key="item.name" style="margin-bottom: 12px">
            <span style="display:inline-block;width:80px">{{ item.name }}</span>
            <el-progress :percentage="item.percentage" :color="item.color" style="display:inline-block;width:calc(100% - 90px)" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card header="任务列表" style="margin-top: 20px">
      <el-table :data="tasks" border>
        <el-table-column prop="name" label="任务名称" min-width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusMap[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="80">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)">{{ priorityMap[row.priority] || row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="end_time" label="截止时间" width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const tasks = ref([])
const stats = ref({})
const statusMap = { COMPLETED: '已完成', IN_PROGRESS: '进行中', PENDING: '待开始', CONFIRMED: '已确认' }
const priorityMap = { URGENT: '紧急', HIGH: '高', MEDIUM: '中', LOW: '低' }

const statusList = computed(() => {
  const total = stats.value.total || 1
  return [
    { name: '已完成', value: stats.value.completed || 0, percentage: Math.round((stats.value.completed || 0) / total * 100), color: '#67C23A' },
    { name: '进行中', value: stats.value.inProgress || 0, percentage: Math.round((stats.value.inProgress || 0) / total * 100), color: '#E6A23C' },
    { name: '待开始', value: stats.value.pending || 0, percentage: Math.round((stats.value.pending || 0) / total * 100), color: '#909399' },
  ]
})

const priorityList = computed(() => {
  const total = stats.value.total || 1
  return [
    { name: '紧急', value: stats.value.urgent || 0, percentage: Math.round((stats.value.urgent || 0) / total * 100), color: '#F56C6C' },
    { name: '高', value: stats.value.high || 0, percentage: Math.round((stats.value.high || 0) / total * 100), color: '#E6A23C' },
    { name: '中', value: stats.value.medium || 0, percentage: Math.round((stats.value.medium || 0) / total * 100), color: '#409EFF' },
    { name: '低', value: stats.value.low || 0, percentage: Math.round((stats.value.low || 0) / total * 100), color: '#909399' },
  ]
})

const statusTagType = (s) => ({ COMPLETED: 'success', IN_PROGRESS: 'warning', PENDING: 'info', CONFIRMED: 'success' }[s] || 'info')
const priorityTagType = (p) => ({ URGENT: 'danger', HIGH: 'warning', MEDIUM: 'primary', LOW: 'info' }[p] || 'info')

const loadData = async () => {
  try {
    const res = await request.get('/ai/project-analysis')
    if (res.data) {
      const ov = res.data.overview || {}
      const pa = res.data.priorityAnalysis || []
      const findP = (p) => pa.find(x => x.priority === p)?.total || 0
      stats.value = {
        total: ov.totalTasks || 0,
        completed: ov.completedTasks || 0,
        inProgress: ov.inProgressTasks || 0,
        pending: ov.pendingTasks || 0,
        urgent: findP('URGENT'),
        high: findP('HIGH'),
        medium: findP('MEDIUM'),
        low: findP('LOW'),
      }
    }
  } catch (e) { /* ignore */ }

  try {
    const res2 = await request.get('/task/list')
    const raw = res2.data || []
    tasks.value = raw.map(t => ({
      name: t.NAME || t.name,
      status: t.STATUS || t.status,
      priority: t.PRIORITY || t.priority,
      end_time: t.END_TIME || t.end_time || t.end_Time,
    }))
  } catch (e) { /* ignore */ }
}

onMounted(loadData)
</script>

<style scoped>
.stat-card { text-align: center; }
.stat-label { font-size: 14px; color: #909399; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: bold; }
</style>