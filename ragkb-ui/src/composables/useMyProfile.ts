import { computed } from 'vue'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { usersService } from '@/services'
import { useAuthStore } from '@/stores/auth'
import type { ChangeOwnPasswordRequest, UpdateOwnProfileRequest, User } from '@/types'

export const MY_PROFILE_KEY = ['my-profile']

export function useMyProfile() {
  const auth = useAuthStore()
  const queryClient = useQueryClient()
  const query = useQuery({
    queryKey: MY_PROFILE_KEY,
    queryFn: usersService.me,
    enabled: computed(() => auth.isAuthenticated),
    staleTime: 0,
    refetchOnMount: 'always',
  })

  async function updateProfile(payload: UpdateOwnProfileRequest) {
    const profile = await usersService.updateMe(payload)
    queryClient.setQueryData<User>(MY_PROFILE_KEY, profile)
    return profile
  }

  async function changePassword(payload: ChangeOwnPasswordRequest) {
    await usersService.changeMyPassword(payload)
  }

  async function uploadAvatar(file: File) {
    const profile = await usersService.uploadMyAvatar(file)
    queryClient.setQueryData<User>(MY_PROFILE_KEY, profile)
    return profile
  }

  async function removeAvatar() {
    const profile = await usersService.removeMyAvatar()
    queryClient.setQueryData<User>(MY_PROFILE_KEY, profile)
    return profile
  }

  return {
    profile: query.data,
    isLoading: query.isLoading,
    isError: query.isError,
    refetch: query.refetch,
    updateProfile,
    changePassword,
    uploadAvatar,
    removeAvatar,
  }
}
