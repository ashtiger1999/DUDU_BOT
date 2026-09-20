package com.dudu.dao;

import com.dudu.db.DB;
import com.dudu.dto.InHouseDTO;
import com.dudu.dto.InHouseTeamDTO;
import com.dudu.dto.MemberDTO;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InHouseDAO {

    // create in-house party
    public static boolean createInHouseParty(
            int partyId,
            LocalDateTime startedAt,
            int maxPlayers,
            int teamCount) {

        String sql = """
                    INSERT INTO in_house (
                        party_id,
                        started_at,
                        max_players,
                        team_count
                    )
                    VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ps.setTimestamp(2, Timestamp.valueOf(startedAt));
            ps.setInt(3, maxPlayers);
            ps.setInt(4, teamCount);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // 내전 생성 중 오류 발생 시, 내전 더미데이터 삭제 로직
    public static boolean deleteInHouseParty(int partyId) {

        String deletePartyJoinSql = """
                    DELETE FROM party_join
                    WHERE party_id = ?
                """;

        String deleteInHouseSql = """
                    DELETE FROM in_house
                    WHERE party_id = ?
                """;

        String deletePartySql = """
                    DELETE FROM party
                    WHERE id = ?
                """;

        try (Connection conn = DB.getConnection()) {

            conn.setAutoCommit(false);

            try (
                    PreparedStatement partyJoinPs = conn.prepareStatement(deletePartyJoinSql);
                    PreparedStatement inHousePs = conn.prepareStatement(deleteInHouseSql);
                    PreparedStatement partyPs = conn.prepareStatement(deletePartySql)) {

                // 1. party_join 삭제
                partyJoinPs.setInt(1, partyId);
                partyJoinPs.executeUpdate();

                // 2. in_house 삭제
                inHousePs.setInt(1, partyId);
                inHousePs.executeUpdate();

                // 3. party 삭제
                partyPs.setInt(1, partyId);
                int deleted = partyPs.executeUpdate();

                if (deleted == 0) {
                    conn.rollback();
                    return false;
                }

                conn.commit();
                return true;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // 내전 파티 상태 조회
    public static int getInHousePartyStatus(int partyId) {

        String sql = """
                    SELECT status
                    FROM in_house
                    WHERE party_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("status");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1; // 상태를 가져오지 못했을 경우
    }

    // 내전 파티 상태 업데이트
    public static boolean updateInHousePartyStatus(int partyId, int status) {

        String sql = """
                    UPDATE in_house
                    SET status = ?
                    WHERE party_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, status);
            ps.setInt(2, partyId);
            int updated = ps.executeUpdate();

            return updated > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // 내전 시작 시간 조회
    public static LocalDateTime getStartedAt(int partyId) {

        String sql = """
                    SELECT started_at
                    FROM in_house
                    WHERE party_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("started_at");
                return timestamp != null ? timestamp.toLocalDateTime() : null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // 시작 시간을 가져오지 못했을 경우
    }

    // 내전 마감 시간 업데이트
    public static boolean updateDeadline(int partyId, LocalDateTime deadline) {

        String sql = """
                    UPDATE party
                    SET deleted_at = ?
                    WHERE id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            ps.setString(1, deadline.format(formatter));
            ps.setInt(2, partyId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // 내전 참여 인원수 제한 조회
    public static int getInHouseMaxPlayers(int partyId) {

        String sql = """
                    SELECT max_players
                    FROM in_house
                    WHERE party_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("max_players");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0; // 참여인원수를 가져오지 못했을 경우
    }

    // 내전 팀 수 조회
    public static int getInHouseTeamCount(int partyId) {

        String sql = """
                    SELECT team_count
                    FROM in_house
                    WHERE party_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("team_count");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0; // 팀 수를 가져오지 못했을 경우
    }

    // 팀 생성하기
    public static boolean createInHouseTeams(int partyId, int teamCount) {

        String selectSql = """
                SELECT id
                FROM in_house
                WHERE party_id = ?
                """;

        String insertSql = """
                INSERT INTO in_house_team (in_house_id, team_name)
                VALUES (?, ?)
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement selectPs = conn.prepareStatement(selectSql)) {

            selectPs.setInt(1, partyId);

            try (ResultSet rs = selectPs.executeQuery()) {

                if (!rs.next()) {
                    return false;
                }

                int inHouseId = rs.getInt("id");

                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {

                    for (int i = 0; i < teamCount; i++) {

                        String teamName = String.valueOf((char) ('A' + i));

                        insertPs.setInt(1, inHouseId);
                        insertPs.setString(2, teamName);
                        insertPs.addBatch();
                    }

                    insertPs.executeBatch();
                    return true;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // 팀 나가기
    public static boolean leaveInHouseTeam(int partyId, long discordId) {

        String sql = """
                DELETE FROM in_house_team_member
                WHERE in_house_id = (
                    SELECT id
                    FROM in_house
                    WHERE party_id = ?
                )
                AND user_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ps.setLong(2, discordId);

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 팀에 참여
    public static boolean joinUserToInHouseTeam(int partyId, long discordId, int team_id) {

        String sql = """
                INSERT INTO in_house_team_member (in_house_id, user_id, team_id)
                VALUES (
                    (SELECT id FROM in_house WHERE party_id = ?),
                    ?,
                    ?
                )
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ps.setLong(2, discordId);
            ps.setInt(3, team_id);

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 팀 변경
    public static boolean changeUserInHouseTeam(int partyId, long discordId, int team_id) {

        String sql = """
                UPDATE in_house_team_member
                SET team_id = ?
                WHERE in_house_id = (
                    SELECT id
                    FROM in_house
                    WHERE party_id = ?
                )
                AND user_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, team_id);
            ps.setInt(2, partyId);
            ps.setLong(3, discordId);

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 사용자가 이미 팀에 소속되어 있는지 확인
    public static boolean isUserInInHouseTeam(int partyId, long discordId) {

        String sql = """
                    SELECT COUNT(*) AS count
                    FROM in_house_team_member
                    WHERE in_house_id = (
                        SELECT id
                        FROM in_house
                        WHERE party_id = ?
                    )
                    AND user_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ps.setLong(2, discordId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // 소속된 팀의 정보 조회
    public static int getUserInHouseTeam(int partyId, long discordId) {

        String sql = """
                    SELECT team_id
                    FROM in_house_team_member
                    WHERE in_house_id = (
                        SELECT id
                        FROM in_house
                        WHERE party_id = ?
                    )
                    AND user_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ps.setLong(2, discordId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("team_id");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1; // 팀에 소속되어 있지 않음
    }

    // party_id와 team_name으로 팀 ID를 조회
    public static int getTeamIdByPartyIdAndTeamName(int partyId, String teamName) {

        String sql = """
                    SELECT id
                    FROM in_house_team
                    WHERE in_house_id = (
                        SELECT id
                        FROM in_house
                        WHERE party_id = ?
                    )
                    AND team_name = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ps.setString(2, teamName);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1; // 팀이 존재하지 않음
    }

    // party_id로 내전 정보 조회
    public static InHouseDTO getInHouseByPartyId(int partyId) {

        String sql = """
                    SELECT
                        ih.id,
                        p.owner_id,
                        p.created_at,
                        ih.started_at,
                        p.deleted_at,
                        p.game_type,
                        ih.max_players,
                        ih.team_count,
                        ih.status
                    FROM in_house ih
                    JOIN party p ON ih.party_id = p.id
                    WHERE ih.party_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    InHouseDTO inHouse = new InHouseDTO();

                    inHouse.setId(rs.getInt("id"));
                    inHouse.setOwnerId(rs.getLong("owner_id"));

                    Timestamp createdAt = rs.getTimestamp("created_at");
                    inHouse.setCreatedAt(
                            createdAt != null
                                    ? createdAt.toLocalDateTime()
                                    : null);

                    Timestamp startedAt = rs.getTimestamp("started_at");
                    inHouse.setStartedAt(
                            startedAt != null
                                    ? startedAt.toLocalDateTime()
                                    : null);

                    Timestamp deletedAt = rs.getTimestamp("deleted_at");
                    inHouse.setDeletedAt(
                            deletedAt != null
                                    ? deletedAt.toLocalDateTime()
                                    : null);

                    inHouse.setGameType(rs.getInt("game_type"));
                    inHouse.setMaxPlayers(rs.getInt("max_players"));
                    inHouse.setTeamCount(rs.getInt("team_count"));
                    inHouse.setStatus(rs.getInt("status"));

                    // 팀 목록 조회 후 DTO에 저장
                    inHouse.setTeams(getTeamsByPartyId(partyId));

                    return inHouse;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // 내전 정보가 존재하지 않음
    }

    // 내전 파티 ID로 팀 목록 조회
    public static List<InHouseTeamDTO> getTeamsByPartyId(int partyId) {

        String sql = """
                    SELECT
                        iht.id AS team_id,
                        iht.in_house_id,
                        iht.team_name,
                        m.discord_id,
                        m.game_nickname,
                        m.k_win_count,
                        m.k_lose_count,
                        m.h_win_count,
                        m.h_lose_count
                    FROM in_house_team iht
                    JOIN in_house ih
                        ON iht.in_house_id = ih.id
                    LEFT JOIN in_house_team_member ihtm
                        ON iht.id = ihtm.team_id
                    LEFT JOIN member m
                        ON ihtm.user_id = m.discord_id
                    WHERE ih.party_id = ?
                    ORDER BY iht.id ASC, m.joined_at ASC
                """;

        Map<Integer, InHouseTeamDTO> teamMap = new LinkedHashMap<>();

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    int teamId = rs.getInt("team_id");

                    InHouseTeamDTO team = teamMap.get(teamId);

                    // 팀 정보가 아직 없으면 생성
                    if (team == null) {

                        team = new InHouseTeamDTO();

                        team.setId(teamId);
                        team.setInHouseId(rs.getInt("in_house_id"));
                        team.setTeamName(rs.getString("team_name"));

                        teamMap.put(teamId, team);
                    }

                    // 참가자 정보가 존재하는 경우에만 추가
                    long discordId = rs.getLong("discord_id");

                    if (!rs.wasNull()) {

                        MemberDTO member = new MemberDTO();

                        member.setDiscordId(discordId);
                        member.setGameNickname(
                                rs.getString("game_nickname"));

                        member.setKWinCount(
                                rs.getInt("k_win_count"));

                        member.setKLoseCount(
                                rs.getInt("k_lose_count"));

                        member.setHWinCount(
                                rs.getInt("h_win_count"));

                        member.setHLoseCount(
                                rs.getInt("h_lose_count"));

                        team.getMembers().add(member);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new ArrayList<>(teamMap.values());
    }

    // 팀 메시지 ID 조회
    public static Long getTeamMessageId(int partyId) {

        String sql = """
                SELECT team_message_id
                FROM in_house
                WHERE party_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, partyId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    long messageId = rs.getLong("team_message_id");

                    // DB 값이 NULL이면 null 반환
                    if (rs.wasNull()) {
                        return null;
                    }

                    return messageId;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // 팀 메시지 ID 업데이트
    public static boolean updateTeamMessageId(
            int partyId,
            long messageId) {

        String sql = """
                UPDATE in_house
                SET team_message_id = ?
                WHERE party_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, messageId);
            pstmt.setInt(2, partyId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // 파티 ID로 내전 ID 조회
    public static int getInHouseIdByPartyId(int partyId) {

        String sql = """
                SELECT id
                FROM in_house
                WHERE party_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, partyId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("id");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    // 파티 ID로 매치 메시지 ID 조회
    public static Long getMatchMessageId(int partyId) {

        String sql = """
                SELECT match_message_id
                FROM in_house
                WHERE party_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, partyId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {

                    long messageId = rs.getLong("match_message_id");

                    if (rs.wasNull()) {
                        return null;
                    }

                    return messageId;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // 매치 메시지 ID를 업데이트하는 메서드
    public static boolean updateMatchMessageId(
            int partyId,
            long messageId) {

        String sql = """
                UPDATE in_house
                SET match_message_id = ?
                WHERE party_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, messageId);
            pstmt.setInt(2, partyId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // 팀 ID로 팀 이름 조회
    public static String getTeamNameByTeamId(int teamId) {

        String sql = """
                SELECT team_name
                FROM in_house_team
                WHERE id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, teamId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getString("team_name");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "알 수 없는 팀";
    }

    // 인하우스 ID로 파티 ID 조회
    public static int getPartyIdByInHouseId(int inHouseId) {

        String sql = """
                SELECT party_id
                FROM in_house
                WHERE id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, inHouseId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("party_id");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    // 내전 시작 시간과 파티 종료 시간 업데이트
    public static boolean updateStartAndDeadline(
            int partyId,
            LocalDateTime startedAt,
            LocalDateTime deletedAt) {

        String sql = """
                UPDATE in_house
                SET started_at = ?
                WHERE party_id = ?
                """;

        String partySql = """
                UPDATE party
                SET deleted_at = ?
                WHERE id = ?
                """;

        try (Connection conn = DB.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                    PreparedStatement partyPstmt = conn.prepareStatement(partySql)) {

                pstmt.setTimestamp(
                        1,
                        Timestamp.valueOf(startedAt));
                pstmt.setInt(2, partyId);

                if (pstmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                partyPstmt.setTimestamp(
                        1,
                        Timestamp.valueOf(deletedAt));
                partyPstmt.setInt(2, partyId);

                if (partyPstmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // 내전 참여자 추방
    public static boolean kickParticipant(
            int partyId,
            long discordId) {

        String sql = """
                DELETE FROM in_house_team_member
                WHERE in_house_id = (
                    SELECT id
                    FROM in_house
                    WHERE party_id = ?
                )
                AND user_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, partyId);
            pstmt.setLong(2, discordId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
