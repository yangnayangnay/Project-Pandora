<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>账号审批</span>
          <el-button type="primary" plain @click="loadPending" :loading="loading">刷新</el-button>
        </div>
      </template>

      <el-table :data="pendingList" v-loading="loading" border style="width: 100%">
        <el-table-column prop="ID" label="ID" width="60" />
        <el-table-column prop="USERNAME" label="用户名" width="140" />
        <el-table-column prop="REAL_NAME" label="真实姓名" width="120" />
        <el-table-column prop="EMAIL" label="邮箱" width="180" />
        <el-table-column prop="ID_CARD" label="身份证号" width="180" />
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
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="approve(row.ID)" :loading="acting === row.ID">通过</el-button>
            <el-button type="danger" size="small" @click="reject(row.ID)" :loading="acting === row.ID">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && pendingList.length === 0" description="暂无待审批账号" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const pendingList = ref([])
const loading = ref(false)
const acting = ref(null)
import { roleMap } from '@/utils/roleConstants'

const loadPending = async () => {
  loading.value = true
  try {
    const res = await request.get('/approval/pending')
    pendingList.value = res.data || []
  } catch (e) { /* handled by interceptor */ }
  loading.value = false
}

const approve = async (id) => {
  await ElMessageBox.confirm('确认通过该账号的注册申请？', '审批确认', { type: 'warning' })
  acting.value = id
  try {
    await request.post(`/approval/approve/${id}`)
    ElMessage.success('已批准')
    await loadPending()
  } catch (e) { /* handled by interceptor */ }
  acting.value = null
}

const reject = async (id) => {
  await ElMessageBox.confirm('确认拒绝该账号的注册申请？', '审批确认', { type: 'warning' })
  acting.value = id
  try {
    await request.post(`/approval/reject/${id}`)
    ElMessage.success('已拒绝')
    await loadPending()
  } catch (e) { /* handled by interceptor */ }
  acting.value = null
}

const formatTime = (t) => {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

onMounted(loadPending)
</script>
