import { Navigate, Route, Routes } from "react-router-dom";
import { LoginPage } from "@/pages/auth/LoginPage";
import { SignupPage } from "@/pages/auth/SignupPage";
import { LobbyPage } from "@/pages/LobbyPage";
import { ProfilePage } from "@/pages/ProfilePage";
import { GamePage } from "@/pages/game/GamePage";
import { AppShell } from "@/components/layout/AppShell";
import { ProtectedRoute } from "@/routes/ProtectedRoute";

export function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />

      <Route
        element={
          <ProtectedRoute>
            <AppShell />
          </ProtectedRoute>
        }
      >
        <Route path="/app" element={<LobbyPage />} />
        <Route path="/app/profile" element={<ProfilePage />} />
        <Route path="/game/:gameId" element={<GamePage />} />
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
