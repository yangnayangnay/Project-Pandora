<template>
  <div>
    <h2>任务管理看板</h2>
    <el-table :data="tasks" border>
      <el-table-column prop="name" label="任务名称" />
      <el-table-column prop="priority" label="优先级" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="assignee" label="责任人" />
    </el-table>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const tasks = ref([])
</script>