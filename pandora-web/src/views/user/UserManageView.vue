<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>人员管理</span>
          <div>
            <el-select v-model="filterRole" placeholder="按角色筛选" clearable size="small" style="width: 140px; margin-right: 8px;" @change="loadUsers">
              <el-option label="管理员" value="ADMIN" />
              <el-option label="创始人" value="FOUNDER" />
              <el-option label="部门负责人" value="DEPT_HEAD" />
              <el-option label="团队负责人" value="TEAM_LEADER" />
              <el-option label="员工" value="EMPLOYEE" />
            </el-select>
            <el-button type="primary" plain size="small" @click="loadUsers" :loading="loading">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="users" v-loading="loading" border style="width: 100%">
        <el-table-column prop="ID" label="ID" width="60" />
        <el-table-column prop="USERNAME" label="用户名" width="160" />
        <el-table-column prop="REAL_NAME" label="真实姓名" width="120" />
        <el-table-column prop="ROLE" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.ROLE)">{{ roleMap[row.ROLE] || row.ROLE }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="DEPARTMENT_ID" label="部门ID" width="80" />
        <el-table-column prop="TEAM_ID" label="团队ID" width="80" />
        <el-table-column prop="EMAIL" label="邮箱" width="200" />
        <el-table-column prop="PHONE" label="手机号" width="140" />
        <el-table-column prop="APPROVAL_STATUS" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.APPROVAL_STATUS === 'PENDING'" type="warning">待审批</el-tag>
            <el-tag v-else-if="row.APPROVAL_STATUS === 'APPROVED'" type="success">已批准</el-tag>
            <el-tag v-else-if="row.APPROVAL_STATUS === 'REJECTED'" type="danger">已拒绝</el-tag>
            <el-tag v-else type="info">{{ row.APPROVAL_STATUS || '正常' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && users.length === 0" description="暂无人员数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import request from '@/utils/request'

const users = ref([])
const loading = ref(false)
const filterRole = ref('')
import { roleMap } from '@/utils/roleConstants'

const roleTagType = (role) => {
  const map = { ADMIN: 'danger', FOUNDER: 'warning', DEPT_HEAD: 'success', TEAM_LEADER: 'primary', EMPLOYEE: 'info' }
  return map[role] || 'info'
}

const loadUsers = async () => {
  loading.value = true
  try {
    const params = {}
    if (filterRole.value) params.role = filterRole.value
    const res = await request.get('/user/list', { params })
    users.value = res.data || []
  } catch (e) { /* handled by interceptor */ }
  loading.value = false
}

onMounted(loadUsers)
onActivated(loadUsers)
</script>
