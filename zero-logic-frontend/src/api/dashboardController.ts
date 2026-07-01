import request from '@/request'

export async function getGenerationDashboard(
  params?: API.getGenerationDashboardParams,
  options?: { [key: string]: unknown },
) {
  return request<API.BaseResponseGenerationDashboardVO>('/dashboard/generation', {
    method: 'GET',
    params,
    ...(options || {}),
  })
}
