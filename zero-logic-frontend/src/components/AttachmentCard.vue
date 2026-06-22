<script setup lang="ts">
import { FileOutlined } from '@ant-design/icons-vue'

defineProps<{
  attachment: API.promptAttachmentVO
}>()

const formatFileSize = (size?: number) => {
  if (size === undefined) return ''

  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }

  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
</script>

<template>
  <div class="attachment-card">
    <FileOutlined class="file-icon" />

    <div class="file-info">
      <div class="file-name" :title="attachment.fileName">
        {{ attachment.fileName }}
      </div>

      <div class="file-meta">
        {{ formatFileSize(attachment.fileSize) }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.attachment-card {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 240px;
  padding: 10px 12px;
  margin-bottom: 8px;
  color: #334155;
  background: #fff;
  border: 1px solid #dbeafe;
  border-radius: 8px;
}

.file-icon {
  flex-shrink: 0;
  font-size: 24px;
  color: #1677ff;
}

.file-info {
  min-width: 0;
}

.file-name {
  overflow: hidden;
  font-size: 14px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  margin-top: 2px;
  font-size: 12px;
  color: #94a3b8;
}
</style>
