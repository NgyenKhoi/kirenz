import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "@/components/Header";
import { ChatWindow, ConversationList, NewConversationDialog } from "@/components/chat";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { MessageSquarePlus, Loader2 } from "lucide-react";
import { useChatStore } from "@/stores/chatStore";
import { useAuthStore } from "@/stores/authStore";
import { useConversations, useCreateConversation } from "@/hooks/queries/useChatQueries";

const Chat = () => {
  const navigate = useNavigate();
  const { userId, accessToken } = useAuthStore();
  const { 
    activeConversation, 
    onlineUsers,
    setActiveConversation,
    setConversations,
    clearChatData
  } = useChatStore();
  
  // Use React Query for conversations
  const { data: conversations = [], isLoading: isLoadingConversations, refetch: refetchConversations } = useConversations();
  const createConversationMutation = useCreateConversation();

  // Sync conversations to store when they change
  useEffect(() => {
    if (conversations.length > 0) {
      setConversations(conversations);
    }
  }, [conversations, setConversations]);
  
  const [showNewConversationDialog, setShowNewConversationDialog] = useState(false);

  useEffect(() => {
    if (!userId || !accessToken) {
      navigate('/login');
      return;
    }

    // Just refetch conversations when entering chat page
    refetchConversations();

    return () => {
      // Don't disconnect WebSocket here - it's managed at App level
      // Just clear local chat data
      clearChatData();
    };
  }, [userId, accessToken, navigate, clearChatData, refetchConversations]);

  const handleSelectConversation = (conversationId: string) => {
    setActiveConversation(conversationId);
  };

  const handleNewConversation = () => {
    setShowNewConversationDialog(true);
  };

  const handleSelectUser = async (selectedUserId: number) => {
    if (!userId) return;
    
    try {
      const response = await createConversationMutation.mutateAsync({
        type: 'DIRECT',
        participantIds: [userId, selectedUserId],
      });
      
      if (response.result) {
        setActiveConversation(response.result.id);
        setShowNewConversationDialog(false);
      }
    } catch (error) {
      console.error('Failed to create conversation:', error);
    }
  };

  const selectedConversation = conversations.find(c => c.id === activeConversation);

  return (
    <div className="min-h-screen bg-background">
      <Header />
      
      <main className="container mx-auto px-4 py-6">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 h-[calc(100vh-200px)]">
          <div className={`lg:col-span-1 ${selectedConversation ? 'hidden lg:block' : 'block'}`}>
            <Card className="h-full flex flex-col">
              <div className="p-4 border-b border-border flex items-center justify-between">
                <h2 className="font-semibold text-lg">Messages</h2>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={handleNewConversation}
                >
                  <MessageSquarePlus className="h-5 w-5" />
                </Button>
              </div>
              
              <div className="flex-1 overflow-y-auto p-4">
                <ConversationList
                  conversations={conversations}
                  activeConversationId={activeConversation}
                  currentUserId={userId || 0}
                  onlineUsers={onlineUsers}
                  onSelectConversation={handleSelectConversation}
                  isLoading={isLoadingConversations}
                />
              </div>
            </Card>
          </div>

          <div className={`lg:col-span-2 ${!selectedConversation ? 'hidden lg:block' : 'block'}`}>
            {selectedConversation ? (
              <ChatWindow
                conversation={selectedConversation}
                currentUserId={userId || 0}
                onBack={() => setActiveConversation(null)}
              />
            ) : (
              <Card className="h-full flex items-center justify-center">
                <div className="text-center space-y-4 p-8">
                  <MessageSquarePlus className="h-16 w-16 mx-auto text-muted-foreground" />
                  <div>
                    <h3 className="font-semibold text-lg mb-2">No conversation selected</h3>
                    <p className="text-muted-foreground">
                      Select a conversation from the list or start a new one
                    </p>
                  </div>
                  <Button onClick={handleNewConversation}>
                    Start New Conversation
                  </Button>
                </div>
              </Card>
            )}
          </div>
        </div>
      </main>

      <NewConversationDialog
        open={showNewConversationDialog}
        onOpenChange={setShowNewConversationDialog}
        onSelectUser={handleSelectUser}
        currentUserId={userId || 0}
      />

      {createConversationMutation.isPending && (
        <div className="fixed inset-0 bg-background/80 backdrop-blur-sm z-50 flex items-center justify-center">
          <Card className="p-6">
            <div className="flex items-center space-x-4">
              <Loader2 className="h-6 w-6 animate-spin" />
              <p>Creating conversation...</p>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
};

export default Chat;
