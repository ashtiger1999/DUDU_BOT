package com.dudu.util;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.Duration;

import com.dudu.dao.MemberDAO;
import com.dudu.dao.PartyDAO;
import com.dudu.dao.InHouseDAO;
import com.dudu.dao.InHouseMatchDAO;
import com.dudu.dto.MemberDTO;
import com.dudu.dto.InHouseDTO;
import com.dudu.dto.InHouseMatchDTO;
import com.dudu.dto.InHouseTeamDTO;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

// 임베드 빌더 유틸리티 클래스
public class PartyEmbedBuilder {

    // 비내전 파티 정보 임베드 생성
    public static EmbedBuilder buildDefaultEmbed(Guild guild, int partyId) {

        // ** 최신 참여자 명단 **
        List<MemberDTO> members = PartyDAO.getMembers(partyId);

        // ** 파티장 디스코드 ID**
        long ownerId = PartyDAO.getOwnerId(partyId);

        // ** 파티장 정보 **
        Member hostMember = guild.getMemberById(ownerId);

        String hostName = hostMember != null
                ? hostMember.getEffectiveName()
                : "(파티장 조회 실패)";

        // ** 참여자 명단 처리 **
        StringBuilder participantSb = new StringBuilder();

        // 최대 5명까지만 표시
        for (int i = 0; i < 5; i++) {

            if (i < members.size()) {

                MemberDTO member = members.get(i);

                Member discordMember = guild.getMemberById(member.getDiscordId());

                String discordNickname = discordMember != null
                        ? discordMember.getEffectiveName()
                        : "(멤버 조회 실패)";

                participantSb.append(i + 1)
                        .append(". ")
                        .append(discordNickname)
                        .append(" / ")
                        .append(member.getGameNickname())
                        .append("\n");

            } else {
                participantSb.append(i + 1).append(". \n");
            }
        }

        // ** 대기자 처리 **
        StringBuilder waitSb = new StringBuilder();

        // 대기자 명단은 5명 이후부터 표시
        if (members.size() > 5) {

            for (int i = 5; i < members.size(); i++) {

                MemberDTO member = members.get(i);

                Member discordMember = guild.getMemberById(member.getDiscordId());

                String discordNickname = discordMember != null
                        ? discordMember.getEffectiveName()
                        : "(멤버 조회 실패)";

                waitSb.append((i - 4))
                        .append(". ")
                        .append(discordNickname)
                        .append(" / ")
                        .append(member.getGameNickname())
                        .append("\n");
            }

        } else {
            waitSb.append("1. 비어있음");
        }

        // 파티 정보 조회
        int partyType = PartyDAO.getGameType(partyId); // 1: 증바람, 2: 협곡, 3: 내전
        String partyTypeLabel = "";
        switch (partyType) {
            case 1:
                partyTypeLabel = "아수라장";
                break;
            case 2:
                partyTypeLabel = "협곡";
                break;
            case 3:
                partyTypeLabel = "내전";
                break;
            default:
                partyTypeLabel = "알 수 없는 유형";
        }
        int gameMode = PartyDAO.getGameMode(partyId); // 1: 일반, 2: 랭크, 3: 이벤트
        String gameModeLabel = "";
        switch (gameMode) {
            case 1:
                gameModeLabel = "일반";
                break;
            case 2:
                gameModeLabel = "랭크";
                break;
            case 3:
                gameModeLabel = "내전";
                break;
            default:
                gameModeLabel = "알 수 없는 모드";
        }
        String description = PartyDAO.getDescription(partyId); // 파티 비고
        LocalDateTime deletedAt = PartyDAO.getDeletedAt(partyId); // 파티 삭제시간
        // 자동삭제까지 남은 시간을 1시간 이상일때는 시간단위만 표시, 이후 분단위만 표시
        String remainingTime = "";
        if (deletedAt != null) {
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
            long minutesRemaining = java.time.Duration.between(now, deletedAt).toMinutes();

            if (minutesRemaining >= 60) {
                long hours = minutesRemaining / 60;
                remainingTime = "약 " + hours + "시간";
            } else {
                remainingTime = "약 " + minutesRemaining + "분";
            }
        }

        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(hostName + "님의 " + partyTypeLabel + " 파티");
        embed.addField("👥 참여자 명단", participantSb.toString(), false);
        embed.addField("⏳ 대기자 명단", waitSb.toString(), false);
        embed.addField("게임 모드", gameModeLabel + " ( " + partyTypeLabel + " ) ", true);
        embed.addField("파티 마감", remainingTime, true);

        embed.addField(
                "📝 파티 비고",
                description != null && !description.isBlank()
                        ? description
                        : "없음",
                false);

        embed.setFooter("파티 ID: " + partyId);

        return embed;
    }

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 내전 정보 임베드 생성
    public static EmbedBuilder buildInHouseEmbed(Guild guild, int partyId) {

        // 참여자 목록
        List<MemberDTO> members = PartyDAO.getMembers(partyId);

        // 파티장 Discord ID
        long ownerId = PartyDAO.getOwnerId(partyId);

        // 파티장 정보
        Member hostMember = guild.getMemberById(ownerId);

        String hostName = hostMember != null
                ? hostMember.getEffectiveName()
                : "(파티장 조회 실패)";

        // 내전 정보
        int gameType = PartyDAO.getGameType(partyId);
        int status = InHouseDAO.getInHousePartyStatus(partyId);

        LocalDateTime startedAt = InHouseDAO.getStartedAt(partyId);
        LocalDateTime deletedAt = PartyDAO.getDeletedAt(partyId);
        String description = PartyDAO.getDescription(partyId);

        int maxPlayers = InHouseDAO.getInHouseMaxPlayers(partyId);
        int teamCount = InHouseDAO.getInHouseTeamCount(partyId);

        // 게임 종류
        String gameTypeLabel;

        switch (gameType) {
            case 1:
                gameTypeLabel = "아수라장";
                break;

            case 2:
                gameTypeLabel = "협곡";
                break;

            default:
                gameTypeLabel = "알 수 없는 게임";
                break;
        }

        // 상태
        String statusLabel;

        switch (status) {
            case 0:
                statusLabel = "모집중";
                break;

            case 1:
                statusLabel = "모집마감";
                break;

            case 2:
                statusLabel = "진행중";
                break;

            case 3:
                statusLabel = "종료됨";
                break;

            default:
                statusLabel = "알 수 없는 상태";
                break;
        }

        // 참여자 명단
        StringBuilder participantSb = new StringBuilder();

        for (int i = 0; i < maxPlayers; i++) {

            if (i < members.size()) {

                MemberDTO member = members.get(i);

                Member discordMember = guild.getMemberById(member.getDiscordId());

                String discordNickname = discordMember != null
                        ? discordMember.getEffectiveName()
                        : "(멤버 조회 실패)";

                // 게임 전적
                int winCount;
                int loseCount;
                double winRate;

                if (gameType == 1) {
                    winCount = MemberDAO.getKWinCount(member.getDiscordId());
                    loseCount = MemberDAO.getKLoseCount(member.getDiscordId());
                    // 승률
                    winRate = (winCount + loseCount) > 0
                            ? (double) winCount / (winCount + loseCount) * 100
                            : 0;
                } else {
                    winCount = MemberDAO.getHWinCount(member.getDiscordId());
                    loseCount = MemberDAO.getHLoseCount(member.getDiscordId());
                    // 승률
                    winRate = (winCount + loseCount) > 0
                            ? (double) winCount / (winCount + loseCount) * 100
                            : 0;
                }

                participantSb.append(i + 1)
                        .append(". ")
                        .append(discordNickname)
                        .append(" / ")
                        .append(member.getGameNickname())
                        .append(" (")
                        .append(winCount)
                        .append("승 ")
                        .append(loseCount)
                        .append("패 ")
                        .append(String.format("(승률: %.2f%%)", winRate))
                        .append(")")
                        .append("\n");

            } else {

                participantSb.append(i + 1)
                        .append(". \n");
            }
        }

        // 대기자 명단
        StringBuilder waitSb = new StringBuilder();

        if (members.size() > maxPlayers) {

            for (int i = maxPlayers; i < members.size(); i++) {

                MemberDTO member = members.get(i);

                Member discordMember = guild.getMemberById(member.getDiscordId());

                String discordNickname = discordMember != null
                        ? discordMember.getEffectiveName()
                        : "(멤버 조회 실패)";

                waitSb.append((i - maxPlayers + 1))
                        .append(". ")
                        .append(discordNickname)
                        .append(" / ")
                        .append(member.getGameNickname())
                        .append("\n");
            }

        } else {

            waitSb.append("없음");
        }

        // 시작 시간
        String startTime = startedAt != null
                ? startedAt.format(DATE_TIME_FORMAT)
                : "미정";

        // 마감까지 남은 시간
        String remainingTime = "";

        if (deletedAt != null) {

            LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

            long minutesRemaining = Duration.between(now, deletedAt).toMinutes();

            if (minutesRemaining >= 60) {

                long hours = minutesRemaining / 60;

                remainingTime = "약 " + hours + "시간";

            } else if (minutesRemaining > 0) {

                remainingTime = "약 " + minutesRemaining + "분";

            } else {

                remainingTime = "마감됨";
            }
        }

        // 팀 정보
        String teamInfo = teamCount + "팀 / 팀당 "
                + (maxPlayers / teamCount)
                + "명";

        // 임베드 생성
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(
                hostName + "님의 " + gameTypeLabel + " 내전");

        embed.addField(
                "📌 상태",
                statusLabel,
                true);

        embed.addField(
                "🎮 게임",
                gameTypeLabel,
                true);

        embed.addField(
                "👥 참여자",
                participantSb.toString(),
                false);

        embed.addField(
                "⏳ 대기자",
                waitSb.toString(),
                false);

        embed.addField(
                "⚔️ 팀 구성",
                teamInfo,
                true);

        embed.addField(
                "🕐 내전 시작",
                startTime,
                true);

        embed.addField(
                "📝 내전 비고",
                description != null && !description.isBlank()
                        ? description
                        : "없음",
                false);

        embed.setFooter(
                "파티 ID: " + partyId);

        return embed;
    }

