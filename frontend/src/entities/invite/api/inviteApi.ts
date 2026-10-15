import { apiClient } from '@/shared/api/base';
import { ApiResponse, CourseInvite, CreateCourseInvitePayload, RedeemInviteResult } from '@/shared/types';

export const inviteApi = {
  getInviteByCode: async (code: string): Promise<CourseInvite> => {
    const res = await apiClient.get<ApiResponse<CourseInvite>>(`/v1/invites/${code}`);
    return res.data.data;
  },

  redeemInvite: async (code: string): Promise<RedeemInviteResult> => {
    const res = await apiClient.post<ApiResponse<RedeemInviteResult>>(`/v1/invites/${code}/redeem`);
    return res.data.data;
  },

  getAdminInvites: async (courseId?: number): Promise<CourseInvite[]> => {
    const query = courseId ? `?courseId=${courseId}` : '';
    const res = await apiClient.get<ApiResponse<CourseInvite[]>>(`/v1/admin/invites${query}`);
    return res.data.data;
  },

  createInvite: async (payload: CreateCourseInvitePayload): Promise<CourseInvite> => {
    const res = await apiClient.post<ApiResponse<CourseInvite>>('/v1/admin/invites', payload);
    return res.data.data;
  },

  deleteInvite: async (id: number): Promise<void> => {
    await apiClient.delete<ApiResponse<void>>(`/v1/admin/invites/${id}`);
  },
};
