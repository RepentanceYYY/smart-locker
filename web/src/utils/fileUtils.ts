/**
 * 格式化图片或图标的完整 URL 路径（兼容常规网络路径、相对路径以及 Base64）
 * @param url 后端传过来的原始路径/数据
 */
export const formatImageUrl = (url: string | undefined | null): string => {
  if (!url) {
    // 💡 这里放一个本地的默认占位图，防止破图
    return './default-icon.png' 
  }
  
  // 本地预览
  if (url.startsWith('blob:')) {
    return url
  }

  // 1. 💡 新增兼容：如果是 Base64 格式（以 data: 开头，如 data:image/png;base64,...），直接原样返回
  if (url.startsWith('data:')) {
    return url
  }
  
  // 2. 如果已经是完整 http:// 或 https:// 开头的网络路径，直接原样返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  
  // 3. 如果是相对路径（比如 /uploads/xxx.jpg），强行拼接到本地内置的 Spring Boot 服务地址上
  const baseUrl = import.meta.env.VITE_API_BASE_URL
  
  // 兼容一下相对路径开头有没有带斜杠 '/' 的情况
  const hasSlash = url.startsWith('/')
  return `${baseUrl}${hasSlash ? '' : '/'}${url}`
}