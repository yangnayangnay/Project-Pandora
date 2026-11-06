<template>
  <div class="account-create-container">
    <el-card>
      <template #header><span class="card-title">创建账号</span></template>
      <p class="form-description">使用工号创建人员账号，以下资料均为必填项。</p>
      <el-alert v-if="orgError" :title="orgError" type="error" :closable="false" show-icon class="org-error">
        <el-button link type="primary" :loading="orgLoading" @click="loadOrganizations">重新加载部门与团队</el-button>
      </el-alert>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" :disabled="submitting" @submit.prevent="handleSubmit">
        <el-form-item label="工号" prop="employeeNo">
          <el-input v-model.trim="form.employeeNo" maxlength="64" placeholder="请输入工号，保留前导零" autocomplete="off" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码（至少6位）" autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色">
            <el-option label="普通员工" value="EMPLOYEE" /><el-option label="团队负责人" value="TEAM_LEADER" />
            <el-option label="部门负责人" value="DEPT_HEAD" /><el-option label="创始人" value="FOUNDER" />
          </el-select>
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName"><el-input v-model.trim="form.realName" maxlength="64" placeholder="请输入真实姓名" /></el-form-item>
        <el-form-item label="邮箱" prop="email"><el-input v-model.trim="form.email" maxlength="128" placeholder="请输入邮箱" /></el-form-item>
        <el-form-item label="手机号" prop="phone"><el-input v-model.trim="form.phone" maxlength="11" placeholder="请输入手机号" inputmode="tel" /></el-form-item>
        <el-form-item label="身份证号" prop="idCard"><el-input v-model.trim="form.idCard" maxlength="18" placeholder="请输入18位身份证号" /></el-form-item>
        <el-form-item label="生日" prop="birthday">
          <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" placeholder="请选择生日" :disabled-date="isFutureBirthday" />
        </el-form-item>
        <el-form-item label="部门" prop="departmentId">
          <el-select v-model="form.departmentId" filterable clearable :loading="orgLoading" :disabled="orgLoading || !!orgError"
            placeholder="请选择部门" no-data-text="暂无可用部门" @change="onDepartmentChange">
            <el-option v-for="department in departments" :key="department.id" :label="department.name" :value="department.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="团队" prop="teamId">
          <el-select v-model="form.teamId" filterable clearable :disabled="form.departmentId == null || orgLoading || !!orgError"
            placeholder="请先选择部门，再选择团队" no-data-text="所选部门暂无可用团队">
            <el-option v-for="team in availableTeams" :key="team.id" :label="team.name" :value="team.id" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" native-type="submit" :loading="submitting" :disabled="orgLoading || !!orgError">创建账号</el-button>
          <el-button @click="handleReset">重置</el-button></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { createUser } from '@/api/user'
import request from '@/utils/request'
import { emptyAccountForm, teamsForDepartment, accountPayload, isFutureBirthday } from '@/utils/accountCreation'

const formRef = ref(), submitting = ref(false), orgLoading = ref(false), orgError = ref('')
const departments = ref([]), teams = ref([])
const form = reactive(emptyAccountForm())
const availableTeams = computed(() => teamsForDepartment(teams.value, form.departmentId))
const required = (message, trigger = 'blur') => ({ required: true, message, trigger })
const rules = {
  employeeNo: [required('请输入工号'), { pattern: /^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$/, message: '工号须为1至64位字母、数字、点、下划线或短横线', trigger: 'blur' }],
  password: [required('请输入密码'), { min: 6, message: '密码至少6位', trigger: 'blur' },
    { validator: (_rule, value, done) => done(new TextEncoder().encode(value || '').length > 72 ? new Error('密码不能超过72个UTF-8字节') : undefined), trigger: 'blur' }],
  role: [required('请选择角色', 'change')], realName: [required('请输入真实姓名')],
  email: [required('请输入邮箱'), { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [required('请输入手机号'), { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  idCard: [required('请输入身份证号'), { pattern: /^\d{17}[\dXx]$/, message: '请输入18位身份证号，末位可为X', trigger: 'blur' }],
  birthday: [required('请选择生日', 'change')], departmentId: [required('请选择部门', 'change')],
  teamId: [required('请选择团队', 'change'), { validator: (_rule, value, done) => done(value != null && !availableTeams.value.some(team => String(team.id) === String(value)) ? new Error('团队必须属于所选部门') : undefined), trigger: 'change' }]
}
const onDepartmentChange = () => { form.teamId = null; formRef.value?.clearValidate('teamId') }
const loadOrganizations = async () => {
  orgLoading.value = true; orgError.value = ''
  try {
    const response = await request.get('/user/directory/options')
    departments.value = response.data?.departments || []; teams.value = response.data?.teams || []
  } catch { orgError.value = '部门与团队加载失败，请重试。' }
  finally { orgLoading.value = false }
}
const handleSubmit = async () => {
  if (submitting.value || orgLoading.value || orgError.value || !formRef.value) return
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    await createUser(accountPayload(form))
    ElMessage.success('账号创建成功，请使用工号登录安卓端')
    handleReset()
  } catch { /* The request interceptor displays the server validation error. */ }
  finally { submitting.value = false }
}
const handleReset = () => { formRef.value?.resetFields(); Object.assign(form, emptyAccountForm()) }
onMounted(loadOrganizations)
</script>

<style scoped>
.account-create-container { max-width: 680px; margin: 20px auto; }
.card-title { font-size: 18px; font-weight: bold; }
.form-description { margin: 0 0 20px; color: var(--el-text-color-secondary); line-height: 1.6; }
.org-error { margin-bottom: 16px; }
:deep(.el-select), :deep(.el-date-editor) { width: 100%; }
@media (max-width: 600px) { .account-create-container { margin: 0; } }
</style>