    // 내전 팀 정보 임베드
    public static EmbedBuilder buildInHouseTeamEmbed(
            Guild guild,
            int partyId) {

        InHouseDTO inHouse = InHouseDAO.getInHouseByPartyId(partyId);

        EmbedBuilder embed = new EmbedBuilder();

        if (inHouse == null) {
            return embed
                    .setTitle("내전 정보를 찾을 수 없습니다.")
                    .setDescription("존재하지 않거나 삭제된 내전입니다.");
        }

        List<InHouseTeamDTO> teams = inHouse.getTeams();

        // 게임 유형
        String gameType = switch (inHouse.getGameType()) {
            case 1 -> "칼바람";
            case 2 -> "협곡";
            default -> "알 수 없는 게임";
        };

        // 생성자 Discord 닉네임
        Member owner = guild.getMemberById(inHouse.getOwnerId());

        String ownerName = owner != null
                ? owner.getEffectiveName()
                : "알 수 없는 사용자";

        // 시작 시간
        String startedAt = inHouse.getStartedAt() != null
                ? inHouse.getStartedAt()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : "미정";

        embed.setTitle("⚔️ " + ownerName + "님의 "
                + gameType + " 내전 팀 정보");

        StringBuilder sb = new StringBuilder();

        sb.append("시작 시간 : ")
                .append(startedAt)
                .append("\n\n");

        if (teams == null || teams.isEmpty()) {

            sb.append("팀 정보가 없습니다.");

        } else {

            for (int i = 0; i < teams.size(); i++) {

                InHouseTeamDTO team = teams.get(i);

                // 팀 색상
                String teamIcon = switch (i % 4) {
                    case 0 -> "🔵";
                    case 1 -> "🔴";
                    case 2 -> "🟢";
                    default -> "🟡";
                };

                sb.append("━━━━━━━━━━━━━━━━\n")
                        .append(teamIcon)
                        .append(" ")
                        .append(team.getTeamName())
                        .append("\n")
                        .append("━━━━━━━━━━━━━━━━\n");

                sb.append("디스코드닉네임 | 소환사명 | 전적\n");

                for (MemberDTO member : team.getMembers()) {

                    Member discordMember = guild.getMemberById(
                            member.getDiscordId());

                    String discordName = discordMember != null
                            ? discordMember.getEffectiveName()
                            : "알 수 없는 사용자";

                    String gameNickname = member.getGameNickname();

                    int win = member.getKWinCount();
                    int lose = member.getKLoseCount();

                    int total = win + lose;

                    double winRate = total > 0
                            ? (double) win / total * 100
                            : 0.0;

                    sb.append(discordName)
                            .append(" | ")
                            .append(gameNickname)
                            .append(" | 승 ")
                            .append(win)
                            .append(" / 패 ")
                            .append(lose)
                            .append(" (승률 ")
                            .append(String.format("%.1f", winRate))
                            .append("%)\n");
                }

                sb.append("\n");
            }
        }

        embed.setDescription(sb.toString());
        embed.setFooter("파티 ID: " + partyId);

        return embed;
    }

