<template>
  <div id="appChatPage">
    <!-- 顶部工作台栏 -->
    <div class="workspace-header">
      <RouterLink to="/" class="workspace-logo">
        <img class="workspace-logo-img" src="@/assets/ZeroLogic_logo.png" alt="ZeroLogic" />
        <span>ZeroLogic</span>
      </RouterLink>

      <div class="workspace-app-bar">
        <div class="header-left">
          <h1 class="app-name">{{ appInfo?.appName || '网站生成器' }}</h1>
          <a-tag v-if="appInfo?.codeGenType" color="blue" class="code-gen-type-tag">
            {{ formatCodeGenType(appInfo.codeGenType) }}
          </a-tag>
        </div>
        <div class="header-right">
          <div class="toolbar-group">
            <a-tooltip title="查看应用详情" placement="bottom" overlay-class-name="header-action-tooltip">
              <span class="header-tooltip-trigger">
                <a-button
                  class="header-icon-btn"
                  type="text"
                  aria-label="查看应用详情"
                  @click="showAppDetail"
                >
                  <template #icon>
                    <InfoCircleOutlined />
                  </template>
                </a-button>
              </span>
            </a-tooltip>
            <a-tooltip title="下载当前应用代码" placement="bottom" overlay-class-name="header-action-tooltip">
              <span class="header-tooltip-trigger">
                <a-button
                  class="header-icon-btn"
                  type="text"
                  aria-label="下载当前应用代码"
                  @click="downloadCode"
                  :loading="downloading"
                  :disabled="!isOwner"
                >
                  <template #icon>
                    <DownloadOutlined />
                  </template>
                </a-button>
              </span>
            </a-tooltip>
            <a-tooltip title="查看和部署历史版本" placement="bottom" overlay-class-name="header-action-tooltip">
              <span class="header-tooltip-trigger">
                <a-button
                  class="header-icon-btn"
                  type="text"
                  aria-label="查看和部署历史版本"
                  @click="openVersionDrawer"
                  :disabled="!isOwner"
                >
                  <template #icon>
                    <HistoryOutlined />
                  </template>
                </a-button>
              </span>
            </a-tooltip>
            <a-tooltip
              v-if="isOwner && previewUrl"
              :title="isEditMode ? '退出可视化编辑模式' : '进入可视化编辑模式'"
              placement="bottom"
              overlay-class-name="header-action-tooltip"
            >
              <span class="header-tooltip-trigger">
                <a-button
                  class="header-icon-btn"
                  type="text"
                  :danger="isEditMode"
                  :aria-label="isEditMode ? '退出可视化编辑模式' : '进入可视化编辑模式'"
                  @click="toggleEditMode"
                  :class="{ 'edit-mode-active': isEditMode }"
                >
                  <template #icon>
                    <EditOutlined />
                  </template>
                </a-button>
              </span>
            </a-tooltip>
            <a-tooltip
              v-if="previewUrl"
              title="在新窗口打开预览"
              placement="bottom"
              overlay-class-name="header-action-tooltip"
            >
              <span class="header-tooltip-trigger">
                <a-button
                  class="header-icon-btn"
                  type="text"
                  aria-label="在新窗口打开预览"
                  @click="openInNewTab"
                >
                  <template #icon>
                    <ExportOutlined />
                  </template>
                </a-button>
              </span>
            </a-tooltip>
          </div>
          <a-tooltip title="部署当前应用" placement="bottom" overlay-class-name="header-action-tooltip">
            <span class="header-tooltip-trigger">
              <a-button
                class="header-icon-btn deploy-icon-btn"
                type="primary"
                aria-label="部署当前应用"
                @click="deployApp"
                :loading="deploying"
              >
                <template #icon>
                  <CloudUploadOutlined />
                </template>
              </a-button>
            </span>
          </a-tooltip>
        </div>
      </div>

      <a-dropdown v-if="loginUserStore.loginUser.id">
        <a-space class="workspace-user">
          <a-avatar :src="loginUserStore.loginUser.userAvatar" />
          {{ loginUserStore.loginUser.userName ?? '无名' }}
        </a-space>
        <template #overlay>
          <a-menu>
            <a-menu-item @click="router.push('/user/center')">
              <UserOutlined />
              个人中心
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="doLogout">
              <LogoutOutlined />
              退出登录
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
      <a-button v-else type="primary" @click="router.push('/user/login')">登录</a-button>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧对话区域 -->
      <div class="chat-section">
        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer">
          <!-- 加载更多按钮 -->
          <div v-if="hasMoreHistory" class="load-more-container">
            <a-button type="link" @click="loadMoreHistory" :loading="loadingHistory" size="small">
              加载更多历史消息
            </a-button>
          </div>
          <div v-for="(message, index) in messages" :key="index" class="message-item">
            <div v-if="message.type === 'user'" class="user-message">
              <div class="message-content">
                <AttachmentCard v-if="message.attachment" :attachment="message.attachment" />
                <div v-if="message.content" class="message-text">
                  {{ message.content }}
                </div>
              </div>
              <div class="message-avatar">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              </div>
            </div>
            <div v-else class="ai-message">
              <div class="message-avatar">
                <a-avatar :src="aiAvatar" />
              </div>
              <div class="message-content">
                <MarkdownRenderer v-if="message.content" :content="message.content" />
                <RagReferenceList
                  v-if="message.ragRetrieval"
                  :retrieval="message.ragRetrieval"
                />
                <div v-if="message.loading" class="loading-indicator">
                  <a-spin size="small" />
                  <span>AI 正在思考...</span>
                </div>
                <a-alert
                  v-if="message.generationError"
                  class="generation-error"
                  type="error"
                  show-icon
                  :message="message.generationError"
                />
                <div v-if="message.taskId && !message.loading" class="message-meta-actions">
                  <a-button type="link" size="small" @click="openTaskDetail(message.taskId)">
                    查看任务详情
                  </a-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 选中元素信息展示 -->
        <a-alert
          v-if="selectedElementInfo"
          class="selected-element-alert"
          type="info"
          closable
          @close="clearSelectedElement"
        >
          <template #message>
            <div class="selected-element-info">
              <div class="element-header">
                <span class="element-tag">
                  选中元素：{{ selectedElementInfo.tagName.toLowerCase() }}
                </span>
                <span v-if="selectedElementInfo.id" class="element-id">
                  #{{ selectedElementInfo.id }}
                </span>
                <span v-if="selectedElementInfo.className" class="element-class">
                  .{{ selectedElementInfo.className.split(' ').join('.') }}
                </span>
              </div>
              <div class="element-details">
                <div v-if="selectedElementInfo.textContent" class="element-item">
                  内容: {{ selectedElementInfo.textContent.substring(0, 50) }}
                  {{ selectedElementInfo.textContent.length > 50 ? '...' : '' }}
                </div>
                <div v-if="selectedElementInfo.pagePath" class="element-item">
                  页面路径: {{ selectedElementInfo.pagePath }}
                </div>
                <div class="element-item">
                  选择器:
                  <code class="element-selector-code">{{ selectedElementInfo.selector }}</code>
                </div>
              </div>
            </div>
          </template>
        </a-alert>

        <!-- 用户消息输入框 -->
        <div class="input-container">
          <div class="input-wrapper">
            <a-tooltip v-if="!isOwner" title="无法在别人的作品下对话哦~" placement="top">
              <a-textarea
                v-model:value="userInput"
                :placeholder="getInputPlaceholder()"
                :rows="4"
                :maxlength="1000"
                @keydown.enter.exact.prevent="sendMessage"
                :disabled="isGenerating || !isOwner"
              />
            </a-tooltip>
            <a-textarea
              v-else
              v-model:value="userInput"
              :placeholder="getInputPlaceholder()"
              :rows="4"
              :maxlength="1000"
              @keydown.enter.exact.prevent="sendMessage"
              :disabled="isGenerating"
            />
            <div class="upload-actions">
              <a-tooltip
                title="支持上传 TXT、Markdown、PDF，单个文件不超过 5 MB"
                placement="top"
                overlay-class-name="header-action-tooltip"
              >
                <span class="composer-tooltip-trigger">
                  <a-upload
                    v-model:file-list="fileList"
                    :max-count="1"
                    :show-upload-list="true"
                    :before-upload="beforeUpload"
                    :disabled="uploading || isGenerating || !isOwner"
                    @remove="handleRemoveAttachment()"
                  >
                    <a-button
                      class="composer-icon-btn composer-upload-btn"
                      type="text"
                      size="small"
                      aria-label="上传附件"
                      :loading="uploading"
                      :disabled="isGenerating || !isOwner"
                    >
                      <template #icon>
                        <PaperClipOutlined />
                      </template>
                      {{ fileList.length ? '更换' : '上传' }}
                    </a-button>
                  </a-upload>
                </span>
              </a-tooltip>
            </div>
            <div class="input-actions">
              <a-tooltip
                title="Enter 发送，Shift + Enter 换行"
                placement="top"
                overlay-class-name="header-action-tooltip"
              >
                <span class="composer-tooltip-trigger">
                  <a-button
                    class="composer-icon-btn composer-send-btn"
                    :class="{ 'composer-send-active': userInput.trim() || selectedFile }"
                    type="primary"
                    aria-label="发送消息"
                    @click="sendMessage"
                    :loading="isGenerating"
                    :disabled="!isOwner"
                  >
                    <template #icon>
                      <ArrowUpOutlined />
                    </template>
                  </a-button>
                </span>
              </a-tooltip>
            </div>
          </div>
        </div>
      </div>
      <!-- 右侧网页展示区域 -->
      <div class="preview-section">
        <div class="preview-content">
          <div v-if="!previewUrl && !isGenerating" class="preview-placeholder">
            <div class="placeholder-icon">🌐</div>
            <p>网站文件生成完成后将在这里展示</p>
          </div>
          <div v-else-if="isGenerating" class="preview-loading">
            <a-spin size="large" />
            <p>正在生成网站...</p>
          </div>
          <iframe
            v-else
            :src="previewUrl"
            class="preview-iframe"
            frameborder="0"
            @load="onIframeLoad"
          ></iframe>
        </div>
      </div>
    </div>

    <!-- 应用详情弹窗 -->
    <AppDetailModal
      v-model:open="appDetailVisible"
      :app="appInfo"
      :show-actions="isOwner || isAdmin"
      @edit="editApp"
      @delete="deleteApp"
    />

    <!-- 部署成功弹窗 -->
    <DeploySuccessModal
      v-model:open="deployModalVisible"
      :deploy-url="deployUrl"
      @open-site="openDeployedSite"
    />
    <GenerationTaskDrawer
      v-model:open="taskDrawerVisible"
      :task-id="selectedTaskId"
    />
    <ProjectVersionDrawer
      v-model:open="versionDrawerVisible"
      :app-id="appId"
      @deployed="handleVersionDeployed"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Upload, type UploadFile, type UploadProps } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  getAppVoById,
  deployApp as deployAppApi,
  deleteApp as deleteAppApi,
} from '@/api/appController'
import { userLogout } from '@/api/userController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import { CodeGenTypeEnum, formatCodeGenType } from '@/utils/codeGenTypes'
import request from '@/request'

