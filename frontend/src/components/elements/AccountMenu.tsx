import { Link } from 'react-router'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuLinkItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useLogout } from '@/hooks/useLogout'
import type { User } from '@/services/auth'

type AccountMenuProps = {
  user: User
}

export function AccountMenu({ user }: AccountMenuProps) {
  const logoutMutation = useLogout()
  const initials = `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`
  const isOwner = user.roles.includes('ROLE_OWNER')

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        aria-label="Account menu"
        className="flex size-10 items-center justify-center rounded-full bg-blue-100 text-sm font-bold text-blue-900 uppercase outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
      >
        {initials}
      </DropdownMenuTrigger>

      <DropdownMenuContent align="end" className="w-60 p-1.5">
        <DropdownMenuGroup>
          <DropdownMenuLabel className="flex flex-col gap-0.5 px-2.5 py-2">
            <span className="text-sm font-semibold text-foreground">
              {user.firstName} {user.lastName}
            </span>
            <span className="truncate">{user.email}</span>
          </DropdownMenuLabel>
        </DropdownMenuGroup>

        <DropdownMenuSeparator />

        {isOwner ? (
          <DropdownMenuLinkItem
            render={<Link to="/owner" />}
            className="px-2.5 py-2"
          >
            Owner panel
          </DropdownMenuLinkItem>
        ) : (
          <DropdownMenuLinkItem
            render={<Link to="/business/new" />}
            className="px-2.5 py-2"
          >
            Create your business
          </DropdownMenuLinkItem>
        )}

        <DropdownMenuItem
          disabled={logoutMutation.isPending}
          onClick={() => logoutMutation.mutate()}
          className="px-2.5 py-2"
        >
          Log out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
