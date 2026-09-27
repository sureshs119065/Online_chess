import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { AuthLayout } from "@/pages/AuthLayout";
import { TextField } from "@/components/ui/TextField";
import { Button } from "@/components/ui/Button";
import { login } from "@/lib/auth";
import { apiErrorMessage } from "@/lib/api";
import { useAuthStore } from "@/store/authStore";

export function LoginPage() {
  const navigate = useNavigate();
  const setSession = useAuthStore((state) => state.setSession);

  const [usernameOrEmail, setUsernameOrEmail] = useState("");
  const [password, setPassword] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);
    setIsSubmitting(true);

    try {
      const session = await login({ usernameOrEmail, password });
      setSession(session);
      navigate("/app");
    } catch (error) {
      setFormError(apiErrorMessage(error, "Couldn't sign you in. Check your details and try again."));
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
        <h1 className="font-display text-3xl text-ivory">Welcome back</h1>
        <p className="mt-2 text-ivory-muted">Sign in to pick up your next game.</p>

        <form onSubmit={handleSubmit} className="mt-8 flex flex-col gap-5">
          <TextField
            label="Username or email"
            type="text"
            autoComplete="username"
            required
            value={usernameOrEmail}
            onChange={(event) => setUsernameOrEmail(event.target.value)}
          />
          <TextField
            label="Password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />

          {formError && (
            <p role="alert" className="text-sm text-brick">
              {formError}
            </p>
          )}

          <Button type="submit" isLoading={isSubmitting} className="mt-2 w-full">
            Sign in
          </Button>
        </form>

        <p className="mt-8 text-sm text-ivory-muted">
          New here?{" "}
          <Link to="/signup" className="text-brass hover:underline">
            Create an account
          </Link>
        </p>
      </motion.div>
    </AuthLayout>
  );
}