import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import aiAvatar from '@/assets/ZeroLogic_avatar.png'
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'
import { VisualEditor, type ElementInfo } from '@/utils/visualEditor'

import {
  CloudUploadOutlined,
  ArrowUpOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  DownloadOutlined,
  EditOutlined,
  PaperClipOutlined,
  UserOutlined,
  LogoutOutlined,
  HistoryOutlined,
} from '@ant-design/icons-vue'
import { uploadAttachment } from '@/api/attachmentControllers.ts'
import { createGenerationTask } from '@/api/generationTaskController'
import AttachmentCard from '@/components/AttachmentCard.vue'
import RagReferenceList from '@/components/RagReferenceList.vue'
import GenerationTaskDrawer from '@/components/GenerationTaskDrawer.vue'
import ProjectVersionDrawer from '@/components/ProjectVersionDrawer.vue'

const route = useRoute()

// 附件 id，从 HomePage 的路由参数中获取
const initialAttachmentId = computed(() => {
  const value = route.query.attachmentId
  const target = Array.isArray(value) ? value[0] : value
  if (typeof target !== 'string') {
    return undefined
  }

  const id = Number(target)
  return Number.isSafeInteger(id) && id > 0 ? id : undefined
})
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用信息
const appInfo = ref<API.AppVO>()
const appId = ref<any>()

