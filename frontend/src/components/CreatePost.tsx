import { useState } from "react";
import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Image, Smile, Loader2 } from "lucide-react";
import { useUserProfile } from "@/hooks/queries/useUserQueries";
import { useCreatePost } from "@/hooks/queries/usePostQueries";
import { useAuthStore } from "@/stores/authStore";
import { toast } from "sonner";

const CreatePost = () => {
  const [content, setContent] = useState("");
  
  // Get current user from auth store
  const currentUserId = useAuthStore((state) => state.userId);
  const { data: currentUser } = useUserProfile(currentUserId || 0);
  const createPost = useCreatePost();

  const handleSubmit = async () => {
    if (!content.trim() || !currentUserId) return;
    
    try {
      // userId is obtained from JWT token on backend
      await createPost.mutateAsync({
        content: content.trim(),
        media: []
      });
      
      toast.success("Post created successfully!", {
        description: "Your post is now live and visible to everyone.",
      });
      
      setContent("");
    } catch (error) {
      toast.error("Failed to create post", {
        description: "Please try again later.",
      });
    }
  };

  const displayName = currentUser?.profile?.fullName || currentUser?.email?.split('@')[0] || "User";
  const avatarUrl = currentUser?.profile?.avatarUrl;

  return (
    <Card className="overflow-hidden">
      <div className="p-6">
        <div className="flex space-x-4">
          <Avatar className="h-12 w-12 ring-2 ring-background">
            {avatarUrl && <AvatarImage src={avatarUrl} alt={displayName} />}
            <AvatarFallback>{displayName[0].toUpperCase()}</AvatarFallback>
          </Avatar>

          <div className="flex-1 space-y-4">
            <Textarea
              placeholder="What's on your mind?"
              className="min-h-[120px] resize-none border-0 p-0 text-lg placeholder:text-muted-foreground focus-visible:ring-0"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              disabled={createPost.isPending}
            />

            <div className="flex items-center justify-between border-t border-border pt-4">
              <div className="flex items-center space-x-2">
                <Button variant="ghost" size="icon" className="h-9 w-9 text-primary" disabled>
                  <Image className="h-5 w-5" />
                </Button>
                <Button variant="ghost" size="icon" className="h-9 w-9 text-accent-foreground" disabled>
                  <Smile className="h-5 w-5" />
                </Button>
              </div>

              <Button
                onClick={handleSubmit}
                disabled={!content.trim() || createPost.isPending || !currentUserId}
                className="px-6"
              >
                {createPost.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                Post
              </Button>
            </div>
          </div>
        </div>
      </div>
    </Card>
  );
};

export default CreatePost;
