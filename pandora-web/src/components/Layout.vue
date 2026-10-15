<template>
  <el-container class="layout-container">
    <el-aside width="220px">
      <el-menu :default-active="route.path" router>
        <div class="logo">潘多拉管理端</div>
        <el-menu-item index="/dashboard"><el-icon><DataBoard /></el-icon>全局看板</el-menu-item>
        <el-menu-item index="/task"><el-icon><List /></el-icon>任务管理</el-menu-item>
        <el-menu-item index="/task/all"><el-icon><Tickets /></el-icon>全量任务</el-menu-item>
        <el-menu-item index="/log/all"><el-icon><Notebook /></el-icon>全量日志</el-menu-item>
        <el-menu-item index="/top10/company-important"><el-icon><Trophy /></el-icon>公司十大事项</el-menu-item>
        <el-menu-item index="/user"><el-icon><User /></el-icon>人员管理</el-menu-item>
        <el-menu-item index="/permission/approval"><el-icon><CircleCheck /></el-icon>账号审批</el-menu-item>
        <el-menu-item index="/ai"><el-icon><MagicStick /></el-icon>任务完成分析</el-menu-item>
        <el-menu-item index="/config/audit-log"><el-icon><Document /></el-icon>系统日志</el-menu-item>
        <el-menu-item index="/config/system-config"><el-icon><Setting /></el-icon>系统配置</el-menu-item>
        <el-menu-item index="/permission/function"><el-icon><Lock /></el-icon>功能权限</el-menu-item>
        <el-menu-item index="/config/ai"><el-icon><Cpu /></el-icon>AI配置</el-menu-item>
        <el-menu-item index="/template"><el-icon><Files /></el-icon>模板管理</el-menu-item>
        <el-menu-item index="/profile"><el-icon><UserFilled /></el-icon>个人中心</el-menu-item>
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