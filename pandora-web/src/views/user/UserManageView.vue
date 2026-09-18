<template>
  <div>
    <h2>人员管理</h2>
    <el-table :data="users" border>
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="role" label="角色" />
      <el-table-column prop="department" label="部门" />
    </el-table>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const users = ref([])
</script>