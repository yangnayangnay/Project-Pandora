<template>
  <div class="hierarchy-relation-view">
    <!-- R38: 添加人员关系（服务端设置入口） -->
    <el-card class="add-card" shadow="never">
      <template #header>
        <span class="card-title">添加人员关系</span>
      </template>
      <el-form inline>
        <el-form-item label="上级">
          <el-select v-model="addForm.superiorId" placeholder="选择上级" filterable style="width: 200px">
            <el-option v-for="u in userOptions" :key="u.id" :value="u.id"
                       :label="(u.real_name || u.realName || u.username) + '（' + u.role + '）'" />
          </el-select>
        </el-form-item>
        <el-form-item label="下属">
          <el-select v-model="addForm.subordinateId" placeholder="选择下属" filterable style="width: 200px">
            <el-option v-for="u in userOptions" :key="u.id" :value="u.id"
                       :label="(u.real_name || u.realName || u.username) + '（' + u.role + '）'" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="addForm.forceReplace">下属已有上级时替换</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="adding" @click="handleAddRelation">添加关系</el-button>
        </el-form-item>
      </el-form>
      <div v-if="addError" class="error-text">{{ addError }}</div>
    </el-card>

    <el-card class="tree-card" shadow="never">
      <template #header>
        <span class="card-title">人员关系树</span>
      </template>
      <div v-if="treeLoading" class="loading-placeholder">加载中...</div>
      <div v-else-if="roots.length === 0 && orphanNodes.length === 0" class="empty-placeholder">
        暂无人员关系
      </div>
      <div v-else>
        <ul class="tree-list">
          <li v-for="root in roots" :key="root.id || root.ID" class="tree-node root-node">
            <span class="node-name">{{ getDisplayName(root) }}</span>
            <span class="node-role">{{ getDisplayRole(root) }}</span>
            <ul v-if="root.children && root.children.length > 0" class="tree-children">
              <li v-for="child in root.children" :key="child.id" class="tree-node">
                <tree-node :node="child" />
              </li>
            </ul>
          </li>
        </ul>
        <div v-if="orphanNodes.length > 0" class="orphan-section">
          <div class="orphan-title">⚠️ 孤儿节点（上级已删除）</div>
          <ul class="orphan-list">
            <li v-for="orphan in orphanNodes" :key="orphan.id || orphan.ID" class="orphan-node">
              <span class="node-name">{{ getDisplayName(orphan) }}</span>
              <span class="node-role">{{ getDisplayRole(orphan) }}</span>
            </li>
          </ul>
        </div>
      </div>
    </el-card>

    <el-card class="relation-card" shadow="never">
      <template #header>
        <span class="card-title">人员关系列表</span>
      </template>
      <div v-if="relationLoading" class="loading-placeholder">加载中...</div>
      <div v-else-if="relations.length === 0" class="empty-placeholder">
        暂无人员关系
      </div>
      <el-table v-else :data="relations" border stripe>
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="上级姓名" prop="sup_name" min-width="120">
          <template #default="{ row }">{{ row.sup_name || '' }}</template>
        </el-table-column>
        <el-table-column label="上级角色" prop="sup_role" min-width="100">
          <template #default="{ row }">{{ row.sup_role || '' }}</template>
        </el-table-column>
        <el-table-column label="下属姓名" prop="sub_name" min-width="120">
          <template #default="{ row }">{{ row.sub_name || '' }}</template>
        </el-table-column>
        <el-table-column label="下属角色" prop="sub_role" min-width="100">
          <template #default="{ row }">{{ row.sub_role || '' }}</template>
        </el-table-column>
        <!-- R38: 删除关系操作列；R40: 加一步式调动 -->
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openTransfer(row)">调动</el-button>
            <el-popconfirm title="确认删除该上下级关系？" @confirm="handleRemoveRelation(row)">
              <template #reference>
                <el-button type="danger" size="small" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- R40 REQ-40-C: 一步式调动对话框（选新上级→addRelation forceReplace） -->
    <el-dialog v-model="transferVisible" title="人员调动" width="420px">
      <div v-if="transferTarget" class="transfer-info">
        将 <b>{{ transferTarget.sub_name }}</b> 从
        <b>{{ transferTarget.sup_name || '（无上级）' }}</b> 调往新上级：
      </div>
      <el-select v-model="transferNewSuperiorId" placeholder="选择新上级" filterable style="width: 100%; margin-top: 12px">
        <el-option v-for="u in transferCandidates" :key="u.id" :value="u.id"
                   :label="(u.real_name || u.realName || u.username) + '（' + u.role + '）'" />
      </el-select>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :loading="transferring" @click="handleTransfer">确认调动</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRelations, getTree, addRelation, removeRelation } from '@/api/hierarchy'
