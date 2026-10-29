<template>
  <el-card v-loading="loading" class="directory-card">
    <template #header>
      <div class="directory-header">
        <div>
          <div class="eyebrow">ORGANIZATION DIRECTORY</div>
          <h2>人员管理</h2>
          <p>查看全员资料与组织关系。管理员可调整职级、部门、团队及直接上级。</p>
        </div>
        <div class="header-actions">
          <el-tag :type="isAdmin ? 'warning' : 'info'" effect="plain">{{ isAdmin ? 'ADMIN 可编辑' : 'FOUNDER 只读' }}</el-tag>
          <el-button :loading="loading" @click="loadUsers">刷新</el-button>
        </div>
      </div>
    </template>

    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon class="load-alert" />

    <el-tabs v-model="activeTab" class="directory-tabs">
      <el-tab-pane label="人员总表" name="directory">
        <div class="filters">
          <el-input v-model="keyword" placeholder="搜索姓名、账号、邮箱或手机号" clearable aria-label="搜索人员" />
          <el-select v-model="filterRole" placeholder="全部职级" clearable aria-label="按职级筛选">
            <el-option v-for="(name, role) in roleMap" :key="role" :label="name" :value="role" />
          </el-select>
          <span class="result-count">共 {{ filteredUsers.length }} 人</span>
        </div>

        <el-table :data="filteredUsers" stripe row-key="id" empty-text="暂无符合条件的人员" style="width: 100%">
          <el-table-column prop="real_name" label="姓名" min-width="110" fixed />
          <el-table-column prop="username" label="用户名" min-width="140" />
          <el-table-column label="职级" min-width="120"><template #default="{ row }">{{ roleMap[row.role] || row.role }}</template></el-table-column>
          <el-table-column prop="department_name" label="部门" min-width="120" />
          <el-table-column prop="team_name" label="团队" min-width="120" />
          <el-table-column label="直接上级" min-width="140"><template #default="{ row }">{{ relationNames(row.superiors, 'superior_name') }}</template></el-table-column>
          <el-table-column label="直接下属" min-width="180"><template #default="{ row }">{{ relationNames(row.subordinates, 'subordinate_name') }}</template></el-table-column>
          <el-table-column prop="phone" label="手机号" min-width="140" />
          <el-table-column prop="email" label="邮箱" min-width="200" />
          <el-table-column label="账号状态" min-width="110"><template #default="{ row }">{{ statusNames[row.approval_status] || row.approval_status }}</template></el-table-column>
          <el-table-column label="操作" :width="isAdmin ? 190 : 110" fixed="right">
            <template #default="{ row }"><el-button link type="primary" @click="detail = row">全部资料</el-button><el-button v-if="isAdmin" link type="primary" @click="startEdit(row)">调整关系</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="组织视图" name="organization">
        <div class="organization-intro">
          <div><h3>部门与团队</h3><p>按组织结构浏览成员及上下级关系。</p></div>
          <span class="result-count">共 {{ users.length }} 人</span>
        </div>
        <el-empty v-if="!organizationDepartments.length && !unassignedUsers.length" description="暂无组织资料" />
        <div v-else class="organization-grid">
          <article v-for="department in organizationDepartments" :key="department.id" class="department-card">
            <div class="department-heading">
              <div><span class="card-kicker">DEPARTMENT</span><h3>{{ department.name }}</h3></div>
              <el-tag effect="plain">{{ departmentMemberCount(department) }} 人</el-tag>
            </div>
            <div v-if="department.teams.length" class="team-list">
              <section v-for="team in department.teams" :key="team.id" class="team-card">
                <div class="team-heading"><strong>{{ team.name }}</strong><span>{{ team.members.length }} 人</span></div>
                <div v-if="team.members.length" class="member-list">
                  <div v-for="member in team.members" :key="member.id" class="member-row">
                    <span class="member-avatar">{{ memberInitial(member) }}</span>
                    <div class="member-copy"><strong>{{ member.real_name || member.username }}</strong><span>{{ roleMap[member.role] || member.role }} · {{ relationSummary(member) }}</span></div>
                    <el-button link type="primary" @click="detail = member">查看</el-button>
                  </div>
                </div>
                <span v-else class="empty-team">暂无成员</span>
              </section>
            </div>
            <section v-if="department.members.length" class="team-card unassigned-card">
              <div class="team-heading"><strong>未分配团队</strong><span>{{ department.members.length }} 人</span></div>
              <div class="member-list">
                <div v-for="member in department.members" :key="member.id" class="member-row">
                  <span class="member-avatar">{{ memberInitial(member) }}</span>
                  <div class="member-copy"><strong>{{ member.real_name || member.username }}</strong><span>{{ roleMap[member.role] || member.role }} · {{ relationSummary(member) }}</span></div>
                  <el-button link type="primary" @click="detail = member">查看</el-button>
                </div>
              </div>
            </section>
          </article>

          <article v-if="unassignedUsers.length" class="department-card unassigned-card">
            <div class="department-heading"><div><span class="card-kicker">ORGANIZATION</span><h3>未分配部门</h3></div><el-tag effect="plain">{{ unassignedUsers.length }} 人</el-tag></div>
            <div class="member-list">
              <div v-for="member in unassignedUsers" :key="member.id" class="member-row">
                <span class="member-avatar">{{ memberInitial(member) }}</span>
                <div class="member-copy"><strong>{{ member.real_name || member.username }}</strong><span>{{ roleMap[member.role] || member.role }} · {{ relationSummary(member) }}</span></div>
                <el-button link type="primary" @click="detail = member">查看</el-button>
              </div>
            </div>
          </article>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog :model-value="!!detail" title="人员全部资料" width="min(800px, 94vw)" @close="detail = null">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item v-for="(label, key) in fieldLabels" :key="key" :label="label">{{ displayField(key, detail[key]) }}</el-descriptions-item>
        <el-descriptions-item label="直接上级">{{ relationNames(detail.superiors, 'superior_name') }}</el-descriptions-item>
        <el-descriptions-item label="直接下属">{{ relationNames(detail.subordinates, 'subordinate_name') }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="editVisible" title="调整职级与人员关系" width="min(540px, 94vw)" :close-on-click-modal="!saving" :show-close="!saving" :close-on-press-escape="!saving">
      <p>{{ selected?.real_name || selected?.username }} · 保存后在各端下一次请求时生效。</p>
      <el-form label-position="top" :model="form" :disabled="saving">
        <el-form-item label="职级"><el-select v-model="form.role" @change="form.superiorId = null"><el-option v-for="(name, role) in roleMap" :key="role" :label="name" :value="role" /></el-select></el-form-item>
        <el-form-item label="部门"><el-select v-model="form.departmentId" clearable @change="form.teamId = null"><el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" /></el-select></el-form-item>
        <el-form-item label="团队"><el-select v-model="form.teamId" clearable :disabled="!form.departmentId"><el-option v-for="t in availableTeams" :key="t.id" :label="t.name" :value="t.id" /></el-select></el-form-item>
        <el-form-item label="直接上级"><el-select v-model="form.superiorId" filterable clearable placeholder="未配置上级"><el-option v-for="u in availableSuperiors" :key="u.id" :label="`${u.real_name || u.username} · ${roleMap[u.role]}`" :value="u.id" /></el-select></el-form-item>
      </el-form>
      <el-alert title="普通职级需有对应团队长、部门负责人或创始人上级，才能提交任务申请。" type="info" :closable="false" />
      <template #footer><el-button :disabled="saving" @click="editVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveEmployment">保存调整</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted, onActivated } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/userStore'
import request from '@/utils/request'
import { roleMap } from '@/utils/roleConstants'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')
const users = ref([]), departments = ref([]), teams = ref([])
const loading = ref(false), saving = ref(false), loadError = ref('')
const activeTab = ref('directory'), keyword = ref(''), filterRole = ref('')
const detail = ref(null), selected = ref(null), editVisible = ref(false)
const form = ref({ role: '', departmentId: null, teamId: null, superiorId: null })
const statusNames = { APPROVED: '已批准', PENDING: '待审批', REJECTED: '已拒绝' }
const ranks = ['ADMIN', 'FOUNDER', 'DEPT_HEAD', 'TEAM_LEADER', 'EMPLOYEE']
const fieldLabels = { id: '人员 ID', username: '用户名', real_name: '真实姓名', role: '职级', department_id: '部门 ID', department_name: '部门', team_id: '团队 ID', team_name: '团队', email: '邮箱', phone: '手机号', id_card: '身份证号', birthday: '生日', last_birthday_change: '上次生日变更', avatar: '头像地址', approval_status: '账号状态', theme_mode: '主题模式', manual_theme: '手动主题', show_completed_tasks: '显示已完成任务', show_yesterday_log: '显示昨日日志', created_at: '创建时间', updated_at: '更新时间' }
const displayField = (key, value) => value == null || value === '' ? '—' : key === 'role' ? roleMap[value] || value : key === 'approval_status' ? statusNames[value] || value : value
const relationNames = (rows, key) => rows?.map(r => r[key]).filter(Boolean).join('、') || '未配置'
const relationSummary = user => user.superiors?.length ? `上级：${relationNames(user.superiors, 'superior_name')}` : '未配置上级'
const memberInitial = user => (user.real_name || user.username || '?').slice(0, 1).toUpperCase()
const filteredUsers = computed(() => users.value.filter(u => (!filterRole.value || u.role === filterRole.value) && [u.real_name, u.username, u.email, u.phone].some(v => String(v || '').toLowerCase().includes(keyword.value.trim().toLowerCase()))))
const availableTeams = computed(() => teams.value.filter(t => String(t.department_id) === String(form.value.departmentId)))
const availableSuperiors = computed(() => users.value.filter(u => u.id !== selected.value?.id && u.approval_status === 'APPROVED' && ranks.indexOf(u.role) < ranks.indexOf(form.value.role)))
const organizationDepartments = computed(() => departments.value.map(department => ({
  ...department,
  teams: teams.value.filter(team => String(team.department_id) === String(department.id)).map(team => ({
    ...team,
    members: users.value.filter(user => String(user.department_id) === String(department.id) && String(user.team_id) === String(team.id))
  })),
  members: users.value.filter(user => String(user.department_id) === String(department.id) && (user.team_id == null || user.team_id === ''))
})).filter(department => department.teams.length || department.members.length || users.value.some(user => String(user.department_id) === String(department.id))))
const unassignedUsers = computed(() => users.value.filter(user => user.department_id == null || user.department_id === ''))
const departmentMemberCount = department => department.teams.reduce((total, team) => total + team.members.length, 0) + department.members.length

const loadUsers = async () => {
  if (loading.value) return
  loading.value = true; loadError.value = ''
  try {
    const [directory, options] = await Promise.all([request.get('/user/directory'), request.get('/user/directory/options')])
    users.value = directory.data || []; departments.value = options.data?.departments || []; teams.value = options.data?.teams || []
  } catch (e) { loadError.value = '人员资料加载失败，请点击刷新重试。' }
  finally { loading.value = false }
}
const startEdit = row => {
  if (!isAdmin.value) return
  selected.value = row
  form.value = { role: row.role, departmentId: row.department_id, teamId: row.team_id, superiorId: row.superiors?.[0]?.superior_id || null }
  editVisible.value = true
}
const saveEmployment = async () => {
  if (!isAdmin.value || saving.value) return
  saving.value = true
  try {
    await request.put(`/user/${selected.value.id}/employment`, { ...form.value })
    ElMessage.success('人员关系已更新'); editVisible.value = false; await loadUsers()
  } catch (e) { /* request interceptor displays the server validation message */ }
  finally { saving.value = false }
}

onMounted(loadUsers)
onActivated(loadUsers)
</script>

<style scoped>
.directory-card { border: 1px solid var(--el-border-color-lighter); border-radius: 14px; }
.directory-header, .organization-intro, .department-heading, .team-heading, .header-actions { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.directory-header { align-items: flex-start; }
.header-actions { flex-shrink: 0; }
.eyebrow, .card-kicker { color: var(--el-color-primary); font-size: 11px; font-weight: 700; letter-spacing: .12em; }
h2, h3, p { margin: 0; }
h2 { margin-top: 4px; font-size: 22px; }
h3 { font-size: 16px; }
p { color: var(--el-text-color-secondary); line-height: 1.6; }
.load-alert { margin-bottom: 16px; }
.filters { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; margin-bottom: 20px; }
.filters .el-input { width: min(360px, 100%); }
.filters .el-select { width: 180px; }
.result-count { color: var(--el-text-color-secondary); font-size: 13px; }
.organization-intro { margin-bottom: 18px; }
.organization-intro p { margin-top: 4px; }
.organization-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 16px; }
.department-card, .team-card { border: 1px solid var(--el-border-color-lighter); border-radius: 12px; background: var(--el-fill-color-blank); }
.department-card { padding: 18px; box-shadow: 0 8px 22px rgba(60, 44, 28, .06); }
.department-heading { align-items: flex-start; padding-bottom: 14px; border-bottom: 1px solid var(--el-border-color-lighter); }
.team-list { display: grid; gap: 12px; padding-top: 14px; }
.team-card { padding: 12px; background: var(--el-fill-color-light); }
.team-heading { font-size: 13px; }
.team-heading span { color: var(--el-text-color-secondary); font-size: 12px; }
.member-list { display: grid; gap: 8px; margin-top: 10px; }
.member-row { display: flex; align-items: center; gap: 10px; min-width: 0; padding: 8px; border-radius: 8px; background: var(--el-bg-color); }
.member-avatar { display: inline-flex; align-items: center; justify-content: center; flex: 0 0 30px; width: 30px; height: 30px; border-radius: 50%; color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-size: 13px; font-weight: 700; }
.member-copy { display: grid; min-width: 0; flex: 1; gap: 2px; }
.member-copy strong, .member-copy span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.member-copy span, .empty-team { color: var(--el-text-color-secondary); font-size: 12px; }
.empty-team { display: block; padding: 10px 0 2px; }
.unassigned-card { background: var(--el-fill-color-extra-light); }
.el-form .el-select { width: 100%; }
@media (max-width: 720px) { .directory-header, .organization-intro { align-items: flex-start; flex-direction: column; } .header-actions { width: 100%; justify-content: flex-end; } }
</style>