// 对话相关
interface Message {
  id?: string
  type: 'user' | 'ai'
  content: string
  attachment?: API.promptAttachmentVO
  loading?: boolean
  createTime?: string
  taskId?: number | string
  ragRetrieval?: API.RagRetrievalVO
  generationError?: string
}

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement>()
const taskDrawerVisible = ref(false)
const selectedTaskId = ref<number | string>()
const versionDrawerVisible = ref(false)

// 对话历史相关
const loadingHistory = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string>()
const historyLoaded = ref(false)

// 预览相关
const previewUrl = ref('')
const previewReady = ref(false)

// 部署相关
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

// 下载相关
const downloading = ref(false)

// 可视化编辑相关
const isEditMode = ref(false)
const selectedElementInfo = ref<ElementInfo | null>(null)
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo: ElementInfo) => {
    selectedElementInfo.value = elementInfo
  },
})

// 权限相关
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

// 应用详情相关
const appDetailVisible = ref(false)

// 显示应用详情
const showAppDetail = () => {
  appDetailVisible.value = true
}

const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}

// 加载对话历史
const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || loadingHistory.value) return
  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: appId.value,
      pageSize: 10,
    }
    // 如果是加载更多，传递最后一条消息的创建时间作为游标
    if (isLoadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }
    const res = await listAppChatHistory(params)
    // console.log('qwe: ', res)
    if (res.data.code === 0 && res.data.data) {
      const chatHistories = res.data.data.records || []
      if (chatHistories.length > 0) {
        // 将对话历史转换为消息格式，并按时间正序排列（老消息在前）
        // console.log('asd: ', chatHistories)
        const historyMessages: Message[] = chatHistories
          .map((chat) => ({
            type: (chat.messageType === 'user' ? 'user' : 'ai') as 'user' | 'ai',
            content: chat.message || '',
            attachment: chat.promptAttachmentVO,
            createTime: chat.createTime,
            taskId: chat.taskId,
            ragRetrieval: chat.ragRetrieval,
          }))
          .reverse() // 反转数组，让老消息在前
        if (isLoadMore) {
          // 加载更多时，将历史消息添加到开头
          messages.value.unshift(...historyMessages)
        } else {
          // 初始加载，直接设置消息列表
          messages.value = historyMessages
        }
        // 更新游标
        lastCreateTime.value = chatHistories[chatHistories.length - 1]?.createTime
        // 检查是否还有更多历史
        hasMoreHistory.value = chatHistories.length === 10
      } else {
        hasMoreHistory.value = false
      }
      historyLoaded.value = true
    }
  } catch (error) {
    console.error('加载对话历史失败：', error)
    message.error('加载对话历史失败')
  } finally {
    loadingHistory.value = false
  }
}

