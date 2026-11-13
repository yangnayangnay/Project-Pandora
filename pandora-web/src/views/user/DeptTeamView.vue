<template>
  <div class="dept-team-view">
    <el-card>
      <template #header><div class="header-row"><div><h2>部门管理</h2><p class="subtitle">维护部门及所属成员，成员较多时可展开查看。</p></div><el-button type="primary" @click="openDepartmentCreate">新增部门</el-button></div></template>
      <el-table :data="departments" border v-loading="loading">
        <el-table-column prop="name" label="部门名称" min-width="180" />
        <el-table-column label="成员" min-width="280"><template #default="{ row }">
          <OrganizationMembers :members="row.members" />
        </template></el-table-column>
        <el-table-column label="操作" width="170" fixed="right"><template #default="{ row }">
          <el-button link type="primary" @click="openDepartmentEdit(row)">编辑</el-button><el-button link type="danger" @click="removeDepartment(row)">删除</el-button>
        </template></el-table-column>
      </el-table>
    </el-card>
    <el-card class="team-card">
      <template #header><div class="header-row"><div><h2>团队管理</h2><p class="subtitle">维护团队名称和所属部门。</p></div><el-button type="primary" @click="openTeamCreate">新增团队</el-button></div></template>
      <el-table :data="teams" border v-loading="loading">
        <el-table-column prop="name" label="团队名称" min-width="180" />
        <el-table-column label="所属部门" min-width="160"><template #default="{ row }">{{ departmentName(row.department_id) }}</template></el-table-column>
        <el-table-column label="成员" min-width="280"><template #default="{ row }">
          <OrganizationMembers :members="row.members" type="success" />
        </template></el-table-column>
        <el-table-column label="操作" width="170" fixed="right"><template #default="{ row }">
          <el-button link type="primary" @click="openTeamEdit(row)">编辑</el-button><el-button link type="danger" @click="removeTeam(row)">删除</el-button>
        </template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="departmentDialogVisible" :title="departmentEditing ? '编辑部门' : '新增部门'" width="480px">
      <el-form @submit.prevent="saveDepartment"><el-form-item label="部门名称" required><el-input v-model="departmentForm.name" maxlength="128" show-word-limit @keyup.enter="saveDepartment" /></el-form-item></el-form>
      <template #footer><el-button @click="departmentDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveDepartment">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="teamDialogVisible" :title="teamEditing ? '编辑团队' : '新增团队'" width="480px">
      <el-form label-width="88px" @submit.prevent="saveTeam">
        <el-form-item label="团队名称" required><el-input v-model="teamForm.name" maxlength="128" show-word-limit @keyup.enter="saveTeam" /></el-form-item>
        <el-form-item label="所属部门" required>
          <el-select v-model="teamForm.departmentId" placeholder="请选择所属部门" style="width: 100%">
            <el-option v-for="department in departments" :key="department.id" :label="department.name" :value="department.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="teamDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveTeam">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onActivated, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import OrganizationMembers from '@/components/OrganizationMembers.vue'
import { normalizeOrganization, departmentMembers, teamMembers } from '@/utils/organization'

const departments = ref([]), teams = ref([]), users = ref([])
const loading = ref(false), saving = ref(false)
const departmentDialogVisible = ref(false), departmentEditing = ref(null), departmentForm = ref({ name: '' })
const teamDialogVisible = ref(false), teamEditing = ref(null), teamForm = ref({ name: '', departmentId: null })
const departmentById = computed(() => new Map(departments.value.map(d => [String(d.id), d.name])))
const departmentName = id => departmentById.value.get(String(id)) || '未分配部门'

async function load() {
  if (loading.value) return
  loading.value = true
  try {
    const [orgRes, userRes] = await Promise.all([request.get('/user/org-options'), request.get('/user/list')])
    const org = orgRes.data || {}; users.value = (userRes.data || []).map(normalizeOrganization)
    departments.value = (org.departments || []).map(d => ({ ...d, members: departmentMembers(users.value, d.id) }))
    teams.value = (org.teams || []).map(t => ({ ...t, members: teamMembers(users.value, t.id) }))
  } catch (error) { ElMessage.error('部门数据加载失败，请确认后端服务已启动') }
  finally { loading.value = false }
}
function openDepartmentCreate() { departmentEditing.value = null; departmentForm.value = { name: '' }; departmentDialogVisible.value = true }
function openDepartmentEdit(row) { departmentEditing.value = row; departmentForm.value = { name: row.name }; departmentDialogVisible.value = true }
async function saveDepartment() {
  const name = departmentForm.value.name.trim(); if (!name) return ElMessage.warning('请输入部门名称')
  saving.value = true
  try {
    if (departmentEditing.value) await request.put(`/user/departments/${departmentEditing.value.id}`, { name })
    else await request.post('/user/departments', { name })
    ElMessage.success(departmentEditing.value ? '部门已更新' : '部门已创建'); departmentDialogVisible.value = false; await load()
  } finally { saving.value = false }
}
async function removeDepartment(row) {
  try { await ElMessageBox.confirm(`确定删除部门“${row.name}”吗？`, '删除部门', { type: 'warning' }); await request.delete(`/user/departments/${row.id}`); ElMessage.success('部门已删除'); await load() }
  catch (error) { if (error !== 'cancel' && error !== 'close') throw error }
}
function openTeamCreate() {
  teamEditing.value = null
  teamForm.value = { name: '', departmentId: departments.value.length === 1 ? departments.value[0].id : null }
  teamDialogVisible.value = true
}
function openTeamEdit(row) {
  teamEditing.value = row
  teamForm.value = { name: row.name, departmentId: row.department_id }
  teamDialogVisible.value = true
}
async function saveTeam() {
  const name = teamForm.value.name.trim()
  if (!name) return ElMessage.warning('请输入团队名称')
  if (!teamForm.value.departmentId) return ElMessage.warning('请选择所属部门')
  saving.value = true
  try {
    const payload = { name, departmentId: teamForm.value.departmentId }
    if (teamEditing.value) await request.put(`/user/teams/${teamEditing.value.id}`, payload)
    else await request.post('/user/teams', payload)
    ElMessage.success(teamEditing.value ? '团队已更新' : '团队已创建')
    teamDialogVisible.value = false
    await load()
  } finally { saving.value = false }
}
async function removeTeam(row) {
  try {
    await ElMessageBox.confirm(`确定删除团队“${row.name}”吗？`, '删除团队', { type: 'warning' })
    await request.delete(`/user/teams/${row.id}`)
    ElMessage.success('团队已删除')
    await load()
  } catch (error) { if (error !== 'cancel' && error !== 'close') throw error }
}
onMounted(load)
onActivated(load)
</script>

<style scoped>
.header-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.team-card { margin-top: 20px; }
.subtitle { margin: 6px 0 0; color: var(--el-text-color-secondary); font-size: 13px; font-weight: 400; }
.muted { color: var(--el-text-color-secondary); }
</style>
