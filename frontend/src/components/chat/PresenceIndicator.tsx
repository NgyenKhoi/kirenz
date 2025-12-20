import { cn } from "@/lib/utils";

interface PresenceIndicatorProps {
  isOnline: boolean;
  size?: 'sm' | 'md' | 'lg';
  showLabel?: boolean;
  className?: string;
}

const PresenceIndicator = ({ 
  isOnline, 
  size = 'md', 
  showLabel = false,
  className 
}: PresenceIndicatorProps) => {
  const sizeClasses = {
    sm: 'h-2 w-2',
    md: 'h-3 w-3',
    lg: 'h-4 w-4'
  };

  return (
    <div className={cn("flex items-center gap-1.5", className)}>
      <span 
        className={cn(
          "rounded-full",
          sizeClasses[size],
          isOnline ? "bg-green-500" : "bg-gray-400",
          isOnline && "animate-pulse"
        )}
      />
      {showLabel && (
        <span className="text-xs text-muted-foreground">
          {isOnline ? 'Online' : 'Offline'}
        </span>
      )}
    </div>
  );
};

export default PresenceIndicator;
