package com.dudu.dto;

import java.time.LocalDateTime;

public class JoinDTO {

    private int Id; // 참여정보 고유 ID
    private int partyId; // 파티 고유 ID
    private long discordId; // 참여자 Discord ID
    private LocalDateTime joinedAt; // 참여 시간

    // getter / setter
    public int getId() { return Id; }
    public void setId(int id) { Id = id; }

    public int getPartyId() { return partyId; }
    public void setPartyId(int partyId) { this.partyId = partyId; }

    public long getDiscordId() { return discordId; }
    public void setDiscordId(long discordId) { this.discordId = discordId; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}
