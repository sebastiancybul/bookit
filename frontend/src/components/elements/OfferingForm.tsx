import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import {
  formatDuration,
  groszeToZloty,
  zlotyAmountPattern,
  zlotyToGrosze,
} from '@/lib/format'
import { nativeControlClassName } from '@/lib/styles'
import type { Offering, OfferingRequest } from '@/services/offerings'

const MAX_PRICE_GROSZE = 10_000_000

function minuteSteps(from: number, to: number) {
  return Array.from({ length: (to - from) / 5 + 1 }, (_, i) => from + i * 5)
}

const durationOptions = minuteSteps(5, 480)
const bufferOptions = minuteSteps(0, 120)

const offeringSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'Enter the service name')
    .max(100, 'Use at most 100 characters'),
  description: z.string().trim().max(1000, 'Use at most 1000 characters'),
  price: z
    .string()
    .trim()
    .regex(zlotyAmountPattern, 'Enter a price like 80 or 79,99')
    .refine(
      (value) => zlotyToGrosze(value) <= MAX_PRICE_GROSZE,
      'Use at most 100 000 zł',
    ),
  durationMinutes: z.string(),
  bufferMinutes: z.string(),
})

type OfferingFormValues = z.infer<typeof offeringSchema>

const emptyValues: OfferingFormValues = {
  name: '',
  description: '',
  price: '',
  durationMinutes: '30',
  bufferMinutes: '0',
}

const serverFieldNames: Record<string, keyof OfferingFormValues> = {
  name: 'name',
  description: 'description',
  priceAmount: 'price',
  durationMinutes: 'durationMinutes',
  bufferMinutes: 'bufferMinutes',
}

function toFormValues(offering: Offering): OfferingFormValues {
  return {
    name: offering.name,
    description: offering.description ?? '',
    price: groszeToZloty(offering.priceAmount),
    durationMinutes: String(offering.durationMinutes),
    bufferMinutes: String(offering.bufferMinutes),
  }
}

function toRequest(values: OfferingFormValues): OfferingRequest {
  return {
    name: values.name,
    description: values.description || undefined,
    priceAmount: zlotyToGrosze(values.price),
    durationMinutes: Number(values.durationMinutes),
    bufferMinutes: Number(values.bufferMinutes),
  }
}

type OfferingFormProps = {
  offering?: Offering
  onSubmit: (request: OfferingRequest) => Promise<unknown>
  onCancel: () => void
}

export function OfferingForm({
  offering,
  onSubmit,
  onCancel,
}: OfferingFormProps) {
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<OfferingFormValues>({
    resolver: zodResolver(offeringSchema),
    defaultValues: offering ? toFormValues(offering) : emptyValues,
  })

  const showServerErrors = (error: unknown) => {
    if (error instanceof ApiError) {
      if (error.problem.code === 'OFFERING_NAME_TAKEN') {
        setError('name', {
          message: 'You already have a service with this name',
        })
        return
      }
      const fieldErrors = (error.problem.errors ?? []).filter((fieldError) =>
        Object.hasOwn(serverFieldNames, fieldError.field),
      )
      if (fieldErrors.length > 0) {
        fieldErrors.forEach((fieldError) =>
          setError(serverFieldNames[fieldError.field], {
            message: fieldError.message,
          }),
        )
        return
      }
    }
    setError('root', { message: 'Something went wrong. Please try again.' })
  }

  const submitLabel = offering ? 'Save changes' : 'Add service'

  const submit = async (values: OfferingFormValues) => {
    try {
      await onSubmit(toRequest(values))
    } catch (error) {
      showServerErrors(error)
    }
  }

  return (
    <form
      noValidate
      onSubmit={handleSubmit(submit)}
      className="flex flex-col gap-4"
    >
      {errors.root && (
        <p
          role="alert"
          className="rounded-lg bg-destructive/10 px-3.5 py-3 text-sm font-medium text-destructive"
        >
          {errors.root.message}
        </p>
      )}

      <Field data-invalid={!!errors.name}>
        <FieldLabel htmlFor="offering-name">Service name</FieldLabel>
        <Input
          id="offering-name"
          required
          maxLength={100}
          aria-invalid={!!errors.name}
          {...register('name')}
        />
        <FieldError errors={[errors.name]} />
      </Field>

      <Field data-invalid={!!errors.description}>
        <FieldLabel htmlFor="offering-description">
          Description (optional)
        </FieldLabel>
        <textarea
          id="offering-description"
          rows={3}
          maxLength={1000}
          aria-invalid={!!errors.description}
          className={`${nativeControlClassName} min-h-24 py-3`}
          {...register('description')}
        />
        <FieldError errors={[errors.description]} />
      </Field>

      <Field data-invalid={!!errors.price}>
        <FieldLabel htmlFor="offering-price">Price (zł)</FieldLabel>
        <Input
          id="offering-price"
          inputMode="decimal"
          required
          placeholder="80"
          aria-invalid={!!errors.price}
          {...register('price')}
        />
        <FieldError errors={[errors.price]} />
      </Field>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field data-invalid={!!errors.durationMinutes}>
          <FieldLabel htmlFor="offering-duration">Duration</FieldLabel>
          <select
            id="offering-duration"
            aria-invalid={!!errors.durationMinutes}
            className={`${nativeControlClassName} h-12`}
            {...register('durationMinutes')}
          >
            {durationOptions.map((minutes) => (
              <option key={minutes} value={minutes}>
                {formatDuration(minutes)}
              </option>
            ))}
          </select>
          <FieldError errors={[errors.durationMinutes]} />
        </Field>

        <Field data-invalid={!!errors.bufferMinutes}>
          <FieldLabel htmlFor="offering-buffer">Break after</FieldLabel>
          <select
            id="offering-buffer"
            aria-invalid={!!errors.bufferMinutes}
            aria-describedby="offering-buffer-hint"
            className={`${nativeControlClassName} h-12`}
            {...register('bufferMinutes')}
          >
            {bufferOptions.map((minutes) => (
              <option key={minutes} value={minutes}>
                {minutes === 0 ? 'No break' : formatDuration(minutes)}
              </option>
            ))}
          </select>
          <p
            id="offering-buffer-hint"
            className="text-xs text-muted-foreground"
          >
            Time to clean up. Clients don't see it.
          </p>
          <FieldError errors={[errors.bufferMinutes]} />
        </Field>
      </div>

      <div className="flex flex-col-reverse gap-2 pt-2 sm:flex-row sm:justify-end">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Saving…' : submitLabel}
        </Button>
      </div>
    </form>
  )
}
