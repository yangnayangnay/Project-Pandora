<template>
  <div class="system-config">
    <el-card>
      <template #header>系统配置</template>
      <el-form :model="config" label-width="140px">
        <el-form-item label="系统名称">
          <el-input v-model="config.systemName" />
        </el-form-item>
        <el-form-item label="任务自动过期天数">
          <el-input-number v-model="config.taskExpireDays" :min="1" :max="365" />
        </el-form-item>
        <el-form-item label="日志保留天数">
          <el-input-number v-model="config.logRetentionDays" :min="7" :max="365" />
        </el-form-item>
        <el-form-item label="通知轮询间隔(分钟)">
          <el-input-number v-model="config.pollIntervalMinutes" :min="5" :max="120" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const config = ref({
  systemName: '潘多拉信息面板系统',
  taskExpireDays: 30,
  logRetentionDays: 90,
  pollIntervalMinutes: 15
})

const handleSave = () => {
  ElMessage.success('配置已保存')
}
</script>

<style scoped>
.system-config {
  max-width: 600px;
}
</style>