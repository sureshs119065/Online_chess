import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { AuthLayout } from "@/pages/AuthLayout";
import { TextField } from "@/components/ui/TextField";
import { Button } from "@/components/ui/Button";
import { signup } from "@/lib/auth";
import { apiErrorMessage } from "@/lib/api";
import { useAuthStore } from "@/store/authStore";

interface FieldErrors {
  username?: string;
  email?: string;
  password?: string;
}

// Mirrors auth-service's SignupRequest bean validation (@Size(min=3,
// max=50) on username, @Size(min=8) on password) so the person sees the
// same rule client-side before ever hitting the network.
function validate(username: string, email: string, password: string): FieldErrors {
  const errors: FieldErrors = {};
  if (username.length < 3 || username.length > 50) {
    errors.username = "Username must be between 3 and 50 characters.";
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errors.email = "Enter a valid email address.";
  }
  if (password.length < 8) {
    errors.password = "Password must be at least 8 characters.";
  }
  return errors;
}

export function SignupPage() {
  const navigate = useNavigate();
  const setSession = useAuthStore((state) => state.setSession);

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);

    const errors = validate(username, email, password);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setIsSubmitting(true);
    try {
      const session = await signup({ username, email, password });
      setSession(session);
      navigate("/app");
    } catch (error) {
      setFormError(apiErrorMessage(error, "Couldn't create your account. Please try again."));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthLayout>
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
      >
        <h1 className="font-display text-3xl text-ivory">Create your account</h1>
        <p className="mt-2 text-ivory-muted">Start at a 1200 rating, same as everyone else.</p>

        <form onSubmit={handleSubmit} noValidate className="mt-8 flex flex-col gap-5">
          <TextField
            label="Username"
            type="text"
            autoComplete="username"
            required
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            error={fieldErrors.username}
          />
          <TextField
            label="Email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            error={fieldErrors.email}
          />
          <TextField
            label="Password"
            type="password"
            autoComplete="new-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            error={fieldErrors.password}
          />

          {formError && (
            <p role="alert" className="text-sm text-brick">
              {formError}
            </p>
          )}

          <Button type="submit" isLoading={isSubmitting} className="mt-2 w-full">
            Create account
          </Button>
        </form>

        <p className="mt-8 text-sm text-ivory-muted">
          Already playing?{" "}
          <Link to="/login" className="text-brass hover:underline">
            Sign in
          </Link>
        </p>
      </motion.div>
    </AuthLayout>
  );
}
