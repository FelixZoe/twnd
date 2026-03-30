import http from '@/utils/request'

/**
 * 文件库专用上传
 * @param {FormData} formData
 */
export const uploadFileLibrary = (formData) =>
  http.post('/admin/file-library/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })

/**
 * 获取文件列表
 * @param {{ category?: string, page: number, pageSize: number }} params
 */
export const getFileList = (params) =>
  http.get('/admin/file-library/list', { params })

/**
 * 获取文件分类统计
 */
export const getFileStats = () =>
  http.get('/admin/file-library/stats')

/**
 * 删除文件
 * @param {string} fileName 文件名（如 xxx.webp）
 */
export const deleteFile = (fileName) =>
  http.delete('/admin/file-library', { params: { fileName } })

/**
 * 批量删除文件
 * @param {string[]} fileNames 文件名列表
 */
export const batchDeleteFiles = (fileNames) =>
  http.delete('/admin/file-library/batch', { data: fileNames })
