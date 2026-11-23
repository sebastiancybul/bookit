import { describe, expect, it } from 'vitest'
import { initialsOf, pastelOf } from '@/lib/avatar'

describe('initialsOf', () => {
  it('takes the first letters of the first two words', () => {
    expect(initialsOf('Anna Kowalska')).toBe('AK')
    expect(initialsOf('łukasz żak')).toBe('ŁŻ')
  })

  it('takes two letters of a single word and skips punctuation', () => {
    expect(initialsOf('Barber')).toBe('BA')
    expect(initialsOf('Anna & K.')).toBe('AK')
  })

  it('falls back to a question mark', () => {
    expect(initialsOf('  ')).toBe('?')
  })
})

describe('pastelOf', () => {
  it('returns the same colour for the same seed', () => {
    expect(pastelOf('0193a1b2')).toBe(pastelOf('0193a1b2'))
  })
})
