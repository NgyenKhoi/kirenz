import { useState } from 'react';
import { Card } from '@/components/ui/card';
import { ReactionButton } from '@/components/ReactionButton';
import { EmojiReactionPicker } from '@/components/EmojiReactionPicker';

const ReactionDemo = () => {
  const [likeCount, setLikeCount] = useState(42);
  const [isLiked, setIsLiked] = useState(false);

  const [loveCount, setLoveCount] = useState(128);
  const [isLoved, setIsLoved] = useState(false);

  const [hahaCount, setHahaCount] = useState(56);
  const [isHaha, setIsHaha] = useState(false);

  const [pickerCount, setPickerCount] = useState(234);
  const [currentReaction, setCurrentReaction] = useState<string | null>(null);

  const handleLike = () => {
    setIsLiked(!isLiked);
    setLikeCount((prev) => (isLiked ? prev - 1 : prev + 1));
  };

  const handleLove = () => {
    setIsLoved(!isLoved);
    setLoveCount((prev) => (isLoved ? prev - 1 : prev + 1));
  };

  const handleHaha = () => {
    setIsHaha(!isHaha);
    setHahaCount((prev) => (isHaha ? prev - 1 : prev + 1));
  };

  const handleReactionChange = (emoji: string | null) => {
    const wasReacted = currentReaction !== null;
    const isReacted = emoji !== null;

    if (wasReacted && !isReacted) {
      setPickerCount((prev) => prev - 1);
    } else if (!wasReacted && isReacted) {
      setPickerCount((prev) => prev + 1);
    }

    setCurrentReaction(emoji);
  };

  return (
    <div className="container mx-auto py-8 px-4 max-w-4xl">
      <h1 className="text-3xl font-bold mb-8">Reaction Animation Demo</h1>

      <div className="space-y-8">
        <Card className="p-8">
          <h2 className="text-xl font-semibold mb-4">1. Basic Reaction Button</h2>
          <p className="text-muted-foreground mb-6">
            Click để xem particle burst animation và wiggle effect
          </p>
          <div className="flex gap-4">
            <ReactionButton
              emoji={isLiked ? '👍' : '👍🏻'}
              count={likeCount}
              isActive={isLiked}
              onClick={handleLike}
              className={isLiked ? 'text-blue-500' : 'text-muted-foreground'}
            />
            <ReactionButton
              emoji={isLoved ? '❤️' : '🤍'}
              count={loveCount}
              isActive={isLoved}
              onClick={handleLove}
              className={isLoved ? 'text-red-500' : 'text-muted-foreground'}
            />
            <ReactionButton
              emoji={isHaha ? '😂' : '😄'}
              count={hahaCount}
              isActive={isHaha}
              onClick={handleHaha}
              className={isHaha ? 'text-yellow-500' : 'text-muted-foreground'}
            />
          </div>
        </Card>

        <Card className="p-8">
          <h2 className="text-xl font-semibold mb-4">2. Emoji Reaction Picker</h2>
          <p className="text-muted-foreground mb-6">
            Hover để hiện picker, click để chọn reaction (giống Facebook)
          </p>
          <EmojiReactionPicker
            currentReaction={currentReaction}
            count={pickerCount}
            onReactionChange={handleReactionChange}
          />
        </Card>

        <Card className="p-8">
          <h2 className="text-xl font-semibold mb-4">3. Animation Features</h2>
          <div className="space-y-4 text-sm">
            <div>
              <h3 className="font-semibold mb-2">✨ Particle Burst/Explosion</h3>
              <p className="text-muted-foreground">
                8 emoji particles bay tung ra khi click, mỗi particle có random direction và rotation
              </p>
            </div>
            <div>
              <h3 className="font-semibold mb-2">🎯 Spring Scale Animation</h3>
              <p className="text-muted-foreground">
                Hover để scale 1.05x, tap để scale 0.95x với physics đàn hồi
              </p>
            </div>
            <div>
              <h3 className="font-semibold mb-2">🎪 Wiggle/Shake Animation</h3>
              <p className="text-muted-foreground">
                Khi active, emoji scale lên 1.4x và xoay lắc -15° → 15° → 0°
              </p>
            </div>
            <div>
              <h3 className="font-semibold mb-2">🎨 Staggered Animation</h3>
              <p className="text-muted-foreground">
                Emoji picker hiện lên với stagger effect (mỗi emoji delay 0.05s)
              </p>
            </div>
          </div>
        </Card>

        <Card className="p-8 bg-muted/50">
          <h2 className="text-xl font-semibold mb-4">💡 Usage Tips</h2>
          <ul className="space-y-2 text-sm text-muted-foreground">
            <li>• Click vào reaction button để toggle và xem particle burst</li>
            <li>• Hover vào emoji picker để xem menu reactions</li>
            <li>• Mỗi lần click sẽ tạo 8 particles bay ra với random trajectory</li>
            <li>• Animation sử dụng Framer Motion với spring physics</li>
            <li>• Particles tự động cleanup sau 800ms</li>
          </ul>
        </Card>
      </div>
    </div>
  );
};

export default ReactionDemo;
