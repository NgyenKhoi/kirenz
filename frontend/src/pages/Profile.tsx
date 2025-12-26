import React from 'react';
import { useParams } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { useOwnProfile, useUserProfile } from '../hooks/queries/useProfileQueries';
import { useUserPosts } from '../hooks/queries/usePostQueries';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader } from '../components/ui/card';
import { Avatar, AvatarFallback, AvatarImage } from '../components/ui/avatar';
import { Badge } from '../components/ui/badge';
import { CalendarDays, MapPin, Globe, Edit, Users, Heart } from 'lucide-react';
import EditProfileModal from '../components/profile/EditProfileModal';

const Profile: React.FC = () => {
  const { id } = useParams<{ id?: string }>();
  const { userId, email } = useAuthStore();
  
  const isOwnProfile = !id || (userId && id === userId.toString());
  
  // Queries
  const { data: ownProfile, isLoading: isLoadingOwnProfile } = useOwnProfile();
  const { data: userProfile, isLoading: isLoadingUserProfile } = useUserProfile(id!, { enabled: !isOwnProfile });
  const { data: posts, isLoading: isLoadingPosts } = useUserPosts(
    isOwnProfile ? userId! : parseInt(id!)
  );

  // Determine which data to use
  const profile = isOwnProfile ? ownProfile : userProfile?.profile;
  const user = isOwnProfile ? { id: userId, email } : userProfile;
  const isLoading = isOwnProfile ? isLoadingOwnProfile : isLoadingUserProfile;

  const getDisplayName = () => {
    return profile?.fullName || user?.email?.split('@')[0] || 'User';
  };

  const getInitials = () => {
    const name = getDisplayName();
    return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  };

  const handleProfileUpdate = (updatedProfile: any) => {
    // The query will automatically update due to cache invalidation
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      {/* Profile Header */}
      <Card className="mb-6">
        <CardContent className="pt-6">
          <div className="flex flex-col md:flex-row items-start md:items-center gap-6">
            {/* Avatar */}
            <Avatar className="h-32 w-32 ring-4 ring-background">
              {profile?.avatarUrl && (
                <AvatarImage src={profile.avatarUrl} alt={getDisplayName()} />
              )}
              <AvatarFallback className="text-2xl font-semibold">
                {getInitials()}
              </AvatarFallback>
            </Avatar>

            {/* Profile Info */}
            <div className="flex-1 space-y-4">
              <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                <div>
                  <h1 className="text-3xl font-bold">{getDisplayName()}</h1>
                  <p className="text-muted-foreground">{user?.email}</p>
                </div>
                
                {isOwnProfile && profile && (
                  <EditProfileModal 
                    profile={profile} 
                    onProfileUpdate={handleProfileUpdate}
                    trigger={
                      <Button variant="outline" className="flex items-center gap-2">
                        <Edit className="h-4 w-4" />
                        Edit Profile
                      </Button>
                    }
                  />
                )}
              </div>

              {/* Bio */}
              {profile?.bio && (
                <p className="text-lg">{profile.bio}</p>
              )}

              {/* Profile Details */}
              <div className="flex flex-wrap gap-4 text-sm text-muted-foreground">
                {profile?.location && (
                  <div className="flex items-center gap-1">
                    <MapPin className="h-4 w-4" />
                    {profile.location}
                  </div>
                )}
                
                {profile?.website && (
                  <div className="flex items-center gap-1">
                    <Globe className="h-4 w-4" />
                    <a 
                      href={profile.website} 
                      target="_blank" 
                      rel="noopener noreferrer"
                      className="text-blue-600 hover:underline"
                    >
                      {profile.website.replace(/^https?:\/\//, '')}
                    </a>
                  </div>
                )}
                
                {profile?.dateOfBirth && (
                  <div className="flex items-center gap-1">
                    <CalendarDays className="h-4 w-4" />
                    Born {new Date(profile.dateOfBirth).toLocaleDateString()}
                  </div>
                )}
                
                {profile?.createdAt && (
                  <div className="flex items-center gap-1">
                    <Users className="h-4 w-4" />
                    Joined {new Date(profile.createdAt).toLocaleDateString()}
                  </div>
                )}
              </div>

              {/* Stats */}
              <div className="flex gap-6">
                <div className="text-center">
                  <div className="text-2xl font-bold">{posts?.length || 0}</div>
                  <div className="text-sm text-muted-foreground">Posts</div>
                </div>
                <div className="text-center">
                  <div className="text-2xl font-bold">0</div>
                  <div className="text-sm text-muted-foreground">Following</div>
                </div>
                <div className="text-center">
                  <div className="text-2xl font-bold">0</div>
                  <div className="text-sm text-muted-foreground">Followers</div>
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Posts Section */}
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h2 className="text-2xl font-bold">
            {isOwnProfile ? 'Your Posts' : `${getDisplayName()}'s Posts`}
          </h2>
          <Badge variant="secondary">{posts?.length || 0} posts</Badge>
        </div>

        {isLoadingPosts ? (
          <div className="flex justify-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          </div>
        ) : posts && posts.length > 0 ? (
          <div className="grid gap-6">
            {posts.map((post) => (
              <Card key={post.id} className="hover:shadow-md transition-shadow">
                <CardHeader className="pb-3">
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-3">
                      <Avatar className="h-10 w-10">
                        {profile?.avatarUrl && (
                          <AvatarImage src={profile.avatarUrl} alt={getDisplayName()} />
                        )}
                        <AvatarFallback>{getInitials()}</AvatarFallback>
                      </Avatar>
                      <div>
                        <p className="font-semibold">{getDisplayName()}</p>
                        <p className="text-sm text-muted-foreground">
                          {new Date(post.createdAt).toLocaleDateString()}
                        </p>
                      </div>
                    </div>
                  </div>
                </CardHeader>
                <CardContent className="pt-0">
                  <p className="mb-4">{post.content}</p>
                  
                  {/* Post Media */}
                  {post.media && post.media.length > 0 && (
                    <div className="grid grid-cols-2 gap-2 mb-4">
                      {post.media.slice(0, 4).map((media, index) => (
                        <div key={index} className="relative aspect-square rounded-lg overflow-hidden">
                          <img 
                            src={media.url} 
                            alt={`Post media ${index + 1}`}
                            className="w-full h-full object-cover"
                          />
                        </div>
                      ))}
                    </div>
                  )}
                  
                  {/* Post Actions */}
                  <div className="flex items-center gap-4 pt-2 border-t">
                    <Button variant="ghost" size="sm" className="flex items-center gap-2">
                      <Heart className="h-4 w-4" />
                      Like
                    </Button>
                    <Button variant="ghost" size="sm">
                      Comment
                    </Button>
                    <Button variant="ghost" size="sm">
                      Share
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        ) : (
          <Card>
            <CardContent className="py-12 text-center">
              <p className="text-muted-foreground">
                {isOwnProfile ? "You haven't posted anything yet." : `${getDisplayName()} hasn't posted anything yet.`}
              </p>
              {isOwnProfile && (
                <Button className="mt-4">Create your first post</Button>
              )}
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
};

export default Profile;