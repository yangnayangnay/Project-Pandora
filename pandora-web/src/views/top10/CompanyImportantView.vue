<template>
  <div class="company-important-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>公司十大重要事项管理</span>
          <el-button type="primary" size="small" @click="openCreate">新增</el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="rank_order" label="排名" width="80" />
        <el-table-column prop="content" label="内容" />
        <el-table-column prop="updated_at" label="更新时间" width="180" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑' : '新增'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="排名">
          <el-select v-model="form.rankOrder" placeholder="选择排名">
            <el-option v-for="n in 10" :key="n" :label="n" :value="n" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const list = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editId = ref(null)
const form = ref({ rankOrder: 1, content: '' })

const loadList = async () => {
  loading.value = true
  try {
    const res = await request.get('/top10/company-important')
    list.value = res.data || []
  } catch (e) { /* handled by interceptor */ }
  loading.value = false
}

const openCreate = () => {
  editId.value = null
  form.value = { rankOrder: 1, content: '' }
  dialogVisible.value = true
}

const openEdit = (row) => {
  editId.value = row.id || row.ID
  form.value = {
    rankOrder: row.rank_order || row.RANK_ORDER,
    content: row.content || row.CONTENT || ''
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.content.trim()) {
    ElMessage.warning('内容不能为空')
    return
  }
  try {
    if (editId.value) {
      await request.put(`/top10/company-important/${editId.value}`, form.value)
    } else {
      await request.post('/top10/company-important', form.value)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadList()
  } catch (e) { /* handled by interceptor */ }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除这条记录吗？', '提示', { type: 'warning' })
    const id = row.id || row.ID
    await request.delete(`/top10/company-important/${id}`)
    ElMessage.success('已删除')
    loadList()
  } catch (e) {
    if (e !== 'cancel') { /* handled by interceptor */ }
  }
}

onMounted(loadList)
</script>

<style scoped>
.company-important-view { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>