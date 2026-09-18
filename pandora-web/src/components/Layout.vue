<template>
  <el-container class="layout-container">
    <el-aside width="220px">
      <el-menu :default-active="route.path" router>
        <div class="logo">潘多拉管理端</div>
        <el-menu-item index="/dashboard"><el-icon><DataBoard /></el-icon>全局看板</el-menu-item>
        <el-menu-item index="/task"><el-icon><List /></el-icon>任务管理</el-menu-item>
        <el-menu-item index="/user"><el-icon><User /></el-icon>人员管理</el-menu-item>
        <el-menu-item index="/ai"><el-icon><MagicStick /></el-icon>AI分析</el-menu-item>
        <el-menu-item index="/config"><el-icon><Setting /></el-icon>系统配置</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <div class="header-right">
          <span>{{ userStore.username }}</span>
          <el-button text @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.logo {
  padding: 20px;
  font-size: 18px;
  font-weight: bold;
  text-align: center;
  color: #409EFF;
}
.header-right {
  float: right;
  display: flex;
  align-items: center;
  gap: 12px;
  height: 60px;
}
</style>