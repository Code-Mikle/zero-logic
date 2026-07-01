<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Upload, type UploadFile, type UploadProps } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, listMyAppVoByPage, listGoodAppVoByPage } from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import { ArrowUpOutlined, PaperClipOutlined } from '@ant-design/icons-vue'
import { uploadAttachment } from '@/api/attachmentControllers'
import AppCard from '@/components/AppCard.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 用户提示词
const userPrompt = ref('')
const creating = ref(false)

// 文件上传
// 状态
const fileList = ref<UploadFile[]>([])
const selectedFile = ref<File>()
const attachmentId = ref<number>()
const uploading = ref(false)

// 移除文件时清理状态
const handleRemove = () => {
  selectedFile.value = undefined
  attachmentId.value = undefined
  fileList.value = []
  return true
}

const allowedExtensions = ['txt', 'md', 'markdown', 'pdf']

const getFileExtension = (fileName: string) => {
  const index = fileName.lastIndexOf('.')
  return index >= 0 ? fileName.substring(index + 1).toLowerCase() : ''
}

const isAllowedAttachment = (file: File) => {
  return allowedExtensions.includes(getFileExtension(file.name))
}

// 添加上传前校验逻辑
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录后再上传附件')
    return Upload.LIST_IGNORE
  }

  const maxSize = 5 * 1024 * 1024
  if (file.size > maxSize) {
    message.error(`${file.name} 文件大小不能超过 5 MB.`)
    return Upload.LIST_IGNORE
  }

  if (!isAllowedAttachment(file)) {
    message.error('仅支持 TXT、Markdown、PDF 格式文件')
    return Upload.LIST_IGNORE
  }

  selectedFile.value = file
  attachmentId.value = undefined

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

  // 阻止 a-upload 立即上传
  return false
}

// 我的应用数据
const myApps = ref<API.AppVO[]>([])
const myAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

// 精选应用数据
const featuredApps = ref<API.AppVO[]>([])
const featuredAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

// 设置提示词
const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
}

// 优化提示词功能已移除

// 创建应用
const createApp = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请输入应用描述')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return
  }

  creating.value = true
  try {
    if (selectedFile.value && !attachmentId.value) {
      uploading.value = true

      const uploadRes = await uploadAttachment(selectedFile.value)

      if (uploadRes.data.code !== 0 || !uploadRes.data.data) {
        throw new Error(uploadRes.data.message || '附件上传失败')
      }

      attachmentId.value = uploadRes.data.data.id
    }

    const res = await addApp({
      initPrompt: userPrompt.value.trim(),
      attachmentId: attachmentId.value,
    })

    if (res.data.code !== 0 || !res.data.data) {
      throw new Error(res.data.message || '应用创建失败')
    }

    const appId = String(res.data.data)
    message.success('应用创建成功')

    await router.push({
      path: `/app/chat/${appId}`,
      query: attachmentId.value ? { attachmentId: String(attachmentId.value) } : undefined,
    })
  } catch (error) {
    message.error(error instanceof Error ? error.message : '应用创建失败，请重试')
  } finally {
    uploading.value = false
    creating.value = false
  }
}

// 加载我的应用
const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) {
    return
  }

  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPage.current,
      pageSize: myAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载我的应用失败：', error)
  }
}

