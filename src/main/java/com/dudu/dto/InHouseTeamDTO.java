package com.dudu.dto;

import java.util.ArrayList;
import java.util.List;

public class InHouseTeamDTO {

    private int id; // 내전 팀 고유 ID
    private int inHouseId; // 내전 고유 ID
    private String teamName; // 팀 이름

    private List<MemberDTO> members = new ArrayList<>(); // 팀원 목록

    // getter / setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInHouseId() {
        return inHouseId;
    }

    public void setInHouseId(int inHouseId) {
        this.inHouseId = inHouseId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public List<MemberDTO> getMembers() {
        return members;
    }

    public void setMembers(List<MemberDTO> members) {
        this.members = members;
    }
}