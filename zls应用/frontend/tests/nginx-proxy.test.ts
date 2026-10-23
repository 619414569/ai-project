import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { describe, it } from 'node:test'
import { fileURLToPath } from 'node:url'

const nginxConfigPath = fileURLToPath(new URL('../nginx/default.conf.template', import.meta.url))

describe('Nginx API proxy headers', () => {
  it('preserves the public Host port for same-origin CORS checks', async () => {
    const config = await readFile(nginxConfigPath, 'utf8')

    assert.match(config, /proxy_set_header Host \$http_host;/)
    assert.match(config, /proxy_set_header X-Forwarded-Host \$http_host;/)
    assert.match(config, /proxy_set_header X-Forwarded-Port \$server_port;/)
    assert.doesNotMatch(config, /proxy_set_header Host \$host;/)
  })
})
