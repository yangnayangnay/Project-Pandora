<template>
  <div class="template-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>模板管理</span>
          <div>
            <el-radio-group v-model="templateType" @change="loadTemplates">
              <el-radio-button label="LOG">日志模板</el-radio-button>
              <el-radio-button label="TASK">任务模板</el-radio-button>
            </el-radio-group>
            <el-button type="primary" @click="showAddDialog" style="margin-left: 12px">新增模板</el-button>
          </div>
        </div>
      </template>

      <el-table :data="templates" v-loading="loading" stripe>
        <el-table-column prop="name" label="模板名称" />
        <el-table-column label="内容">
          <template #default="{ row }">
            <span>{{ JSON.stringify(row.content) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑模板' : '新增模板'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="工作事项" v-if="templateType === 'LOG'">
          <el-input v-model="form.content.workItem" />
        </el-form-item>
        <el-form-item label="时间花费" v-if="templateType === 'LOG'">
          <el-input v-model="form.content.timeCost" />
        </el-form-item>
        <el-form-item label="任务名称" v-if="templateType === 'TASK'">
          <el-input v-model="form.content.name" />
        </el-form-item>
        <el-form-item label="紧急" v-if="templateType === 'TASK'">
          <el-switch v-model="form.content.isUrgent" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTemplates, saveTemplate, updateTemplate, deleteTemplate } from '@/api/template'

const templateType = ref('LOG')
const templates = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const form = reactive({
  name: '',
  content: {}
})

const loadTemplates = async () => {
  loading.value = true
  try {
    const res = await listTemplates(templateType.value)
    templates.value = res.data || []
  } catch (e) {
    templates.value = []
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  editingId.value = null
  form.name = ''
  form.content = templateType.value === 'LOG' ? { workItem: '', timeCost: '' } : { name: '', isUrgent: false }
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  editingId.value = row.templateId
  form.name = row.name
  form.content = { ...row.content }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.name.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  try {
    if (editingId.value) {
      await updateTemplate(editingId.value, { name: form.name, content: form.content })
      ElMessage.success('模板更新成功')
    } else {
      await saveTemplate({ templateType: templateType.value, name: form.name, content: form.content })
      ElMessage.success('模板保存成功')
    }
    dialogVisible.value = false
    loadTemplates()
  } catch (e) {
    // error handled by interceptor
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除模板"${row.name}"？`, '提示', { type: 'warning' })
    await deleteTemplate(row.templateId)
    ElMessage.success('删除成功')
    loadTemplates()
  } catch (e) {
    // cancelled or error
  }
}

loadTemplates()
</script>

<style scoped>
.template-manage {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>