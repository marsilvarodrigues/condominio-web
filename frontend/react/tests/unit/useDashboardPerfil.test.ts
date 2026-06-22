import { describe, it, expect, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useDashboardPerfil } from '@/hooks/useDashboardPerfil'
import { useAuthStore } from '@/store/authStore'
import { ROLES } from '@/utils/constants'

function setRoles(roles: string[]) {
  useAuthStore.setState({
    user: { id: 1, email: 'test@test.com', roles, condominioIds: [] },
    accessToken: 'token',
    refreshToken: 'refresh',
    activeCondominioId: null,
  })
}

beforeEach(() => {
  useAuthStore.setState({
    user: null,
    accessToken: null,
    refreshToken: null,
    activeCondominioId: null,
  })
})

describe('useDashboardPerfil', () => {
  it('admin → perfil admin, podeAlternar false', () => {
    setRoles([ROLES.ADMIN])
    const { result } = renderHook(() => useDashboardPerfil())
    expect(result.current.perfil).toBe('admin')
    expect(result.current.podeAlternar).toBe(false)
  })

  it('sindico → perfil sindico, podeAlternar false', () => {
    setRoles([ROLES.SINDICO])
    const { result } = renderHook(() => useDashboardPerfil())
    expect(result.current.perfil).toBe('sindico')
    expect(result.current.podeAlternar).toBe(false)
  })

  it('somente morador → perfil morador, podeAlternar false', () => {
    setRoles([ROLES.MORADOR])
    const { result } = renderHook(() => useDashboardPerfil())
    expect(result.current.perfil).toBe('morador')
    expect(result.current.podeAlternar).toBe(false)
  })

  it('somente proprietario → perfil proprietario, podeAlternar false', () => {
    setRoles([ROLES.PROPRIETARIO])
    const { result } = renderHook(() => useDashboardPerfil())
    expect(result.current.perfil).toBe('proprietario')
    expect(result.current.podeAlternar).toBe(false)
  })

  it('morador + proprietario → perfil proprietario (precedência), podeAlternar true', () => {
    setRoles([ROLES.MORADOR, ROLES.PROPRIETARIO])
    const { result } = renderHook(() => useDashboardPerfil())
    expect(result.current.perfil).toBe('proprietario')
    expect(result.current.podeAlternar).toBe(true)
    expect(result.current.perfisDisponiveis).toEqual(['proprietario', 'morador'])
  })

  it('somente user → perfil usuario', () => {
    setRoles([ROLES.USER])
    const { result } = renderHook(() => useDashboardPerfil())
    expect(result.current.perfil).toBe('usuario')
  })

  it('sem roles → perfil usuario (fallback)', () => {
    setRoles([])
    const { result } = renderHook(() => useDashboardPerfil())
    expect(result.current.perfil).toBe('usuario')
  })

  it('setPerfil alterna entre perfis disponíveis', () => {
    setRoles([ROLES.MORADOR, ROLES.PROPRIETARIO])
    const { result } = renderHook(() => useDashboardPerfil())
    act(() => result.current.setPerfil('morador'))
    expect(result.current.perfil).toBe('morador')
  })
})
