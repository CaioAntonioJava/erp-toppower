import { describe, it, expect } from 'vitest'
import { BRAZILIAN_STATES, VALID_UFS, isValidUf } from '../../lib/brazilianStates'

describe('BRAZILIAN_STATES', () => {
  it('contém 27 estados', () => {
    expect(BRAZILIAN_STATES).toHaveLength(27)
  })

  it('cada estado tem uf e name', () => {
    for (const s of BRAZILIAN_STATES) {
      expect(s.uf).toHaveLength(2)
      expect(s.name).toBeTruthy()
    }
  })

  it('inclui SP e RJ', () => {
    const ufs = BRAZILIAN_STATES.map((s) => s.uf)
    expect(ufs).toContain('SP')
    expect(ufs).toContain('RJ')
  })
})

describe('VALID_UFS', () => {
  it('contém SP', () => {
    expect(VALID_UFS.has('SP')).toBe(true)
  })

  it('não contém XX', () => {
    expect(VALID_UFS.has('XX')).toBe(false)
  })
})

describe('isValidUf', () => {
  it('retorna true para SP', () => {
    expect(isValidUf('SP')).toBe(true)
  })

  it('retorna true para sp (case insensitive)', () => {
    expect(isValidUf('sp')).toBe(true)
  })

  it('retorna false para XX', () => {
    expect(isValidUf('XX')).toBe(false)
  })

  it('retorna false para string vazia', () => {
    expect(isValidUf('')).toBe(false)
  })
})