// 加载精选应用
const loadFeaturedApps = async () => {
  try {
    const res = await listGoodAppVoByPage({
      pageNum: featuredAppsPage.current,
      pageSize: featuredAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载精选应用失败：', error)
  }
}

// 查看对话
const viewChat = (appId: string | number | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}?view=1`)
  }
}

// 查看作品
const viewWork = (app: API.AppVO) => {
  if (app.deployKey) {
    const url = getDeployUrl(app.deployKey)
    window.open(url, '_blank')
  }
}

// 格式化时间函数已移除，不再需要显示创建时间

// 鼠标跟随光效
const handleMouseMove = (e: MouseEvent) => {
  const { clientX, clientY } = e
  const { innerWidth, innerHeight } = window
  const x = (clientX / innerWidth) * 100
  const y = (clientY / innerHeight) * 100

  document.documentElement.style.setProperty('--mouse-x', `${x}%`)
  document.documentElement.style.setProperty('--mouse-y', `${y}%`)
}
// 页面加载时获取数据
onMounted(() => {
  loadMyApps()
  loadFeaturedApps()

  document.addEventListener('mousemove', handleMouseMove)
})

onUnmounted(() => {
  // 清理事件监听器
  document.removeEventListener('mousemove', handleMouseMove)
})
</script>

<template>
  <div id="homePage">
    <div class="container">
      <!-- 网站标题和描述 -->
      <div class="hero-section">
        <h1 class="hero-title">让灵感直接落地</h1>
        <p class="hero-description">定义下一代智能应用构建方式</p>
      </div>

      <!-- 用户提示词输入框 -->
      <div class="input-section">
        <a-textarea
          v-model:value="userPrompt"
          placeholder="在这里输入你的灵感..."
          :rows="4"
          :maxlength="1000"
          class="prompt-input"
          @keydown.enter.exact.prevent="createApp"
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
                name="file"
                :before-upload="beforeUpload"
                :disabled="uploading || creating"
                @remove="handleRemove"
              >
                <a-button
                  class="composer-icon-btn composer-upload-btn"
                  type="text"
                  size="small"
                  aria-label="上传附件"
                  :loading="uploading"
                  :disabled="uploading || creating"
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
                :class="{ 'composer-send-active': userPrompt.trim() }"
                type="primary"
                size="large"
                aria-label="发送并创建应用"
                @click="createApp"
                :loading="creating"
              >
                <template #icon>
                  <ArrowUpOutlined />
                </template>
              </a-button>
            </span>
          </a-tooltip>
        </div>
      </div>

      <!-- 快捷按钮 -->
      <div class="quick-actions">
        <a-button
          type="default"
          @click="
            setPrompt(
              '创建一个现代化的个人博客网站，包含文章列表、详情页、分类标签、搜索功能、评论系统和个人简介页面。采用简洁的设计风格，支持响应式布局，文章支持Markdown格式，首页展示最新文章和热门推荐。',
            )
          "
          >个人博客网站</a-button
        >
        <a-button
          type="default"
          @click="
            setPrompt(
              '设计一个专业的企业官网，包含公司介绍、产品服务展示、新闻资讯、联系我们等页面。采用商务风格的设计，包含轮播图、产品展示卡片、团队介绍、客户案例展示，支持多语言切换和在线客服功能。',
            )
          "
          >企业官网</a-button
        >
        <a-button
          type="default"
          @click="
            setPrompt(
              '构建一个功能完整的在线商城，包含商品展示、购物车、用户注册登录、订单管理、支付结算等功能。设计现代化的商品卡片布局，支持商品搜索筛选、用户评价、优惠券系统和会员积分功能。',
            )
          "
          >在线商城</a-button
        >
        <a-button
          type="default"
          @click="
            setPrompt(
              '制作一个精美的作品展示网站，适合设计师、摄影师、艺术家等创作者。包含作品画廊、项目详情页、个人简历、联系方式等模块。采用瀑布流或网格布局展示作品，支持图片放大预览和作品分类筛选。',
            )
          "
          >作品展示网站</a-button
        >
      </div>

      <!-- 我的作品 -->
      <div class="section">
        <h2 class="section-title">我的作品</h2>
        <div class="app-grid">
          <AppCard
            v-for="app in myApps"
            :key="app.id"
            :app="app"
            @view-chat="viewChat"
            @view-work="viewWork"
          />
        </div>
        <div class="pagination-wrapper">
          <a-pagination
            v-model:current="myAppsPage.current"
            v-model:page-size="myAppsPage.pageSize"
            :total="myAppsPage.total"
            :show-size-changer="false"
            :show-total="(total: number) => `共 ${total} 个应用`"
            @change="loadMyApps"
          />
        </div>
      </div>

      <!-- 精选案例 -->
      <div class="section">
        <h2 class="section-title">精选案例</h2>
        <div class="featured-grid">
          <AppCard
            v-for="app in featuredApps"
            :key="app.id"
            :app="app"
            :featured="true"
            @view-chat="viewChat"
            @view-work="viewWork"
          />
        </div>
        <div class="pagination-wrapper">
          <a-pagination
            v-model:current="featuredAppsPage.current"
            v-model:page-size="featuredAppsPage.pageSize"
            :total="featuredAppsPage.total"
            :show-size-changer="false"
            :show-total="(total: number) => `共 ${total} 个案例`"
            @change="loadFeaturedApps"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
#homePage {
  width: 100%;
  margin: 0;
  padding: 0;
  min-height: 100vh;
  background:
    radial-gradient(circle at 50% 0%, rgba(22, 119, 255, 0.1), transparent 34%),
    radial-gradient(circle at 12% 32%, rgba(20, 184, 166, 0.08), transparent 28%),
    linear-gradient(180deg, #f8fafc 0%, #f4f7fb 45%, #eef3f8 100%);
  position: relative;
  overflow: hidden;
}

/* 柔和背景光感 */
#homePage::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 84% 12%, rgba(22, 119, 255, 0.08), transparent 30%),
    radial-gradient(circle at 18% 78%, rgba(16, 185, 129, 0.07), transparent 28%);
  pointer-events: none;
}

/* 鼠标轻微光感 */
#homePage::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(
      600px circle at var(--mouse-x, 50%) var(--mouse-y, 50%),
      rgba(22, 119, 255, 0.05) 0%,
      transparent 72%
    );
  pointer-events: none;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  position: relative;
  z-index: 2;
  width: 100%;
  box-sizing: border-box;
}

/* 移除居中光束效果 */

/* 英雄区域 */
.hero-section {
  text-align: center;
  padding: 80px 0 60px;
  margin-bottom: 28px;
  color: #1e293b;
  position: relative;
}

.hero-title {
  font-size: 56px;
  font-weight: 700;
  margin: 0 0 20px;
  line-height: 1.2;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 50%, #10b981 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0;
  position: relative;
  z-index: 2;
}

.hero-description {
  font-size: 20px;
  margin: 0;
  opacity: 0.8;
  color: #64748b;
  position: relative;
  z-index: 2;
}

/* 输入区域 */
.input-section {
  position: relative;
  margin: 0 auto 24px;
  max-width: 800px;
}

.prompt-input {
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  font-size: 16px;

  /* 底部预留上传按钮空间 */
  padding: 20px 60px 52px 20px;

  /* padding: 20px 60px 20px 20px; */
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.1);
}

.prompt-input:focus {
  background: rgba(255, 255, 255, 1);
  border-color: rgba(22, 119, 255, 0.42);
  box-shadow: 0 20px 54px rgba(15, 23, 42, 0.12);
  transform: translateY(-2px);
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
  display: flex;
  gap: 8px;
  align-items: center;
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

/* 快捷按钮 */
.quick-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 60px;
  flex-wrap: wrap;
}

.quick-actions .ant-btn {
  border-radius: 25px;
  padding: 8px 20px;
  height: auto;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(148, 163, 184, 0.24);
  color: #475569;
  backdrop-filter: blur(15px);
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    color 0.2s ease,
    transform 0.2s ease;
  position: relative;
  overflow: hidden;
}

.quick-actions .ant-btn:hover {
  background: rgba(255, 255, 255, 0.94);
  border-color: rgba(22, 119, 255, 0.28);
  color: #1677ff;
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

/* 区域标题 */
.section {
  margin-bottom: 60px;
}

.section-title {
  font-size: 32px;
  font-weight: 600;
  margin-bottom: 32px;
  color: #1e293b;
}

/* 我的作品网格 */
.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

/* 精选案例网格 */
.featured-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .hero-title {
    font-size: 32px;
  }

  .hero-description {
    font-size: 16px;
  }

  .app-grid,
  .featured-grid {
    grid-template-columns: 1fr;
  }

  .quick-actions {
    justify-content: center;
  }
}
</style>