// 加载更多历史消息
const loadMoreHistory = async () => {
  await loadChatHistory(true)
}

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return
  }

  appId.value = id

  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 先加载对话历史
      await loadChatHistory()
      // 如果有至少2条对话记录，展示对应的网站
      if (messages.value.length >= 2) {
        updatePreview()
      }
      // 检查是否需要自动发送初始提示词
      // 只有在是自己的应用且没有对话历史时才自动发送
      if (
        appInfo.value.initPrompt &&
        isOwner.value &&
        messages.value.length === 0 &&
        historyLoaded.value
      ) {
        await sendInitialMessage(appInfo.value.initPrompt, appInfo.value.promptAttachmentVO)
      }
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error('获取应用信息失败')
    router.push('/')
  }
}

// 发送初始消息
const sendInitialMessage = async (prompt: string, attachment?: API.promptAttachmentVO) => {
  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: prompt,
    attachment,
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex, initialAttachmentId.value)
}

// 文件上传

// 状态
const fileList = ref<UploadFile[]>([])
const selectedFile = ref<File>()
const pendingAttachment = ref<API.promptAttachmentVO>()
const pendingAttachmentId = ref<number>()
const uploading = ref(false)

// 上传校验 包括文件类型校验
const allowedExtensions = ['txt', 'md', 'markdown', 'pdf']

const getFileExtension = (fileName: string) => {
  const index = fileName.lastIndexOf('.')
  return index >= 0 ? fileName.substring(index + 1).toLowerCase() : ''
}

const isAllowedAttachment = (file: File) => {
  return allowedExtensions.includes(getFileExtension(file.name))
}

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const maxSize = 5 * 1024 * 1024

  if (file.size > maxSize) {
    message.error(`${file.name} 文件大小不能超过 5 MB`)
    return Upload.LIST_IGNORE
  }

  if (!isAllowedAttachment(file)) {
    message.error('仅支持 TXT、Markdown、PDF 格式文件')
    return Upload.LIST_IGNORE
  }

  selectedFile.value = file
  pendingAttachmentId.value = undefined
  pendingAttachment.value = undefined

  fileList.value = [
    {
      uid: file.uid,
      name: file.name,
      size: file.size,
      type: file.type,
      status: 'done',
      originFileObj: file,
    },
  ]
  return false
}

// 移除文件
const handleRemoveAttachment = () => {
  selectedFile.value = undefined
  pendingAttachmentId.value = undefined
  pendingAttachment.value = undefined
  fileList.value = []
  return true
}

// 发送消息
const DEFAULT_ATTACHMENT_PROMPT = '请根据附件内容生成应用'

