package com.dudu.dto;

public class InHouseTeamMemberDTO {
    
    private int id; // 내전 팀 멤버 고유 ID
    private int inHouseId; // 내전 ID
    private int teamId; // 내전 팀 ID
    private long userId; // 참여자 Discord ID

    // getter / setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getInHouseId() { return inHouseId; }
    public void setInHouseId(int inHouseId) { this.inHouseId = inHouseId; }

    public int getTeamId() { return teamId; }
    public void setTeamId(int teamId) { this.teamId = teamId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
}
