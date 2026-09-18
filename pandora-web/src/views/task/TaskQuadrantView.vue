<template>
  <div class="task-quadrant-view">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="quadrant-card important-urgent">
          <template #header>重要且紧急</template>
          <div v-for="task in quadrant.importantUrgent" :key="task.id" class="task-item">
            {{ task.name }}
          </div>
          <el-empty v-if="!quadrant.importantUrgent?.length" description="暂无任务" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="quadrant-card important-not-urgent">
          <template #header>重要不紧急</template>
          <div v-for="task in quadrant.importantNotUrgent" :key="task.id" class="task-item">
            {{ task.name }}
          </div>
          <el-empty v-if="!quadrant.importantNotUrgent?.length" description="暂无任务" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="quadrant-card not-important-urgent">
          <template #header>紧急不重要</template>
          <div v-for="task in quadrant.notImportantUrgent" :key="task.id" class="task-item">
            {{ task.name }}
          </div>
          <el-empty v-if="!quadrant.notImportantUrgent?.length" description="暂无任务" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="quadrant-card not-important-not-urgent">
          <template #header>不重要不紧急</template>
          <div v-for="task in quadrant.notImportantNotUrgent" :key="task.id" class="task-item">
            {{ task.name }}
          </div>
          <el-empty v-if="!quadrant.notImportantNotUrgent?.length" description="暂无任务" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getQuadrantTasks } from '@/api/task'

const quadrant = ref({})

onMounted(async () => {
  try {
    const res = await getQuadrantTasks()
    quadrant.value = res.data.data
  } catch (e) {
    console.error('加载四象限任务失败', e)
  }
})
</script>

<style scoped>
.quadrant-card { margin-bottom: 20px; }
.task-item { padding: 8px 0; border-bottom: 1px solid #eee; }
.important-urgent { border-left: 4px solid #F56C6C; }
.important-not-urgent { border-left: 4px solid #E6A23C; }
.not-important-urgent { border-left: 4px solid #409EFF; }
.not-important-not-urgent { border-left: 4px solid #909399; }
</style>