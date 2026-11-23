const GROSZE_PER_ZLOTY = 100

export const zlotyAmountPattern = /^\d{1,6}([.,]\d{1,2})?$/

export function formatPrice(amount: number, currency: string) {
  return new Intl.NumberFormat('pl-PL', {
    style: 'currency',
    currency,
    minimumFractionDigits: amount % GROSZE_PER_ZLOTY === 0 ? 0 : 2,
  }).format(amount / GROSZE_PER_ZLOTY)
}

export function zlotyToGrosze(value: string) {
  const [zloty, fraction = ''] = value.trim().replace(',', '.').split('.')
  return Number(zloty) * GROSZE_PER_ZLOTY + Number(fraction.padEnd(2, '0'))
}

export function groszeToZloty(amount: number) {
  const zloty = Math.floor(amount / GROSZE_PER_ZLOTY)
  const grosze = amount % GROSZE_PER_ZLOTY
  return grosze === 0
    ? String(zloty)
    : `${zloty},${String(grosze).padStart(2, '0')}`
}

export function formatDuration(minutes: number) {
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  if (hours === 0) {
    return `${rest} min`
  }
  return rest === 0 ? `${hours} h` : `${hours} h ${rest} min`
}
