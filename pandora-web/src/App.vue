<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getToken, removeToken, removeUser } from '@/utils/auth'
import request from '@/utils/request'

const router = useRouter()

onMounted(async () => {
  const token = getToken()
  if (token) {
    try {
      await request.get('/user/me')
    } catch (e) {
      removeToken()
      removeUser()
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
