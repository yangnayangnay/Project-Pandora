<template>
  <div>
    <el-row :gutter="20">
      <el-col :xs="24" :lg="12">
        <el-card header="个人信息">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户名">{{ user.username || '-' }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ roleMap[user.role] || user.role || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ user.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="真实姓名">{{ user.real_name || user.realName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ user.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="部门ID">{{ user.department_id || user.departmentId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="团队ID">{{ user.team_id || user.teamId || '-' }}</el-descriptions-item>
          </el-descriptions>
          <el-divider />
          <el-form :model="editForm" label-width="80px" v-if="editing">
            <el-form-item label="用户名">
              <el-input :model-value="user.username || ''" readonly />
            </el-form-item>
            <el-form-item label="真实姓名">
              <el-input :model-value="user.real_name || user.realName || ''" readonly />
            </el-form-item>
            <el-form-item label="邮箱"><el-input v-model="editForm.email" /></el-form-item>
            <el-form-item label="手机号"><el-input v-model="editForm.phone" /></el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveProfile">保存</el-button>
              <el-button @click="editing = false">取消</el-button>
            </el-form-item>
          </el-form>
          <el-button type="primary" plain @click="startEdit" v-else>编辑信息</el-button>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card header="修改密码">
          <el-button type="primary" plain @click="showPwdPanel = !showPwdPanel" v-if="!showPwdPanel">修改密码</el-button>
          <template v-if="showPwdPanel">
            <el-form :model="pwdForm" label-width="100px">
              <el-form-item label="当前密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
              <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
              <el-form-item label="确认新密码"><el-input v-model="pwdForm.confirmPassword" type="password" show-password /></el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="savingPassword" :disabled="savingPassword" @click="savePassword">确认修改</el-button>
                <el-button :disabled="savingPassword" @click="showPwdPanel = false">取消</el-button>
              </el-form-item>
            </el-form>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentUser, updateProfile, changePassword } from '@/api/user'

const user = ref({})
const editing = ref(false)
const showPwdPanel = ref(false)
const savingPassword = ref(false)
const editForm = ref({})
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
import { roleMap } from '@/utils/roleConstants'

const utf8ByteLength = (value) => new TextEncoder().encode(value).length

const loadUser = async () => {
  try {
    const res = await getCurrentUser()
    user.value = res.data
  } catch (e) { /* ignore */ }
}

const startEdit = () => {
  editing.value = true
  editForm.value = { email: user.value.email, phone: user.value.phone }
}

const saveProfile = async () => {
  try {
    await updateProfile({
      email: editForm.value.email,
      phone: editForm.value.phone
    })
    ElMessage.success('保存成功')
    editing.value = false
    await loadUser()
  } catch (e) { /* ignore */ }
}

const savePassword = async () => {
  if (savingPassword.value) return

  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword || !pwdForm.value.confirmPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  if (pwdForm.value.newPassword.length < 6) {
    ElMessage.warning('新密码不能少于 6 个字符')
    return
  }
  if (utf8ByteLength(pwdForm.value.newPassword) > 72) {
    ElMessage.warning('新密码不能超过 72 个 UTF-8 字节')
    return
  }
  if (pwdForm.value.newPassword === pwdForm.value.oldPassword) {
    ElMessage.warning('新密码不能与当前密码相同')
    return
  }
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }

  savingPassword.value = true
  try {
    await changePassword({ oldPassword: pwdForm.value.oldPassword, newPassword: pwdForm.value.newPassword })
    ElMessage.success('密码修改成功')
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    showPwdPanel.value = false
  } catch (e) { /* ignore */ } finally {
    savingPassword.value = false
  }
}

onMounted(loadUser)
</script>