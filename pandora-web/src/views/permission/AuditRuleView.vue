<template>
  <div class="audit-rule-view">
    <el-button type="primary" @click="showDialog = true" style="margin-bottom: 16px">新增规则</el-button>
    <el-table :data="rules" border>
      <el-table-column prop="trigger_action" label="触发动作" />
      <el-table-column prop="sequence" label="序号" width="80" />
      <el-table-column prop="enabled" label="启用" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="showDialog" title="新增审核规则">
      <el-form :model="form">
        <el-form-item label="触发动作"><el-input v-model="form.triggerAction" /></el-form-item>
        <el-form-item label="序号"><el-input-number v-model="form.sequence" :min="1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listAuditRules, createAuditRule } from '@/api/permission'

const rules = ref([])
const showDialog = ref(false)
const form = ref({ triggerAction: '', sequence: 1 })

onMounted(async () => {
  const res = await listAuditRules()
  rules.value = res.data.data
})

async function save() {
  await createAuditRule(form.value)
  showDialog.value = false
  const res = await listAuditRules()
  rules.value = res.data.data
}
</script>