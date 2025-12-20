import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Button } from '@/components/ui/button';

interface ReactionButtonProps {
  emoji: string;
  count: number;
  isActive?: boolean;
  onClick?: () => void;
  variant?: 'ghost' | 'default' | 'outline';
  size?: 'sm' | 'default' | 'lg';
  className?: string;
}

interface Particle {
  id: number;
  x: number;
  y: number;
  rotation: number;
}

export const ReactionButton = ({
  emoji,
  count,
  isActive = false,
  onClick,
  variant = 'ghost',
  size = 'sm',
  className = '',
}: ReactionButtonProps) => {
  const [particles, setParticles] = useState<Particle[]>([]);
  const [showBurst, setShowBurst] = useState(false);

  const handleClick = () => {
    if (!isActive) {
      const newParticles = Array(8)
        .fill(0)
        .map((_, i) => ({
          id: Date.now() + i,
          x: (Math.random() - 0.5) * 100,
          y: -Math.random() * 80 - 20,
          rotation: (Math.random() - 0.5) * 360,
        }));

      setParticles(newParticles);
      setShowBurst(true);

      setTimeout(() => {
        setShowBurst(false);
        setParticles([]);
      }, 800);
    }

    onClick?.();
  };

  return (
    <div className="relative inline-block">
      <motion.div
        whileHover={{ scale: 1.05 }}
        whileTap={{ scale: 0.95 }}
      >
        <Button
          variant={variant}
          size={size}
          onClick={handleClick}
          className={`group space-x-2 ${className}`}
        >
          <motion.span
            className="text-lg"
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
          <span className="text-sm font-medium">{count}</span>
        </Button>
      </motion.div>

      <AnimatePresence>
        {showBurst &&
          particles.map((particle) => (
            <motion.span
              key={particle.id}
              className="absolute text-lg pointer-events-none"
              style={{
                left: '50%',
                top: '50%',
                transform: 'translate(-50%, -50%)',
              }}
              initial={{ opacity: 1, scale: 1, x: 0, y: 0, rotate: 0 }}
              animate={{
                opacity: 0,
                scale: 0,
                x: particle.x,
                y: particle.y,
                rotate: particle.rotation,
              }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.8, ease: 'easeOut' }}
            >
              {emoji}
            </motion.span>
          ))}
      </AnimatePresence>
    </div>
  );
};
