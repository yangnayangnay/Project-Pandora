<template>
  <div class="function-permission-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>功能权限管理</span>
        </div>
      </template>

      <el-form :inline="true" class="filter-form">
        <el-form-item label="功能客体">
          <el-select v-model="selectedFunctionCode" placeholder="选择功能客体" @change="loadSubjects">
            <el-option
              v-for="code in functionCodes"
              :key="code.code"
              :label="code.description || code.code"
              :value="code.code"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="subjects" v-loading="loading" border>
        <el-table-column prop="user_id" label="用户ID" width="100" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="real_name" label="真实姓名" width="150" />
        <el-table-column prop="role" label="角色" width="120" />
        <el-table-column prop="created_at" label="授权时间" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="handleRemove(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="action-bar">
        <el-button type="primary" @click="addDialogVisible = true">新增权限主体</el-button>
        <el-button @click="historyDialogVisible = true">变更历史</el-button>
      </div>
    </el-card>

    <el-dialog v-model="addDialogVisible" title="新增权限主体" width="500px">
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="用户ID">
          <el-input v-model="addForm.userId" placeholder="输入用户ID（多个用逗号分隔）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="historyDialogVisible" title="变更历史" width="700px">
      <el-table :data="historyList" border>
        <el-table-column prop="created_at" label="时间" width="180" />
        <el-table-column prop="action" label="操作" width="100" />
        <el-table-column prop="operator_name" label="操作人" width="120" />
        <el-table-column prop="detail" label="详情" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFunctionCodes, getSubjects, addSubjects, removeSubjects, getHistory } from '@/api/permission'

const functionCodes = ref([])
const selectedFunctionCode = ref('')
const subjects = ref([])
const loading = ref(false)
const addDialogVisible = ref(false)
const historyDialogVisible = ref(false)
const historyList = ref([])
const addForm = ref({ userId: '' })

onMounted(() => {
  loadFunctionCodes()
})

async function loadFunctionCodes() {
  try {
    const res = await getFunctionCodes()
    functionCodes.value = res.data?.data || res.data || []
    if (functionCodes.value.length > 0) {
      selectedFunctionCode.value = functionCodes.value[0].code
      loadSubjects()
    }
  } catch (e) {
    ElMessage.error('加载功能客体失败')
  }
}

async function loadSubjects() {
  if (!selectedFunctionCode.value) return
  loading.value = true
  try {
    const res = await getSubjects(selectedFunctionCode.value)
    subjects.value = res.data?.data || res.data || []
  } catch (e) {
    ElMessage.error('加载权限主体失败')
  } finally {
    loading.value = false
  }
}

async function handleAdd() {
  const userIds = addForm.value.userId.split(',').map(s => s.trim()).filter(s => s)
  if (userIds.length === 0) {
    ElMessage.warning('请输入用户ID')
    return
  }
  try {
    await addSubjects({
      functionCode: selectedFunctionCode.value,
      userIds: userIds.map(Number)
    })
    ElMessage.success('添加成功')
    addDialogVisible.value = false
    addForm.value.userId = ''
    loadSubjects()
  } catch (e) {
    ElMessage.error('添加失败')
  }
}

async function handleRemove(row) {
  try {
    await ElMessageBox.confirm('确定移除该权限主体？', '提示', { type: 'warning' })
    await removeSubjects({
      functionCode: selectedFunctionCode.value,
      userIds: [row.user_id || row.USER_ID]
    })
    ElMessage.success('移除成功')
    loadSubjects()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('移除失败')
  }
}

async function loadHistory() {
  try {
    const res = await getHistory({ functionCode: selectedFunctionCode.value })
    historyList.value = res.data?.data || res.data || []
  } catch (e) {
    ElMessage.error('加载历史失败')
  }
}

function openHistory() {
  loadHistory()
  historyDialogVisible.value = true
}
</script>

<style scoped>
.function-permission-view {
  padding: 20px;
}
.filter-form {
  margin-bottom: 16px;
}
.action-bar {
  margin-top: 16px;
}
</style>