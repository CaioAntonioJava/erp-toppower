import { describe, it, expect } from 'vitest'
import { MODULES, MODULE_SECTIONS, moduleForRoute } from '../../lib/modules'

describe('MODULES', () => {
  it('contém todos os 15 módulos', () => {
    expect(MODULES).toHaveLength(15)
  })

  it('cada módulo tem key, label, section e to', () => {
    for (const m of MODULES) {
      expect(m.key).toBeTruthy()
      expect(m.label).toBeTruthy()
      expect(m.section).toBeTruthy()
      expect(m.to).toBeTruthy()
    }
  })

  it('agrupa módulos em 3 seções', () => {
    const sections = new Set(MODULES.map((m) => m.section))
    expect(sections.has('Cadastros')).toBe(true)
    expect(sections.has('Comercial')).toBe(true)
    expect(sections.has('Financeiro')).toBe(true)
  })
})

describe('MODULE_SECTIONS', () => {
  it('contém as 3 seções na ordem correta', () => {
    expect(MODULE_SECTIONS).toEqual(['Cadastros', 'Comercial', 'Financeiro'])
  })
})

describe('moduleForRoute', () => {
  it('retorna MODULE_COMPANIES para /companies', () => {
    expect(moduleForRoute('/companies')).toBe('MODULE_COMPANIES')
  })

  it('retorna MODULE_COMPANIES para /companies/1/edit', () => {
    expect(moduleForRoute('/companies/1/edit')).toBe('MODULE_COMPANIES')
  })

  it('retorna MODULE_QUOTATIONS para /quotations', () => {
    expect(moduleForRoute('/quotations')).toBe('MODULE_QUOTATIONS')
  })

  it('retorna MODULE_QUOTATIONS para /quotations/1/pdf', () => {
    expect(moduleForRoute('/quotations/1/pdf')).toBe('MODULE_QUOTATIONS')
  })

  it('retorna MODULE_PURCHASES_IMPORT para /purchases/import', () => {
    expect(moduleForRoute('/purchases/import')).toBe('MODULE_PURCHASES_IMPORT')
  })

  it('retorna MODULE_CARRIERS para /carriers', () => {
    expect(moduleForRoute('/carriers')).toBe('MODULE_CARRIERS')
  })

  it('retorna MODULE_SERVICE_TEMPLATES para /service-templates', () => {
    expect(moduleForRoute('/service-templates')).toBe('MODULE_SERVICE_TEMPLATES')
  })

  it('retorna undefined para rota não coberta', () => {
    expect(moduleForRoute('/dashboard')).toBeUndefined()
  })

  it('retorna undefined para rota admin', () => {
    expect(moduleForRoute('/users')).toBeUndefined()
  })

  it('retorna undefined para /login', () => {
    expect(moduleForRoute('/login')).toBeUndefined()
  })
})
