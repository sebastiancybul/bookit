import { describe, expect, it } from 'vitest'
import {
  formatDuration,
  formatPrice,
  groszeToZloty,
  zlotyAmountPattern,
  zlotyToGrosze,
} from '@/lib/format'

describe('format', () => {
  it('converts typed złoty amounts to grosze without floating point errors', () => {
    expect(zlotyToGrosze('80')).toBe(8000)
    expect(zlotyToGrosze('80,5')).toBe(8050)
    expect(zlotyToGrosze(' 0.10 ')).toBe(10)
    expect(zlotyToGrosze('19,99')).toBe(1999)
  })

  it('accepts only up to two decimal places', () => {
    expect(zlotyAmountPattern.test('80')).toBe(true)
    expect(zlotyAmountPattern.test('80,50')).toBe(true)
    expect(zlotyAmountPattern.test('80.505')).toBe(false)
    expect(zlotyAmountPattern.test('-5')).toBe(false)
    expect(zlotyAmountPattern.test('abc')).toBe(false)
  })

  it('turns grosze back into the value shown in the form', () => {
    expect(groszeToZloty(8000)).toBe('80')
    expect(groszeToZloty(8050)).toBe('80,50')
    expect(groszeToZloty(5)).toBe('0,05')
  })

  it('formats prices and durations for display', () => {
    expect(formatPrice(8000, 'PLN').replace(/\s/g, ' ')).toBe('80 zł')
    expect(formatPrice(8050, 'PLN').replace(/\s/g, ' ')).toBe('80,50 zł')
    expect(formatDuration(45)).toBe('45 min')
    expect(formatDuration(60)).toBe('1 h')
    expect(formatDuration(75)).toBe('1 h 15 min')
  })
})
