import { RouteObject } from "react-router-dom";
import Home from "@/pages/Home";
import Login from "@/pages/Login";
import Register from "@/pages/Register";
import PostDetail from "@/pages/PostDetail";
import Profile from "@/pages/Profile";
import Users from "@/pages/Users";
import DatabaseDemo from "@/pages/DatabaseDemo";
import Chat from "@/pages/Chat";
import ReactionDemo from "@/pages/ReactionDemo";
import NotFound from "@/pages/NotFound";
import ProtectedRoute from "@/components/ProtectedRoute";

export const routes: RouteObject[] = [
  {
    path: "/",
    element: (
      <ProtectedRoute>
        <Home />
      </ProtectedRoute>
    ),
  },
  {
    path: "/login",
    element: <Login />,
  },
  {
    path: "/register",
    element: <Register />,
  },
  {
    path: "/post/:id",
    element: (
      <ProtectedRoute>
        <PostDetail />
      </ProtectedRoute>
    ),
  },
  {
    path: "/profile/:id?",
    element: (
      <ProtectedRoute>
        <Profile />
      </ProtectedRoute>
    ),
  },
  {
    path: "/users",
    element: (
      <ProtectedRoute>
        <Users />
      </ProtectedRoute>
    ),
  },
  {
    path: "/database-demo",
    element: (
      <ProtectedRoute>
        <DatabaseDemo />
      </ProtectedRoute>
    ),
  },
  {
    path: "/chat",
    element: (
      <ProtectedRoute>
        <Chat />
      </ProtectedRoute>
    ),
  },
  {
    path: "/reaction-demo",
    element: (
      <ProtectedRoute>
        <ReactionDemo />
      </ProtectedRoute>
    ),
  },
  {
    path: "*",
    element: <NotFound />,
  },
];
