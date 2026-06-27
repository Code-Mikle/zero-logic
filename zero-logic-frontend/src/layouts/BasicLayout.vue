<template>
  <a-layout class="basic-layout" :class="{ 'full-page-layout': isFullPage }">
    <!-- 顶部导航栏 -->
    <GlobalHeader v-if="!hideGlobalHeader" />
    <!-- 主要内容区域 -->
    <a-layout-content class="main-content">
      <router-view />
    </a-layout-content>
    <!-- 底部版权信息 -->
    <GlobalFooter v-if="!hideGlobalFooter" />
  </a-layout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'

const route = useRoute()

const hideGlobalHeader = computed(() => route.meta.hideGlobalHeader)
const hideGlobalFooter = computed(() => route.meta.hideGlobalFooter)
const isFullPage = computed(() => route.meta.fullPage)
</script>

<style scoped>
.basic-layout {
  background: none;
  min-height: 100vh;
}

.full-page-layout {
  height: 100vh;
  overflow: hidden;
}

.main-content {
  width: 100%;
  padding: 0;
  background: none;
  margin: 0;
}

.full-page-layout .main-content {
  height: 100vh;
  overflow: hidden;
}
</style>
