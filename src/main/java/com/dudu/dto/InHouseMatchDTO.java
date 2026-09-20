package com.dudu.dto;

public class InHouseMatchDTO {
    
    private int id; // 내전 경기 고유 ID
    private int inHouseId; // 내전 ID
    private int roundNumber; // 경기 라운드
    private int team1Id; // 팀 1 ID
    private int team2Id; // 팀 2 ID
    private int team1Score; // 팀 1 점수
    private int team2Score; // 팀 2 점수
    private int winnerTeamId; // 승리 팀 ID (0 = 무승부, team1Id = 팀 1 승리, team2Id = 팀 2 승리)

    // getter / setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getInHouseId() { return inHouseId; }
    public void setInHouseId(int inHouseId) { this.inHouseId = inHouseId; }

    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }

    public int getTeam1Id() { return team1Id; }
    public void setTeam1Id(int team1Id) { this.team1Id = team1Id; }

    public int getTeam2Id() { return team2Id; }
    public void setTeam2Id(int team2Id) { this.team2Id = team2Id; }

    public int getTeam1Score() { return team1Score; }
    public void setTeam1Score(int team1Score) { this.team1Score = team1Score; }

    public int getTeam2Score() { return team2Score; }
    public void setTeam2Score(int team2Score) { this.team2Score = team2Score; }

    public int getWinnerTeamId() { return winnerTeamId; }
    public void setWinnerTeamId(int winnerTeamId) { this.winnerTeamId = winnerTeamId; }
}
