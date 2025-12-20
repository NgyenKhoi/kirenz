import { useState } from "react";
import { Link } from "react-router-dom";
import { MessageCircle, Share2 } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ReactionButton } from "@/components/ReactionButton";
import { useUserProfile } from "@/hooks/queries/useUserQueries";
import type { PostDTO } from "@/types/dto/post.dto";
import { formatDistanceToNow } from "date-fns";

interface PostCardProps {
  post: PostDTO;
}

const PostCard = ({ post }: PostCardProps) => {
  const [isLiked, setIsLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(post.likes);

  const shouldFetchProfile = !post.author;
  const { data: fetchedAuthor, isLoading } = useUserProfile(post.userId, {
    staleTime: 5 * 60 * 1000,
  });
  
  const author = post.author ? {
    id: post.userId,
    email: post.author.email,
    profile: {
      fullName: post.author.fullName,
      avatarUrl: post.author.avatarUrl,
    }
  } : fetchedAuthor;

  const handleLike = () => {
    setIsLiked(!isLiked);
    setLikeCount(prev => isLiked ? prev - 1 : prev + 1);
  };

  if (shouldFetchProfile && isLoading) {
    return (
      <Card className="p-6">
        <div className="flex space-x-4">
          <Skeleton className="h-12 w-12 rounded-full" />
          <div className="flex-1 space-y-3">
            <Skeleton className="h-4 w-32" />
            <Skeleton className="h-20 w-full" />
            <Skeleton className="h-4 w-24" />
          </div>
        </div>
      </Card>
    );
  }

  if (!author) return null;

  const displayName = post.author?.fullName || author.profile?.fullName || post.author?.email || author.email?.split('@')[0] || 'Unknown';
  const avatarUrl = post.author?.avatarUrl || author.profile?.avatarUrl;

  return (
    <Card className="overflow-hidden transition-all hover:shadow-md animate-fade-in">
      <div className="p-6">
        <div className="flex items-start space-x-4">
          <Link to={`/profile/${author.id}`}>
            <Avatar className="h-12 w-12 ring-2 ring-background transition-transform hover:scale-105">
              {avatarUrl && <AvatarImage src={avatarUrl} alt={displayName} />}
              <AvatarFallback>{displayName[0].toUpperCase()}</AvatarFallback>
            </Avatar>
          </Link>

          <div className="flex-1 space-y-3">
            <div className="flex items-center justify-between">
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
            </div>

            <Link to={`/post/${post.slug || post.id}`} className="block">
              <p className="text-foreground leading-relaxed whitespace-pre-wrap">
                {post.content}
              </p>
            </Link>

            <div className="flex items-center justify-between pt-2">
              <div className="flex items-center space-x-6">
                <ReactionButton
                  emoji={isLiked ? "❤️" : "🤍"}
                  count={likeCount}
                  isActive={isLiked}
                  onClick={handleLike}
                  className={isLiked ? "text-red-500" : "text-muted-foreground hover:text-red-500"}
                />

                <Link to={`/post/${post.slug || post.id}`}>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="group space-x-2 text-muted-foreground hover:text-primary"
                  >
                    <MessageCircle className="h-5 w-5 transition-transform group-hover:scale-110" />
                    <span className="text-sm font-medium">{post.commentsCount}</span>
                  </Button>
                </Link>

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
      </div>
    </Card>
  );
};

export default PostCard;
