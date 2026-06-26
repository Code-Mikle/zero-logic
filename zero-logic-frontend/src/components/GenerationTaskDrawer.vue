<template>
  <a-drawer :open="open" title="生成任务详情" width="520" @close="closeDrawer">
    <a-spin :spinning="loading">
      <template v-if="task">
        <a-descriptions :column="1" size="small" bordered>
          <a-descriptions-item label="任务 ID">{{ task.id }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="statusColor(task.status)">{{ statusText(task.status) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="当前步骤">{{ task.currentStep || '-' }}</a-descriptions-item>
          <a-descriptions-item label="生成类型">{{ task.codeGenType || '-' }}</a-descriptions-item>
          <a-descriptions-item label="开始时间">{{ formatTime(task.startTime) }}</a-descriptions-item>
          <a-descriptions-item label="结束时间">{{ formatTime(task.endTime) }}</a-descriptions-item>
        </a-descriptions>

        <section class="drawer-section">
          <h3>原始要求</h3>
          <p class="prompt-text">{{ task.inputPrompt || '-' }}</p>
        </section>

        <a-alert
          v-if="task.errorMessage"
          class="drawer-section"
          type="error"
          show-icon
          :message="task.errorMessage"
        />

        <section class="drawer-section">
          <h3>知识库检索</h3>
          <div v-if="task.ragRetrieval" class="retrieval-summary">
            命中 {{ task.ragRetrieval.hitCount || 0 }} 个片段，注入
            {{ task.ragRetrieval.injectedCharLength || 0 }} 个字符
          </div>
          <RagReferenceList v-if="task.ragRetrieval" :retrieval="task.ragRetrieval" />
          <a-empty v-else description="该任务没有检索记录" :image="simpleImage" />
        </section>

        <section class="drawer-section">
          <h3>构建检查</h3>
          <template v-if="task.latestBuild">
            <a-descriptions :column="1" size="small" bordered>
              <a-descriptions-item label="构建状态">
                <a-tag :color="buildStatusColor(task.latestBuild.status)">
                  {{ buildStatusText(task.latestBuild.status) }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="构建轮次">
                {{ task.latestBuild.attemptNo || 1 }}
              </a-descriptions-item>
              <a-descriptions-item label="退出码">
                {{ task.latestBuild.exitCode ?? '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="耗时">
                {{ formatDuration(task.latestBuild.durationMs) }}
              </a-descriptions-item>
              <a-descriptions-item label="构建产物">
                {{ task.latestBuild.artifactPath || '-' }}
              </a-descriptions-item>
            </a-descriptions>
            <a-collapse v-if="task.latestBuild.logText" class="build-log" ghost>
              <a-collapse-panel key="log" header="查看构建日志">
                <pre>{{ task.latestBuild.logText }}</pre>
              </a-collapse-panel>
            </a-collapse>
          </template>
          <a-empty v-else description="该任务尚无构建记录" :image="simpleImage" />
        </section>
        <section class="drawer-section">
          <h3>自动修复</h3>
          <a-timeline v-if="task.repairs?.length">
            <a-timeline-item
              v-for="repair in task.repairs"
              :key="String(repair.id)"
              :color="repair.status === 'success' ? 'green' : repair.status === 'running' ? 'blue' : 'red'"
            >
              <strong>第 {{ repair.repairAttempt }} 轮：{{ repairStatusText(repair.status) }}</strong>
              <span class="repair-duration">{{ formatDuration(repair.durationMs) }}</span>
              <p v-if="repair.changedFiles?.length" class="repair-files">
                修改文件：{{ repair.changedFiles.join('、') }}
              </p>
              <a-alert v-if="repair.errorMessage" type="error" :message="repair.errorMessage" />
              <a-collapse v-if="repair.errorSummary || repair.aiResponse" ghost>
                <a-collapse-panel key="detail" header="查看诊断与修复说明">
                  <pre v-if="repair.errorSummary">{{ repair.errorSummary }}</pre>
                  <pre v-if="repair.aiResponse">{{ repair.aiResponse }}</pre>
                </a-collapse-panel>
              </a-collapse>
            </a-timeline-item>
          </a-timeline>
          <a-empty v-else description="该任务未触发自动修复" :image="simpleImage" />
        </section>

        <section class="drawer-section">
          <h3>工具调用审计</h3>
          <a-timeline v-if="task.toolCalls?.length">
            <a-timeline-item
              v-for="toolCall in task.toolCalls"
              :key="String(toolCall.id)"
              :color="toolStatusColor(toolCall.status)"
            >
              <div class="tool-call-title">
                <strong>{{ toolCall.displayName || toolCall.toolName }}</strong>
                <a-tag :color="riskColor(toolCall.riskLevel)">
                  {{ riskText(toolCall.riskLevel) }}
                </a-tag>
                <a-tag>{{ sourceText(toolCall.callSource) }}</a-tag>
              </div>
              <div class="tool-call-meta">
                {{ toolCall.toolName }} · {{ toolStatusText(toolCall.status) }} ·
                {{ formatDuration(toolCall.durationMs) }}
              </div>
              <a-alert v-if="toolCall.errorMessage" type="error" :message="toolCall.errorMessage" />
              <a-collapse v-if="toolCall.argumentsJson || toolCall.resultSummary" ghost>
                <a-collapse-panel key="detail" header="查看参数与结果摘要">
                  <pre v-if="toolCall.argumentsJson">{{ toolCall.argumentsJson }}</pre>
                  <pre v-if="toolCall.resultSummary">{{ toolCall.resultSummary }}</pre>
                </a-collapse-panel>
              </a-collapse>
            </a-timeline-item>
          </a-timeline>
          <a-empty v-else description="该任务暂无工具调用记录" :image="simpleImage" />
        </section>
      </template>
    </a-spin>
  </a-drawer>
</template>

<script setup lang="ts">
import { onUnmounted, ref, watch } from 'vue'
import { Empty, message } from 'ant-design-vue'
import { getGenerationTask } from '@/api/generationTaskController'
import RagReferenceList from '@/components/RagReferenceList.vue'

const props = defineProps<{ open: boolean; taskId?: number | string }>()
const emit = defineEmits<{ 'update:open': [value: boolean] }>()

const task = ref<API.GenerationTaskVO>()
const loading = ref(false)
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
let pollTimer: ReturnType<typeof setTimeout> | undefined

const loadTask = async () => {
  if (!props.open || !props.taskId) return
  loading.value = true
  try {
    const res = await getGenerationTask(props.taskId)
    if (res.data.code !== 0 || !res.data.data) throw new Error(res.data.message || '任务加载失败')
    task.value = res.data.data
  } catch (error) {
    message.error(error instanceof Error ? error.message : '任务加载失败')
  } finally {
    loading.value = false
    if (props.open && ['pending', 'running'].includes(task.value?.status || '')) {
      pollTimer = setTimeout(loadTask, 1500)
    }
  }
}

watch(() => [props.open, props.taskId], () => {
  if (pollTimer) clearTimeout(pollTimer)
  if (props.open) loadTask()
})
onUnmounted(() => pollTimer && clearTimeout(pollTimer))

const closeDrawer = () => emit('update:open', false)
const formatTime = (value?: string) => (value ? new Date(value).toLocaleString('zh-CN') : '-')
const statusText = (status?: string) => ({ pending: '等待中', running: '执行中', success: '成功', failed: '失败', canceled: '已取消' })[status || ''] || status || '-'
const statusColor = (status?: string) => ({ pending: 'default', running: 'processing', success: 'success', failed: 'error', canceled: 'warning' })[status || ''] || 'default'
const buildStatusText = (status?: string) => ({ running: '构建中', success: '通过', failed: '失败', timeout: '超时' })[status || ''] || status || '-'
const buildStatusColor = (status?: string) => ({ running: 'processing', success: 'success', failed: 'error', timeout: 'warning' })[status || ''] || 'default'
const formatDuration = (duration?: number) => duration === undefined ? '-' : duration < 1000 ? `${duration} ms` : `${(duration / 1000).toFixed(2)} s`
const repairStatusText = (status?: string) => ({ running: '修复中', success: '已修改', failed: '修复失败', timeout: '超时' })[status || ''] || status || '-'
const toolStatusText = (status?: string) => ({ success: '成功', failed: '失败', rejected: '已拒绝' })[status || ''] || status || '-'
const toolStatusColor = (status?: string) => ({ success: 'green', failed: 'red', rejected: 'orange' })[status || ''] || 'gray'
const riskText = (risk?: string) => ({ low: '低风险', medium: '中风险', high: '高风险' })[risk || ''] || risk || '-'
const riskColor = (risk?: string) => ({ low: 'green', medium: 'orange', high: 'red' })[risk || ''] || 'default'
const sourceText = (source?: string) => ({ generate: '生成', repair: '修复', manual: '手动' })[source || ''] || source || '-'
</script>

<style scoped>
.drawer-section { margin-top: 22px; }
.drawer-section h3 { margin: 0 0 9px; color: #243b35; font-size: 14px; }
.prompt-text { margin: 0; padding: 12px; border-radius: 8px; background: #f5f7f6; line-height: 1.7; white-space: pre-wrap; }
.retrieval-summary { color: #6c7b76; font-size: 12px; }
.build-log { margin-top: 8px; border: 1px solid #e7ecea; border-radius: 8px; }
.build-log pre { max-height: 360px; margin: 0; overflow: auto; color: #31433e; font-size: 12px; line-height: 1.55; white-space: pre-wrap; }
.repair-duration { margin-left: 8px; color: #8a9692; font-size: 12px; }
.repair-files { margin: 6px 0; color: #53645f; font-size: 12px; word-break: break-all; }
.tool-call-title { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
.tool-call-meta { margin: 4px 0 6px; color: #8a9692; font-size: 12px; }
</style>
