import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { formatDistanceToNow } from "date-fns";
import type { ConversationResponse } from "@/types/dto/chat.dto";
import PresenceIndicator from "./PresenceIndicator";
import { MessageCircle } from "lucide-react";

interface ConversationListProps {
  conversations: ConversationResponse[];
  activeConversationId: string | null;
  currentUserId: number;
  onlineUsers: Map<number, boolean>;
  onSelectConversation: (conversationId: string) => void;
  isLoading?: boolean;
  className?: string;
}

const ConversationList = ({
  conversations,
  activeConversationId,
  currentUserId,
  onlineUsers,
  onSelectConversation,
  isLoading = false,
  className
}: ConversationListProps) => {
  const getConversationName = (conversation: ConversationResponse): string => {
    if (conversation.type === 'GROUP') {
      return conversation.name || 'Group Chat';
    }
    
    const otherParticipant = conversation.participants.find(
      p => p.userId !== currentUserId
    );
    return otherParticipant?.username || 'Unknown User';
  };

  const getOtherParticipantId = (conversation: ConversationResponse): number | null => {
    if (conversation.type === 'DIRECT') {
      const otherParticipant = conversation.participants.find(
        p => p.userId !== currentUserId
      );
      return otherParticipant?.userId || null;
    }
    return null;
  };

  const isUserOnline = (userId: number | null): boolean => {
    if (userId === null) return false;
    return onlineUsers.get(userId) || false;
  };

  if (isLoading) {
    return (
      <div className={cn("space-y-2", className)}>
        {[1, 2, 3, 4, 5].map((i) => (
          <Card key={i} className="p-4">
            <div className="flex items-center gap-3">
              <Skeleton className="h-12 w-12 rounded-full" />
              <div className="flex-1 space-y-2">
                <Skeleton className="h-4 w-32" />
                <Skeleton className="h-3 w-48" />
              </div>
            </div>
          </Card>
        ))}
      </div>
    );
  }

  if (conversations.length === 0) {
    return (
      <div className={cn("flex flex-col items-center justify-center p-8 text-center", className)}>
        <MessageCircle className="h-12 w-12 text-muted-foreground mb-4" />
        <p className="text-muted-foreground">
          No conversations yet. Start chatting!
        </p>
      </div>
    );
  }

  return (
    <div className={cn("space-y-2", className)}>
      {conversations.map((conversation) => {
        const isActive = conversation.id === activeConversationId;
        const conversationName = getConversationName(conversation);
        const otherParticipantId = getOtherParticipantId(conversation);
        const isOnline = isUserOnline(otherParticipantId);
        const hasUnread = conversation.unreadCount > 0;

        return (
          <Card
            key={conversation.id}
            className={cn(
              "p-4 cursor-pointer transition-all hover:shadow-md",
              isActive && "ring-2 ring-primary bg-accent"
            )}
            onClick={() => onSelectConversation(conversation.id)}
          >
            <div className="flex items-center gap-3">
              <div className="relative">
                <Avatar className="h-12 w-12">
                  <AvatarFallback>
                    {conversationName[0].toUpperCase()}
                  </AvatarFallback>
                </Avatar>
                {conversation.type === 'DIRECT' && (
                  <div className="absolute -bottom-1 -right-1">
                    <PresenceIndicator isOnline={isOnline} size="sm" />
                  </div>
                )}
              </div>

              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between gap-2 mb-1">
                  <h3 className={cn(
                    "font-semibold text-sm truncate",
                    hasUnread && "text-foreground"
                  )}>
                    {conversationName}
                  </h3>
                  {conversation.lastMessage && (
                    <span className="text-xs text-muted-foreground shrink-0">
                      {formatDistanceToNow(new Date(conversation.lastMessage.sentAt), { 
                        addSuffix: false 
                      })}
                    </span>
                  )}
                </div>

                <div className="flex items-center justify-between gap-2">
                  <p className={cn(
                    "text-sm truncate",
                    hasUnread ? "font-medium text-foreground" : "text-muted-foreground"
                  )}>
                    {conversation.lastMessage ? (
                      <>
                        {conversation.lastMessage.senderId === currentUserId && "You: "}
                        {conversation.lastMessage.content || "Media"}
                      </>
                    ) : (
                      "No messages yet"
                    )}
                  </p>
                  
                  {hasUnread && (
                    <Badge 
                      variant="default" 
                      className="shrink-0 h-5 min-w-[20px] flex items-center justify-center px-1.5"
                    >
                      {conversation.unreadCount > 99 ? '99+' : conversation.unreadCount}
                    </Badge>
                  )}
                </div>
              </div>
            </div>
          </Card>
        );
      })}
    </div>
  );
};

export default ConversationList;
