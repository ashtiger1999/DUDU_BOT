package com.dudu.dao;

import com.dudu.db.DB;
import com.dudu.dto.InHouseMatchDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InHouseMatchDAO {

    // 매치를 삽입하는 메서드
    public static boolean insertMatch(
            InHouseMatchDTO match) {

        String sql = """
                INSERT INTO in_house_match (
                        in_house_id,
                        round_number,
                        team1_id,
                        team2_id,
                        team1_score,
                        team2_score,
                        winner_team_id
                )
                VALUES (?, ?, ?, ?, 0, 0, null)
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, match.getInHouseId());
            pstmt.setInt(2, match.getRoundNumber());
            pstmt.setInt(3, match.getTeam1Id());
            pstmt.setInt(4, match.getTeam2Id());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // 내전 ID로 매치 정보를 조회하는 메서드
    public static List<InHouseMatchDTO> getMatchesByInHouseId(
            int inHouseId) {

        String sql = """
                SELECT
                        id,
                        in_house_id,
                        round_number,
                        team1_id,
                        team2_id,
                        team1_score,
                        team2_score,
                        winner_team_id
                FROM in_house_match
                WHERE in_house_id = ?
                ORDER BY round_number ASC, id ASC
                """;

        List<InHouseMatchDTO> matches = new ArrayList<>();

        try (Connection conn = DB.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, inHouseId);

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {

                    InHouseMatchDTO match = new InHouseMatchDTO();

                    match.setId(
                            rs.getInt("id"));

                    match.setInHouseId(
                            rs.getInt("in_house_id"));

                    match.setRoundNumber(
                            rs.getInt("round_number"));

                    match.setTeam1Id(
                            rs.getInt("team1_id"));

                    match.setTeam2Id(
                            rs.getInt("team2_id"));

                    match.setTeam1Score(
                            rs.getInt("team1_score"));

                    match.setTeam2Score(
                            rs.getInt("team2_score"));

                    Integer winnerTeamId = (Integer) rs.getObject(
                            "winner_team_id");

                    match.setWinnerTeamId(
                            winnerTeamId != null
                                    ? winnerTeamId
                                    : 0);

                    matches.add(match);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return matches;
    }

    // 팀 점수와 팀원 승수를 추가하는 메서드
    public static boolean addTeamScore(
            int matchId,
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

        String scoreSql = """
                UPDATE in_house_match
                SET
                    team1_score = CASE
                        WHEN team1_id = ? THEN team1_score + 1
                        ELSE team1_score
                    END,
                    team2_score = CASE
                        WHEN team2_id = ? THEN team2_score + 1
                        ELSE team2_score
                    END
                WHERE id = ?
                  AND (team1_id = ? OR team2_id = ?)
                  AND winner_team_id IS NULL
                """;

        String memberSql = """
                UPDATE member
                SET %s = %s + 1
                WHERE discord_id IN (
                    SELECT user_id
                    FROM in_house_team_member
                    WHERE in_house_id = (
                        SELECT in_house_id
                        FROM in_house_match
                        WHERE id = ?
                    )
                    AND team_id = ?
                )
                """.formatted(
                winColumn,
                winColumn);

        try (Connection conn = DB.getConnection()) {

            conn.setAutoCommit(false);

            try (
                    PreparedStatement scoreStmt = conn.prepareStatement(scoreSql);

                    PreparedStatement memberStmt = conn.prepareStatement(memberSql)) {

                // 매치 점수 +1
                scoreStmt.setInt(1, teamId);
                scoreStmt.setInt(2, teamId);
                scoreStmt.setInt(3, matchId);
                scoreStmt.setInt(4, teamId);
                scoreStmt.setInt(5, teamId);

                int scoreUpdated = scoreStmt.executeUpdate();

                if (scoreUpdated == 0) {
                    conn.rollback();
                    return false;
                }

                // 팀원 승수 +1
                memberStmt.setInt(1, matchId);
                memberStmt.setInt(2, teamId);

                int memberUpdated = memberStmt.executeUpdate();

                if (memberUpdated == 0) {
                    conn.rollback();
                    return false;
                }

                // 둘 다 성공
                conn.commit();

                return true;

            } catch (SQLException e) {

                conn.rollback();
                e.printStackTrace();

                return false;
            }

        } catch (SQLException e) {

            e.printStackTrace();

            return false;
        }
    }

    // 매치 ID로 인하우스 ID 조회
    public static int getInHouseIdByMatchId(int matchId) {

        String sql = """
                SELECT in_house_id
                FROM in_house_match
                WHERE id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, matchId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("in_house_id");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    // 인하우스 ID로 매치 삭제
    public static boolean deleteMatchesByInHouseId(int inHouseId) {

        String sql = """
                DELETE FROM in_house_match
                WHERE in_house_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, inHouseId);

            pstmt.executeUpdate();

            return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // 세트 승과 승리 횟수를 감소시키는 메서드
    public static boolean decreaseScoreAndWinCount(
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

        String scoreSql = """
                UPDATE in_house_match
                SET
                    team1_score = CASE
                        WHEN team1_id = ? AND team1_score > 0
                        THEN team1_score - 1
                        ELSE team1_score
                    END,
                    team2_score = CASE
                        WHEN team2_id = ? AND team2_score > 0
                        THEN team2_score - 1
                        ELSE team2_score
                    END
                WHERE in_house_id = ?
                  AND (team1_id = ? OR team2_id = ?)
                  AND winner_team_id IS NULL
                  AND (
                      (team1_id = ? AND team1_score > 0)
                      OR
                      (team2_id = ? AND team2_score > 0)
                  )
                """;

        String memberSql = """
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

        try (Connection conn = DB.getConnection()) {

            // 트랜잭션 시작
            conn.setAutoCommit(false);

            try (
                    PreparedStatement scoreStmt = conn.prepareStatement(scoreSql);

                    PreparedStatement memberStmt = conn.prepareStatement(memberSql)) {

                // =========================
                // 1. 매치 점수 감소
                // =========================

                scoreStmt.setInt(1, teamId);
                scoreStmt.setInt(2, teamId);
                scoreStmt.setInt(3, inHouseId);
                scoreStmt.setInt(4, teamId);
                scoreStmt.setInt(5, teamId);
                scoreStmt.setInt(6, teamId);
                scoreStmt.setInt(7, teamId);

                int scoreUpdated = scoreStmt.executeUpdate();

                // 점수 감소가 되지 않았다면 실패
                if (scoreUpdated == 0) {

                    conn.rollback();

                    return false;
                }

                // =========================
                // 2. 팀원 승수 감소
                // =========================

                memberStmt.setInt(1, inHouseId);
                memberStmt.setInt(2, teamId);

                int memberUpdated = memberStmt.executeUpdate();

                // 팀원이 한 명도 없다면 실패
                if (memberUpdated == 0) {

                    conn.rollback();

                    return false;
                }

                // =========================
                // 3. 모든 작업 성공
                // =========================

                conn.commit();

                return true;

            } catch (SQLException e) {

                // 하나라도 실패하면 전체 취소
                conn.rollback();

                e.printStackTrace();

                return false;
            }

        } catch (SQLException e) {

            e.printStackTrace();

            return false;
        }
    }

    // 매치 종료 처리
    public static List<InHouseMatchDTO> getCurrentRoundMatches(int inHouseId) {

        List<InHouseMatchDTO> matches = new ArrayList<>();

        String sql = """
                SELECT *
                FROM in_house_match
                WHERE in_house_id = ?
                  AND round_number = (
                      SELECT MAX(round_number)
                      FROM in_house_match
                      WHERE in_house_id = ?
                  )""";

        try (Connection conn = DB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, inHouseId);
            stmt.setInt(2, inHouseId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    InHouseMatchDTO match = new InHouseMatchDTO();
                    match.setId(rs.getInt("id"));
                    match.setInHouseId(rs.getInt("in_house_id"));
                    match.setRoundNumber(rs.getInt("round_number"));
                    match.setTeam1Id(rs.getInt("team1_id"));
                    match.setTeam2Id(rs.getInt("team2_id"));
                    match.setTeam1Score(rs.getInt("team1_score"));
                    match.setTeam2Score(rs.getInt("team2_score"));
                    match.setWinnerTeamId(rs.getInt("winner_team_id"));
                    matches.add(match);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return matches;
    }

    // 매치의 승리 팀을 업데이트
    public static boolean updateWinnerTeam(
            int matchId,
            int winnerTeamId) {

        String sql = """
                UPDATE in_house_match
                SET winner_team_id = ?
                WHERE id = ?
                  AND winner_team_id IS NULL
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, winnerTeamId);
            stmt.setInt(2, matchId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // 현재 라운드에서 승리한 팀의 수를 조회
    public static int getWinnerTeamCount(int inHouseId) {

        String sql = """
                SELECT COUNT(DISTINCT winner_team_id)
                FROM in_house_match
                WHERE in_house_id = ?
                  AND round_number = (
                      SELECT MAX(round_number)
                      FROM in_house_match
                      WHERE in_house_id = ?
                  )
                  AND winner_team_id IS NOT NULL
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, inHouseId);
            stmt.setInt(2, inHouseId);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    // 현재 라운드에서 승리한 팀들의 ID 목록을 조회
    public static List<Integer> getWinnerTeamIds(int inHouseId) {

        List<Integer> winnerTeamIds = new ArrayList<>();

        String sql = """
                SELECT DISTINCT winner_team_id
                FROM in_house_match
                WHERE in_house_id = ?
                  AND round_number = (
                      SELECT MAX(round_number)
                      FROM in_house_match
                      WHERE in_house_id = ?
                  )
                  AND winner_team_id IS NOT NULL
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, inHouseId);
            stmt.setInt(2, inHouseId);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    winnerTeamIds.add(
                            rs.getInt("winner_team_id"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return winnerTeamIds;
    }
}
