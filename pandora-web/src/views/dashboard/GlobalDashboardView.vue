<template>
  <div>
    <h2>全局看板</h2>
    <el-row :gutter="20" v-loading="loading">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>任务总数</template>
          <div class="stat-number">{{ stats.taskCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>日志总数</template>
          <div class="stat-number">{{ stats.logCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>人员总数</template>
          <div class="stat-number">{{ stats.userCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>待审批账号</template>
          <el-table :data="pendingUsers" border size="small" style="width: 100%">
            <el-table-column prop="USERNAME" label="用户名" />
            <el-table-column prop="ROLE" label="角色" width="100">
              <template #default="{ row }">
                {{ roleMap[row.ROLE] || row.ROLE }}
              </template>
            </el-table-column>
            <el-table-column prop="CREATED_AT" label="注册时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.CREATED_AT) }}
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="pendingUsers.length === 0" description="暂无待审批" :image-size="60" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>最近任务</template>
          <el-table :data="recentTasks" border size="small" style="width: 100%">
            <el-table-column prop="NAME" label="任务名称" />
            <el-table-column prop="STATUS" label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.STATUS)">{{ statusMap[row.STATUS] || row.STATUS }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="recentTasks.length === 0" description="暂无任务" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'

const loading = ref(false)
const stats = reactive({ taskCount: 0, logCount: 0, userCount: 0 })
const pendingUsers = ref([])
const recentTasks = ref([])
import { roleMap } from '@/utils/roleConstants'
const statusMap = { PENDING: '待处理', IN_PROGRESS: '进行中', COMPLETED: '已完成', CONFIRMED: '已确认' }

const statusTagType = (s) => {
  const map = { PENDING: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success', CONFIRMED: 'success' }
  return map[s] || 'info'
}

const formatTime = (t) => {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

const loadStats = async () => {
  loading.value = true
  try {
    const [tasks, logs, users, pending] = await Promise.all([
      request.get('/task/list').catch(() => ({ data: [] })),
      request.get('/log/list').catch(() => ({ data: [] })),
      request.get('/user/list').catch(() => ({ data: [] })),
      request.get('/approval/pending').catch(() => ({ data: [] }))
    ])
    stats.taskCount = tasks.data?.length || 0
    stats.logCount = logs.data?.length || 0
    stats.userCount = users.data?.length || 0
    pendingUsers.value = pending.data || []
    recentTasks.value = (tasks.data || []).slice(0, 10)
  } catch (e) { /* handled by interceptor */ }
  loading.value = false
}

onMounted(loadStats)
</script>

<style scoped>
.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #409EFF;
  text-align: center;
  padding: 10px 0;
}
</style>
