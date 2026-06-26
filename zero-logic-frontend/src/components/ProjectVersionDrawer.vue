<template>
  <a-drawer :open="open" title="版本管理" width="560" @close="closeDrawer">
    <a-spin :spinning="loading">
      <a-tabs>
        <a-tab-pane key="versions" tab="版本记录">
          <a-list v-if="versions.length" :data-source="versions" item-layout="vertical">
            <template #renderItem="{ item }">
              <a-list-item>
                <template #actions>
                  <a-button
                    type="link"
                    size="small"
                    :loading="actionLoading === `deploy-${item.id}`"
                    @click="deploySelectedVersion(item)"
                  >
                    部署该版本
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    :loading="actionLoading === `rollback-${item.id}`"
                    @click="rollbackSelectedVersion(item)"
                  >
                    回滚到该版本
                  </a-button>
                </template>
                <a-list-item-meta>
                  <template #title>
                    <div class="version-title">
                      <strong>{{ item.versionName || `v${item.versionNo}` }}</strong>
                      <a-tag :color="versionStatusColor(item.status)">
                        {{ versionStatusText(item.status) }}
                      </a-tag>
                    </div>
                  </template>
                  <template #description>
                    <div class="meta-line">
                      任务 ID：{{ item.taskId || '-' }} · 构建记录：{{ item.buildRecordId || '-' }}
                    </div>
                    <div class="meta-line">创建时间：{{ formatTime(item.createTime) }}</div>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
          <a-empty v-else description="暂无版本记录" />
        </a-tab-pane>

        <a-tab-pane key="deploys" tab="部署记录">
          <a-timeline v-if="deployRecords.length">
            <a-timeline-item
              v-for="record in deployRecords"
              :key="String(record.id)"
              :color="deployStatusColor(record.status)"
            >
              <div class="deploy-title">
                <strong>{{ deployTypeText(record.deployType) }}</strong>
                <a-tag :color="deployStatusColor(record.status)">
                  {{ deployStatusText(record.status) }}
                </a-tag>
              </div>
              <div class="meta-line">
                版本 ID：{{ record.versionId }} · deployKey：{{ record.deployKey || '-' }}
              </div>
              <div class="meta-line">时间：{{ formatTime(record.createTime) }}</div>
              <a-alert
                v-if="record.errorMessage"
                class="deploy-error"
                type="error"
                :message="record.errorMessage"
              />
              <a-button
                v-if="record.deployUrl"
                type="link"
                size="small"
                @click="openDeployUrl(record.deployUrl)"
              >
                打开部署地址
              </a-button>
            </a-timeline-item>
          </a-timeline>
          <a-empty v-else description="暂无部署记录" />
        </a-tab-pane>
      </a-tabs>
    </a-spin>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  deployVersion,
  listAppVersions,
  listDeployRecords,
  rollbackVersion,
} from '@/api/appController'

const props = defineProps<{ open: boolean; appId?: number | string }>()
const emit = defineEmits<{
  'update:open': [value: boolean]
  deployed: [deployUrl: string]
}>()

const loading = ref(false)
const actionLoading = ref('')
const versions = ref<API.ProjectVersionVO[]>([])
const deployRecords = ref<API.DeployRecordVO[]>([])

const closeDrawer = () => emit('update:open', false)

const loadData = async () => {
  if (!props.open || !props.appId) return
  loading.value = true
  try {
    const [versionRes, deployRes] = await Promise.all([
      listAppVersions({ appId: props.appId }),
      listDeployRecords({ appId: props.appId }),
    ])
    if (versionRes.data.code !== 0) throw new Error(versionRes.data.message || '版本记录加载失败')
    if (deployRes.data.code !== 0) throw new Error(deployRes.data.message || '部署记录加载失败')
    versions.value = versionRes.data.data || []
    deployRecords.value = deployRes.data.data || []
  } catch (error) {
    message.error(error instanceof Error ? error.message : '版本管理数据加载失败')
  } finally {
    loading.value = false
  }
}

watch(() => [props.open, props.appId], () => {
  if (props.open) loadData()
})

const deploySelectedVersion = async (version: API.ProjectVersionVO) => {
  if (!props.appId || !version.id) return
  actionLoading.value = `deploy-${version.id}`
  try {
    const res = await deployVersion({ appId: props.appId, versionId: version.id })
    if (res.data.code !== 0 || !res.data.data) throw new Error(res.data.message || '部署失败')
    message.success('部署成功')
    emit('deployed', res.data.data)
    await loadData()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '部署失败')
  } finally {
    actionLoading.value = ''
  }
}

const rollbackSelectedVersion = (version: API.ProjectVersionVO) => {
  if (!props.appId || !version.id) return
  Modal.confirm({
    title: '确认回滚版本？',
    content: `将当前应用回滚部署到 ${version.versionName || `v${version.versionNo}`}。`,
    okText: '确认回滚',
    cancelText: '取消',
    onOk: async () => {
      actionLoading.value = `rollback-${version.id}`
      try {
        const res = await rollbackVersion({ appId: props.appId!, versionId: version.id! })
        if (res.data.code !== 0 || !res.data.data) throw new Error(res.data.message || '回滚失败')
        message.success('回滚成功')
        emit('deployed', res.data.data)
        await loadData()
      } catch (error) {
        message.error(error instanceof Error ? error.message : '回滚失败')
      } finally {
        actionLoading.value = ''
      }
    },
  })
}

const openDeployUrl = (url: string) => window.open(url, '_blank')
const formatTime = (value?: string) => (value ? new Date(value).toLocaleString('zh-CN') : '-')
const versionStatusText = (status?: string) => ({ built: '可部署', deployed: '已部署', created: '已创建', failed: '失败' })[status || ''] || status || '-'
const versionStatusColor = (status?: string) => ({ built: 'blue', deployed: 'green', created: 'default', failed: 'red' })[status || ''] || 'default'
const deployStatusText = (status?: string) => ({ running: '部署中', success: '成功', failed: '失败', rolled_back: '已回滚' })[status || ''] || status || '-'
const deployStatusColor = (status?: string) => ({ running: 'blue', success: 'green', failed: 'red', rolled_back: 'orange' })[status || ''] || 'gray'
const deployTypeText = (type?: string) => ({ deploy: '部署', rollback: '回滚' })[type || ''] || type || '-'
</script>

<style scoped>
.version-title,
.deploy-title {
  display: flex;
  gap: 8px;
  align-items: center;
}

.meta-line {
  color: #6f7d78;
  font-size: 12px;
  line-height: 1.8;
}

.deploy-error {
  margin-top: 8px;
}
</style>
