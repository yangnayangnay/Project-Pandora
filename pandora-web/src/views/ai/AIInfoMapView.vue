<template>
  <div class="ai-info-map-view">
    <div ref="chartRef" style="width: 100%; height: 600px;" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

const chartRef = ref(null)

onMounted(async () => {
  try {
    const res = await request.get('/view/info-map')
    const data = res.data.data
    const chart = echarts.init(chartRef.value)
    chart.setOption({
      tooltip: {},
      series: [{
        type: 'graph',
        layout: 'force',
        data: (data.nodes || []).map(n => ({ name: n.name || n.id })),
        links: (data.edges || []).map(e => ({ source: e.source, target: e.target })),
        roam: true,
        label: { show: true },
        force: { repulsion: 100 }
      }]
    })
  } catch (e) {
    console.error('加载信息地图失败', e)
  }
})
</script>