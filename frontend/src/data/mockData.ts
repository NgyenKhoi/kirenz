import { User, Post, Comment } from "@/types";

export const mockUsers: User[] = [
  {
    id: "1",
    username: "sarah_creative",
    displayName: "Sarah Johnson",
    avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Sarah",
    bio: "Digital artist & designer 🎨 Creating beautiful things",
  },
  {
    id: "2",
    username: "mike_tech",
    displayName: "Mike Chen",
    avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Mike",
    bio: "Software engineer | Coffee enthusiast ☕",
  },
  {
    id: "3",
    username: "emma_photos",
    displayName: "Emma Wilson",
    avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Emma",
    bio: "Travel photographer 📸 Capturing moments around the world",
  },
];

export const mockPosts: Post[] = [
  {
    id: "1",
    userId: "1",
    content: "Just finished working on a new design project! Really excited about how the color palette turned out. The combination of warm and cool tones creates such a welcoming vibe. 🎨✨",
    createdAt: new Date("2024-01-15T10:30:00"),
    likes: 24,
    commentsCount: 5,
  },
  {
    id: "2",
    userId: "2",
    content: "Learning React hooks has been a game changer for my development workflow. The way useState and useEffect work together is just beautiful. Anyone else feel the same way?",
    createdAt: new Date("2024-01-15T09:15:00"),
    likes: 18,
    commentsCount: 8,
  },
  {
    id: "3",
    userId: "3",
    content: "Caught the most amazing sunset yesterday at the beach. Sometimes you just need to stop and appreciate the beauty around you. 🌅",
    createdAt: new Date("2024-01-14T18:45:00"),
    likes: 42,
    commentsCount: 3,
  },
  {
    id: "4",
    userId: "1",
    content: "Tips for aspiring designers: 1) Master the fundamentals 2) Study great design 3) Practice daily 4) Accept feedback gracefully 5) Never stop learning!",
    createdAt: new Date("2024-01-14T14:20:00"),
    likes: 31,
    commentsCount: 12,
  },
];

export const mockComments: Comment[] = [
  {
    id: "1",
    postId: "1",
    userId: "2",
    content: "This looks amazing! Would love to see the final result.",
    createdAt: new Date("2024-01-15T11:00:00"),
  },
  {
    id: "2",
    postId: "1",
    userId: "3",
    content: "The color theory behind this must be fascinating!",
    createdAt: new Date("2024-01-15T11:30:00"),
  },
  {
    id: "3",
    postId: "2",
    userId: "1",
    content: "Totally agree! Hooks made functional components so much more powerful.",
    createdAt: new Date("2024-01-15T09:45:00"),
  },
  {
    id: "4",
    postId: "2",
    userId: "3",
    content: "Have you tried useReducer yet? It's great for complex state management.",
    createdAt: new Date("2024-01-15T10:00:00"),
  },
];

export const getCurrentUser = (): User => mockUsers[0];

export const getUserById = (userId: string): User | undefined => {
  return mockUsers.find((user) => user.id === userId);
};

export const getPostById = (postId: string): Post | undefined => {
  return mockPosts.find((post) => post.id === postId);
};

export const getCommentsByPostId = (postId: string): Comment[] => {
  return mockComments.filter((comment) => comment.postId === postId);
};

export const getPostsByUserId = (userId: string): Post[] => {
  return mockPosts.filter((post) => post.userId === userId);
};