    // 내전 매치 정보를 보여주는 임베드 생성
    public static EmbedBuilder buildInHouseMatchEmbed(
            Guild guild,
            int partyId) {

        // 내전 ID 조회
        int inHouseId = InHouseDAO.getInHouseIdByPartyId(partyId);

        // 팀 목록 조회
        List<InHouseTeamDTO> teams = InHouseDAO.getTeamsByPartyId(partyId);

        // 매치 목록 조회
        List<InHouseMatchDTO> matches = InHouseMatchDAO.getMatchesByInHouseId(
                inHouseId);

        // 팀 ID → 팀 이름 매핑
        Map<Integer, String> teamNames = new HashMap<>();

        for (InHouseTeamDTO team : teams) {

            teamNames.put(
                    team.getId(),
                    team.getTeamName());
        }

        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle("🏆 내전 경기 정보");

        if (matches.isEmpty()) {

            embed.setDescription("진행할 경기가 없습니다.");

            return embed;
        }

        // 라운드별 경기 출력
        StringBuilder description = new StringBuilder();

        int currentRound = 0;

        for (InHouseMatchDTO match : matches) {

            if (currentRound != match.getRoundNumber()) {

                currentRound = match.getRoundNumber();

                description
                        .append("\n**")
                        .append(currentRound)
                        .append("라운드**\n");
            }

            String team1Name = teamNames.getOrDefault(
                    match.getTeam1Id(),
                    "알 수 없는 팀");

            String team2Name = teamNames.getOrDefault(
                    match.getTeam2Id(),
                    "알 수 없는 팀");

            description
                    .append(team1Name)
                    .append("  **")
                    .append(match.getTeam1Score())
                    .append(" : ")
                    .append(match.getTeam2Score())
                    .append("**  ")
                    .append(team2Name)
                    .append("\n");
        }

        embed.setDescription(
                description.toString());

        return embed;
    }

}