<template>
  <div class="hierarchy-relation-view">
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
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getRelations, getTree } from '@/api/hierarchy'

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
      const data = response.data || response
      if (data && data.code === 200 && data.data) {
        roots.value = data.data.roots || []
        // TASK-R18-021: 孤儿分组取值改为 orphannodes || orphanNodes 双取
        // lowercaseKeys 将驼峰 orphanNodes 转换后为 orphannodes，原三重兜底全部命不中
        orphanNodes.value = data.data.orphannodes || data.data.orphanNodes || []
        // TASK-R18-021: 渲染前对 roots 按 role 等级排序（展示端兜底）
        sortNodesByRoleRank(roots.value)
      } else if (data && data.data) {
        // 兼容直接返回数组的情况
        roots.value = Array.isArray(data.data) ? data.data : (data.data.roots || [])
        orphanNodes.value = Array.isArray(data.data) ? [] : (data.data.orphannodes || data.data.orphanNodes || [])
        sortNodesByRoleRank(roots.value)
      }
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
      const data = response.data || response
      if (data && data.code === 200 && data.data) {
        relations.value = data.data
      } else if (data && data.data) {
        relations.value = data.data
      }
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
})
</script>

<style scoped>
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