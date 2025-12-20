import { useState } from "react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Loader2 } from "lucide-react";
import { useUserProfile } from "@/hooks/queries/useUserQueries";
import { useCreateComment } from "@/hooks/queries/useCommentQueries";
import { useAuthStore } from "@/stores/authStore";
import { toast } from "sonner";

interface CreateCommentProps {
  postId: string;
}

const CreateComment = ({ postId }: CreateCommentProps) => {
  const [content, setContent] = useState("");
  
  // Get current user from auth store
  const currentUserId = useAuthStore((state) => state.userId);
  const { data: currentUser } = useUserProfile(currentUserId || 0);
  const createComment = useCreateComment();

  const handleSubmit = async () => {
    if (!content.trim() || !currentUserId) return;
    
    try {
      // userId is obtained from JWT token on backend
      await createComment.mutateAsync({
        postId,
        content: content.trim()
      });
      
      toast.success("Comment posted!", {
        description: "Your comment has been added to the conversation.",
      });
      
      setContent("");
    } catch (error) {
      toast.error("Failed to post comment", {
        description: "Please try again later.",
      });
    }
  };

  const displayName = currentUser?.profile?.fullName || currentUser?.email?.split('@')[0] || "User";
  const avatarUrl = currentUser?.profile?.avatarUrl;

  return (
    <div className="flex space-x-4">
      <Avatar className="h-10 w-10 ring-2 ring-background">
        {avatarUrl && <AvatarImage src={avatarUrl} alt={displayName} />}
        <AvatarFallback>{displayName[0].toUpperCase()}</AvatarFallback>
      </Avatar>

      <div className="flex-1 space-y-3">
        <Textarea
          placeholder="Write a comment..."
          className="min-h-[80px] resize-none"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          disabled={createComment.isPending}
        />

        <div className="flex justify-end">
          <Button
            onClick={handleSubmit}
            disabled={!content.trim() || createComment.isPending || !currentUserId}
            size="sm"
          >
            {createComment.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Comment
          </Button>
        </div>
      </div>
    </div>
  );
};

export default CreateComment;