const sendMessage = async () => {
  const input = userInput.value.trim()
  const hasSelectedFile = Boolean(selectedFile.value)
  if ((!input && !hasSelectedFile) || isGenerating.value || uploading.value) {
    return
  }

  let userMessage = input || DEFAULT_ATTACHMENT_PROMPT

  // 如果有选中的元素，将元素信息添加到提示词中
  if (selectedElementInfo.value) {
    let elementContext = `\n\n选中元素信息：`
    if (selectedElementInfo.value.pagePath) {
      elementContext += `\n- 页面路径: ${selectedElementInfo.value.pagePath}`
    }
    elementContext += `\n- 标签: ${selectedElementInfo.value.tagName.toLowerCase()}\n- 选择器: ${selectedElementInfo.value.selector}`
    if (selectedElementInfo.value.textContent) {
      elementContext += `\n- 当前内容: ${selectedElementInfo.value.textContent.substring(0, 100)}`
    }
    userMessage += elementContext
  }
  userInput.value = ''

  // 附件上传

  let currentAttachmentId: number | undefined
  let currentAttachment: API.promptAttachmentVO | undefined

  if (selectedFile.value) {
    const currentAppId = Number(appId.value || route.params.id)
    if (!Number.isSafeInteger(currentAppId) || currentAppId <= 0) {
      message.error('应用ID不存在，无法上传附件')
      return
    }

    uploading.value = true
    try {
      const uploadRes = await uploadAttachment(selectedFile.value, currentAppId)

      if (uploadRes.data.code !== 0 || !uploadRes.data.data) {
        throw new Error(uploadRes.data.message || '文件上传失败')
      }

      currentAttachmentId = uploadRes.data.data.id
      currentAttachment = {
        id: uploadRes.data.data.id,
        fileName: selectedFile.value.name,
        fileSize: selectedFile.value.size,
        contentType: selectedFile.value.type,
      }
    } catch (error) {
      message.error(error instanceof Error ? error.message : '文件上传失败')
      return
    } finally {
      uploading.value = false
    }
  }

  // 添加用户消息（包含元素信息）
  messages.value.push({
    type: 'user',
    content: userMessage,
    attachment: currentAttachment,
  })

  selectedFile.value = undefined
  pendingAttachmentId.value = undefined
  pendingAttachment.value = undefined
  fileList.value = []

  // 发送消息后，清除选中元素并退出编辑模式
  if (selectedElementInfo.value) {
    clearSelectedElement()
    if (isEditMode.value) {
      toggleEditMode()
    }
  }

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(userMessage, aiMessageIndex, currentAttachmentId)
}

// 生成代码 - 使用 EventSource 处理流式响应
const generateCode = async (
  userMessage: string,
  aiMessageIndex: number,
  currentAttachmentId?: number,
) => {
  let eventSource: EventSource | null = null
  let streamCompleted = false

  try {
    // 获取 axios 配置的 baseURL
    const baseURL = request.defaults.baseURL || API_BASE_URL

    const createRes = await createGenerationTask({
      appId: appId.value,
      message: userMessage,
      attachmentId: currentAttachmentId,
    })

    if (createRes.data.code !== 0 || !createRes.data.data) {
      throw new Error(createRes.data.message || '创建生成任务失败')
    }

    const taskId = createRes.data.data
    messages.value[aiMessageIndex].taskId = taskId
    const url = `${baseURL}/generation/task/${taskId}/stream`

    eventSource = new EventSource(url, {
      withCredentials: true,
    })

    let fullContent = ''

    eventSource.addEventListener('rag-references', function (event: MessageEvent) {
      if (streamCompleted) return
      try {
        messages.value[aiMessageIndex].ragRetrieval = JSON.parse(event.data) as API.RagRetrievalVO
      } catch (error) {
        console.error('解析 RAG 引用失败:', error)
      }
    })

    // 处理接收到的消息
    eventSource.onmessage = function (event) {
      if (streamCompleted) return

      try {
        // 解析JSON包装的数据
        const parsed = JSON.parse(event.data)
        const content = parsed.d

        // 拼接内容
        if (content !== undefined && content !== null) {
          fullContent += content
          messages.value[aiMessageIndex].content = fullContent
          messages.value[aiMessageIndex].loading = false
          scrollToBottom()
        }
      } catch (error) {
        console.error('解析消息失败:', error)
        handleError(error, aiMessageIndex)
      }
    }

    // 处理done事件
    eventSource.addEventListener('done', function () {
      if (streamCompleted) return

      streamCompleted = true
      isGenerating.value = false
      eventSource?.close()

      // 延迟更新预览，确保后端已完成处理
      setTimeout(async () => {
        await fetchAppInfo()
        updatePreview()
      }, 1000)
    })

    // 处理business-error事件（后端限流等错误）
    eventSource.addEventListener('business-error', function (event: MessageEvent) {
      if (streamCompleted) return

      try {
        const errorData = JSON.parse(event.data)
        console.error('SSE业务错误事件:', errorData)

        // 显示具体的错误信息
        const errorMessage = errorData.message || '生成过程中出现错误'
        if (!messages.value[aiMessageIndex].content) {
          messages.value[aiMessageIndex].content = '生成流程未能完成。'
        }
        messages.value[aiMessageIndex].generationError = errorMessage
        messages.value[aiMessageIndex].loading = false
        message.error(errorMessage)

        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()
      } catch (parseError) {
        console.error('解析错误事件失败:', parseError, '原始数据:', event.data)
        handleError(new Error('服务器返回错误'), aiMessageIndex)
      }
    })

    // 处理错误
    eventSource.onerror = function () {
      if (streamCompleted || !isGenerating.value) return
      // 检查是否是正常的连接关闭
      if (eventSource?.readyState === EventSource.CONNECTING) {
        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()

        setTimeout(async () => {
          await fetchAppInfo()
          updatePreview()
        }, 1000)
      } else {
        handleError(new Error('SSE连接错误'), aiMessageIndex)
      }
    }
  } catch (error) {
    console.error('创建 EventSource 失败：', error)
    handleError(error, aiMessageIndex)
  }
}