import { listUsers } from '@/api/user'

// R38: 添加关系表单与用户下拉
const addForm = reactive({ superiorId: null, subordinateId: null, forceReplace: false })
const userOptions = ref([])
const adding = ref(false)
const addError = ref('')

function loadUserOptions() {
  // 拦截器返回 lowercaseKeys(ApiResponse)：response.data 即用户数组（对齐 AllTasksView 用法）
  listUsers()
    .then(response => {
      const d = response.data || response
      userOptions.value = Array.isArray(d) ? d : (d && d.data) || []
    })
    .catch(() => {
      addError.value = '用户列表加载失败，无法添加关系'
    })
}

function handleAddRelation() {
  addError.value = ''
  if (!addForm.superiorId || !addForm.subordinateId) {
    addError.value = '请先选择上级与下属'
    return
  }
  if (addForm.superiorId === addForm.subordinateId) {
    addError.value = '上级与下属不能是同一人'
    return
  }
  adding.value = true
  addRelation(addForm.superiorId, addForm.subordinateId, addForm.forceReplace)
    .then(() => {
      adding.value = false
      ElMessage.success('关系添加成功')
      addForm.superiorId = null
      addForm.subordinateId = null
      addForm.forceReplace = false
      loadTree()
      loadRelations()
    })
    .catch(err => {
      adding.value = false
      const resp = err && err.response ? err.response.data : null
      const detail = (resp && resp.message) ? `后端 ${resp.code || err.response.status}: ${resp.message}`
        : (`请求失败（HTTP ${err && err.response ? err.response.status : '网络'}）`)
      addError.value = detail
    })
}

// R40 REQ-40-C: 一步式调动（转移上级）
const transferVisible = ref(false)
const transferTarget = ref(null)
const transferNewSuperiorId = ref(null)
const transferring = ref(false)
const transferCandidates = ref([])

function openTransfer(row) {
  transferTarget.value = row
  transferNewSuperiorId.value = null
  // 候选=全体用户中排除本人与当前上级
  transferCandidates.value = (userOptions.value || []).filter(u =>
    u.id !== (row.subordinate_id || row.subordinateId) && u.id !== (row.superior_id || row.superiorId))
  transferVisible.value = true
}

function handleTransfer() {
  const subId = transferTarget.value && (transferTarget.value.subordinate_id || transferTarget.value.subordinateId)
  if (!transferNewSuperiorId.value || !subId) {
    ElMessage.warning('请先选择新上级')
    return
  }
  if (transferNewSuperiorId.value === subId) {
    ElMessage.warning('新上级不能是被调动人本人')
    return
  }
  transferring.value = true
  addRelation(transferNewSuperiorId.value, subId, true)
    .then(response => {
      transferring.value = false
      const d = response.data || response
      // R40 坑修正：后端 forceReplace 拒绝时返回 success:false（HTTP 200），须显式判定
      if (d && d.success === false) {
        ElMessage.error((d.message || '调动被拒绝（可能形成环或层级非法）'))
        return
      }
      ElMessage.success('调动成功')
      transferVisible.value = false
      loadTree()
      loadRelations()
    })
    .catch(err => {
      transferring.value = false
      const resp = err && err.response ? err.response.data : null
      ElMessage.error((resp && resp.message) ? '调动失败: ' + resp.message : '调动失败，请重试')
    })
}

function handleRemoveRelation(row) {
  removeRelation(row.superior_id || row.superiorId, row.subordinate_id || row.subordinateId)
    .then(() => {
      ElMessage.success('关系已删除')
      loadTree()
      loadRelations()
    })
    .catch(err => {
      const resp = err && err.response ? err.response.data : null
      ElMessage.error((resp && resp.message) ? `删除失败: ${resp.message}` : '删除失败，请重试')
    })
}

const roots = ref([])
const orphanNodes = ref([])
const relations = ref([])
const treeLoading = ref(true)
const relationLoading = ref(true)

