import request from "@/utils/request"

// 获取文件列表
export const getFileListAPI = (params) => {
  return request.get("/home/file-library/list", { params })
}

// 获取文件统计
export const getFileStatsAPI = () => {
  return request.get("/home/file-library/stats")
}
