<template>
  <div>
    <h2>MBTI报告查看</h2>
    <el-table :data="reports" border>
      <el-table-column prop="username" label="用户" />
      <el-table-column prop="personalityType" label="性格类型" />
      <el-table-column prop="generatedAt" label="生成时间" />
    </el-table>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const reports = ref([])
</script>