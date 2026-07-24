import { describe, it, expect } from 'vitest'
import {
  isValidCpf,
  isValidCnpj,
  maskCpf,
  maskCnpj,
  maskZipCode,
  maskPhone,
} from '../../lib/documents'

describe('isValidCpf', () => {
  it('valida CPF formatado', () => {
    expect(isValidCpf('123.456.789-09')).toBe(true)
    expect(isValidCpf('111.444.777-35')).toBe(true)
    expect(isValidCpf('529.982.247-25')).toBe(true)
  })

  it('valida CPF sem formatação', () => {
    expect(isValidCpf('12345678909')).toBe(true)
    expect(isValidCpf('11144477735')).toBe(true)
    expect(isValidCpf('52998224725')).toBe(true)
  })

  it('rejeita CPF com dígitos errados', () => {
    expect(isValidCpf('123.456.789-00')).toBe(false)
    expect(isValidCpf('111.444.777-00')).toBe(false)
  })

  it('rejeita CPF com sequência repetida', () => {
    expect(isValidCpf('111.111.111-11')).toBe(false)
    expect(isValidCpf('000.000.000-00')).toBe(false)
    expect(isValidCpf('999.999.999-99')).toBe(false)
  })

  it('rejeita CPF com tamanho incorreto', () => {
    expect(isValidCpf('')).toBe(false)
    expect(isValidCpf('123')).toBe(false)
    expect(isValidCpf('123.456.789-0123')).toBe(false)
  })

  it('rejeita string vazia', () => {
    expect(isValidCpf('')).toBe(false)
  })
})

describe('isValidCnpj', () => {
  it('valida CNPJ formatado', () => {
    expect(isValidCnpj('11.222.333/0001-81')).toBe(true)
    expect(isValidCnpj('11.444.777/0001-61')).toBe(true)
  })

  it('valida CNPJ sem formatação', () => {
    expect(isValidCnpj('11222333000181')).toBe(true)
    expect(isValidCnpj('11444777000161')).toBe(true)
  })

  it('rejeita CNPJ com dígitos errados', () => {
    expect(isValidCnpj('11.222.333/0001-00')).toBe(false)
    expect(isValidCnpj('11.222.333/0001-82')).toBe(false)
  })

  it('rejeita CNPJ com sequência repetida', () => {
    expect(isValidCnpj('11.111.111/1111-11')).toBe(false)
    expect(isValidCnpj('00.000.000/0000-00')).toBe(false)
  })

  it('rejeita CNPJ com tamanho incorreto', () => {
    expect(isValidCnpj('')).toBe(false)
    expect(isValidCnpj('11.222.333')).toBe(false)
  })
})

describe('maskCpf', () => {
  it('aplica máscara completa', () => {
    expect(maskCpf('12345678909')).toBe('123.456.789-09')
  })

  it('aplica máscara parcial (3 dígitos)', () => {
    expect(maskCpf('123')).toBe('123')
  })

  it('aplica máscara parcial (6 dígitos)', () => {
    expect(maskCpf('123456')).toBe('123.456')
  })

  it('aplica máscara parcial (9 dígitos)', () => {
    expect(maskCpf('123456789')).toBe('123.456.789')
  })

  it('limita a 11 dígitos', () => {
    expect(maskCpf('12345678909123')).toBe('123.456.789-09')
  })

  it('remove caracteres não dígitos', () => {
    expect(maskCpf('123.456.789-09')).toBe('123.456.789-09')
  })
})

describe('maskCnpj', () => {
  it('aplica máscara completa', () => {
    expect(maskCnpj('11222333000181')).toBe('11.222.333/0001-81')
  })

  it('aplica máscara parcial (2 dígitos)', () => {
    expect(maskCnpj('11')).toBe('11')
  })

  it('aplica máscara parcial (5 dígitos)', () => {
    expect(maskCnpj('11222')).toBe('11.222')
  })

  it('aplica máscara parcial (8 dígitos)', () => {
    expect(maskCnpj('11222333')).toBe('11.222.333')
  })

  it('aplica máscara parcial (12 dígitos)', () => {
    expect(maskCnpj('112223330001')).toBe('11.222.333/0001')
  })

  it('limita a 14 dígitos', () => {
    expect(maskCnpj('11222333000181123')).toBe('11.222.333/0001-81')
  })
})

describe('maskZipCode', () => {
  it('aplica máscara completa', () => {
    expect(maskZipCode('01310100')).toBe('01310-100')
  })

  it('aplica máscara parcial (5 dígitos)', () => {
    expect(maskZipCode('01310')).toBe('01310')
  })

  it('limita a 8 dígitos', () => {
    expect(maskZipCode('01310100123')).toBe('01310-100')
  })

  it('remove caracteres não dígitos', () => {
    expect(maskZipCode('01310-100')).toBe('01310-100')
  })
})

describe('maskPhone', () => {
  it('aplica máscara completa celular (11 dígitos)', () => {
    expect(maskPhone('11999999999')).toBe('(11) 99999-9999')
  })

  it('aplica máscara completa fixo (10 dígitos)', () => {
    expect(maskPhone('1133334444')).toBe('(11) 3333-4444')
  })

  it('aplica máscara parcial (2 dígitos)', () => {
    expect(maskPhone('11')).toBe('11')
  })

  it('aplica máscara parcial (6 dígitos)', () => {
    expect(maskPhone('119999')).toBe('(11) 9999')
  })

  it('limita a 11 dígitos', () => {
    expect(maskPhone('11999999999123')).toBe('(11) 99999-9999')
  })
})
