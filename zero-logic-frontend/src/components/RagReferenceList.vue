<template>
  <section v-if="retrieval" class="rag-reference-panel">
    <button class="reference-trigger" type="button" :aria-expanded="expanded" @click="expanded = !expanded">
      <span class="reference-title">
        <BookOutlined />
        本次参考资料
        <span class="reference-count">{{ retrieval.hitCount || references.length }}</span>
      </span>
      <DownOutlined class="expand-icon" :class="{ expanded }" />
    </button>

    <div v-show="expanded" class="reference-list">
      <div v-if="references.length === 0" class="empty-reference">本次未命中可用资料</div>
      <article v-for="reference in references" :key="reference.chunkId" class="reference-item">
        <div class="reference-meta">
          <span class="document-name">{{ reference.documentName || '未命名文档' }}</span>
          <span class="chunk-index">片段 {{ (reference.chunkIndex ?? 0) + 1 }}</span>
          <span v-if="reference.score !== undefined" class="score">
            相似度 {{ formatScore(reference.score) }}
          </span>
        </div>
        <p v-if="reference.contentSnippet" class="reference-snippet">
          {{ reference.contentSnippet }}
        </p>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { BookOutlined, DownOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  retrieval: API.RagRetrievalVO
}>()

const expanded = ref(false)
const references = computed(() => props.retrieval.references || [])
const formatScore = (score: number) => Number(score).toFixed(3)
</script>

<style scoped>
.rag-reference-panel {
  margin-top: 12px;
  overflow: hidden;
  border: 1px solid #dbe5e1;
  border-radius: 10px;
  background: #f8fbfa;
}

.reference-trigger {
  width: 100%;
  padding: 9px 11px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 0;
  background: transparent;
  color: #31544a;
  cursor: pointer;
  font: inherit;
}

.reference-title {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  font-weight: 600;
}

.reference-count {
  min-width: 20px;
  padding: 1px 6px;
  border-radius: 10px;
  background: #dcece7;
  color: #276557;
  font-size: 11px;
  text-align: center;
}

.expand-icon {
  font-size: 11px;
  transition: transform 0.2s ease;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.reference-list {
  padding: 0 10px 10px;
}

.reference-item {
  padding: 9px 10px;
  border-top: 1px solid #e2ebe8;
}

.reference-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 11px;
  color: #74827e;
}

.document-name {
  color: #294d43;
  font-size: 12px;
  font-weight: 600;
}

.score {
  margin-left: auto;
  font-variant-numeric: tabular-nums;
}

.reference-snippet {
  margin: 6px 0 0;
  color: #52635e;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.empty-reference {
  padding: 10px;
  color: #84918d;
  font-size: 12px;
}
</style>
