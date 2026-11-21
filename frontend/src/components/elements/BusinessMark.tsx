import { cn } from 'cn'

const palette = [
  'bg-blue-100 text-blue-900',
  'bg-green-100 text-green-900',
  'bg-orange-100 text-orange-900',
  'bg-purple-100 text-purple-900',
  'bg-amber-100 text-amber-900',
  'bg-sky-100 text-sky-900',
]

const sizes = {
  md: 'size-13 rounded-[14px] text-[17px]',
  lg: 'size-24 rounded-[26px] text-[32px]',
}

function initialsOf(name: string): string {
  const words = name.split(/\s+/).filter((word) => /^[\p{L}\p{N}]/u.test(word))
  if (words.length === 0) {
    return '?'
  }
  const first = Array.from(words[0])
  const initials =
    words.length === 1
      ? first.slice(0, 2).join('')
      : first[0] + Array.from(words[1])[0]
  return initials.toUpperCase()
}

function colorOf(publicId: string): string {
  let sum = 0
  for (const char of publicId) {
    sum += char.charCodeAt(0)
  }
  return palette[sum % palette.length]
}

type BusinessMarkProps = {
  name: string
  publicId: string
  size?: keyof typeof sizes
}

export function BusinessMark({
  name,
  publicId,
  size = 'md',
}: BusinessMarkProps) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        'flex flex-none items-center justify-center font-extrabold tracking-[-0.03em]',
        sizes[size],
        colorOf(publicId),
      )}
    >
      {initialsOf(name)}
    </span>
  )
}
