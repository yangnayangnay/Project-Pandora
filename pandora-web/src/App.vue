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
body {
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}
</style>
