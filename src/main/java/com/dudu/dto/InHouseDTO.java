package com.dudu.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InHouseDTO {
    
    private int id; // 내전 고유 ID
    private long ownerId; // 내전장 Discord ID
    private LocalDateTime createdAt; // 생성 시간
    private LocalDateTime startedAt; // 시작 시간
    private LocalDateTime deletedAt; // 삭제 시간 (기본 : 현재 시간 +6시간)
    private int gameType; // 0 = 일반, 1 = 아레나, 2 = 내전
    private int maxPlayers; // 최대 참여자 수
    private int teamCount; // 내전 팀 수
    private int status; // 내전 상태 (0 = 모집중, 1 = 모집마감, 2 = 시작, 3 = 종료)
    private Long teamMessageId; // 팀 정보 메시지 ID (팀 정보 갱신 시 필요)
    private Long matchMessageId; // 경기 정보 메시지 ID (경기 정보 갱신 시 필요)

    private List<InHouseTeamDTO> teams = new ArrayList<>(); // 내전 팀 목록
    private List<InHouseMatchDTO> matches = new ArrayList<>(); // 내전 매치 목록

    // getter / setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getOwnerId() { return ownerId; }
    public void setOwnerId(long ownerId) { this.ownerId = ownerId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public int getGameType() { return gameType; }
    public void setGameType(int gameType) { this.gameType = gameType; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public int getTeamCount() { return teamCount; }
    public void setTeamCount(int teamCount) { this.teamCount = teamCount; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public Long getTeamMessageId() { return teamMessageId; }
    public void setTeamMessageId(Long teamMessageId) { this.teamMessageId = teamMessageId; }

    public Long getMatchMessageId() { return matchMessageId; }
    public void setMatchMessageId(Long matchMessageId) { this.matchMessageId = matchMessageId; }

    public List<InHouseTeamDTO> getTeams() { return teams; }
    public void setTeams(List<InHouseTeamDTO> teams) { this.teams = teams; }

    public List<InHouseMatchDTO> getMatches() { return matches; }
    public void setMatches(List<InHouseMatchDTO> matches) { this.matches = matches; }
}
