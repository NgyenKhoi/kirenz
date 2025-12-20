import { Link, useNavigate } from "react-router-dom";
import { Home, User, Bell, Database, MessageCircle, Smile } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useUserProfile } from "@/hooks/queries/useUserQueries";
import { useAuthStore } from "@/stores/authStore";
import { useChatStore } from "@/stores/chatStore";

const Header = () => {
  const navigate = useNavigate();
  const currentUserId = useAuthStore((state) => state.userId);
  const clearAuthData = useAuthStore((state) => state.clearAuthData);
  const conversations = useChatStore((state) => state.conversations);
  const { data: currentUser } = useUserProfile(currentUserId || 0, {
    staleTime: 5 * 60 * 1000, // Cache for 5 minutes
  });
  
  const displayName = currentUser?.profile?.fullName || currentUser?.email?.split('@')[0] || "User";
  const avatarUrl = currentUser?.profile?.avatarUrl;
  
  // Calculate total unread count
  const totalUnreadCount = conversations.reduce((sum, conv) => sum + conv.unreadCount, 0);

  const handleLogout = () => {
    clearAuthData();
    navigate('/login');
  };

  return (
    <header className="sticky top-0 z-50 w-full border-b border-border bg-card/95 backdrop-blur supports-[backdrop-filter]:bg-card/80">
      <div className="container mx-auto flex h-16 items-center justify-between px-4">
        <Link to="/" className="flex items-center space-x-2">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary">
            <span className="text-xl font-bold text-primary-foreground">S</span>
          </div>
          <span className="text-xl font-bold text-foreground">SocialHub</span>
        </Link>

        <nav className="flex items-center space-x-4">
          <Link to="/">
            <Button variant="ghost" size="icon" className="relative">
              <Home className="h-5 w-5" />
            </Button>
          </Link>

          <Link to="/chat">
            <Button variant="ghost" size="icon" className="relative">
              <MessageCircle className="h-5 w-5" />
              {totalUnreadCount > 0 && (
                <Badge 
                  variant="destructive" 
                  className="absolute -right-1 -top-1 h-5 min-w-5 px-1 text-xs"
                >
                  {totalUnreadCount > 99 ? '99+' : totalUnreadCount}
                </Badge>
              )}
            </Button>
          </Link>

          <Link to="/users">
            <Button variant="ghost" size="sm" className="gap-2">
              <User className="h-4 w-4" />
              <span className="hidden sm:inline">Users</span>
            </Button>
          </Link>

          <Link to="/database-demo">
            <Button variant="ghost" size="sm" className="gap-2">
              <Database className="h-4 w-4" />
              <span className="hidden sm:inline">DB Demo</span>
            </Button>
          </Link>

          <Link to="/reaction-demo">
            <Button variant="ghost" size="sm" className="gap-2">
              <Smile className="h-4 w-4" />
              <span className="hidden sm:inline">Reactions</span>
              <Badge variant="secondary" className="ml-1">New</Badge>
            </Button>
          </Link>

          <Button variant="ghost" size="icon" className="relative">
            <Bell className="h-5 w-5" />
            <span className="absolute right-1 top-1 flex h-2 w-2">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-primary opacity-75"></span>
              <span className="relative inline-flex h-2 w-2 rounded-full bg-primary"></span>
            </span>
          </Button>

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="rounded-full">
                <Avatar className="h-9 w-9">
                  {avatarUrl && <AvatarImage src={avatarUrl} alt={displayName} />}
                  <AvatarFallback>{displayName[0].toUpperCase()}</AvatarFallback>
                </Avatar>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel>My Account</DropdownMenuLabel>
              <DropdownMenuSeparator />
              <Link to="/profile">
                <DropdownMenuItem className="cursor-pointer">
                  <User className="mr-2 h-4 w-4" />
                  <span>Profile</span>
                </DropdownMenuItem>
              </Link>
              <Link to="/database-demo">
                <DropdownMenuItem className="cursor-pointer">
                  <Database className="mr-2 h-4 w-4" />
                  <span>Database Demo</span>
                </DropdownMenuItem>
              </Link>
              <DropdownMenuSeparator />
              <DropdownMenuItem 
                className="cursor-pointer text-destructive"
                onClick={handleLogout}
              >
                Logout
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </nav>
      </div>
    </header>
  );
};

export default Header;
