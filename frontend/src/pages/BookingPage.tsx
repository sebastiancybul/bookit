import { useState, type ReactNode } from 'react'
import { Link, Navigate, useParams, useSearchParams } from 'react-router'
import { ArrowLeftIcon } from 'lucide-react'
import { ApiError } from '@/api/client'
import { BusinessMark } from '@/components/elements/BusinessMark'
import { SlotPicker } from '@/components/elements/SlotPicker'
import { SpecialistPicker } from '@/components/elements/SpecialistPicker'
import { Button, buttonVariants } from '@/components/ui/button'
import { useBusiness } from '@/hooks/useBusiness'
import { useBusinessEmployees } from '@/hooks/useBusinessEmployees'
import { useBusinessOfferings } from '@/hooks/useBusinessOfferings'
import { useToday } from '@/hooks/useToday'
import { formatSlotTime } from '@/lib/booking-dates'
import { formatDuration, formatPrice } from '@/lib/format'
import type { AvailableSlot } from '@/services/availability'
import type { Business } from '@/services/businesses'

export function BookingPage() {
  const { slug = '' } = useParams()
  const [searchParams] = useSearchParams()
  const offeringId = searchParams.get('offeringId') ?? ''
  const { data: business, isPending, error } = useBusiness(slug)

  if (business && business.slug !== slug) {
    return (
      <Navigate
        to={`/businesses/${business.slug}/book?offeringId=${encodeURIComponent(offeringId)}`}
        replace
      />
    )
  }

  return (
    <div className="mx-auto flex max-w-300 flex-col gap-7 px-4 pt-8 pb-24 sm:px-8">
      <div className="flex flex-col gap-2">
        <Link
          to={`/businesses/${slug}`}
          className="flex items-center gap-1.5 self-start text-sm font-medium text-muted-foreground hover:text-foreground"
        >
          <ArrowLeftIcon aria-hidden="true" className="size-4" />
          {business?.name ?? 'Back to the business'}
        </Link>
        <h1 className="text-4xl leading-[1.05] sm:text-[40px]">Book a visit</h1>
      </div>

      {isPending && <p className="text-muted-foreground">Loading…</p>}

      {error && !business && (
        <p role="alert" className="text-muted-foreground">
          {error instanceof ApiError && error.status === 404
            ? 'This business does not exist or is no longer listed.'
            : "We couldn't load this business. Please try again."}
        </p>
      )}

      {business && <BookingSteps business={business} offeringId={offeringId} />}
    </div>
  )
}

type BookingStepsProps = {
  business: Business
  offeringId: string
}

