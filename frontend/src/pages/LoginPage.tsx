import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
import { z } from 'zod'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { useLogin } from '@/hooks/useLogin'

const loginSchema = z.object({
  email: z.email('Enter a valid email address'),
  password: z.string().min(1, 'Enter your password'),
})

type LoginForm = z.infer<typeof loginSchema>

function loginErrorMessage(error: Error) {
  if (error instanceof ApiError && error.status === 401) {
    return error.message
  }
  return 'Something went wrong. Please try again.'
}

export function LoginPage() {
  const loginMutation = useLogin()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  })

  const onSubmit = (data: LoginForm) => {
    loginMutation.mutate(data)
  }

  return (
    <section className="mx-auto w-full max-w-md px-4 py-10 sm:py-16">
      <div className="flex flex-col gap-6 rounded-3xl border border-border bg-card p-6 sm:p-9">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-3xl">Welcome back</h1>
          <p className="text-muted-foreground">Log in to manage your visits.</p>
        </div>

        <form
          noValidate
          onSubmit={handleSubmit(onSubmit)}
          className="flex flex-col gap-4"
        >
          {loginMutation.error && (
            <p
              role="alert"
              className="rounded-lg bg-destructive/10 px-3.5 py-3 text-sm font-medium text-destructive"
            >
              {loginErrorMessage(loginMutation.error)}
            </p>
          )}

          <Field data-invalid={!!errors.email}>
            <FieldLabel htmlFor="email">Email</FieldLabel>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              required
              aria-invalid={!!errors.email}
              {...register('email')}
            />
            <FieldError errors={[errors.email]} />
          </Field>

          <Field data-invalid={!!errors.password}>
            <FieldLabel htmlFor="password">Password</FieldLabel>
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              required
              aria-invalid={!!errors.password}
              {...register('password')}
            />
            <FieldError errors={[errors.password]} />
          </Field>

          <Button type="submit" size="lg" disabled={loginMutation.isPending}>
            {loginMutation.isPending ? 'Logging in…' : 'Log in'}
          </Button>
        </form>

        <p className="text-sm text-muted-foreground">
          No account yet?{' '}
          <Link
            to="/register"
            className="font-semibold text-foreground underline-offset-4 hover:underline"
          >
            Sign up
          </Link>
        </p>
      </div>
    </section>
  )
}
