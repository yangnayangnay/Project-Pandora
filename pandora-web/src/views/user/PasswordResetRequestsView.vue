<template>
  <div class="reset-requests-view">
    <el-card>
      <template #header>
        <div class="header-row">
          <div>
            <div class="title">密码重置申请</div>
            <div class="subtitle">处理手机端提交的忘记密码申请。批准后旧会话会立即失效。</div>
          </div>
          <div class="header-actions">
            <el-select v-model="status" style="width: 150px" @change="load">
              <el-option label="待处理" value="PENDING" />
              <el-option label="已批准" value="APPROVED" />
              <el-option label="已拒绝" value="REJECTED" />
              <el-option label="全部申请" value="" />
            </el-select>
            <el-button :loading="loading" @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="records" border v-loading="loading" empty-text="暂无密码重置申请">
        <el-table-column prop="username" label="工号" min-width="110" />
        <el-table-column label="姓名" min-width="120"><template #default="{ row }">{{ row.real_name || '未设置' }}</template></el-table-column>
        <el-table-column prop="request_reason" label="申请说明" min-width="220"><template #default="{ row }">{{ row.request_reason || '未填写' }}</template></el-table-column>
        <el-table-column label="申请时间" min-width="170"><template #default="{ row }">{{ formatTime(row.created_at) }}</template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="处理结果" min-width="180"><template #default="{ row }">
          <span v-if="row.status === 'REJECTED'">{{ row.reject_reason || '已拒绝' }}</span>
          <span v-else-if="row.status === 'APPROVED'">已于 {{ formatTime(row.processed_at) }} 重置</span>
          <span v-else class="muted">等待管理员处理</span>
        </template></el-table-column>
        <el-table-column label="操作" width="170" fixed="right"><template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button link type="primary" @click="openApprove(row)">重置密码</el-button>
            <el-button link type="danger" @click="reject(row)">拒绝</el-button>
          </template>
          <span v-else class="muted">已处理</span>
        </template></el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="approveVisible" title="重置用户密码" width="440px" @closed="clearPasswordForm">
      <el-alert type="warning" :closable="false" show-icon>
        <template #title>将为 {{ current?.real_name || current?.username }}（{{ current?.username }}）设置新密码</template>
      </el-alert>
      <el-form label-position="top" class="password-form" @submit.prevent="approve">
        <el-form-item label="新密码" required>
          <el-input v-model="passwordForm.password" type="password" show-password maxlength="72" autocomplete="new-password" placeholder="至少 6 个字符" />
        </el-form-item>
        <el-form-item label="确认新密码" required>
          <el-input v-model="passwordForm.confirm" type="password" show-password maxlength="72" autocomplete="new-password" placeholder="再次输入新密码" @keyup.enter="approve" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="approve">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const records = ref([]), status = ref('PENDING')
const loading = ref(false), saving = ref(false), approveVisible = ref(false), current = ref(null)
const passwordForm = ref({ password: '', confirm: '' })

const statusText = value => ({ PENDING: '待处理', APPROVED: '已批准', REJECTED: '已拒绝' }[value] || value)
const statusType = value => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[value] || 'info')
const formatTime = value => value ? String(value).replace('T', ' ').slice(0, 19) : '—'

async function load() {
  loading.value = true
  try {
    const response = await request.get('/user/reset-requests', { params: status.value ? { status: status.value } : {} })
    records.value = response.data?.records || []
  } finally { loading.value = false }
}

function openApprove(row) {
  current.value = row
  passwordForm.value = { password: '', confirm: '' }
  approveVisible.value = true
}

function clearPasswordForm() {
  current.value = null
  passwordForm.value = { password: '', confirm: '' }
}

async function approve() {
  const account = current.value
  if (!account) return
  const password = passwordForm.value.password
  if (password.length < 6) return ElMessage.warning('新密码不能少于 6 个字符')
  if (password !== passwordForm.value.confirm) return ElMessage.warning('两次输入的密码不一致')
  saving.value = true
  try {
    const response = await request.post(`/user/reset-requests/${account.id}/approve`, { newPassword: password })
    approveVisible.value = false
    await ElMessageBox.alert(`工号 ${account.username} 的密码已重置为：${response.data?.newpassword || password}\n请通过安全方式告知用户。`, '密码重置成功', {
      confirmButtonText: '我已记录', type: 'success', dangerouslyUseHTMLString: false
    })
    await load()
  } finally { saving.value = false }
}

async function reject(row) {
  try {
    const result = await ElMessageBox.prompt('请填写拒绝原因，用户可据此联系管理员。', '拒绝密码重置申请', {
      confirmButtonText: '确认拒绝', cancelButtonText: '取消', inputPlaceholder: '拒绝原因', inputValidator: value => value?.trim() ? true : '请填写拒绝原因'
    })
    await request.post(`/user/reset-requests/${row.id}/reject`, { reason: result.value.trim() })
    ElMessage.success('申请已拒绝')
    await load()
  } catch (error) { if (error !== 'cancel' && error !== 'close') throw error }
}

onMounted(load)
</script>

<style scoped>
.header-row { display: flex; align-items: center; justify-content: space-between; gap: 20px; }
.header-actions { display: flex; align-items: center; gap: 10px; }
.title { font-size: 20px; font-weight: 600; line-height: 1.4; }
.subtitle { margin-top: 5px; color: var(--el-text-color-secondary); font-size: 13px; }
.muted { color: var(--el-text-color-secondary); }
.password-form { margin-top: 18px; }
</style>
