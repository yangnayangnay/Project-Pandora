<template>
  <div class="login-container">
    <div class="login-backdrop" aria-hidden="true">
      <span class="login-orb login-orb-peach"></span>
      <span class="login-orb login-orb-cream"></span>
      <span class="login-orb login-orb-light"></span>
    </div>
    <el-card class="login-card">
      <template #header>
        <div class="login-header">
          <h2>潘多拉 - 智能掌上工作系统</h2>
          <p>管理端登录</p>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" aria-label="用户名" autocomplete="username" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" aria-label="密码" autocomplete="current-password" prefix-icon="Lock" size="large" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
        <el-form-item>
          <el-link type="primary" @click="$router.push('/register')">没有账号？去注册</el-link>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/userStore'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login(form)
    if (res.data.user.role !== 'ADMIN') {
      ElMessage.error('管理端仅允许管理员登录')
      return
    }
    userStore.setLoginData(res.data)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    console.error('Login failed:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  --login-base: #fff8f0;
  --login-white: #ffffff;
  --login-peach: #f4bf9c;
  --login-cream: #f5dca8;
  --login-surface: rgba(255, 255, 255, .93);
  --login-stroke: rgba(255, 255, 255, .85);
  --login-shadow: rgba(117, 79, 49, .10);
  position: relative;
  isolation: isolate;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  min-height: 100svh;
  box-sizing: border-box;
  padding: 32px 24px;
  background: linear-gradient(155deg, var(--login-base), var(--login-white));
}
.login-backdrop {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: -1;
}
.login-orb {
  position: absolute;
  display: block;
  width: clamp(360px, 58vw, 880px);
  aspect-ratio: 1;
  border-radius: 50%;
}
.login-orb-peach {
  top: -22%;
  left: -15%;
  background: radial-gradient(circle, var(--login-peach) 0%, color-mix(in srgb, var(--login-peach) 85%, transparent) 55%, transparent 72%);
  animation: peach-drift 28s ease-in-out infinite alternate;
}
.login-orb-cream {
  right: -13%;
  bottom: -28%;
  background: radial-gradient(circle, var(--login-cream) 0%, color-mix(in srgb, var(--login-cream) 85%, transparent) 55%, transparent 72%);
  animation: cream-drift 24s ease-in-out infinite alternate;
}
.login-orb-light {
  width: clamp(320px, 48vw, 760px);
  top: 20%;
  left: 24%;
  background: radial-gradient(circle, var(--login-white) 0%, transparent 72%);
}
@keyframes peach-drift {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(7%, 9%) scale(1.06); }
}
@keyframes cream-drift {
  from { transform: translate(0, 0) scale(1.04); }
  to { transform: translate(-8%, -7%) scale(.98); }
}
.login-card {
  width: min(440px, 100%);
  box-sizing: border-box;
  border-radius: 28px;
  background: var(--login-surface);
  border: 1px solid var(--login-stroke);
  box-shadow: 0 16px 48px var(--login-shadow), 0 2px 8px rgba(117, 79, 49, .04);
}
.login-card :deep(.el-card__header) {
  padding: 32px 32px 24px;
  border-bottom: 0;
}
.login-card :deep(.el-card__body) {
  padding: 0 32px 24px;
}
.login-header {
  text-align: center;
}
.login-header h2 {
  margin: 0 0 8px 0;
  color: var(--el-text-color-primary);
  font-size: 22px;
  line-height: 1.5;
}
.login-header p {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
@media (max-width: 480px) {
  .login-container { padding: 24px 16px; }
  .login-card :deep(.el-card__header) { padding: 28px 24px 24px; }
  .login-card :deep(.el-card__body) { padding: 0 24px 20px; }
  .login-orb-peach { top: -8%; left: -50%; }
  .login-orb-cream { right: -55%; bottom: -8%; }
}
@media (prefers-color-scheme: dark) {
  .login-container {
    --login-base: #30251f;
    --login-white: #1c1916;
    --login-peach: #986547;
    --login-cream: #887044;
    --login-surface: rgba(36, 33, 30, .95);
    --login-stroke: rgba(111, 94, 80, .25);
    --login-shadow: rgba(0, 0, 0, .18);
  }
  .login-orb-light { background: radial-gradient(circle, #6c5c4d 0%, transparent 72%); }
}
@media (prefers-reduced-motion: reduce) {
  .login-orb { animation: none; }
}
</style>
