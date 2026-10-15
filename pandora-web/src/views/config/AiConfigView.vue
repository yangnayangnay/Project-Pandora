<template>
  <div class="ai-config-view">
    <el-card class="config-card">
      <template #header>
        <span>AI接入配置</span>
      </template>

      <el-form :model="config" label-width="140px" v-loading="loading">
        <el-form-item label="API名称">
          <el-input v-model="config.apiName" />
        </el-form-item>
        <el-form-item label="API地址">
          <el-input v-model="config.apiUrl" />
        </el-form-item>
        <el-form-item label="API密钥">
          <el-input v-model="config.apiKey" type="password" show-password placeholder="输入新密钥以更新" />
        </el-form-item>
        <el-form-item label="额度阈值">
          <el-input-number v-model="config.quotaThreshold" :min="0" />
        </el-form-item>
        <el-form-item label="超时(ms)">
          <el-input-number v-model="config.timeoutMs" :min="1000" :step="1000" />
        </el-form-item>
        <el-form-item label="启用外部AI">
          <el-switch v-model="config.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="兜底地址">
          <el-input v-model="config.fallbackUrl" />
        </el-form-item>
        <el-form-item label="启用兜底">
          <el-switch v-model="config.fallbackEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="status-card">
      <template #header>
        <span>AI通道状态</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="当前通道">{{ channelStatus.currentChannel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="外部可用">{{ channelStatus.externalAvailable ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="兜底可用">{{ channelStatus.fallbackAvailable ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="最后切换原因">{{ channelStatus.lastSwitchReason || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="catalog-card">
      <template #header>
        <span>免费AI API候选</span>
      </template>
      <el-row :gutter="16">
        <el-col :span="8" v-for="item in freeCatalog" :key="item.name">
          <el-card shadow="hover">
            <template #header>
              <div class="catalog-header">
                <span>{{ item.name }}</span>
                <el-tag v-if="item.recommended" type="success" size="small">推荐</el-tag>
              </div>
            </template>
            <p>免费额度：{{ item.freeQuota || '未知' }}</p>
            <p>兼容性：{{ item.compatibility || 'OpenAI兼容' }}</p>
            <el-link v-if="item.docUrl" :href="item.docUrl" target="_blank" type="primary">文档链接</el-link>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAiConfig, updateAiConfig, getChannelStatus, getFreeCatalog } from '@/api/aiConfig'

const loading = ref(false)
const config = ref({
  apiName: '', apiUrl: '', apiKey: '', quotaThreshold: 100,
  timeoutMs: 30000, enabled: 1, fallbackUrl: '', fallbackEnabled: 1
})
const channelStatus = ref({})
const freeCatalog = ref([])

onMounted(() => {
  loadConfig()
  loadChannelStatus()
  loadFreeCatalog()
})

async function loadConfig() {
  loading.value = true
  try {
    const res = await getAiConfig()
    const data = res.data?.data || res.data || {}
    config.value = {
      apiName: data.apiName || data.API_NAME || '',
      apiUrl: data.apiUrl || data.API_URL || '',
      apiKey: '',
      quotaThreshold: data.quotaThreshold || data.QUOTA_THRESHOLD || 100,
      timeoutMs: data.timeoutMs || data.TIMEOUT_MS || 30000,
      enabled: data.enabled !== undefined ? data.enabled : 1,
      fallbackUrl: data.fallbackUrl || data.FALLBACK_URL || '',
      fallbackEnabled: data.fallbackEnabled !== undefined ? data.fallbackEnabled : 1
    }
  } catch (e) {
    ElMessage.error('加载AI配置失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  try {
    const data = { ...config.value }
    if (!data.apiKey) delete data.apiKey
    await updateAiConfig(data)
    ElMessage.success('保存成功')
    loadConfig()
    loadChannelStatus()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function loadChannelStatus() {
  try {
    const res = await getChannelStatus()
    channelStatus.value = res.data?.data || res.data || {}
  } catch (e) {
    channelStatus.value = {}
  }
}

async function loadFreeCatalog() {
  try {
    const res = await getFreeCatalog()
    freeCatalog.value = res.data?.data || res.data || []
  } catch (e) {
    freeCatalog.value = []
  }
}
</script>

<style scoped>
.ai-config-view {
  padding: 20px;
}
.config-card, .status-card, .catalog-card {
  margin-bottom: 20px;
}
.catalog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>