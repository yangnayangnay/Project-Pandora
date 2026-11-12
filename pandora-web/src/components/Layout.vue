<template>
  <el-container class="layout-container">
    <el-aside width="220px">
      <el-menu :default-active="route.path" router>
        <div class="logo">潘多拉管理端</div>
        <!-- Round19 REQ-19-01: 菜单精简为5项，按角色动态显隐 -->
        <el-menu-item index="/user"><el-icon><User /></el-icon>人员管理</el-menu-item>
        <el-menu-item index="/user/dept-team" v-if="userStore.role === 'ADMIN'"><el-icon><OfficeBuilding /></el-icon>部门与团队</el-menu-item>
        <el-menu-item index="/user/password-resets" v-if="userStore.role === 'ADMIN'"><el-icon><Key /></el-icon>密码重置申请</el-menu-item>
        <el-menu-item index="/account/create" v-if="userStore.role === 'ADMIN'"><el-icon><CirclePlus /></el-icon>账号创建</el-menu-item>
        <el-menu-item index="/config/audit-log" v-if="userStore.role === 'ADMIN'"><el-icon><Document /></el-icon>系统日志</el-menu-item>
        <el-menu-item index="/top10/company-important"><el-icon><Trophy /></el-icon>公司十大事项</el-menu-item>
        <el-menu-item index="/profile"><el-icon><UserFilled /></el-icon>个人中心</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <span class="page-title">{{ pageTitle }}</span>
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
import { Key, OfficeBuilding } from '@element-plus/icons-vue'
import { computed } from 'vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const pageTitle = computed(() => ({ '/user': '人员管理', '/user/dept-team': '部门与团队', '/user/password-resets': '密码重置申请', '/account/create': '账号创建', '/config/audit-log': '系统日志', '/top10/company-important': '公司十大事项', '/profile': '个人中心' }[route.path] || '潘多拉管理端'))

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  min-height: 100dvh;
  background: var(--el-bg-color-page);
}
.layout-container > .el-aside { background: var(--el-bg-color); border-right: 1px solid var(--el-border-color-light); }
.el-menu { border-right: 0; background: transparent; }
.el-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; background: var(--el-bg-color); border-bottom: 1px solid var(--el-border-color-light); }
.logo {
  padding: 20px;
  font-size: 18px;
  font-weight: bold;
  text-align: center;
  color: var(--el-color-primary);
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 60px;
}
@media (max-width: 900px) { .layout-container > .el-aside { width: 176px !important; } .logo { padding: 20px 12px; font-size: 16px; } }
@media (max-width: 600px) { .layout-container { flex-direction: column; } .layout-container > .el-aside { width: 100% !important; } .el-menu { display: flex; overflow-x: auto; } .logo { display: none; } .el-menu-item { flex-shrink: 0; padding: 0 12px; height: 48px; } .el-header { padding: 0 12px; } .page-title { font-size: 17px; } }
</style>
