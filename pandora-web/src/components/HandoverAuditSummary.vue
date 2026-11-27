<template>
  <dl class="handover-audit" aria-label="离职交接详情">
    <div><dt>离职人员</dt><dd>{{ summary.leaver }}</dd></div>
    <div><dt>交接任务</dt><dd><el-tag size="small" type="info">{{ summary.count }}</el-tag></dd></div>
    <div><dt>岗位接任</dt><dd>{{ summary.position }}</dd></div>
    <div><dt>任务接收</dt><dd>{{ summary.tasks }}</dd></div>
  </dl>
</template>

<script setup>
defineProps({ summary: { type: Object, required: true } })
</script>

<style scoped>
.handover-audit { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 8px 16px; margin: 4px 0; }
.handover-audit div { min-width: 0; }
dt { color: var(--el-text-color-secondary); font-size: 12px; }
dd { margin: 2px 0 0; overflow-wrap: anywhere; color: var(--el-text-color-primary); }
</style>
