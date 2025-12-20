import { Link } from "react-router-dom";
import { ArrowLeft, Loader2, Users as UsersIcon } from "lucide-react";
import Header from "@/components/Header";
import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { useUsers } from "@/hooks/queries/useUserQueries";
import { format } from "date-fns";

const Users = () => {
  const { data: users, isLoading, error } = useUsers();

  return (
    <div className="min-h-screen bg-background">
      <Header />
      
      <main className="container mx-auto max-w-2xl px-4 py-6">
        <Link to="/">
          <Button variant="ghost" size="sm" className="mb-4 -ml-2">
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back to feed
          </Button>
        </Link>

        <div className="space-y-6">
          <div className="flex items-center gap-3">
            <UsersIcon className="h-8 w-8 text-primary" />
            <div>
              <h1 className="text-3xl font-bold text-foreground">All Users</h1>
              <p className="text-muted-foreground">
                Browse community members
              </p>
            </div>
          </div>

          {isLoading && (
            <div className="space-y-4">
              {[1, 2, 3].map((i) => (
                <Card key={i} className="p-6">
                  <div className="flex items-center space-x-4">
                    <Skeleton className="h-16 w-16 rounded-full" />
                    <div className="flex-1 space-y-2">
                      <Skeleton className="h-5 w-32" />
                      <Skeleton className="h-4 w-48" />
                      <Skeleton className="h-4 w-24" />
                    </div>
                  </div>
                </Card>
              ))}
            </div>
          )}

          {error && (
            <Alert variant="destructive">
              <AlertDescription>
                Failed to load users. Please try again later.
              </AlertDescription>
            </Alert>
          )}

          {!isLoading && !error && users && (
            <div className="space-y-4">
              {users.map((user) => {
                const displayName = user.profile?.fullName || user.email.split('@')[0];
                const avatarUrl = user.profile?.avatarUrl;
                const bio = user.profile?.bio;

                return (
                  <Card key={user.id} className="overflow-hidden transition-all hover:shadow-md animate-fade-in">
                    <div className="p-6">
                      <div className="flex items-start space-x-4">
                        <Link to={`/profile/${user.id}`}>
                          <Avatar className="h-16 w-16 ring-2 ring-background transition-transform hover:scale-105">
                            {avatarUrl && <AvatarImage src={avatarUrl} alt={displayName} />}
                            <AvatarFallback className="text-xl">{displayName[0].toUpperCase()}</AvatarFallback>
                          </Avatar>
                        </Link>

                        <div className="flex-1 space-y-2">
                          <div>
                            <Link 
                              to={`/profile/${user.id}`}
                              className="text-xl font-semibold text-foreground hover:text-primary transition-colors"
                            >
                              {displayName}
                            </Link>
                            <p className="text-sm text-muted-foreground">{user.email}</p>
                          </div>

                          {bio && (
                            <p className="text-foreground leading-relaxed">{bio}</p>
                          )}

                          <p className="text-xs text-muted-foreground">
                            Joined {format(new Date(user.createdAt), "MMMM yyyy")}
                          </p>
                        </div>

                        <Link to={`/profile/${user.id}`}>
                          <Button variant="outline" size="sm">
                            View Profile
                          </Button>
                        </Link>
                      </div>
                    </div>
                  </Card>
                );
              })}

              {users.length === 0 && (
                <Card className="p-8">
                  <p className="text-center text-muted-foreground">
                    No users found
                  </p>
                </Card>
              )}
            </div>
          )}
        </div>
      </main>
    </div>
  );
};

export default Users;
