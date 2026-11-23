import { cn } from 'cn'
import { initialsOf, pastelOf } from '@/lib/avatar'

const sizes = {
  sm: 'size-7.5 text-[11px]',
  md: 'size-10 text-sm',
  lg: 'size-14 text-lg',
}

type PersonAvatarProps = {
  name: string
  seed: string
  size?: keyof typeof sizes
  className?: string
}

export function PersonAvatar({
  name,
  seed,
  size = 'md',
  className,
}: PersonAvatarProps) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        'flex flex-none items-center justify-center rounded-full font-bold',
        sizes[size],
        pastelOf(seed),
        className,
      )}
    >
      {initialsOf(name)}
    </span>
  )
}
