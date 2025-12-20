// User queries
export {
  useUsers,
  useUser,
  useUserProfile,
  useUpdateProfile,
  userKeys,
} from './useUserQueries';

// Post queries
export {
  useUserPosts,
  usePost,
  useCreatePost,
  useUpdatePost,
  useDeletePost,
  postKeys,
} from './usePostQueries';

// Comment queries
export {
  usePostComments,
  useComment,
  useCreateComment,
  useUpdateComment,
  useDeleteComment,
  commentKeys,
} from './useCommentQueries';

// Chat queries
export {
  useConversations,
  useMessages,
  useCreateConversation,
  useMarkAsRead,
  useOnlineUsers,
  useAllUserPresence,
  chatKeys,
} from './useChatQueries';
