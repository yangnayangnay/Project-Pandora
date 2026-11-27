<template>
  <div ref="container" class="members">
    <span v-if="!members.length" class="empty">暂无成员</span>
    <el-tag v-for="member in visibleMembers" :key="member.id" size="small" :type="type">{{ label(member) }}</el-tag>
    <el-button v-if="overflow" size="small" type="primary" plain
      :aria-expanded="expanded" :aria-label="expanded ? '收起成员列表' : `展开全部 ${members.length} 位成员`"
      @click="expanded = !expanded">{{ expanded ? '收起' : '展开' }}</el-button>
    <div class="measure" aria-hidden="true">
      <el-tag v-for="member in members" :key="member.id" class="measure-tag" size="small" :type="type">{{ label(member) }}</el-tag>
      <el-button class="measure-button" size="small" type="primary" plain>展开</el-button>
    </div>
  </div>
</template>
<script setup>
import { computed, ref, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { roleMap } from '@/utils/roleConstants'
import { memberRowLayout } from '@/utils/memberRowLayout'
const props = defineProps({ members: { type: Array, default: () => [] }, type: { type: String, default: 'info' } })
const expanded = ref(false)
const container = ref(null), count = ref(Infinity), overflow = ref(false)
const visibleMembers = computed(() => expanded.value ? props.members : props.members.slice(0, count.value))
const label = member => `${member.real_name || member.realName || member.username}（${roleMap[member.role] || '未设置职级'}）`
let observer, frame
const measure = () => {
  if (!container.value) return
  const node = container.value
  // offsetWidth ignores tag entrance transforms, which can temporarily scale a tag to zero.
  const widths = [...node.querySelectorAll('.measure-tag')].map(tag => tag.offsetWidth)
  const button = node.querySelector('.measure-button')?.offsetWidth || 60
  const layout = memberRowLayout(widths, node.clientWidth, button)
  count.value = layout.count; overflow.value = layout.overflow
  if (!layout.overflow) expanded.value = false
}
const schedule = () => { cancelAnimationFrame(frame); frame = requestAnimationFrame(measure) }
watch(() => props.members.map(label), async () => { await nextTick(); schedule() })
onMounted(() => {
  observer = new ResizeObserver(measure); observer.observe(container.value)
  document.fonts?.addEventListener('loadingdone', schedule)
  document.fonts?.ready.then(() => { if (container.value) schedule() })
  schedule()
})
onBeforeUnmount(() => { observer?.disconnect(); cancelAnimationFrame(frame); document.fonts?.removeEventListener('loadingdone', schedule) })
</script>
<style scoped>
.members { position: relative; display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 6px 0; min-width: 0; }
.members :deep(.el-tag) { height: auto; min-height: 26px; max-width: 100%; white-space: normal; line-height: 1.6; overflow-wrap: anywhere; }
.members :deep(.el-button) { border-radius: 10px; min-height: 30px; transition: filter .16s, transform .16s; }
.members :deep(.el-button:active) { transform: scale(.97); }
.empty { color: var(--el-text-color-secondary); }
.measure { position: absolute; top: 0; left: 0; visibility: hidden; pointer-events: none; display: flex; flex-wrap: nowrap; gap: 8px; width: max-content; max-width: none; clip-path: inset(100%); }
.measure :deep(.el-tag), .measure :deep(.el-button) { flex-shrink: 0; max-width: none; white-space: nowrap; animation: none; transition: none; transform: none; }
</style>
