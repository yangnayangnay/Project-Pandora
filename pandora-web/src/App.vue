<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getToken } from '@/utils/auth'
import request from '@/utils/request'
import { useUserStore } from '@/stores/userStore'

const router = useRouter()

onMounted(async () => {
  const token = getToken()
  if (token) {
    try {
      const res = await request.get('/user/me')
      if (res.data?.role !== 'ADMIN') throw new Error('管理端仅允许管理员登录')
    } catch (e) {
      useUserStore().logout()
      router.push('/login')
    }
  }
})
</script>

<style>
:root {
  color-scheme: light;
  --el-color-primary: #725b49;
  --el-color-primary-light-3: #9c8979;
  --el-color-primary-light-5: #b9ac9f;
  --el-color-primary-light-7: #d5ccc2;
  --el-color-primary-light-8: #e3dcd3;
  --el-color-primary-light-9: #f2ece5;
  --el-color-primary-dark-2: #5c4738;
  --el-color-success: #50634f;
  --el-color-success-light-9: #edf1ea;
  --el-color-warning: #805e34;
  --el-color-warning-light-9: #f5eddf;
  --el-color-danger: #a1473a;
  --el-color-danger-light-9: #f8eae5;
  --el-color-info: #71695f;
  --el-color-info-light-9: #f3efe8;
  --el-bg-color: #ffffff;
  --el-bg-color-page: #f8f6f2;
  --el-fill-color-blank: #ffffff;
  --el-fill-color-light: #f3efe8;
  --el-text-color-primary: #2c2823;
  --el-text-color-regular: #514a42;
  --el-text-color-secondary: #71695f;
  --el-border-color: #d8cfc3;
  --el-border-color-light: #e9e3da;
  --el-border-radius-base: 12px;
}
@media (prefers-color-scheme: dark) {
  :root {
    color-scheme: dark;
    --el-color-primary: #d8bca0;
    --el-color-primary-light-3: #bba087;
    --el-color-primary-light-5: #8b7561;
    --el-color-primary-light-7: #615040;
    --el-color-primary-light-8: #4b3d31;
    --el-color-primary-light-9: #372e26;
    --el-color-primary-dark-2: #e8ceb6;
    --el-color-success: #bed0b6;
    --el-color-success-light-9: #2d362a;
    --el-color-warning: #e2c69c;
    --el-color-warning-light-9: #3d3223;
    --el-color-danger: #edb0a2;
    --el-color-danger-light-9: #432d27;
    --el-color-info: #c1b5a7;
    --el-color-info-light-9: #2d2925;
    --el-bg-color: #24211e;
    --el-bg-color-overlay: #2d2925;
    --el-bg-color-page: #191715;
    --el-fill-color-blank: #24211e;
    --el-fill-color: #352f29;
    --el-fill-color-light: #2d2925;
    --el-fill-color-lighter: #292520;
    --el-fill-color-dark: #3e3730;
    --el-text-color-primary: #f1eae1;
    --el-text-color-regular: #dfd3c5;
    --el-text-color-secondary: #c1b5a7;
    --el-text-color-placeholder: #aa9b8b;
    --el-border-color: #6d6053;
    --el-border-color-light: #493f35;
    --el-border-color-lighter: #3e3730;
    --el-mask-color: rgba(25, 23, 21, .85);
  }
  .el-button--primary, .el-button--success, .el-button--warning, .el-button--danger, .el-button--info { --el-button-text-color: #2c241d; --el-button-hover-text-color: #2c241d; --el-button-active-text-color: #2c241d; }
  .el-button--primary { --el-button-hover-bg-color: #e0c5ab; --el-button-hover-border-color: #e0c5ab; --el-button-active-bg-color: #e8ceb6; --el-button-active-border-color: #e8ceb6; }
}
body {
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color-page);
}
.el-container, .el-main { min-width: 0; }
.el-card { border-radius: 18px; border-color: var(--el-border-color-light); box-shadow: 0 3px 14px rgba(50, 38, 26, .035); }
.el-card__header { padding: 20px 24px; }
.el-card__body { padding: 24px; }
.el-card__header h2, .page-title { margin: 0; font-size: 20px; line-height: 1.4; font-weight: 600; }
.header-row, .directory-header { flex-wrap: wrap; row-gap: 12px; }
.header-row > span:first-child { font-size: 20px; font-weight: 600; }
.header-actions { flex-wrap: wrap; }
.el-button { border-radius: 12px; transition: background-color .14s, border-color .14s, transform .1s; }
.el-button--primary { --el-button-hover-bg-color: #675140; --el-button-hover-border-color: #675140; --el-button-active-bg-color: #5c4738; --el-button-active-border-color: #5c4738; }
@media (prefers-color-scheme: dark) { .el-button--primary { --el-button-hover-bg-color: #e0c5ab; --el-button-hover-border-color: #e0c5ab; --el-button-active-bg-color: #e8ceb6; --el-button-active-border-color: #e8ceb6; } }
.el-button:not(.is-disabled):active { transform: scale(.98); }
.el-button.is-link, .el-button.is-text { border-radius: 8px; }
.el-dialog { border-radius: 18px; max-width: calc(100vw - 32px); }
.el-dialog__body { overflow-wrap: anywhere; }
.el-table .cell { overflow-wrap: anywhere; }
@media (max-width: 760px) {
  .el-main { padding: 12px; }
  .el-card__header, .el-card__body { padding: 16px; }
  .header-row, .directory-header { align-items: flex-start; }
  .el-form-item__content { min-width: 0; }
}
@media (prefers-reduced-motion: reduce) { .el-button { transition: none; } .el-button:not(.is-disabled):active { transform: none; } }
</style>
