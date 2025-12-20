import { useState, useRef, KeyboardEvent } from "react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Send, Image, Video, Loader2, X } from "lucide-react";
import { toast } from "sonner";
import type { MediaUploadRequest, MediaUploadResponse } from "@/types/dto/chat.dto";

interface UploadedMedia {
  uploadResponse: MediaUploadResponse;
  previewUrl: string;
  isUploading: boolean;
}

interface MessageInputProps {
  onSendMessage: (content: string, attachments?: MediaUploadResponse[]) => void;
  onTyping?: () => void;
  disabled?: boolean;
  placeholder?: string;
}

const MessageInput = ({ 
  onSendMessage, 
  onTyping,
  disabled = false,
  placeholder = "Type a message..."
}: MessageInputProps) => {
  const [content, setContent] = useState("");
  const [uploadedMedia, setUploadedMedia] = useState<UploadedMedia[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const handleSend = () => {
    // Check if any media is still uploading
    const stillUploading = uploadedMedia.some(m => m.isUploading);
    if (stillUploading) {
      toast.error('Please wait for media to finish uploading');
      return;
    }
    
    if (!content.trim() && uploadedMedia.length === 0) return;
    
    // Extract upload responses
    const attachments = uploadedMedia.map(m => m.uploadResponse);
    
    onSendMessage(content.trim(), attachments.length > 0 ? attachments : undefined);
    setContent("");
    
    // Clean up preview URLs
    uploadedMedia.forEach(m => URL.revokeObjectURL(m.previewUrl));
    setUploadedMedia([]);
    
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
    
    if (onTyping) {
      onTyping();
    }
  };

  const handleFileSelect = async (type: 'IMAGE' | 'VIDEO') => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = type === 'IMAGE' ? 'image/*' : 'video/*';
    input.multiple = true; // Allow multiple files
    
    input.onchange = async (e) => {
      const files = Array.from((e.target as HTMLInputElement).files || []);
      if (files.length === 0) return;

      const maxSize = type === 'IMAGE' ? 10 * 1024 * 1024 : 500 * 1024 * 1024;
      
      for (const file of files) {
        if (file.size > maxSize) {
          toast.error(`${file.name} is too large`, {
            description: `Maximum size is ${type === 'IMAGE' ? '10MB' : '500MB'}`
          });
          continue;
        }

        // Create preview URL immediately
        const previewUrl = URL.createObjectURL(file);
        
        // Add to state with uploading status
        const tempId = Date.now() + Math.random();
        setUploadedMedia(prev => [...prev, {
          uploadResponse: {} as MediaUploadResponse, // Placeholder
          previewUrl,
          isUploading: true
        }]);
        
        // Start upload in background
        try {
          const reader = new FileReader();
          reader.onload = async () => {
            const base64Data = (reader.result as string).split(',')[1];
            
            const mediaRequest: MediaUploadRequest = {
              type,
              base64Data,
              fileName: file.name,
              fileSize: file.size
            };
            
            // Upload to server
            const { chatApi } = await import('@/api/chatApi');
            const response = await chatApi.uploadMedia(mediaRequest);
            
            // Update with upload response
            setUploadedMedia(prev => prev.map(m => 
              m.previewUrl === previewUrl 
                ? { ...m, uploadResponse: response.result!, isUploading: false }
                : m
            ));
            
            toast.success(`${type === 'IMAGE' ? 'Image' : 'Video'} uploaded`);
          };
          
          reader.onerror = () => {
            toast.error('Failed to read file');
            setUploadedMedia(prev => prev.filter(m => m.previewUrl !== previewUrl));
            URL.revokeObjectURL(previewUrl);
          };
          
          reader.readAsDataURL(file);
        } catch (error) {
          console.error('Upload failed:', error);
          toast.error('Failed to upload file');
          setUploadedMedia(prev => prev.filter(m => m.previewUrl !== previewUrl));
          URL.revokeObjectURL(previewUrl);
        }
      }
    };
    
    input.click();
  };

  const removeAttachment = (index: number) => {
    const media = uploadedMedia[index];
    if (media.previewUrl) {
      URL.revokeObjectURL(media.previewUrl);
    }
    setUploadedMedia(prev => prev.filter((_, i) => i !== index));
  };

  const handleTextareaChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setContent(e.target.value);
    
    const textarea = e.target;
    textarea.style.height = 'auto';
    textarea.style.height = `${Math.min(textarea.scrollHeight, 120)}px`;
  };

  const isAnyUploading = uploadedMedia.some(m => m.isUploading);

  return (
    <div className="border-t border-border bg-background p-4">
      {uploadedMedia.length > 0 && (
        <div className="mb-3 grid grid-cols-4 gap-2">
          {uploadedMedia.map((media, index) => (
            <div 
              key={index}
              className="relative group aspect-square"
            >
              <div className="relative rounded-lg overflow-hidden border-2 border-border h-full">
                <img 
                  src={media.previewUrl} 
                  alt="Preview"
                  className="w-full h-full object-cover"
                />
                
                {/* Upload loading overlay */}
                {media.isUploading && (
                  <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
                    <Loader2 className="h-6 w-6 text-white animate-spin" />
                  </div>
                )}
                
                {/* Remove button */}
                <button
                  onClick={() => removeAttachment(index)}
                  className="absolute top-1 right-1 bg-destructive text-destructive-foreground rounded-full p-1 opacity-0 group-hover:opacity-100 transition-opacity"
                  disabled={media.isUploading}
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
      
      <div className="flex items-end gap-2">
        <div className="flex gap-1">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => handleFileSelect('IMAGE')}
            disabled={disabled}
            className="h-9 w-9"
          >
            <Image className="h-5 w-5" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => handleFileSelect('VIDEO')}
            disabled={disabled}
            className="h-9 w-9"
          >
            <Video className="h-5 w-5" />
          </Button>
        </div>

        <Textarea
          ref={textareaRef}
          placeholder={placeholder}
          value={content}
          onChange={handleTextareaChange}
          onKeyDown={handleKeyDown}
          disabled={disabled}
          className="min-h-[40px] max-h-[120px] resize-none"
          rows={1}
        />

        <Button
          onClick={handleSend}
          disabled={(!content.trim() && uploadedMedia.length === 0) || disabled || isAnyUploading}
          size="icon"
          className="h-9 w-9 shrink-0"
        >
          <Send className="h-5 w-5" />
        </Button>
      </div>
    </div>
  );
};

export default MessageInput;
