import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
import { z } from 'zod'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { useRegister } from '@/hooks/useRegister'

const registerSchema = z.object({
  firstName: z
    .string()
    .trim()
    .min(1, 'Enter your first name')
    .max(50, 'Use at most 50 characters'),
  lastName: z
    .string()
    .trim()
    .min(1, 'Enter your last name')
    .max(50, 'Use at most 50 characters'),
  email: z
    .email('Enter a valid email address')
    .max(255, 'Use at most 255 characters'),
  phone: z
    .string()
    .regex(/^\+?[0-9]{9,15}$/, 'Use 9–15 digits, optionally starting with +'),
  password: z
    .string()
    .min(10, 'Use at least 10 characters')
    .max(72, 'Use at most 72 characters')
    .regex(
      /(?=.*\d)(?=.*[^A-Za-z0-9])/,
      'Include at least one digit and one special character',
    ),
})

type RegisterForm = z.infer<typeof registerSchema>

const registerFields = Object.keys(registerSchema.shape)

function isRegisterField(field: string): field is keyof RegisterForm {
  return registerFields.includes(field)
}

export function RegisterPage() {
  const registerMutation = useRegister()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      password: '',
    },
  })

  const showServerErrors = (error: Error) => {
    if (error instanceof ApiError) {
      if (error.problem.code === 'EMAIL_TAKEN') {
        setError('email', { message: error.message })
        return
      }
      const fieldErrors = (error.problem.errors ?? []).filter((fieldError) =>
        isRegisterField(fieldError.field),
      )
      if (fieldErrors.length > 0) {
        fieldErrors.forEach((fieldError) =>
          setError(fieldError.field as keyof RegisterForm, {
            message: fieldError.message,
          }),
        )
        return
      }
    }
    setError('root', { message: 'Something went wrong. Please try again.' })
  }

  const onSubmit = (data: RegisterForm) => {
    registerMutation.mutate(data, {
      onError: showServerErrors,
    })
  }

  return (
    <section className="mx-auto w-full max-w-lg px-4 py-10 sm:py-16">
      <div className="flex flex-col gap-6 rounded-3xl border border-border bg-card p-6 sm:p-9">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-3xl">Create an account</h1>
          <p className="text-muted-foreground">
            Book as a client. Add your own business any time.
          </p>
        </div>

        <form
          noValidate
          onSubmit={handleSubmit(onSubmit)}
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

          <div className="grid gap-4 sm:grid-cols-2">
            <Field data-invalid={!!errors.firstName}>
              <FieldLabel htmlFor="firstName">First name</FieldLabel>
              <Input
                id="firstName"
                autoComplete="given-name"
                required
                maxLength={50}
                aria-invalid={!!errors.firstName}
                {...register('firstName')}
              />
              <FieldError errors={[errors.firstName]} />
            </Field>

            <Field data-invalid={!!errors.lastName}>
              <FieldLabel htmlFor="lastName">Last name</FieldLabel>
              <Input
                id="lastName"
                autoComplete="family-name"
                required
                maxLength={50}
                aria-invalid={!!errors.lastName}
                {...register('lastName')}
              />
              <FieldError errors={[errors.lastName]} />
            </Field>
          </div>

          <Field data-invalid={!!errors.email}>
            <FieldLabel htmlFor="email">Email</FieldLabel>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              required
              maxLength={255}
              aria-invalid={!!errors.email}
              {...register('email')}
            />
            <FieldError errors={[errors.email]} />
          </Field>

          <Field data-invalid={!!errors.phone}>
            <FieldLabel htmlFor="phone">Phone</FieldLabel>
            <Input
              id="phone"
              type="tel"
              autoComplete="tel"
              required
              pattern="\+?[0-9]{9,15}"
              placeholder="+48600123456"
              aria-invalid={!!errors.phone}
              {...register('phone')}
            />
            <FieldError errors={[errors.phone]} />
          </Field>

          <Field data-invalid={!!errors.password}>
            <FieldLabel htmlFor="password">Password</FieldLabel>
            <Input
              id="password"
              type="password"
              autoComplete="new-password"
              required
              minLength={10}
              maxLength={72}
              aria-invalid={!!errors.password}
              aria-describedby="password-hint"
              {...register('password')}
            />
            <FieldDescription id="password-hint">
              10–72 characters, at least one digit and one special character.
            </FieldDescription>
            <FieldError errors={[errors.password]} />
          </Field>

          <Button type="submit" size="lg" disabled={registerMutation.isPending}>
            {registerMutation.isPending
              ? 'Creating account…'
              : 'Create account'}
          </Button>
        </form>

        <p className="text-sm text-muted-foreground">
          Already have an account?{' '}
          <Link
            to="/login"
            className="font-semibold text-foreground underline-offset-4 hover:underline"
          >
            Log in
          </Link>
        </p>
      </div>
    </section>
  )
}
