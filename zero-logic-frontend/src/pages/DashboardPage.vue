<template>
  <div class="dashboard-page">
    <div class="dashboard-header">
      <div>
        <h1>运营看板</h1>
        <p>{{ scopeText }}</p>
      </div>
      <a-button :loading="loading" @click="loadDashboard">刷新</a-button>
    </div>

    <a-spin :spinning="loading">
      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card>
            <a-statistic title="生成任务总数" :value="dashboard.totalTaskCount || 0" />
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card>
            <a-statistic title="成功率" :value="dashboard.successRate || 0" suffix="%" />
            <a-progress
              class="metric-progress"
              :percent="dashboard.successRate || 0"
              size="small"
              :show-info="false"
            />
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card>
            <a-statistic
              title="平均耗时"
              :value="dashboard.avgDurationSeconds || 0"
              suffix="秒"
              :precision="2"
            />
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card>
            <a-statistic title="Token 总量" :value="dashboard.totalTokenUsage || 0" />
          </a-card>
        </a-col>
      </a-row>

      <a-row class="section" :gutter="[16, 16]">
        <a-col :xs="24" :lg="12">
          <a-card title="任务状态">
            <a-row :gutter="16">
              <a-col :span="8">
                <a-statistic title="成功" :value="dashboard.successTaskCount || 0" />
              </a-col>
              <a-col :span="8">
                <a-statistic title="失败" :value="dashboard.failedTaskCount || 0" />
              </a-col>
              <a-col :span="8">
                <a-statistic title="运行中" :value="dashboard.runningTaskCount || 0" />
              </a-col>
            </a-row>
          </a-card>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-card title="工程质量">
            <a-row :gutter="16">
              <a-col :span="8">
                <a-statistic title="构建成功" :value="dashboard.buildSuccessCount || 0" />
              </a-col>
              <a-col :span="8">
                <a-statistic title="构建失败" :value="dashboard.buildFailedCount || 0" />
              </a-col>
              <a-col :span="8">
                <a-statistic title="自动修复成功" :value="dashboard.repairSuccessCount || 0" />
              </a-col>
            </a-row>
          </a-card>
        </a-col>
      </a-row>

      <a-row class="section" :gutter="[16, 16]">
        <a-col :xs="24" :lg="12">
          <a-card title="工具调用">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-statistic title="工具调用次数" :value="dashboard.totalToolCallCount || 0" />
              </a-col>
              <a-col :span="12">
                <a-statistic title="高风险工具次数" :value="dashboard.highRiskToolCallCount || 0" />
              </a-col>
            </a-row>
          </a-card>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-card title="修复统计">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-statistic title="修复总数" :value="dashboard.repairTotalCount || 0" />
              </a-col>
              <a-col :span="12">
                <a-statistic title="修复成功率" :value="repairSuccessRate" suffix="%" />
              </a-col>
            </a-row>
          </a-card>
        </a-col>
      </a-row>

      <a-card class="section" title="最近 7 天生成趋势">
        <a-table
          :columns="dailyColumns"
          :data-source="dashboard.dailyStats || []"
          :pagination="false"
          row-key="date"
        />
      </a-card>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { getGenerationDashboard } from '@/api/dashboardController'

const route = useRoute()
const loading = ref(false)
const dashboard = ref<API.GenerationDashboardVO>({})

const appId = computed(() => {
  const value = route.query.appId
  return Array.isArray(value) ? value[0] : value
})

const scopeText = computed(() => (
  appId.value ? `当前应用统计：${appId.value}` : '当前账号可见范围内的生成统计'
))

const repairSuccessRate = computed(() => {
  const total = dashboard.value.repairTotalCount || 0
  if (total <= 0) return 0
  return Number((((dashboard.value.repairSuccessCount || 0) * 100) / total).toFixed(2))
})

const dailyColumns = [
  { title: '日期', dataIndex: 'date', key: 'date' },
  { title: '任务数', dataIndex: 'taskCount', key: 'taskCount' },
  { title: '成功数', dataIndex: 'successCount', key: 'successCount' },
  { title: '失败数', dataIndex: 'failedCount', key: 'failedCount' },
]

const loadDashboard = async () => {
  loading.value = true
  try {
    const res = await getGenerationDashboard(appId.value ? { appId: appId.value } : undefined)
    if (res.data.code !== 0 || !res.data.data) {
      throw new Error(res.data.message || '看板数据加载失败')
    }
    dashboard.value = res.data.data
  } catch (error) {
    message.error(error instanceof Error ? error.message : '看板数据加载失败')
  } finally {
    loading.value = false
  }
}

watch(appId, loadDashboard)

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.dashboard-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 24px 48px;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.dashboard-header h1 {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 700;
}

.dashboard-header p {
  margin: 0;
  color: #667a74;
}

.section {
  margin-top: 16px;
}

.metric-progress {
  margin-top: 12px;
}
</style>