// 错误处理函数
const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error('生成代码失败：', error)
  messages.value[aiMessageIndex].content = '抱歉，生成过程中出现了错误，请重试。'
  messages.value[aiMessageIndex].loading = false
  message.error('生成失败，请重试')
  isGenerating.value = false
}

const openTaskDetail = (taskId: number | string) => {
  selectedTaskId.value = taskId
  taskDrawerVisible.value = true
}

const openVersionDrawer = () => {
  versionDrawerVisible.value = true
}

const withCacheBust = (url: string) => {
  const separator = url.includes('?') ? '&' : '?'
  return `${url}${separator}_t=${Date.now()}`
}

// 更新预览
const updatePreview = () => {
  if (appId.value) {
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    const newPreviewUrl = getStaticPreviewUrl(codeGenType, appId.value)
    previewUrl.value = withCacheBust(newPreviewUrl)
    previewReady.value = true
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 下载代码
const downloadCode = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }
  downloading.value = true
  try {
    const API_BASE_URL = request.defaults.baseURL || ''
    const url = `${API_BASE_URL}/app/download/${appId.value}`
    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error(`下载失败: ${response.status}`)
    }
    // 获取文件名
    const contentDisposition = response.headers.get('Content-Disposition')
    const fileName = contentDisposition?.match(/filename="(.+)"/)?.[1] || `app-${appId.value}.zip`
    // 下载文件
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    link.click()
    // 清理
    URL.revokeObjectURL(downloadUrl)
    message.success('代码下载成功')
  } catch (error) {
    console.error('下载失败：', error)
    message.error('下载失败，请重试')
  } finally {
    downloading.value = false
  }
}

// 部署应用
const deployApp = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: appId.value as unknown as number,
    })

    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')
      await fetchAppInfo()
      updatePreview()
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch (error) {
    console.error('部署失败：', error)
    message.error('部署失败，请重试')
  } finally {
    deploying.value = false
  }
}

const handleVersionDeployed = async (url: string) => {
  deployUrl.value = url
  deployModalVisible.value = true
  await fetchAppInfo()
  updatePreview()
}

// 在新窗口打开预览
const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 打开部署的网站
const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(withCacheBust(deployUrl.value), '_blank')
  }
}

// iframe加载完成
const onIframeLoad = () => {
  previewReady.value = true
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (iframe) {
    visualEditor.init(iframe)
    visualEditor.onIframeLoad()
  }
}

