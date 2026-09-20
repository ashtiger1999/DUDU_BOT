package com.dudu.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PartyDTO {

    private int id; // 파티 고유 ID
    private long ownerId; // 파티장 Discord ID
    private String messageId; // Discord 메시지 ID
    private LocalDateTime createdAt; // 생성 시간
    private LocalDateTime deletedAt; // 삭제 시간 (기본 : 현재 시간 + 6시간)
    private int gameType; // 0 = 일반, 1 = 아레나, 2 = 내전
    private int gameMode; // 1 = 일반, 2 = 랭크
    private String description; // 파티 설명

    private List<MemberDTO> members = new ArrayList<>(); // 파티원 목록

    // getter/setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getOwnerId() { return ownerId; }
    public void setOwnerId(long ownerId) { this.ownerId = ownerId; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public int getGameType() { return gameType; }
    public void setGameType(int gameType) { this.gameType = gameType; }

    public int getGameMode() { return gameMode; }
    public void setGameMode(int gameMode) { this.gameMode = gameMode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
