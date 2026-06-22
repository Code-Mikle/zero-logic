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
      </template>
    </a-spin>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Empty, message } from 'ant-design-vue'
import { getGenerationTask } from '@/api/generationTaskController'
import RagReferenceList from '@/components/RagReferenceList.vue'

const props = defineProps<{ open: boolean; taskId?: number | string }>()
const emit = defineEmits<{ 'update:open': [value: boolean] }>()

const task = ref<API.GenerationTaskVO>()
const loading = ref(false)
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

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
  }
}

watch(() => [props.open, props.taskId], loadTask)

const closeDrawer = () => emit('update:open', false)
const formatTime = (value?: string) => (value ? new Date(value).toLocaleString('zh-CN') : '-')
const statusText = (status?: string) => ({ pending: '等待中', running: '执行中', success: '成功', failed: '失败', canceled: '已取消' })[status || ''] || status || '-'
const statusColor = (status?: string) => ({ pending: 'default', running: 'processing', success: 'success', failed: 'error', canceled: 'warning' })[status || ''] || 'default'
</script>

<style scoped>
.drawer-section { margin-top: 22px; }
.drawer-section h3 { margin: 0 0 9px; color: #243b35; font-size: 14px; }
.prompt-text { margin: 0; padding: 12px; border-radius: 8px; background: #f5f7f6; line-height: 1.7; white-space: pre-wrap; }
.retrieval-summary { color: #6c7b76; font-size: 12px; }
</style>
