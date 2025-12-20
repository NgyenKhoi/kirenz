import { cn } from "@/lib/utils";

interface TypingIndicatorProps {
  username?: string;
  className?: string;
}

const TypingIndicator = ({ username, className }: TypingIndicatorProps) => {
  return (
    <div className={cn("flex items-center gap-2 px-4 py-2", className)}>
      <div className="flex items-center gap-1">
        <span className="h-2 w-2 rounded-full bg-primary animate-bounce [animation-delay:-0.3s]" />
        <span className="h-2 w-2 rounded-full bg-primary animate-bounce [animation-delay:-0.15s]" />
        <span className="h-2 w-2 rounded-full bg-primary animate-bounce" />
      </div>
      {username && (
        <span className="text-sm text-muted-foreground">
          {username} is typing...
        </span>
      )}
    </div>
  );
};

export default TypingIndicator;
