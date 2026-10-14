import { spawn } from 'node:child_process'
import { mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'

const baseUrl = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:1888'
const username = process.env.E2E_USERNAME
const password = process.env.E2E_PASSWORD
const logAuthor = process.env.E2E_LOG_AUTHOR ?? '619414569'
const chromePath = process.env.CHROME_PATH ?? 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const screenshotPath = process.env.E2E_SCREENSHOT
const resultPath = process.env.E2E_RESULT

if (!username || !password) {
  throw new Error('E2E_USERNAME and E2E_PASSWORD are required')
}

const delay = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds))

async function stopBrowserTree(browser) {
  if (!browser.pid) return
  if (process.platform === 'win32') {
    const taskkill = spawn('taskkill', ['/pid', String(browser.pid), '/T', '/F'], { stdio: 'ignore' })
    await Promise.race([
      new Promise((resolve) => taskkill.once('exit', resolve)),
      delay(5_000),
    ])
    return
  }
  browser.kill('SIGTERM')
  await Promise.race([
    new Promise((resolve) => browser.once('exit', resolve)),
    delay(5_000),
  ])
}

async function waitFor(description, check, timeout = 20_000) {
  const deadline = Date.now() + timeout
  let lastError
  while (Date.now() < deadline) {
    try {
      const result = await check()
      if (result) return result
    } catch (error) {
      lastError = error
    }
    await delay(150)
  }
  throw new Error(`Timed out waiting for ${description}${lastError ? `: ${lastError.message}` : ''}`)
}

class CdpClient {
  constructor(socket) {
    this.socket = socket
    this.nextId = 1
    this.pending = new Map()
    socket.addEventListener('message', (event) => {
      const message = JSON.parse(event.data)
      if (!message.id) return
      const pending = this.pending.get(message.id)
      if (!pending) return
      this.pending.delete(message.id)
      if (message.error) pending.reject(new Error(message.error.message))
      else pending.resolve(message.result)
    })
  }

  send(method, params = {}) {
    const id = this.nextId++
    this.socket.send(JSON.stringify({ id, method, params }))
    return new Promise((resolve, reject) => this.pending.set(id, { resolve, reject }))
  }

  async evaluate(expression) {
    const result = await this.send('Runtime.evaluate', {
      expression,
      awaitPromise: true,
      returnByValue: true,
    })
    if (result.exceptionDetails) {
      throw new Error(result.exceptionDetails.exception?.description ?? 'Browser evaluation failed')
    }
    return result.result.value
  }
}

const profileDirectory = await mkdtemp(path.join(tmpdir(), 'ai-workbench-e2e-'))
const debugPort = 20_000 + Math.floor(Math.random() * 20_000)
const chrome = spawn(
  chromePath,
  [
    '--headless=new',
    '--disable-gpu',
    '--no-first-run',
    '--no-default-browser-check',
    `--remote-debugging-port=${debugPort}`,
    `--user-data-dir=${profileDirectory}`,
    'about:blank',
  ],
  { stdio: 'ignore' },
)

