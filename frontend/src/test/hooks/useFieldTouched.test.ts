import { describe, it, expect } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useFieldTouched } from '../../hooks/useFieldTouched'

describe('useFieldTouched', () => {
  it('shouldShowError retorna undefined para campo não tocado', () => {
    const { result } = renderHook(() => useFieldTouched())
    expect(result.current.shouldShowError('name', 'Erro')).toBeUndefined()
  })

  it('shouldShowError retorna undefined para campo sem erro', () => {
    const { result } = renderHook(() => useFieldTouched())
    expect(result.current.shouldShowError('name', null)).toBeUndefined()
    expect(result.current.shouldShowError('name', undefined)).toBeUndefined()
  })

  it('shouldShowError retorna erro após marcar como tocado', () => {
    const { result } = renderHook(() => useFieldTouched())
    act(() => {
      result.current.markTouched('name')
    })
    expect(result.current.shouldShowError('name', 'Erro')).toBe('Erro')
  })

  it('getBlurHandler marca o campo como tocado no blur', () => {
    const { result } = renderHook(() => useFieldTouched())
    act(() => {
      result.current.getBlurHandler('email')()
    })
    expect(result.current.touched.has('email')).toBe(true)
  })

  it('markAllTouched faz shouldShowError retornar erro para qualquer campo', () => {
    const { result } = renderHook(() => useFieldTouched())
    act(() => {
      result.current.markAllTouched()
    })
    expect(result.current.shouldShowError('qualquer', 'Erro')).toBe('Erro')
  })

  it('reset limpa o estado', () => {
    const { result } = renderHook(() => useFieldTouched())
    act(() => {
      result.current.markTouched('name')
      result.current.markAllTouched()
    })
    act(() => {
      result.current.reset()
    })
    expect(result.current.touched.size).toBe(0)
    expect(result.current.submitAttempted).toBe(false)
    expect(result.current.shouldShowError('name', 'Erro')).toBeUndefined()
  })

  it('markTouched é idempotente', () => {
    const { result } = renderHook(() => useFieldTouched())
    act(() => {
      result.current.markTouched('name')
      result.current.markTouched('name')
    })
    expect(result.current.touched.size).toBe(1)
  })
})
