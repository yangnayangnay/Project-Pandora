<template>
  <div class="invitation-view">
    <el-table :data="invitations" border>
      <el-table-column prop="content" label="内容" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="created_at" label="创建时间" width="180" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" type="success" size="small" @click="respond(row.id, 'APPROVED')">同意</el-button>
          <el-button v-if="row.status === 'PENDING'" type="danger" size="small" @click="respond(row.id, 'REJECTED')">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listInvitations, respondInvitation } from '@/api/permission'

const invitations = ref([])

onMounted(async () => {
  const res = await listInvitations()
  invitations.value = res.data.data
})

async function respond(id, status) {
  await respondInvitation(id, { status })
  const res = await listInvitations()
  invitations.value = res.data.data
}

function statusType(s) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[s] || 'info'
}
function statusText(s) {
  return { PENDING: '待处理', APPROVED: '已同意', REJECTED: '已拒绝' }[s] || s
}
</script>