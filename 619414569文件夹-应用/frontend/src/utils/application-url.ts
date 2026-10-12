export function normalizeApplicationUrl(value: string): string {
  const trimmed = value.trim()
  if (!trimmed) return ''
  if (trimmed.startsWith('//')) throw new Error('请输入站内路径或完整的 HTTP/HTTPS 地址')
  if (trimmed.startsWith('/')) return trimmed

  const withProtocol = /^[a-z][a-z\d+.-]*:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`
  const url = new URL(withProtocol)
  if (url.protocol !== 'http:' && url.protocol !== 'https:') {
    throw new Error('仅支持 HTTP 或 HTTPS 地址')
  }
  return url.toString()
}

export function validateApplicationUrl(value: string): true | string {
  if (!value.trim()) return true
  try {
    normalizeApplicationUrl(value)
    return true
  } catch (error) {
    return error instanceof Error ? error.message : '请输入有效的应用地址'
  }
}
