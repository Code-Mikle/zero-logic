import request from '@/request'

export interface CreateGenerationTaskResponse {
  code?: number
  data?: number | string
  message?: string
}

export interface GenerationTaskResponse {
  code?: number
  data?: API.GenerationTaskVO
  message?: string
}

export interface CancelGenerationTaskResponse {
  code?: number
  data?: boolean
  message?: string
}

export async function createGenerationTask(data: {
  appId: number | string
  message: string
  attachmentId?: number | string
}) {
  return request<CreateGenerationTaskResponse>('/generation/task/create', {
    method: 'POST',
    data,
  })
}

export async function getGenerationTask(taskId: number | string) {
  return request<GenerationTaskResponse>(`/generation/task/${taskId}`, {
    method: 'GET',
  })
}

export async function cancelGenerationTask(taskId: number | string) {
  return request<CancelGenerationTaskResponse>(`/generation/task/${taskId}/cancel`, {
    method: 'POST',
  })
}
