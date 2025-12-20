import { useParams, Link } from "react-router-dom";
import { ArrowLeft, MapPin, Calendar, Loader2 } from "lucide-react";
import Header from "@/components/Header";
import PostCard from "@/components/PostCard";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { useUserProfile } from "@/hooks/queries/useUserQueries";
import { useUserPosts } from "@/hooks/queries/usePostQueries";
import { useAuthStore } from "@/stores/authStore";
import { format } from "date-fns";

const Profile = () => {
  const { id } = useParams<{ id: string }>();
  const currentUserId = useAuthStore((state) => state.userId);
  
  // Use URL id if provided, otherwise use current user id
  const userId = id ? parseInt(id) : (currentUserId || 0);
  const isOwnProfile = !id || userId === currentUserId;
  
  console.log('Profile Debug:', {
    urlId: id,
    currentUserId,
    resolvedUserId: userId,
    isOwnProfile
  });
  
  const { data: user, isLoading: userLoading, error: userError } = useUserProfile(userId);
  const { data: posts, isLoading: postsLoading } = useUserPosts(userId);
  
  console.log('User data:', user);

  // If no userId (not logged in or invalid), redirect to login
  if (!userId || userId === 0) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <div className="container mx-auto max-w-2xl px-4 py-6">
          <Alert variant="destructive">
            <AlertDescription>Please log in to view profile</AlertDescription>
          </Alert>
        </div>
      </div>
    );
  }

  if (userLoading) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <div className="container mx-auto max-w-2xl px-4 py-6">
          <Card className="p-6">
            <div className="flex items-center justify-center">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          </Card>
        </div>
      </div>
    );
  }

  if (userError || !user) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <div className="container mx-auto max-w-2xl px-4 py-6">
          <Alert variant="destructive">
            <AlertDescription>User not found</AlertDescription>
          </Alert>
        </div>
      </div>
    );
  }

  const displayName = user.profile?.fullName || user.email.split('@')[0];
  const avatarUrl = user.profile?.avatarUrl;
  const bio = user.profile?.bio;
  const birthday = user.profile?.birthday;

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

        <Card className="overflow-hidden animate-fade-in">
          <div className="h-32 bg-gradient-to-r from-primary via-accent to-secondary"></div>
          
          <div className="p-6 -mt-16">
            <div className="flex items-end justify-between mb-4">
              <Avatar className="h-32 w-32 ring-4 ring-card">
                {avatarUrl && <AvatarImage src={avatarUrl} alt={displayName} />}
                <AvatarFallback className="text-4xl">{displayName[0].toUpperCase()}</AvatarFallback>
              </Avatar>
              
              {isOwnProfile && (
                <Button variant="outline">Edit Profile</Button>
              )}
            </div>

            <div className="space-y-3">
              <div>
                <h1 className="text-2xl font-bold text-foreground">{displayName}</h1>
                <p className="text-muted-foreground">{user.email}</p>
              </div>

              {bio && (
                <p className="text-foreground leading-relaxed">{bio}</p>
              )}

              <div className="flex items-center space-x-4 text-sm text-muted-foreground">
                {birthday && (
                  <div className="flex items-center space-x-1">
                    <Calendar className="h-4 w-4" />
                    <span>Born {format(new Date(birthday), "MMMM d, yyyy")}</span>
                  </div>
                )}
                <div className="flex items-center space-x-1">
                  <Calendar className="h-4 w-4" />
                  <span>Joined {format(new Date(user.createdAt), "MMMM yyyy")}</span>
                </div>
              </div>
            </div>
          </div>
        </Card>

        <div className="mt-6 space-y-4">
          <h2 className="text-xl font-semibold px-2">Posts</h2>
          
          {postsLoading && (
            <div className="space-y-4">
              {[1, 2].map((i) => (
                <Card key={i} className="p-6">
                  <div className="flex space-x-4">
                    <Skeleton className="h-12 w-12 rounded-full" />
                    <div className="flex-1 space-y-3">
                      <Skeleton className="h-4 w-32" />
                      <Skeleton className="h-20 w-full" />
                    </div>
                  </div>
                </Card>
              ))}
            </div>
          )}

          {!postsLoading && posts && posts.length > 0 && (
            posts.map((post) => (
              <PostCard key={post.id} post={post} />
            ))
          )}

          {!postsLoading && (!posts || posts.length === 0) && (
            <Card className="p-8">
              <p className="text-center text-muted-foreground">
                No posts yet
              </p>
            </Card>
          )}
        </div>
      </main>
    </div>
  );
};

export default Profile;
