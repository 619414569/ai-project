import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { normalizeApplicationUrl, validateApplicationUrl } from '../src/utils/application-url.ts'

describe('application URL utilities', () => {
  it('adds HTTPS to a hostname without a protocol', () => {
    assert.equal(normalizeApplicationUrl('app.example.com/path'), 'https://app.example.com/path')
  })

  it('keeps an internal relative path', () => {
    assert.equal(normalizeApplicationUrl('/internal-app'), '/internal-app')
  })

  it('rejects non HTTP protocols', () => {
    assert.equal(validateApplicationUrl('ftp://example.com'), '仅支持 HTTP 或 HTTPS 地址')
  })

  it('rejects protocol-relative external URLs', () => {
    assert.equal(validateApplicationUrl('//example.com'), '请输入站内路径或完整的 HTTP/HTTPS 地址')
  })

  it('allows an empty value because the field is optional', () => {
    assert.equal(validateApplicationUrl(''), true)
  })
})
