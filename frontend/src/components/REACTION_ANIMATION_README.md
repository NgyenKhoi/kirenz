# Reaction Animation Components

## Overview

Các component reaction với animation sử dụng Framer Motion, bao gồm:
- Particle burst/explosion effect
- Spring scale animation
- Wiggle/shake animation
- Staggered animation cho emoji picker

## Components

### 1. ReactionButton

Component cơ bản với animation khi click và hover.

```tsx
import { ReactionButton } from '@/components/ReactionButton';

<ReactionButton
  emoji="❤️"
  count={42}
  isActive={true}
  onClick={() => console.log('Clicked')}
  className="text-red-500"
/>
```

**Props:**
- `emoji`: string - Emoji hiển thị
- `count`: number - Số lượng reactions
- `isActive`: boolean - Trạng thái active (trigger wiggle animation)
- `onClick`: () => void - Callback khi click
- `variant`: 'ghost' | 'default' | 'outline' - Button variant
- `size`: 'sm' | 'default' | 'lg' - Button size
- `className`: string - Custom CSS classes

**Animations:**
- **Particle Burst**: 8 particles bay ra khi click (chỉ khi chưa active)
- **Spring Scale**: Hover scale 1.05x, tap scale 0.95x
- **Wiggle**: Khi active, scale 1.4x và rotate -15° → 15° → 0°

### 2. EmojiReactionPicker

Component picker với nhiều emoji reactions (giống Facebook).

```tsx
import { EmojiReactionPicker } from '@/components/EmojiReactionPicker';

<EmojiReactionPicker
  currentReaction="❤️"
  count={234}
  onReactionChange={(emoji) => console.log('Selected:', emoji)}
/>
```

**Props:**
- `currentReaction`: string | undefined - Reaction hiện tại
- `count`: number - Số lượng reactions
- `onReactionChange`: (emoji: string | null) => void - Callback khi thay đổi

**Features:**
- Hover để hiện picker
- Click để chọn/bỏ chọn reaction
- Staggered animation khi hiện picker (delay 0.05s mỗi emoji)
- 6 reactions: ❤️ 😂 😮 😢 😡 👍

## Usage in PostCard

```tsx
import { useState } from 'react';
import { ReactionButton } from '@/components/ReactionButton';

const [isLiked, setIsLiked] = useState(false);
const [likeCount, setLikeCount] = useState(42);

const handleLike = () => {
  setIsLiked(!isLiked);
  setLikeCount(prev => isLiked ? prev - 1 : prev + 1);
};

<ReactionButton
  emoji={isLiked ? "❤️" : "🤍"}
  count={likeCount}
  isActive={isLiked}
  onClick={handleLike}
  className={isLiked ? "text-red-500" : "text-muted-foreground"}
/>
```

## Animation Details

### Particle Burst
```typescript
const particles = Array(8).fill(0).map(() => ({
  x: (Math.random() - 0.5) * 100,  // Random x: -50 to 50
  y: -Math.random() * 80 - 20,      // Random y: -20 to -100 (upward)
  rotation: (Math.random() - 0.5) * 360,
}));

<motion.span
  initial={{ opacity: 1, scale: 1, x: 0, y: 0, rotate: 0 }}
  animate={{
    opacity: 0,
    scale: 0,
    x: particle.x,
    y: particle.y,
    rotate: particle.rotation,
  }}
  transition={{ duration: 0.8, ease: 'easeOut' }}
>
  {emoji}
</motion.span>
```

### Spring Scale
```typescript
<motion.div
  whileHover={{ scale: 1.05 }}
  whileTap={{ scale: 0.95 }}
>
  <Button>...</Button>
</motion.div>
```

### Wiggle Animation
```typescript
<motion.span
  animate={
    isActive
      ? {
          scale: [1, 1.4, 1],
          rotate: [0, -15, 15, 0],
        }
      : {}
  }
  transition={{ duration: 0.4 }}
>
  {emoji}
</motion.span>
```

### Staggered Animation
```typescript
{reactions.map((reaction, index) => (
  <motion.button
    key={reaction.emoji}
    initial={{ opacity: 0, scale: 0 }}
    animate={{ opacity: 1, scale: 1 }}
    transition={{ delay: index * 0.05 }}
  >
    {reaction.emoji}
  </motion.button>
))}
```

## Demo

Truy cập `/reaction-demo` để xem demo đầy đủ các animation.

## Dependencies

```json
{
  "framer-motion": "^11.x.x"
}
```

## Performance Notes

- Particles tự động cleanup sau 800ms
- AnimatePresence đảm bảo smooth exit animation
- Sử dụng `pointer-events-none` cho particles để tránh block interactions
- Stagger delay nhỏ (0.05s) để animation mượt mà

## Customization

### Thay đổi số lượng particles
```typescript
const particles = Array(12).fill(0).map(...) // Tăng từ 8 lên 12
```

### Thay đổi particle trajectory
```typescript
x: (Math.random() - 0.5) * 150,  // Tăng spread
y: -Math.random() * 120 - 30,    // Bay cao hơn
```

### Thay đổi animation duration
```typescript
transition={{ duration: 1.2, ease: 'easeOut' }} // Chậm hơn
```

### Thêm reactions mới
```typescript
const reactions: Reaction[] = [
  { emoji: '🔥', label: 'Fire', color: 'text-orange-500' },
  { emoji: '💯', label: 'Perfect', color: 'text-purple-500' },
  // ...
];
```