let failure = null
try {
  const targets = await waitFor('Chrome DevTools endpoint', async () => {
    const response = await fetch(`http://127.0.0.1:${debugPort}/json/list`)
    if (!response.ok) return null
    return response.json()
  })
  const target = targets.find((item) => item.type === 'page')
  if (!target) throw new Error('Chrome did not expose a page target')

  const socket = new WebSocket(target.webSocketDebuggerUrl)
  await new Promise((resolve, reject) => {
    socket.addEventListener('open', resolve, { once: true })
    socket.addEventListener('error', reject, { once: true })
  })
  const cdp = new CdpClient(socket)
  await Promise.all([cdp.send('Page.enable'), cdp.send('Runtime.enable')])

  await cdp.send('Page.navigate', { url: `${baseUrl}/login?redirect=/` })
  await waitFor('login form', () =>
    cdp.evaluate(`document.readyState === 'complete' && document.querySelectorAll('.login-form-wrap input').length === 2`),
  )

  const initialLoginState = await cdp.evaluate(`(() => {
    const inputs = [...document.querySelectorAll('.login-form-wrap input')]
    return { values: inputs.map((input) => input.value), hasDefaultPassword: document.body.innerText.includes('admin123') }
  })()`)
  if (initialLoginState.values.some(Boolean) || initialLoginState.hasDefaultPassword) {
    throw new Error('Login form must be empty and must not display the default password')
  }

  await cdp.evaluate(`(() => {
    const setValue = (input, value) => {
      const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set
      setter.call(input, value)
      input.dispatchEvent(new Event('input', { bubbles: true }))
    }
    const inputs = [...document.querySelectorAll('.login-form-wrap input')]
    setValue(inputs[0], ${JSON.stringify(username)})
    setValue(inputs[1], ${JSON.stringify(password)})
    const button = [...document.querySelectorAll('button')].find((item) => item.innerText.trim() === '登录')
    if (!button) throw new Error('Login button not found')
    button.click()
    return true
  })()`)

  await waitFor('authenticated workbench', () =>
    cdp.evaluate(`location.pathname === '/' && document.body.innerText.includes('提交日志')`),
  )
  await cdp.evaluate(`(() => {
    const later = [...document.querySelectorAll('button')].find((item) => item.innerText.trim() === '稍后修改')
    if (later) later.click()
    location.hash = '#submission-logs'
    return true
  })()`)

  await waitFor('version update log table', () =>
    cdp.evaluate(`document.body.innerText.includes('2026-10-14_V1.1.md') && document.body.innerText.includes('2026-10-12_V1.0.md')`),
  )

  const unfilteredFiles = await cdp.evaluate(`[
    ...document.querySelectorAll('.update-log-table button')
  ].map((button) => button.innerText.trim()).filter((text) => text.endsWith('.md'))`)
  const expectedFiles = ['2026-10-14_V1.1.md', '2026-10-12_V1.0.md']
  if (JSON.stringify(unfilteredFiles) !== JSON.stringify(expectedFiles)) {
    throw new Error(`Unexpected update log order: ${JSON.stringify(unfilteredFiles)}`)
  }

  await cdp.evaluate(`(() => {
    const select = document.querySelector('[aria-label="选择更新人员"]')
    if (!select) throw new Error('Author selector not found')
    select.click()
    return true
  })()`)
  await waitFor('author option', () =>
    cdp.evaluate(`[...document.querySelectorAll('.el-select-dropdown__item')].some((item) => item.innerText.trim() === ${JSON.stringify(logAuthor)})`),
  )
  await cdp.evaluate(`(() => {
    const option = [...document.querySelectorAll('.el-select-dropdown__item')]
      .find((item) => item.innerText.trim() === ${JSON.stringify(logAuthor)})
    option.click()
    return true
  })()`)
  await waitFor('author-filtered version logs', () =>
    cdp.evaluate(`document.querySelectorAll('.update-log-table .el-table__body-wrapper tbody tr').length === 2`),
  )

  await cdp.evaluate(`(() => {
    const button = [...document.querySelectorAll('.update-log-table button')]
      .find((item) => item.innerText.trim() === '2026-10-14_V1.1.md')
    if (!button) throw new Error('V1.1 log button not found')
    button.click()
    return true
  })()`)
  await waitFor('rendered Markdown preview', () =>
    cdp.evaluate(`document.querySelector('.markdown-preview h1')?.innerText.trim() === 'AI 应用工作台 V1.1'`),
  )

  const preview = await cdp.evaluate(`(() => ({
    title: document.querySelector('.update-log-drawer .el-drawer__title')?.innerText.trim(),
    heading: document.querySelector('.markdown-preview h1')?.innerText.trim(),
    sections: [...document.querySelectorAll('.markdown-preview h2')].map((item) => item.innerText.trim()),
    listItems: document.querySelectorAll('.markdown-preview li').length,
  }))()`)
  if (preview.title !== '2026-10-14_V1.1.md' || preview.listItems === 0) {
    throw new Error(`Markdown preview is incomplete: ${JSON.stringify(preview)}`)
  }

  if (screenshotPath) {
    const screenshot = await cdp.send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true })
    await writeFile(screenshotPath, Buffer.from(screenshot.data, 'base64'))
  }

  const result = {
    status: 'passed',
    baseUrl,
    loginFieldsEmpty: true,
    authorFilter: logAuthor,
    files: unfilteredFiles,
    preview,
    screenshotPath: screenshotPath ?? null,
  }
  if (resultPath) await writeFile(resultPath, JSON.stringify(result, null, 2), 'utf8')
  console.log(JSON.stringify(result, null, 2))
  socket.close()
} catch (error) {
  failure = error
  const result = { status: 'failed', baseUrl, error: error.message }
  if (resultPath) await writeFile(resultPath, JSON.stringify(result, null, 2), 'utf8')
  console.error(error.stack ?? error.message)
} finally {
  await stopBrowserTree(chrome)
  try {
    await rm(profileDirectory, { recursive: true, force: true, maxRetries: 5, retryDelay: 250 })
  } catch (error) {
    console.warn(`Temporary Chrome profile cleanup skipped: ${error.message}`)
  }
}

if (failure) process.exitCode = 1
