import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { describe, it } from 'node:test'
import { fileURLToPath } from 'node:url'

const loginViewPath = fileURLToPath(new URL('../src/views/LoginView.vue', import.meta.url))
const passwordDialogPath = fileURLToPath(new URL('../src/components/PasswordChangeDialog.vue', import.meta.url))

describe('login credential presentation', () => {
  it('starts with empty username and password fields', async () => {
    const source = await readFile(loginViewPath, 'utf8')

    assert.match(source, /reactive<LoginForm>\(\{ username: '', password: '' \}\)/)
    assert.doesNotMatch(source, /DEFAULT_PASSWORD|login-default-password/)
  })

  it('does not expose the initial password in authentication screens', async () => {
    const source = await Promise.all([
      readFile(loginViewPath, 'utf8'),
      readFile(passwordDialogPath, 'utf8'),
    ])

    assert.doesNotMatch(source.join('\n'), /admin123|默认密码/)
    assert.match(source[1], /首次登录，请修改密码/)
  })
})
