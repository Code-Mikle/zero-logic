import request from '@/request'

export interface UploadAttachmentResponse {
  code?: number
  data?: API.promptAttachmentVO
  message?: string
}

export async function uploadAttachment(file: File, appId?: number) {
  const formData = new FormData()
  formData.append('file', file)

  if (appId) {
    formData.append('appId', String(appId))
  }

  return request<UploadAttachmentResponse>('/attachment/upload', {
    method: 'POST',
    data: formData,
  })
}
