<template>
  <div class="members">
    <span v-if="!members.length" class="empty">暂无成员</span>
    <el-tag v-for="member in visibleMembers" :key="member.id" size="small" :type="type">{{ label(member) }}</el-tag>
    <el-button v-if="members.length > 3" size="small" type="primary" plain
      :aria-expanded="expanded" :aria-label="expanded ? '收起成员列表' : `展开全部 ${members.length} 位成员`"
      @click="expanded = !expanded">{{ expanded ? '收起' : '展开' }}</el-button>
  </div>
</template>
<script setup>
import { computed, ref } from 'vue'
import { roleMap } from '@/utils/roleConstants'
const props = defineProps({ members: { type: Array, default: () => [] }, type: { type: String, default: 'info' } })
const expanded = ref(false)
const visibleMembers = computed(() => expanded.value ? props.members : props.members.slice(0, 3))
const label = member => `${member.real_name || member.realName || member.username}（${roleMap[member.role] || '未设置职级'}）`
</script>
<style scoped>
.members { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 6px 0; }
.members :deep(.el-tag) { height: auto; min-height: 26px; max-width: 100%; white-space: normal; line-height: 1.6; overflow-wrap: anywhere; }
.members :deep(.el-button) { border-radius: 10px; min-height: 30px; transition: filter .16s, transform .16s; }
.members :deep(.el-button:active) { transform: scale(.97); }
.empty { color: var(--el-text-color-secondary); }
</style>
