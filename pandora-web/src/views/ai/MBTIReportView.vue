<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>MBTI报告查看</span>
          <el-button type="primary" plain size="small" @click="loadReports" :loading="loading">刷新</el-button>
        </div>
      </template>

      <el-table :data="reports" v-loading="loading" border style="width: 100%">
        <el-table-column prop="ID" label="ID" width="60" />
        <el-table-column prop="USER_ID" label="用户ID" width="80" />
        <el-table-column prop="PERSONALITY_TYPE" label="性格类型" width="120">
          <template #default="{ row }">
            <el-tag type="primary">{{ row.PERSONALITY_TYPE || row.personalityType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="E_SCORE" label="E得分" width="80" />
        <el-table-column prop="I_SCORE" label="I得分" width="80" />
        <el-table-column prop="S_SCORE" label="S得分" width="80" />
        <el-table-column prop="N_SCORE" label="N得分" width="80" />
        <el-table-column prop="T_SCORE" label="T得分" width="80" />
        <el-table-column prop="F_SCORE" label="F得分" width="80" />
        <el-table-column prop="J_SCORE" label="J得分" width="80" />
        <el-table-column prop="P_SCORE" label="P得分" width="80" />
        <el-table-column prop="CREATED_AT" label="生成时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.CREATED_AT) }}
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && reports.length === 0" description="暂无MBTI报告" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const reports = ref([])
const loading = ref(false)

const formatTime = (t) => {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

const loadReports = async () => {
  loading.value = true
  try {
    const res = await request.get('/ai/mbti/list')
    reports.value = res.data || []
  } catch (e) { /* handled by interceptor */ }
  loading.value = false
}

onMounted(loadReports)
</script>
