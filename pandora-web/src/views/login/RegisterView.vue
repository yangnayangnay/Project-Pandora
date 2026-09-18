<template>
  <div class="register-container">
    <el-card class="register-card">
      <template #header>
        <div class="register-header">
          <h2>潘多拉 - 智能掌上工作系统</h2>
          <p>用户注册</p>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（至少6位）" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" size="large" show-password @keyup.enter="handleRegister" />
        </el-form-item>
        <el-form-item prop="role">
          <el-select v-model="form.role" placeholder="选择角色" size="large" style="width: 100%">
            <el-option label="普通员工" value="EMPLOYEE" />
            <el-option label="团队负责人" value="TEAM_LEADER" />
            <el-option label="部门负责人" value="DEPT_HEAD" />
            <el-option label="联合创始人" value="FOUNDER" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="handleRegister">注 册</el-button>
        </el-form-item>
        <el-form-item>
          <el-link type="primary" @click="$router.push('/login')">已有账号？去登录</el-link>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/stores/userStore'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  role: 'EMPLOYEE'
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const handleRegister = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await request.post('/auth/register', {
      username: form.username,
      password: form.password,
      role: form.role
    })
    userStore.setLoginData(res.data)
    ElMessage.success('注册成功')
    router.push('/')
  } catch (error) {
    console.error('Register failed:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.register-card {
  width: 400px;
}
.register-header {
  text-align: center;
}
.register-header h2 {
  margin: 0 0 8px 0;
  color: #333;
}
.register-header p {
  margin: 0;
  color: #888;
}
</style>