// 编辑应用
const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// 删除应用
const deleteApp = async () => {
  if (!appInfo.value?.id) return

  try {
    const res = await deleteAppApi({ id: appInfo.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      appDetailVisible.value = false
      router.push('/')
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}

// 可视化编辑相关函数
const toggleEditMode = () => {
  // 检查 iframe 是否已经加载
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (!iframe) {
    message.warning('请等待页面加载完成')
    return
  }
  // 确保 visualEditor 已初始化
  if (!previewReady.value) {
    message.warning('请等待页面加载完成')
    return
  }
  const newEditMode = visualEditor.toggleEditMode()
  isEditMode.value = newEditMode
}

const clearSelectedElement = () => {
  selectedElementInfo.value = null
  visualEditor.clearSelection()
}

const getInputPlaceholder = () => {
  if (selectedElementInfo.value) {
    return `正在编辑 ${selectedElementInfo.value.tagName.toLowerCase()} 元素，描述您想要的修改...`
  }
  return '请描述你想生成的网站，越详细效果越好哦'
}

// 页面加载时获取应用信息
onMounted(() => {
  fetchAppInfo()

  // 监听 iframe 消息
  window.addEventListener('message', (event) => {
    visualEditor.handleIframeMessage(event)
  })
})

// 清理资源
onUnmounted(() => {
  // EventSource 会在组件卸载时自动清理
})
</script>

<style scoped>
#appChatPage {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fdfdfd;
  overflow: hidden;
}

/* 顶部工作台栏 */
.workspace-header {
  height: 56px;
  flex-shrink: 0;
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  padding: 0 24px 0 20px;
  background: rgba(255, 255, 255, 0.88);
  border-bottom: 1px solid rgba(15, 23, 42, 0.07);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.75) inset, 0 10px 30px rgba(15, 23, 42, 0.04);
  backdrop-filter: blur(16px);
}

.workspace-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  width: fit-content;
  padding: 7px 9px;
  border-radius: 14px;
  color: #1677ff;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0;
  transition:
    background-color 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

.workspace-logo:hover {
  color: #1677ff;
  background: rgba(22, 119, 255, 0.08);
  transform: translateY(-1px);
}

.workspace-logo-img {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
}

.workspace-app-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-width: 0;
}

.workspace-user {
  min-height: 40px;
  padding: 4px 10px 4px 5px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.9);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  cursor: pointer;
  white-space: nowrap;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.workspace-user:hover {
  background: #fff;
  border-color: rgba(22, 119, 255, 0.24);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  width: fit-content;
  max-width: 100%;
  padding: 6px 8px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.72);
}

.code-gen-type-tag {
  height: 24px;
  margin-inline-end: 0;
  padding: 0 9px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 22px;
  color: #1668dc;
  background: rgba(22, 119, 255, 0.08);
  border-color: rgba(22, 119, 255, 0.22);
  flex-shrink: 0;
}

