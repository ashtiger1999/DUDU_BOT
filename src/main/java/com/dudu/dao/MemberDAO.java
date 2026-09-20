package com.dudu.dao;

import com.dudu.db.DB;
import com.dudu.dto.MemberDTO;

import java.util.List;
import java.util.ArrayList;
import java.sql.*;

public class MemberDAO {

    // isUserExists 사용자 정보 존재 여부 확인
    public static boolean isUserExists(long discordId) {

        String sql = """
                    SELECT EXISTS(
                        SELECT 1
                        FROM member
                        WHERE discord_id = ?
                    )
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, discordId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getBoolean(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // join 신규 유저 등록
    public static boolean join(long discordId, String gameNickname) {

        String sql = """
                    INSERT INTO member (discord_id, game_nickname)
                    VALUES (?, ?)
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, discordId);
            ps.setString(2, gameNickname);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // get members by partyId 참여자 명단 조회
    public static List<MemberDTO> getMembers(int partyId) {

        String sql = """
                SELECT *
                FROM member
                WHERE party_id = ?
                ORDER BY joined_at asc
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, partyId);
            ResultSet rs = ps.executeQuery();
            List<MemberDTO> members = new ArrayList<>();
            while (rs.next()) {

                MemberDTO member = new MemberDTO();
                member.setDiscordId(rs.getLong("discord_id"));
                member.setGameNickname(rs.getString("game_nickname"));
                members.add(member);
            }

            return members;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    // 소환사명 변경
    public static boolean updateName(long discordId, String newGameNickname) {

        String sql = """
                    UPDATE member
                    SET game_nickname = ?
                    WHERE discord_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newGameNickname);
            ps.setLong(2, discordId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // 소환사명 조회
    public static String getGameNickname(long discordId) {

        String sql = """
                    SELECT game_nickname
                    FROM member
                    WHERE discord_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, discordId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("game_nickname");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    // 내전 협곡 승리 횟수 조회
    public static int getHWinCount(long discordId) {

        String sql = """
                    SELECT h_win_count
                    FROM member
                    WHERE discord_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, discordId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("h_win_count");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // 내전 협곡 패배 횟수 조회
    public static int getHLoseCount(long discordId) {

        String sql = """
                    SELECT h_lose_count
                    FROM member
                    WHERE discord_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, discordId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("h_lose_count");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // 내전 아수라장 승리 횟수 조회
    public static int getKWinCount(long discordId) {

        String sql = """
                    SELECT k_win_count
                    FROM member
                    WHERE discord_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, discordId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("k_win_count");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // 내전 아수라장 패배 횟수 조회
    public static int getKLoseCount(long discordId) {

        String sql = """
                    SELECT k_lose_count
                    FROM member
                    WHERE discord_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, discordId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("k_lose_count");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // 내전 승리 횟수 감소
    public static boolean decreaseInHouseWinCount(
            int inHouseId,
            int teamId,
            int gameType) {

        String winColumn;

        if (gameType == 1) {
            winColumn = "k_win_count";
        } else if (gameType == 2) {
            winColumn = "h_win_count";
        } else {
            return false;
        }

        String sql = """
                UPDATE member
                SET %s = CASE
                    WHEN %s > 0 THEN %s - 1
                    ELSE %s
                END
                WHERE discord_id IN (
                    SELECT user_id
                    FROM in_house_team_member
                    WHERE in_house_id = ?
                      AND team_id = ?
                )
                """.formatted(
                winColumn,
                winColumn,
                winColumn,
                winColumn);

        try (Connection conn = DB.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, inHouseId);
            pstmt.setInt(2, teamId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}