<template>
  <div class="keyword-analysis-view">
    <el-input v-model="text" type="textarea" :rows="4" placeholder="输入分析文本" />
    <el-button type="primary" @click="analyze" :loading="loading" style="margin-top: 12px">分析</el-button>
    <el-table :data="keywords" style="margin-top: 20px" v-if="keywords.length">
      <el-table-column prop="word" label="关键词" />
      <el-table-column prop="count" label="出现次数" width="120" />
    </el-table>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { keywordAnalysis } from '@/api/ai'

const text = ref('')
const keywords = ref([])
const loading = ref(false)

async function analyze() {
  if (!text.value.trim()) return
  loading.value = true
  try {
    const res = await keywordAnalysis({ text: text.value })
    keywords.value = res.data.data
  } catch (e) {
    console.error('分析失败', e)
  } finally {
    loading.value = false
  }
}
</script>