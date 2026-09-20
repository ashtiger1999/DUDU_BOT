package com.dudu.dto;

import java.time.LocalDateTime;

public class MemberDTO {

    private int id; // 파티원 고유 ID
    private long discordId; // 파티원 Discord ID
    private String gameNickname; // 소환사명
    private int kWinCount; // 칼바람 승 수
    private int kLoseCount; // 칼바람 패 수
    private int hWinCount; // 협곡 승 수
    private int hLoseCount; // 협곡 패 수
    private LocalDateTime joinedAt; // 가입 시간

    // getter / setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getDiscordId() { return discordId; }
    public void setDiscordId(long discordId) { this.discordId = discordId; }

    public String getGameNickname() { return gameNickname;}
    public void setGameNickname(String gameNickname) { this.gameNickname = gameNickname;}

    public int getKWinCount() { return kWinCount; }
    public void setKWinCount(int kWinCount) { this.kWinCount = kWinCount; } 

    public int getKLoseCount() { return kLoseCount; }
    public void setKLoseCount(int kLoseCount) { this.kLoseCount = kLoseCount; }

    public int getHWinCount() { return hWinCount; }
    public void setHWinCount(int hWinCount) { this.hWinCount = hWinCount; }

    public int getHLoseCount() { return hLoseCount; }
    public void setHLoseCount(int hLoseCount) { this.hLoseCount = hLoseCount; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}