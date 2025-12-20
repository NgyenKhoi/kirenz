import { useState, useEffect } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Loader2, Search, User } from "lucide-react";
import { useUsers } from "@/hooks/queries/useUserQueries";
import { useAllUserPresence } from "@/hooks/queries/useChatQueries";
import { cn } from "@/lib/utils";

interface NewConversationDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSelectUser: (userId: number) => void;
  currentUserId: number;
}

export const NewConversationDialog = ({
  open,
  onOpenChange,
  onSelectUser,
  currentUserId,
}: NewConversationDialogProps) => {
  const [searchQuery, setSearchQuery] = useState("");

  // Use the existing useUsers hook
  const { data: users, isLoading } = useUsers();
  
  // Fetch user presence when dialog opens
  const { data: presenceData, refetch: refetchPresence } = useAllUserPresence(open);

  // Refetch presence when dialog opens
  useEffect(() => {
    if (open) {
      refetchPresence();
    }
  }, [open, refetchPresence]);

  // Create a map of userId to presence
  const presenceMap = new Map(
    (presenceData || []).map(p => [p.userId, p])
  );

  // Helper function to format time ago
  const formatTimeAgo = (lastSeen: string | null): string => {
    if (!lastSeen) return "";
    
    const now = new Date();
    const lastSeenDate = new Date(lastSeen);
    const diffMs = now.getTime() - lastSeenDate.getTime();
    const diffMinutes = Math.floor(diffMs / 60000);
    
    if (diffMinutes < 1) return "vừa xong";
    if (diffMinutes < 60) return `${diffMinutes} phút trước`;
    
    const diffHours = Math.floor(diffMinutes / 60);
    if (diffHours < 24) return `${diffHours} giờ trước`;
    
    const diffDays = Math.floor(diffHours / 24);
    return `${diffDays} ngày trước`;
  };

  const handleSelectUser = (userId: number) => {
    onSelectUser(userId);
    onOpenChange(false);
    setSearchQuery("");
  };

  const filteredUsers = (users || [])
    .filter((user) => user.id !== currentUserId)
    .filter((user) => {
      if (!searchQuery) return true;
      const query = searchQuery.toLowerCase();
      return (
        user.email.toLowerCase().includes(query) ||
        user.profile?.fullName?.toLowerCase().includes(query)
      );
    });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>New Conversation</DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search users..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9"
            />
          </div>

          <div className="max-h-[400px] overflow-y-auto space-y-2">
            {isLoading ? (
              <div className="flex items-center justify-center py-8">
                <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
              </div>
            ) : filteredUsers.length === 0 ? (
              <div className="text-center py-8 text-muted-foreground">
                <User className="h-12 w-12 mx-auto mb-2 opacity-50" />
                <p>No users found</p>
              </div>
            ) : (
              filteredUsers.map((user) => {
                const presence = presenceMap.get(user.id);
                const isOnline = presence?.status === 'ONLINE';
                const timeAgo = !isOnline && presence?.lastSeen 
                  ? formatTimeAgo(presence.lastSeen) 
                  : "";

                return (
                  <Button
                    key={user.id}
                    variant="ghost"
                    className="w-full justify-start h-auto py-3"
                    onClick={() => handleSelectUser(user.id)}
                  >
                    <div className="relative mr-3">
                      <Avatar className="h-10 w-10">
                        {user.profile?.avatarUrl && (
                          <AvatarImage src={user.profile.avatarUrl} alt={user.profile.fullName || user.email} />
                        )}
                        <AvatarFallback>
                          {(user.profile?.fullName || user.email)[0].toUpperCase()}
                        </AvatarFallback>
                      </Avatar>
                      <div
                        className={cn(
                          "absolute bottom-0 right-0 h-3 w-3 rounded-full border-2 border-background",
                          isOnline ? "bg-green-500" : "bg-gray-400"
                        )}
                      />
                    </div>
                    <div className="flex-1 text-left">
                      <p className="font-medium">
                        {user.profile?.fullName || user.email}
                      </p>
                      {user.profile?.fullName && (
                        <p className="text-sm text-muted-foreground">{user.email}</p>
                      )}
                      {!isOnline && timeAgo && (
                        <p className="text-xs text-muted-foreground">
                          Offline {timeAgo}
                        </p>
                      )}
                    </div>
                  </Button>
                );
              })
            )}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};
