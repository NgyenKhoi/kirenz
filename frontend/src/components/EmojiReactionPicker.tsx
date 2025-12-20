import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ReactionButton } from './ReactionButton';

interface Reaction {
  emoji: string;
  label: string;
  color: string;
}

const reactions: Reaction[] = [
  { emoji: '❤️', label: 'Love', color: 'text-red-500' },
  { emoji: '😂', label: 'Haha', color: 'text-yellow-500' },
  { emoji: '😮', label: 'Wow', color: 'text-blue-500' },
  { emoji: '😢', label: 'Sad', color: 'text-blue-400' },
  { emoji: '😡', label: 'Angry', color: 'text-orange-500' },
  { emoji: '👍', label: 'Like', color: 'text-blue-600' },
];

interface EmojiReactionPickerProps {
  currentReaction?: string;
  count: number;
  onReactionChange?: (emoji: string | null) => void;
}

export const EmojiReactionPicker = ({
  currentReaction,
  count,
  onReactionChange,
}: EmojiReactionPickerProps) => {
  const [showPicker, setShowPicker] = useState(false);
  const [selectedReaction, setSelectedReaction] = useState<string | null>(
    currentReaction || null
  );

  const handleReactionClick = (emoji: string) => {
    const newReaction = selectedReaction === emoji ? null : emoji;
    setSelectedReaction(newReaction);
    setShowPicker(false);
    onReactionChange?.(newReaction);
  };

  const displayEmoji = selectedReaction || '🤍';
  const displayColor = selectedReaction
    ? reactions.find((r) => r.emoji === selectedReaction)?.color || ''
    : 'text-muted-foreground hover:text-red-500';

  return (
    <div
      className="relative"
      onMouseEnter={() => setShowPicker(true)}
      onMouseLeave={() => setShowPicker(false)}
    >
      <ReactionButton
        emoji={displayEmoji}
        count={count}
        isActive={!!selectedReaction}
        onClick={() => handleReactionClick(selectedReaction || reactions[0].emoji)}
        className={displayColor}
      />

      <AnimatePresence>
        {showPicker && (
          <motion.div
            initial={{ opacity: 0, y: 10, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 10, scale: 0.9 }}
            transition={{ duration: 0.2 }}
            className="absolute bottom-full left-0 mb-2 bg-background border rounded-full shadow-lg px-2 py-2 flex gap-1 z-50"
          >
            {reactions.map((reaction, index) => (
              <motion.button
                key={reaction.emoji}
                initial={{ opacity: 0, scale: 0 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: index * 0.05 }}
                whileHover={{ scale: 1.3, y: -5 }}
                whileTap={{ scale: 0.9 }}
                onClick={() => handleReactionClick(reaction.emoji)}
                className={`text-2xl p-2 rounded-full hover:bg-accent transition-colors ${
                  selectedReaction === reaction.emoji ? 'bg-accent' : ''
                }`}
                title={reaction.label}
              >
                {reaction.emoji}
              </motion.button>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};
