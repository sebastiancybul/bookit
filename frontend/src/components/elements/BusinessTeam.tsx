import { PersonAvatar } from '@/components/elements/PersonAvatar'
import { useBusinessEmployees } from '@/hooks/useBusinessEmployees'
import { useBusinessOfferings } from '@/hooks/useBusinessOfferings'
import type { PublicEmployee } from '@/services/employees'
import type { PublicOffering } from '@/services/offerings'

function describeOfferings(
  employee: PublicEmployee,
  offerings: PublicOffering[],
): string | null {
  const names = offerings
    .filter((offering) => employee.offeringIds.includes(offering.id))
    .map((offering) => offering.name)
  if (names.length === 0) {
    return null
  }
  if (names.length === offerings.length && names.length > 1) {
    return 'All services'
  }
  return names.join(', ')
}

type BusinessTeamProps = {
  slug: string
}

export function BusinessTeam({ slug }: BusinessTeamProps) {
  const { data: employees } = useBusinessEmployees(slug)
  const { data: offerings = [] } = useBusinessOfferings(slug)

  if (!employees || employees.length === 0) {
    return null
  }

  return (
    <section aria-labelledby="team-heading" className="flex flex-col gap-4">
      <div className="flex items-baseline justify-between gap-3">
        <h2 id="team-heading" className="text-2xl">
          Team
        </h2>
        <span className="text-sm text-muted-foreground">
          {employees.length === 1
            ? '1 specialist'
            : `${employees.length} specialists`}
        </span>
      </div>
      <ul className="grid grid-cols-[repeat(auto-fill,minmax(15rem,1fr))] gap-4">
        {employees.map((employee) => {
          const offers = describeOfferings(employee, offerings)
          return (
            <li
              key={employee.id}
              className="flex flex-col items-start gap-3.5 rounded-[20px] border border-border p-5.5"
            >
              <PersonAvatar
                name={employee.displayName}
                seed={employee.id}
                size="lg"
              />
              <div className="flex flex-col gap-0.5">
                <span className="text-base font-bold tracking-[-0.02em]">
                  {employee.displayName}
                </span>
                {employee.position && (
                  <span className="text-sm text-secondary-foreground">
                    {employee.position}
                  </span>
                )}
                {offers && (
                  <span className="text-sm text-muted-foreground">
                    {offers}
                  </span>
                )}
              </div>
            </li>
          )
        })}
      </ul>
    </section>
  )
}
