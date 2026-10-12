import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { accessTokenForRequest } from '../src/utils/auth-request.ts'

describe('authentication request policy', () => {
  it('does not attach a stale token to the public login request', () => {
    assert.equal(accessTokenForRequest('/v1/auth/login', 'stale-token'), null)
  })

  it('attaches the current token to protected requests', () => {
    assert.equal(accessTokenForRequest('/v1/auth/me', 'current-token'), 'current-token')
  })

  it('does not create an authorization header when no token exists', () => {
    assert.equal(accessTokenForRequest('/v1/workspace/applications', null), null)
  })
})
