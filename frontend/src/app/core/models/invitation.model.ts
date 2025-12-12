export interface Invitation {
  id: number;
  fromUserId: number;
  fromUserName: string;
  toUserId: number;
  toUserName: string;
  status: InvitationStatus;
  createdAt: string;
  respondedAt?: string;
  gameId?: number;
}

export enum InvitationStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  DECLINED = 'DECLINED',
  EXPIRED = 'EXPIRED'
}

export interface InvitationRequest {
  toUserId: number;
}



