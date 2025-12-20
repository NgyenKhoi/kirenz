import Header from "@/components/Header";
import PostCard from "@/components/PostCard";
import CreatePost from "@/components/CreatePost";
import { useAllPosts } from "@/hooks/queries/usePostQueries";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { AlertCircle } from "lucide-react";

const Home = () => {
  // Fetch all posts
  const { data: posts, isLoading, error } = useAllPosts();

  return (
    <div className="min-h-screen bg-background">
      <Header />
      
      <main className="container mx-auto max-w-2xl px-4 py-6">
        <div className="space-y-6">
          <CreatePost />
          
          {isLoading && (
            <div className="space-y-4">
              {[1, 2, 3].map((i) => (
                <Card key={i} className="p-6">
                  <div className="flex space-x-4">
                    <Skeleton className="h-12 w-12 rounded-full" />
                    <div className="flex-1 space-y-3">
                      <Skeleton className="h-4 w-32" />
                      <Skeleton className="h-20 w-full" />
                      <Skeleton className="h-4 w-24" />
                    </div>
                  </div>
                </Card>
              ))}
            </div>
          )}

          {error && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                Failed to load posts. Please try again later.
              </AlertDescription>
            </Alert>
          )}

          {!isLoading && !error && (
            <div className="space-y-4">
              {posts && posts.length > 0 ? (
                posts.map((post) => (
                  <PostCard key={post.id} post={post} />
                ))
              ) : (
                <Card className="p-8">
                  <p className="text-center text-muted-foreground">
                    No posts yet. Be the first to create one!
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

export default Home;