function getDisplayName(node) {
  // TASK-R18-021: lowercaseKeys 将驼峰 realName 转为 realname，
  // TASK-R18-004 后端追加 real_name 后双命中，三重兜底 + '未知'
  const realName = node.realName || node.realname || node.real_name || node.REAL_NAME
  const username = node.username || node.USERNAME
  return realName || username || '未知'
}

function getDisplayRole(node) {
  const role = node.role || node.ROLE
  return role ? `(${role})` : ''
}

// TASK-R18-021: RoleRank 等级排序（展示端兜底，与 Android 端 RoleRank 序一致）
const ROLE_RANK = { ADMIN: 1, FOUNDER: 2, DEPT_HEAD: 3, TEAM_LEADER: 4, EMPLOYEE: 5 }
function roleRankOf(role) {
  return ROLE_RANK[role] || 99
}
function sortNodesByRoleRank(nodes) {
  if (!Array.isArray(nodes)) return
  nodes.sort((a, b) => {
    const rankA = roleRankOf(a.role || a.ROLE)
    const rankB = roleRankOf(b.role || b.ROLE)
    if (rankA !== rankB) return rankA - rankB
    const idA = a.id || a.ID || 0
    const idB = b.id || b.ID || 0
    return idA - idB
  })
}

function loadTree() {
  treeLoading.value = true
  getTree()
    .then(response => {
      // R38 修复：拦截器已返回 lowercaseKeys(ApiResponse)——response.data 即 {roots, orphannodes}
      // （原写法 data.code===200 判不中 → 空树，同 loadRelations 多层坑）
      // lowercaseKeys 将驼峰 orphanNodes 转换后为 orphannodes（TASK-R18-021）
      const payload = response.data || {}
      roots.value = payload.roots || []
      orphanNodes.value = payload.orphannodes || payload.orphanNodes || []
      sortNodesByRoleRank(roots.value)
    })
    .catch(() => {
      roots.value = []
      orphanNodes.value = []
    })
    .finally(() => {
      treeLoading.value = false
    })
}

function loadRelations() {
  relationLoading.value = true
  getRelations()
    .then(response => {
      // R38 修复：拦截器已返回 lowercaseKeys(ApiResponse)——response.data 即关系数组
      // （原写法 data.code===200 判不中 → 空列表，历轮 res.data.data 多层坑复发）
      relations.value = response.data || []
    })
    .catch(() => {
      relations.value = []
    })
    .finally(() => {
      relationLoading.value = false
    })
}

// 递归树节点组件
const TreeNode = {
  name: 'TreeNode',
  props: ['node'],
  template: `
    <span class="node-name">{{ node.realName || node.realname || node.real_name || '未知' }}</span>
    <span class="node-role">{{ node.role ? '(' + node.role + ')' : '' }}</span>
    <ul v-if="node.children && node.children.length > 0" class="tree-children">
      <li v-for="child in node.children" :key="child.id" class="tree-node">
        <tree-node :node="child" />
      </li>
    </ul>
  `
}

onMounted(() => {
  loadTree()
  loadRelations()
  loadUserOptions()
})
</script>

<style scoped>
.hierarchy-relation-view .add-card { margin-bottom: 16px; }
.hierarchy-relation-view .error-text { color: #f56c6c; font-size: 13px; margin-top: 8px; }

.hierarchy-relation-view {
  padding: 16px;
}

.card-title {
  font-weight: 600;
  font-size: 16px;
}

.tree-card {
  margin-bottom: 16px;
}

.tree-list,
.tree-children,
.orphan-list {
  list-style: none;
  padding-left: 20px;
  margin: 0;
}

.tree-node {
  padding: 4px 0;
  position: relative;
}

.tree-node::before {
  content: '├─';
  position: absolute;
  left: -16px;
  color: #c0c4cc;
}

.root-node::before {
  content: '';
}

.node-name {
  font-weight: 500;
  margin-right: 6px;
}

.node-role {
  color: #909399;
  font-size: 13px;
}

.orphan-section {
  margin-top: 16px;
  padding: 12px;
  background: #fdf6ec;
  border-radius: 4px;
}

.orphan-title {
  color: #e6a23c;
  font-weight: 600;
  margin-bottom: 8px;
}

.orphan-node {
  padding: 2px 0;
}

.empty-placeholder,
.loading-placeholder {
  text-align: center;
  color: #909399;
  padding: 24px 0;
}
</style>