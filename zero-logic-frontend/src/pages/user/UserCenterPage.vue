<template>
  <div class="user-center-page">
    <div class="profile-card">
      <div class="profile-header">
        <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="72">
          {{ loginUserStore.loginUser.userName?.charAt(0) || 'U' }}
        </a-avatar>

        <div>
          <h2>{{ loginUserStore.loginUser.userName || '未命名用户' }}</h2>
          <a-tag :color="isAdmin ? 'blue' : 'default'">
            {{ isAdmin ? '管理员' : '普通用户' }}
          </a-tag>
        </div>
      </div>

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
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')
</script>

<style scoped>
.user-center-page {
  min-height: calc(100vh - 64px);
  padding: 40px 24px;
  background: #f5f7fb;
}

.profile-card {
  max-width: 520px;
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

.profile-header h2 {
  margin: 0 0 8px;
}

.action-list {
  display: grid;
  gap: 12px;
}
</style>
