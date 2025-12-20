import { Link } from "react-router-dom";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Skeleton } from "@/components/ui/skeleton";
import { useUserProfile } from "@/hooks/queries/useUserQueries";
import type { CommentDTO } from "@/types/dto/comment.dto";
import { formatDistanceToNow } from "date-fns";

interface CommentCardProps {
  comment: CommentDTO;
}

const CommentCard = ({ comment }: CommentCardProps) => {
  const { data: author, isLoading } = useUserProfile(comment.userId);

  if (isLoading) {
    return (
      <div className="flex space-x-3 py-4">
        <Skeleton className="h-10 w-10 rounded-full" />
        <div className="flex-1 space-y-2">
          <Skeleton className="h-4 w-24" />
          <Skeleton className="h-12 w-full" />
        </div>
      </div>
    );
  }

  if (!author) return null;

  const displayName = author.profile?.fullName || author.email.split('@')[0];
  const avatarUrl = author.profile?.avatarUrl;

  return (
    <div className="flex items-start space-x-4 py-4 animate-slide-in">
      <Link to={`/profile/${author.id}`}>
        <Avatar className="h-10 w-10 ring-2 ring-background transition-transform hover:scale-105">
          {avatarUrl && <AvatarImage src={avatarUrl} alt={displayName} />}
          <AvatarFallback>{displayName[0].toUpperCase()}</AvatarFallback>
        </Avatar>
      </Link>

      <div className="flex-1 space-y-1">
        <div className="flex items-center space-x-2">
          <Link 
            to={`/profile/${author.id}`}
            className="font-semibold text-sm text-foreground hover:text-primary transition-colors"
          >
            {displayName}
          </Link>
          <span className="text-xs text-muted-foreground">{author.email}</span>
          <span className="text-xs text-muted-foreground">·</span>
          <span className="text-xs text-muted-foreground">
            {formatDistanceToNow(new Date(comment.createdAt), { addSuffix: true })}
          </span>
        </div>
        <p className="text-sm text-foreground leading-relaxed">{comment.content}</p>
      </div>
    </div>
  );
};

export default CommentCard;
