<template>
  <div class="members">
    <span v-if="!members.length" class="empty">暂无成员</span>
    <el-tag v-for="member in members.slice(0, 3)" :key="member.id" size="small" :type="type">{{ label(member) }}</el-tag>
    <el-popover v-if="members.length > 3" trigger="click" placement="bottom-start" :width="320">
      <template #reference><el-button link type="primary" :aria-label="`展开全部 ${members.length} 位成员`">另 {{ members.length - 3 }} 人</el-button></template>
      <div class="member-panel" role="list" aria-label="全部成员">
        <div v-for="member in members" :key="member.id" role="listitem" class="member-row"><strong>{{ member.real_name || member.realName || member.username }}</strong><span>{{ roleMap[member.role] || '未设置职级' }}</span></div>
      </div>
    </el-popover>
  </div>
</template>
<script setup>
import { roleMap } from '@/utils/roleConstants'
defineProps({ members: { type: Array, default: () => [] }, type: { type: String, default: 'info' } })
const label = member => `${member.real_name || member.realName || member.username}（${roleMap[member.role] || '未设置职级'}）`
</script>
<style scoped>
.members { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; padding: 4px 0; }
.members :deep(.el-tag) { height: auto; min-height: 24px; white-space: normal; line-height: 1.6; overflow-wrap: anywhere; }
.empty, .member-row span { color: var(--el-text-color-secondary); }
.member-panel { max-height: 300px; overflow-y: auto; }
.member-row { display: flex; justify-content: space-between; gap: 12px; padding: 10px 4px; border-bottom: 1px solid var(--el-border-color-lighter); overflow-wrap: anywhere; }
.member-row:last-child { border: 0; }
</style>
