import { cn } from 'cn'
import { initialsOf, pastelOf } from '@/lib/avatar'

const sizes = {
  md: 'size-13 rounded-[14px] text-[17px]',
  lg: 'size-24 rounded-[26px] text-[32px]',
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
        pastelOf(publicId),
      )}
    >
      {initialsOf(name)}
    </span>
  )
}
