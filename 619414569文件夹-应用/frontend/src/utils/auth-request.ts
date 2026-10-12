export const LOGIN_API_PATH = '/v1/auth/login'

export function accessTokenForRequest(url: string | undefined, storedToken: string | null): string | null {
  if (!storedToken || url === LOGIN_API_PATH) return null
  return storedToken
}
