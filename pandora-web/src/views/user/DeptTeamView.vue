<template>
  <div class="dept-team-view">
    <el-card>
      <template #header>部门管理</template>
      <el-table :data="departments" border>
        <el-table-column prop="name" label="部门名称" min-width="120" />
        <el-table-column label="成员" min-width="260">
          <template #default="{ row }">
            <template v-if="row.members && row.members.length">
              <el-tag v-for="m in row.members" :key="m.id" size="small" style="margin: 2px">
                {{ (m.real_name || m.realName || m.username) + '（' + m.role + '）' }}
              </el-tag>
            </template>
            <span v-else style="color: #999">暂无成员</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-card style="margin-top: 20px">
      <template #header>团队管理</template>
      <el-table :data="teams" border>
        <el-table-column prop="name" label="团队名称" min-width="120" />
        <el-table-column label="成员" min-width="260">
          <template #default="{ row }">
            <template v-if="row.members && row.members.length">
              <el-tag v-for="m in row.members" :key="m.id" size="small" type="success" style="margin: 2px">
                {{ (m.real_name || m.realName || m.username) + '（' + m.role + '）' }}
              </el-tag>
            </template>
            <span v-else style="color: #999">暂无成员</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const departments = ref([])
const teams = ref([])

onMounted(async () => {
  try {
    // R40 修复：原 /user/list?type=dept 参数后端不存在（数据源坏损）。
    // 改用 R40 新端点 /user/org-options（departments/teams 权威数据）+ /user/list 全员按归属分组。
    const orgRes = await request.get('/user/org-options')
    const orgData = orgRes.data || {}
    const deptList = orgData.departments || []
    const teamList = orgData.teams || []
    const userRes = await request.get('/user/list')
    const users = userRes.data || []
    // 部门视图：部门 + 成员（department_id 匹配）
    departments.value = deptList.map(d => ({
      ...d,
      members: users.filter(u => u.department_id === d.id)
    }))
    // 团队视图：团队 + 成员（team_id 匹配）
    teams.value = teamList.map(t => ({
      ...t,
      members: users.filter(u => u.team_id === t.id)
    }))
  } catch (e) {
    console.error('加载部门团队失败', e)
  }
})
</script>