.app-name {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.toolbar-group {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 3px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.88);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.header-tooltip-trigger {
  display: inline-flex;
}

.header-icon-btn {
  width: 36px;
  height: 34px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  color: #334155;
  border: none;
  box-shadow: none;
  transition:
    background-color 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.header-icon-btn:hover {
  color: #0f172a;
  background: #fff;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.header-icon-btn:disabled {
  background: transparent;
  box-shadow: none;
  transform: none;
}

.deploy-icon-btn {
  width: 42px;
  height: 38px;
  border-radius: 12px;
  color: #fff;
  background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
  box-shadow: 0 8px 18px rgba(22, 119, 255, 0.24);
}

.deploy-icon-btn:hover {
  color: #fff;
  background: linear-gradient(135deg, #4096ff 0%, #1677ff 100%);
  box-shadow: 0 10px 22px rgba(22, 119, 255, 0.3);
}

.header-icon-btn :deep(.anticon) {
  font-size: 16px;
}

:global(.header-action-tooltip .ant-tooltip-inner) {
  min-height: 30px;
  padding: 6px 10px;
  border-radius: 6px;
  background: #1f2937;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.18);
}

:global(.header-action-tooltip .ant-tooltip-arrow::before) {
  background: #1f2937;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 8px;
  padding: 8px;
  overflow: hidden;
}

/* 左侧对话区域 */
.chat-section {
  flex: 2;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.messages-container {
  flex: 1;
  min-height: 0;
  padding: 16px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.message-item {
  margin-bottom: 12px;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 8px;
}

.ai-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 8px;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.5;
  word-wrap: break-word;
}

.user-message .message-content {
  background: #1890ff;
  color: white;
}

.ai-message .message-content {
  background: #f5f5f5;
  color: #1a1a1a;
  padding: 8px 12px;
}

.message-avatar {
  flex-shrink: 0;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
}

.message-meta-actions {
  margin-top: 6px;
  text-align: right;
}

.generation-error {
  margin-top: 10px;
}

.message-meta-actions :deep(.ant-btn) {
  height: auto;
  padding: 0;
  color: #667a74;
  font-size: 12px;
}

/* 加载更多按钮 */
.load-more-container {
  text-align: center;
  padding: 8px 0;
  margin-bottom: 16px;
}

/* 输入区域 */
.input-container {
  padding: 16px;
  background: white;
}

.input-wrapper {
  position: relative;
}

.input-wrapper :deep(.ant-input) {
  padding: 12px 56px 48px 12px;
  resize: none;
}

.upload-actions {
  position: absolute;
  left: 12px;
  bottom: 12px;
  z-index: 2;
}

.upload-actions :deep(.ant-upload-list) {
  position: absolute;
  left: 0;
  bottom: 38px;
  width: 260px;
}

.input-actions {
  position: absolute;
  bottom: 12px;
  right: 12px;
}

.composer-tooltip-trigger {
  display: inline-flex;
}

.composer-icon-btn {
  min-width: 38px;
  height: 38px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.composer-icon-btn :deep(.anticon) {
  font-size: 16px;
}

.composer-upload-btn {
  gap: 5px;
  min-width: auto;
  height: 32px;
  padding: 0 12px;
  color: #111827;
  background: #f3f4f6;
  border: 1px solid transparent;
  box-shadow: none;
  font-size: 14px;
  font-weight: 500;
}

.composer-upload-btn:hover {
  color: #111827;
  background: #e9edf2;
  border-color: transparent;
  box-shadow: none;
  transform: translateY(-1px);
}

.composer-send-btn {
  color: #fff;
  border: none;
  background: #c4c8cf;
  box-shadow: none;
}

.composer-send-active {
  background: #111827;
}

.composer-send-btn:hover,
.composer-send-active:hover {
  color: #fff;
  background: #111827;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.18);
  transform: translateY(-1px);
}

.composer-upload-btn:disabled,
.composer-upload-btn:disabled:hover {
  color: #94a3b8;
  background: #f3f4f6;
  box-shadow: none;
  transform: none;
}

.composer-send-btn:disabled,
.composer-send-btn:disabled:hover {
  color: #fff;
  background: #c4c8cf;
  box-shadow: none;
  transform: none;
}

/* 右侧预览区域 */
.preview-section {
  flex: 3;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.preview-content {
  flex: 1;
  min-height: 0;
  position: relative;
  overflow: hidden;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.preview-loading p {
  margin-top: 16px;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.selected-element-alert {
  margin: 0 16px;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .workspace-header {
    grid-template-columns: 170px minmax(0, 1fr) auto;
    padding: 0 16px;
  }

  .workspace-app-bar {
    gap: 12px;
  }

  .header-right {
    gap: 8px;
  }

  .toolbar-group {
    gap: 1px;
  }

  .main-content {
    flex-direction: column;
  }

  .chat-section,
  .preview-section {
    flex: 1 1 0;
    height: auto;
  }
}

@media (max-width: 768px) {
  .workspace-header {
    height: auto;
    min-height: 64px;
    grid-template-columns: 1fr auto;
    gap: 8px 12px;
    padding: 10px 12px;
  }

  .workspace-app-bar {
    grid-column: 1 / -1;
    order: 3;
    align-items: flex-start;
    gap: 10px;
  }

  .workspace-logo-img {
    width: 36px;
    height: 36px;
  }

  .app-name {
    font-size: 16px;
  }

  .header-left {
    max-width: calc(100vw - 160px);
  }

  .header-right {
    overflow-x: auto;
    max-width: 100%;
    padding-bottom: 2px;
  }

  .header-icon-btn {
    width: 34px;
    height: 32px;
  }

  .deploy-icon-btn {
    width: 38px;
    height: 34px;
  }

  .main-content {
    padding: 8px;
    gap: 8px;
  }

  .message-content {
    max-width: 85%;
  }

  /* 选中元素信息样式 */
  .selected-element-alert {
    margin: 0 16px;
  }

  .selected-element-info {
    line-height: 1.4;
  }

  .element-header {
    margin-bottom: 8px;
  }

  .element-details {
    margin-top: 8px;
  }

  .element-item {
    margin-bottom: 4px;
    font-size: 13px;
  }

  .element-item:last-child {
    margin-bottom: 0;
  }

  .element-tag {
    font-family: 'Monaco', 'Menlo', monospace;
    font-size: 14px;
    font-weight: 600;
    color: #007bff;
  }

  .element-id {
    color: #28a745;
    margin-left: 4px;
  }

  .element-class {
    color: #ffc107;
    margin-left: 4px;
  }

  .element-selector-code {
    font-family: 'Monaco', 'Menlo', monospace;
    background: #f6f8fa;
    padding: 2px 4px;
    border-radius: 3px;
    font-size: 12px;
    color: #d73a49;
    border: 1px solid #e1e4e8;
  }

  /* 编辑模式按钮样式 */
  .edit-mode-active {
    background-color: #52c41a !important;
    border-color: #52c41a !important;
    color: white !important;
  }

  .edit-mode-active:hover {
    background-color: #73d13d !important;
    border-color: #73d13d !important;
  }
}
</style>
