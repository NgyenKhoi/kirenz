import { useParams, Link } from "react-router-dom";
import { ArrowLeft, Heart, MessageCircle, Share2, Loader2 } from "lucide-react";
import Header from "@/components/Header";
import CommentCard from "@/components/CommentCard";
import CreateComment from "@/components/CreateComment";
import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { usePost } from "@/hooks/queries/usePostQueries";
import { useUserProfile } from "@/hooks/queries/useUserQueries";
import { usePostComments } from "@/hooks/queries/useCommentQueries";
import { formatDistanceToNow } from "date-fns";

const PostDetail = () => {
  const { id } = useParams<{ id: string }>();
  const identifier = id || ""; // Can be either slug or ID
  
  const { data: post, isLoading: postLoading, error: postError } = usePost(identifier);
  const { data: author, isLoading: authorLoading } = useUserProfile(post?.userId || 0);
  const { data: comments, isLoading: commentsLoading } = usePostComments(post?.id || "");

  if (postLoading || authorLoading) {
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

  if (postError || !post || !author) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <div className="container mx-auto max-w-2xl px-4 py-6">
          <Alert variant="destructive">
            <AlertDescription>Post not found</AlertDescription>
          </Alert>
        </div>
      </div>
    );
  }

  const displayName = author.profile?.fullName || author.email.split('@')[0];
  const avatarUrl = author.profile?.avatarUrl;

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
          <div className="p-6">
            <div className="flex items-start space-x-4">
              <Link to={`/profile/${author.id}`}>
                <Avatar className="h-12 w-12 ring-2 ring-background transition-transform hover:scale-105">
                  {avatarUrl && <AvatarImage src={avatarUrl} alt={displayName} />}
                  <AvatarFallback>{displayName[0].toUpperCase()}</AvatarFallback>
                </Avatar>
              </Link>

              <div className="flex-1 space-y-4">
                <div>
                  <Link 
                    to={`/profile/${author.id}`}
                    className="font-semibold text-foreground hover:text-primary transition-colors"
                  >
                    {displayName}
                  </Link>
                  <p className="text-sm text-muted-foreground">
                    {author.email} · {formatDistanceToNow(new Date(post.createdAt), { addSuffix: true })}
                  </p>
                </div>

                <p className="text-lg text-foreground leading-relaxed whitespace-pre-wrap">
                  {post.content}
                </p>

                <div className="flex items-center space-x-6 pt-2">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="group space-x-2 text-muted-foreground hover:text-destructive"
                  >
                    <Heart className="h-5 w-5 transition-transform group-hover:scale-110" />
                    <span className="text-sm font-medium">{post.likes}</span>
                  </Button>

                  <div className="flex items-center space-x-2 text-muted-foreground">
                    <MessageCircle className="h-5 w-5" />
                    <span className="text-sm font-medium">{post.commentsCount}</span>
                  </div>

                  <Button
                    variant="ghost"
                    size="sm"
                    className="group text-muted-foreground hover:text-accent-foreground"
                  >
                    <Share2 className="h-5 w-5 transition-transform group-hover:scale-110" />
                  </Button>
                </div>
              </div>
            </div>
          </div>

          <Separator />

          <div className="p-6 space-y-6">
            <h3 className="font-semibold text-lg">Comments</h3>
            
            <CreateComment postId={post.id} />

            {commentsLoading && (
              <div className="space-y-4">
                {[1, 2].map((i) => (
                  <div key={i} className="flex space-x-3">
                    <Skeleton className="h-10 w-10 rounded-full" />
                    <div className="flex-1 space-y-2">
                      <Skeleton className="h-4 w-24" />
                      <Skeleton className="h-12 w-full" />
                    </div>
                  </div>
                ))}
              </div>
            )}

            {!commentsLoading && comments && (
              <div className="space-y-2">
                {comments.map((comment) => (
                  <CommentCard key={comment.id} comment={comment} />
                ))}
              </div>
            )}

            {!commentsLoading && (!comments || comments.length === 0) && (
              <p className="text-center text-muted-foreground py-8">
                No comments yet. Be the first to comment!
              </p>
            )}
          </div>
        </Card>
      </main>
    </div>
  );
};

export default PostDetail;
