import { Link } from 'react-router'

export function Logo() {
  return (
    <Link
      to="/"
      className="flex items-center gap-2.5 text-xl font-extrabold tracking-[-0.04em] text-foreground"
    >
      <span
        aria-hidden="true"
        className="flex size-7.5 items-center justify-center rounded-[9px] bg-neutral-900"
      >
        <span className="size-3 rounded-full bg-white" />
      </span>
      bookit
    </Link>
  )
}
