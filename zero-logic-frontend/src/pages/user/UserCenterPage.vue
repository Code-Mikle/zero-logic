<template>
  <div class="user-center-page">
    <div class="profile-card">
      <div class="profile-header">
        <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="72">
          {{ loginUserStore.loginUser.userName?.charAt(0) || 'U' }}
        </a-avatar>

        <div class="profile-title">
          <h2>{{ loginUserStore.loginUser.userName || '未命名用户' }}</h2>
          <a-tag :color="isAdmin ? 'blue' : 'default'">
            {{ isAdmin ? '管理员' : '普通用户' }}
          </a-tag>
        </div>

        <a-button type="primary" ghost @click="openEditModal">编辑资料</a-button>
      </div>

      <a-divider />

      <a-descriptions :column="1" bordered size="small" class="profile-info">
        <a-descriptions-item label="用户 ID">
          {{ loginUserStore.loginUser.id || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="登录账号">
          {{ loginUserStore.loginUser.userAccount || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="用户昵称">
          {{ loginUserStore.loginUser.userName || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="个人简介">
          {{ loginUserStore.loginUser.userProfile || '这个人很低调，还没有填写简介。' }}
        </a-descriptions-item>
      </a-descriptions>

      <a-divider />

      <div class="action-list">
        <a-button v-if="isAdmin" block @click="router.push('/admin/userManage')">
          用户管理
        </a-button>

        <a-button v-if="isAdmin" block @click="router.push('/admin/appManage')">
          应用管理
        </a-button>
      </div>
    </div>

    <a-modal
      v-model:open="editModalOpen"
      title="编辑个人资料"
      :confirm-loading="submitting"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleSubmit"
    >
      <a-form layout="vertical" :model="formState">
        <a-form-item label="用户 ID">
          <a-input :value="String(loginUserStore.loginUser.id || '')" disabled />
        </a-form-item>
        <a-form-item label="登录账号">
          <a-input :value="loginUserStore.loginUser.userAccount || ''" disabled />
        </a-form-item>
        <a-form-item label="用户昵称">
          <a-input
            v-model:value="formState.userName"
            placeholder="请输入用户昵称"
            :maxlength="80"
            show-count
          />
        </a-form-item>
        <a-form-item label="头像地址">
          <a-input
            v-model:value="formState.userAvatar"
            placeholder="请输入头像图片 URL"
            :maxlength="1024"
          />
        </a-form-item>
        <a-form-item label="个人简介">
          <a-textarea
            v-model:value="formState.userProfile"
            placeholder="介绍一下自己"
            :maxlength="512"
            :rows="4"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { updateMyUserProfile } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')
const editModalOpen = ref(false)
const submitting = ref(false)
const formState = reactive<API.UserProfileUpdateRequest>({
  userName: '',
  userAvatar: '',
  userProfile: '',
})

const openEditModal = () => {
  formState.userName = loginUserStore.loginUser.userName || ''
  formState.userAvatar = loginUserStore.loginUser.userAvatar || ''
  formState.userProfile = loginUserStore.loginUser.userProfile || ''
  editModalOpen.value = true
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const res = await updateMyUserProfile({
      userName: formState.userName,
      userAvatar: formState.userAvatar,
      userProfile: formState.userProfile,
    })
    if (res.data.code !== 0 || !res.data.data) {
      throw new Error(res.data.message || '保存失败')
    }
    loginUserStore.setLoginUser(res.data.data)
    message.success('个人资料已更新')
    editModalOpen.value = false
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.user-center-page {
  min-height: calc(100vh - 64px);
  padding: 40px 24px;
  background: #f5f7fb;
}

.profile-card {
  max-width: 640px;
  margin: 0 auto;
  padding: 24px;
  background: #fff;
  border-radius: 8px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-title {
  flex: 1;
  min-width: 0;
}

.profile-header h2 {
  margin: 0 0 8px;
}

.profile-info {
  margin-bottom: 4px;
}

.action-list {
  display: grid;
  gap: 12px;
}
</style>
