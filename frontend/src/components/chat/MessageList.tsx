import { useEffect, useRef, useState } from "react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { formatDistanceToNow } from "date-fns";
import type { MessageResponse } from "@/types/dto/chat.dto";
import { Image as ImageIcon, Video as VideoIcon } from "lucide-react";
import ImageViewer from "./ImageViewer";

interface MessageListProps {
  messages: MessageResponse[];
  currentUserId: number;
  isLoading?: boolean;
}

const MessageList = ({ 
  messages, 
  currentUserId, 
  isLoading = false
}: MessageListProps) => {
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [viewerOpen, setViewerOpen] = useState(false);
  const [viewerImages, setViewerImages] = useState<string[]>([]);
  const [viewerIndex, setViewerIndex] = useState(0);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleImageClick = (images: string[], index: number) => {
    setViewerImages(images);
    setViewerIndex(index);
    setViewerOpen(true);
  };

  if (isLoading && messages.length === 0) {
    return (
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {[1, 2, 3, 4, 5].map((i) => (
          <div key={i} className={cn("flex gap-3", i % 2 === 0 ? "justify-end" : "justify-start")}>
            {i % 2 !== 0 && <Skeleton className="h-10 w-10 rounded-full shrink-0" />}
            <div className={cn("space-y-2", i % 2 === 0 ? "items-end" : "items-start")}>
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-16 w-64 rounded-2xl" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (messages.length === 0) {
    return (
      <div className="flex-1 flex items-center justify-center p-8">
        <p className="text-muted-foreground text-center">
          No messages yet. Start the conversation!
        </p>
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto p-4 space-y-4">
      {messages.map((message, index) => {
        const isOwnMessage = message.senderId === currentUserId;
        const showAvatar = !isOwnMessage && (
          index === 0 || 
          messages[index - 1].senderId !== message.senderId
        );
        const showName = !isOwnMessage && showAvatar;
        
        // Show time only for last 2 messages
        const isLastMessage = index === messages.length - 1;
        const isSecondLastMessage = index === messages.length - 2;
        const showTime = isLastMessage || isSecondLastMessage;

        return (
          <div
            key={message.id}
            className={cn(
              "flex gap-3 animate-fade-in group",
              isOwnMessage ? "justify-end" : "justify-start"
            )}
          >
            {!isOwnMessage && (
              <div className="shrink-0">
                {showAvatar ? (
                  <Avatar className="h-10 w-10">
                    <AvatarFallback>
                      {message.senderName ? message.senderName[0].toUpperCase() : 'U'}
                    </AvatarFallback>
                  </Avatar>
                ) : (
                  <div className="h-10 w-10" />
                )}
              </div>
            )}

            <div className={cn("flex flex-col gap-2 max-w-[70%]", isOwnMessage && "items-end")}>
              {showName && (
                <span className="text-xs text-muted-foreground px-3">
                  {message.senderName || 'Unknown User'}
                </span>
              )}
              
              {/* Media attachments - separate from text bubble */}
              {message.attachments && message.attachments.length > 0 && (
                <div className={cn(
                  "gap-1",
                  message.attachments.length === 1 ? "w-[250px]" : 
                  message.attachments.length === 2 ? "grid grid-cols-2 w-[250px]" :
                  message.attachments.length === 3 ? "grid grid-cols-2 w-[250px]" :
                  "grid grid-cols-2 w-[250px]"
                )}>
                  {message.attachments.map((attachment, idx) => {
                    const imageUrls = message.attachments!
                      .filter(a => a.type === 'IMAGE')
                      .map(a => a.url);
                    
                    return (
                      <div 
                        key={idx} 
                        className={cn(
                          "rounded-lg overflow-hidden cursor-pointer hover:opacity-90 transition-opacity",
                          message.attachments!.length === 3 && idx === 0 ? "col-span-2" : "",
                          message.attachments!.length === 1 ? "aspect-[4/3]" : "aspect-square"
                        )}
                        onClick={() => {
                          if (attachment.type === 'IMAGE') {
                            handleImageClick(imageUrls, imageUrls.indexOf(attachment.url));
                          } else {
                            window.open(attachment.url, '_blank');
                          }
                        }}
                      >
                        {attachment.type === 'IMAGE' ? (
                          <img 
                            src={attachment.url} 
                            alt="Attachment"
                            className="w-full h-full object-cover"
                          />
                        ) : attachment.type === 'VIDEO' ? (
                          <div className="relative w-full h-full bg-black">
                            <video 
                              src={attachment.url}
                              className="w-full h-full object-cover"
                            />
                            <div className="absolute inset-0 flex items-center justify-center bg-black/30">
                              <VideoIcon className="h-12 w-12 text-white" />
                            </div>
                          </div>
                        ) : (
                          <div className="flex items-center gap-2 p-2 bg-muted rounded-lg h-full">
                            <ImageIcon className="h-4 w-4" />
                            <span className="text-sm">Media attachment</span>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
              
              {/* Text message bubble - only show if there's content */}
              {message.content && (
                <div
                  className={cn(
                    "rounded-2xl px-4 py-2 break-words",
                    isOwnMessage
                      ? "bg-primary text-primary-foreground rounded-br-sm"
                      : "bg-muted text-foreground rounded-bl-sm"
                  )}
                >
                  <p className="text-sm whitespace-pre-wrap">{message.content}</p>
                </div>
              )}

              {showTime ? (
                <span className="text-xs text-muted-foreground px-3">
                  {formatDistanceToNow(new Date(message.sentAt), { addSuffix: true })}
                </span>
              ) : (
                <span className="text-xs text-muted-foreground px-3 opacity-0 group-hover:opacity-100 transition-opacity">
                  {formatDistanceToNow(new Date(message.sentAt), { addSuffix: true })}
                </span>
              )}
            </div>
          </div>
        );
      })}
      
      <div ref={messagesEndRef} />
      
      {/* Image Viewer Dialog */}
      <ImageViewer
        images={viewerImages}
        currentIndex={viewerIndex}
        open={viewerOpen}
        onOpenChange={setViewerOpen}
      />
    </div>
  );
};

export default MessageList;
