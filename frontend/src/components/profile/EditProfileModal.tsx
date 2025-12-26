import React, { useState } from 'react';
import { ProfileResponse, UpdateProfileRequest } from '../../types/dto';
import { useUpdateProfile } from '../../hooks/queries/useProfileQueries';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Textarea } from '../ui/textarea';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '../ui/dialog';
import { toast } from 'sonner';

interface EditProfileModalProps {
  profile: ProfileResponse;
  onProfileUpdate: (updatedProfile: ProfileResponse) => void;
  trigger?: React.ReactNode;
}

const EditProfileModal: React.FC<EditProfileModalProps> = ({ 
  profile, 
  onProfileUpdate, 
  trigger 
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const updateProfileMutation = useUpdateProfile();
  
  const [formData, setFormData] = useState<UpdateProfileRequest>({
    fullName: profile.fullName || '',
    bio: profile.bio || '',
    location: profile.location || '',
    website: profile.website || '',
    dateOfBirth: profile.dateOfBirth || ''
  });

  const handleSave = async () => {
    try {
      await updateProfileMutation.mutateAsync(formData);
      onProfileUpdate(profile); // The query cache will be updated automatically
      setIsOpen(false);
      toast.success('Profile updated successfully');
    } catch (error) {
      toast.error('Failed to update profile');
    }
  };

  const handleCancel = () => {
    setFormData({
      fullName: profile.fullName || '',
      bio: profile.bio || '',
      location: profile.location || '',
      website: profile.website || '',
      dateOfBirth: profile.dateOfBirth || ''
    });
    setIsOpen(false);
  };

  const handleInputChange = (field: keyof UpdateProfileRequest, value: string) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  return (
    <Dialog open={isOpen} onOpenChange={setIsOpen}>
      <DialogTrigger asChild>
        {trigger || <Button>Edit Profile</Button>}
      </DialogTrigger>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>Edit Profile</DialogTitle>
        </DialogHeader>
        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label htmlFor="fullName">Full Name</Label>
            <Input
              id="fullName"
              value={formData.fullName}
              onChange={(e) => handleInputChange('fullName', e.target.value)}
              placeholder="Enter your full name"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="bio">Bio</Label>
            <Textarea
              id="bio"
              value={formData.bio}
              onChange={(e) => handleInputChange('bio', e.target.value)}
              placeholder="Tell us about yourself"
              rows={3}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="location">Location</Label>
            <Input
              id="location"
              value={formData.location}
              onChange={(e) => handleInputChange('location', e.target.value)}
              placeholder="Where are you located?"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="website">Website</Label>
            <Input
              id="website"
              type="url"
              value={formData.website}
              onChange={(e) => handleInputChange('website', e.target.value)}
              placeholder="https://your-website.com"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="dateOfBirth">Date of Birth</Label>
            <Input
              id="dateOfBirth"
              type="date"
              value={formData.dateOfBirth}
              onChange={(e) => handleInputChange('dateOfBirth', e.target.value)}
            />
          </div>
        </div>
        
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={handleCancel} disabled={updateProfileMutation.isPending}>
            Cancel
          </Button>
          <Button onClick={handleSave} disabled={updateProfileMutation.isPending}>
            {updateProfileMutation.isPending ? 'Saving...' : 'Save Changes'}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};

export default EditProfileModal;