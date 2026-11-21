import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { useCategories } from '@/hooks/useCategories'
import type { Business, BusinessRequest } from '@/services/businesses'

const businessSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'Enter the business name')
    .max(100, 'Use at most 100 characters'),
  categoryCode: z.string().min(1, 'Choose a category'),
  phone: z
    .string()
    .transform((value) => value.replace(/[\s-]/g, ''))
    .pipe(
      z
        .string()
        .regex(
          /^\+?[0-9]{9,15}$/,
          'Use 9–15 digits, optionally starting with +',
        ),
    ),
  email: z
    .string()
    .trim()
    .max(255, 'Use at most 255 characters')
    .pipe(z.union([z.literal(''), z.email('Enter a valid email address')])),
  description: z.string().trim().max(2000, 'Use at most 2000 characters'),
  street: z
    .string()
    .trim()
    .min(1, 'Enter the street and number')
    .max(150, 'Use at most 150 characters'),
  city: z
    .string()
    .trim()
    .min(1, 'Enter the city')
    .max(80, 'Use at most 80 characters'),
  postalCode: z
    .string()
    .trim()
    .regex(/^\d{2}-\d{3}$/, 'Use the format 00-000'),
})

type BusinessFormValues = z.infer<typeof businessSchema>

const emptyValues: BusinessFormValues = {
  name: '',
  categoryCode: '',
  phone: '',
  email: '',
  description: '',
  street: '',
  city: '',
  postalCode: '',
}

const businessFields = Object.keys(businessSchema.shape)

function isBusinessField(field: string): field is keyof BusinessFormValues {
  return businessFields.includes(field)
}

function toFormValues(business: Business): BusinessFormValues {
  return {
    name: business.name,
    categoryCode: business.category.code,
    phone: business.phone,
    email: business.email ?? '',
    description: business.description ?? '',
    street: business.street,
    city: business.city,
    postalCode: business.postalCode,
  }
}

function toRequest(values: BusinessFormValues): BusinessRequest {
  return {
    ...values,
    email: values.email || undefined,
    description: values.description || undefined,
  }
}

const nativeControlClassName =
  'w-full rounded-lg border border-input bg-transparent px-3.5 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-3 aria-invalid:ring-destructive/20 md:text-[0.9375rem]'

type BusinessFormProps = {
  business?: Business
  submitLabel: string
  pendingLabel: string
  onSubmit: (request: BusinessRequest) => Promise<unknown>
}

export function BusinessForm({
  business,
  submitLabel,
  pendingLabel,
  onSubmit,
}: BusinessFormProps) {
  const categoriesQuery = useCategories()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<BusinessFormValues>({
    resolver: zodResolver(businessSchema),
    defaultValues: business ? toFormValues(business) : emptyValues,
  })

  const showServerErrors = (error: unknown) => {
    if (error instanceof ApiError) {
      const fieldErrors = (error.problem.errors ?? []).filter((fieldError) =>
        isBusinessField(fieldError.field),
      )
      if (fieldErrors.length > 0) {
        fieldErrors.forEach((fieldError) =>
          setError(fieldError.field as keyof BusinessFormValues, {
            message: fieldError.message,
          }),
        )
        return
      }
      if (error.problem.code) {
        setError('root', { message: error.message })
        return
      }
    }
    setError('root', { message: 'Something went wrong. Please try again.' })
  }

  const submit = async (values: BusinessFormValues) => {
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
        <FieldLabel htmlFor="name">Business name</FieldLabel>
        <Input
          id="name"
          autoComplete="organization"
          required
          maxLength={100}
          aria-invalid={!!errors.name}
          {...register('name')}
        />
        <FieldError errors={[errors.name]} />
      </Field>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field data-invalid={!!errors.categoryCode}>
          <FieldLabel htmlFor="categoryCode">Category</FieldLabel>
          {categoriesQuery.data ? (
            <select
              id="categoryCode"
              required
              aria-invalid={!!errors.categoryCode}
              className={`${nativeControlClassName} h-12`}
              {...register('categoryCode')}
            >
              <option value="" disabled>
                Choose a category
              </option>
              {categoriesQuery.data.map((category) => (
                <option key={category.code} value={category.code}>
                  {category.name}
                </option>
              ))}
            </select>
          ) : (
            <select
              id="categoryCode"
              disabled
              className={`${nativeControlClassName} h-12`}
            >
              <option>
                {categoriesQuery.isError
                  ? 'Categories unavailable'
                  : 'Loading…'}
              </option>
            </select>
          )}
          <FieldError errors={[errors.categoryCode]} />
        </Field>

        <Field data-invalid={!!errors.phone}>
          <FieldLabel htmlFor="phone">Phone</FieldLabel>
          <Input
            id="phone"
            type="tel"
            autoComplete="tel"
            required
            pattern="\+?[0-9 \-]{9,20}"
            placeholder="+48 600 123 456"
            aria-invalid={!!errors.phone}
            {...register('phone')}
          />
          <FieldError errors={[errors.phone]} />
        </Field>
      </div>

      <Field data-invalid={!!errors.email}>
        <FieldLabel htmlFor="email">Contact email (optional)</FieldLabel>
        <Input
          id="email"
          type="email"
          autoComplete="email"
          maxLength={255}
          aria-invalid={!!errors.email}
          {...register('email')}
        />
        <FieldError errors={[errors.email]} />
      </Field>

      <Field data-invalid={!!errors.description}>
        <FieldLabel htmlFor="description">Description (optional)</FieldLabel>
        <textarea
          id="description"
          rows={4}
          maxLength={2000}
          aria-invalid={!!errors.description}
          className={`${nativeControlClassName} min-h-28 py-3`}
          {...register('description')}
        />
        <FieldError errors={[errors.description]} />
      </Field>

      <Field data-invalid={!!errors.street}>
        <FieldLabel htmlFor="street">Street and number</FieldLabel>
        <Input
          id="street"
          autoComplete="street-address"
          required
          maxLength={150}
          aria-invalid={!!errors.street}
          {...register('street')}
        />
        <FieldError errors={[errors.street]} />
      </Field>

      <div className="grid gap-4 sm:grid-cols-[1fr_10rem]">
        <Field data-invalid={!!errors.city}>
          <FieldLabel htmlFor="city">City</FieldLabel>
          <Input
            id="city"
            autoComplete="address-level2"
            required
            maxLength={80}
            aria-invalid={!!errors.city}
            {...register('city')}
          />
          <FieldError errors={[errors.city]} />
        </Field>

        <Field data-invalid={!!errors.postalCode}>
          <FieldLabel htmlFor="postalCode">Postal code</FieldLabel>
          <Input
            id="postalCode"
            autoComplete="postal-code"
            required
            pattern="\d{2}-\d{3}"
            placeholder="00-000"
            aria-invalid={!!errors.postalCode}
            {...register('postalCode')}
          />
          <FieldError errors={[errors.postalCode]} />
        </Field>
      </div>

      <Button type="submit" size="lg" disabled={isSubmitting}>
        {isSubmitting ? pendingLabel : submitLabel}
      </Button>
    </form>
  )
}
