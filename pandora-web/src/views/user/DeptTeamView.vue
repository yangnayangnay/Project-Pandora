<template>
  <div class="dept-team-view">
    <el-card>
      <template #header>部门管理</template>
      <el-table :data="departments" border>
        <el-table-column prop="name" label="部门名称" />
        <el-table-column prop="head_user_id" label="负责人ID" />
        <el-table-column prop="parent_id" label="上级部门ID" />
      </el-table>
    </el-card>
    <el-card style="margin-top: 20px">
      <template #header>团队管理</template>
      <el-table :data="teams" border>
        <el-table-column prop="name" label="团队名称" />
        <el-table-column prop="leader_user_id" label="团队长ID" />
        <el-table-column prop="department_id" label="所属部门ID" />
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
    const deptRes = await request.get('/user/list', { params: { type: 'dept' } })
    departments.value = deptRes.data.data || []
    const teamRes = await request.get('/user/list', { params: { type: 'team' } })
    teams.value = teamRes.data.data || []
  } catch (e) {
    console.error('加载部门团队失败', e)
  }
})
</script>