function BookingSteps({ business, offeringId }: BookingStepsProps) {
  const offeringsQuery = useBusinessOfferings(business.slug)
  const employeesQuery = useBusinessEmployees(business.slug)
  const [employeeId, setEmployeeId] = useState<string | null>(null)
  const [date, setDate] = useState<string | null>(null)
  const [slot, setSlot] = useState<AvailableSlot | null>(null)
  const today = useToday(business.timezone)

  if (!offeringsQuery.data) {
    if (offeringsQuery.isPending) {
      return <p className="text-muted-foreground">Loading…</p>
    }
    return (
      <p role="alert" className="text-muted-foreground">
        We couldn't load the services. Please refresh the page.
      </p>
    )
  }

  const offering = offeringsQuery.data.find((item) => item.id === offeringId)
  if (!offering) {
    return (
      <div className="flex flex-col items-start gap-4">
        <p className="text-muted-foreground">
          This service is not available. Please pick one from the list.
        </p>
        <Link to={`/businesses/${business.slug}`} className={buttonVariants()}>
          See services
        </Link>
      </div>
    )
  }

  const selectedDate = date && date >= today ? date : today
  const employee =
    employeesQuery.data?.find((item) => item.id === employeeId) ?? null

  return (
    <div className="flex flex-wrap items-start gap-6">
      <div className="flex min-w-0 flex-[999_1_35rem] flex-col gap-4">
        <StepCard number={1} title="Service">
          <div className="flex items-center justify-between gap-3 rounded-[14px] bg-muted px-4.5 py-4">
            <span className="flex flex-col gap-0.5">
              <span className="font-semibold">{offering.name}</span>
              <span className="text-sm text-secondary-foreground">
                {formatDuration(offering.durationMinutes)} ·{' '}
                {formatPrice(offering.priceAmount, offering.currency)}
              </span>
            </span>
            <Link
              to={`/businesses/${business.slug}`}
              className={buttonVariants({ variant: 'outline', size: 'sm' })}
            >
              Change
            </Link>
          </div>
        </StepCard>

        <StepCard number={2} title="Specialist">
          {employeesQuery.isPending && (
            <p className="text-muted-foreground">Loading…</p>
          )}
          {employeesQuery.isError && !employeesQuery.data && (
            <p role="alert" className="text-muted-foreground">
              We couldn't load the specialists. Please refresh the page.
            </p>
          )}
          {employeesQuery.data && (
            <SpecialistPicker
              employees={employeesQuery.data}
              offeringId={offering.id}
              selectedEmployeeId={employeeId}
              onSelect={(picked) => {
                setEmployeeId(picked.id)
                setSlot(null)
              }}
            />
          )}
        </StepCard>

        <StepCard number={3} title="Day and time">
          {employee ? (
            <SlotPicker
              slug={business.slug}
              offeringId={offering.id}
              employeeId={employee.id}
              timeZone={business.timezone}
              today={today}
              date={selectedDate}
              onDateChange={(day) => {
                setDate(day)
                setSlot(null)
              }}
              selectedStartTime={slot?.startTime ?? null}
              onSelectSlot={setSlot}
            />
          ) : (
            <p className="text-muted-foreground">
              Choose a specialist to see free times.
            </p>
          )}
        </StepCard>
      </div>

      <aside
        aria-labelledby="summary-heading"
        className="flex flex-[1_1_20rem] flex-col gap-5 rounded-[20px] bg-muted p-6"
      >
        <h2 id="summary-heading" className="text-[17px] font-bold">
          Summary
        </h2>
        <div className="flex items-center gap-3 rounded-[14px] bg-background p-3.5">
          <BusinessMark name={business.name} publicId={business.publicId} />
          <span className="flex min-w-0 flex-col gap-0.5">
            <span className="font-bold">{business.name}</span>
            <span className="text-[13px] text-secondary-foreground">
              {business.street}, {business.city}
            </span>
          </span>
        </div>
        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2.5 text-sm">
          <dt className="text-secondary-foreground">Service</dt>
          <dd className="text-right font-semibold">{offering.name}</dd>
          <dt className="text-secondary-foreground">Specialist</dt>
          <dd className="text-right font-semibold">
            {employee?.displayName ?? '—'}
          </dd>
          <dt className="text-secondary-foreground">When</dt>
          <dd className="text-right font-semibold">
            {slot ? formatSlotTime(slot.startTime, business.timezone) : '—'}
          </dd>
          <dt className="border-t border-border pt-2.5 text-secondary-foreground">
            Total
          </dt>
          <dd className="border-t border-border pt-2.5 text-right text-lg font-extrabold">
            {formatPrice(offering.priceAmount, offering.currency)}
          </dd>
        </dl>
        <Button type="button" size="lg" disabled className="w-full">
          {slot ? 'Confirm booking' : 'Pick a time to continue'}
        </Button>
        {slot && (
          <p className="text-center text-[13px] text-secondary-foreground">
            Online booking opens soon. Please call the business to book this
            time.
          </p>
        )}
        <p className="text-center text-[13px] text-secondary-foreground">
          {business.freeCancellationHours > 0
            ? `Free cancellation up to ${business.freeCancellationHours} h before.`
            : 'You can cancel any time before your visit.'}
        </p>
      </aside>
    </div>
  )
}

type StepCardProps = {
  number: number
  title: string
  children: ReactNode
}

function StepCard({ number, title, children }: StepCardProps) {
  const headingId = `step-${number}-heading`
  return (
    <section
      aria-labelledby={headingId}
      className="flex flex-col gap-4 rounded-[20px] border border-border p-6"
    >
      <h2
        id={headingId}
        className="flex items-center gap-2.5 text-[17px] font-bold"
      >
        <span
          aria-hidden="true"
          className="flex size-6.5 items-center justify-center rounded-full bg-foreground text-[13px] text-background"
        >
          {number}
        </span>
        {title}
      </h2>
      {children}
    </section>
  )
}
