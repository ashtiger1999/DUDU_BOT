package com.dudu.dao;

import com.dudu.db.DB;
import com.dudu.dto.MemberDTO;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PartyDAO {

    // create party 파티 생성
    public static int createParty(long ownerId, String description, int gameType, int gameMode) {

        String sql = """
                    INSERT INTO party (owner_id, game_type, game_mode, description)
                    VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, ownerId); // 파티 생성자 Discord ID
            ps.setInt(2, gameType); // 게임 종류 (1 = 증바람, 2 = 협곡 등)
            ps.setInt(3, gameMode); // 게임 모드 (1 = 일반, 2 = 랭크, 3 = 내전)
            ps.setString(4, description); // 파티 비고

            ps.executeUpdate();

            // Get the generated party_id
            try (ResultSet rs = ps.getGeneratedKeys()) {

                if (rs.next()) {
                    return rs.getInt(1); // party_id 반환
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    // join party 파티 참여
    public static boolean join(int partyId, long discordId) {

        String sql = """
                    INSERT INTO party_join (party_id, discord_id)
                    VALUES (?, ?)
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ps.setLong(2, discordId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            // 중복 참여 시 예외 발생
            if (e.getErrorCode() == 19) { // SQLite의 UNIQUE 제약 조건 위반 에러 코드
                System.out.println("파티에 참여할 수 없습니다.");
            } else {
                e.printStackTrace();
            }
        }

        return false;
    }

    // leave party 파티 참여 취소
    public static boolean leave(int partyId, long discordId) {

        String sql = """
                    DELETE FROM party_join
                    WHERE party_id = ? AND discord_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ps.setLong(2, discordId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // partyJoin/party/member에서 파티 참여자 정보 조회
    public static List<MemberDTO> getMembers(int partyId) {

        String sql = """
                SELECT m.*
                FROM member m
                JOIN party_join pj ON m.discord_id = pj.discord_id
                WHERE pj.party_id = ?
                ORDER BY pj.joined_at ASC
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);

            ResultSet rs = ps.executeQuery();

            List<MemberDTO> members = new ArrayList<>();

            // ResultSet에서 참여자 정보를 MemberDTO 객체로 변환하여 리스트에 추가
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

    // get party type by partyId 파티 유형 조회
    public static int getGameType(int partyId) {

        String sql = """
                    SELECT game_type
                    FROM party
                    WHERE id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("game_type");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1; // 파티 유형 조회 실패 시 -1 반환
    }

    // get game mode by partyId 파티 모드 조회
    public static int getGameMode(int partyId) {

        String sql = """
                    SELECT game_mode
                    FROM party
                    WHERE id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("game_mode");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1; // 파티 모드 조회 실패 시 -1 반환
    }

    // get description by partyId 파티 비고 조회
    public static String getDescription(int partyId) {

        String sql = """
                    SELECT description
                    FROM party
                    WHERE id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("description");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // 파티 비고 조회 실패 시 null 반환
    }

    // get deleted_at by partyId 파티 삭제시간 조회
    public static LocalDateTime getDeletedAt(int partyId) {

        String sql = """
                    SELECT deleted_at
                    FROM party
                    WHERE id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getObject("deleted_at", LocalDateTime.class);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // 파티 삭제시간 조회 실패 시 null 반환
    }

    // extend party by partyId 파티 연장
    public static boolean extendParty(int partyId) {

        // 파티 마감 시간을 6시간 연장하는 쿼리
        String sql = """
                    UPDATE party
                    SET deleted_at = DATETIME(deleted_at, '+6 hours')
                    WHERE id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // kick participant 파티에서 참여자 강퇴
    public static boolean kick(int partyId, int kickNumber) {

        // partyId가 일치하는 참여자 명단을 joined_at 기준으로 정렬하여 참여자를 삭제
        String sql = """
                DELETE FROM party_join
                WHERE id = (
                    SELECT id
                    FROM (
                        SELECT id,
                               ROW_NUMBER() OVER (ORDER BY joined_at ASC) AS number
                        FROM party_join
                        WHERE party_id = ?
                    )
                    WHERE number = ?
                )
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ps.setInt(2, kickNumber);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // get participant count by party ID 파티 참여 총원
    public static int getParticipantCount(int partyId) {

        String sql = """
                    SELECT COUNT(*) AS count
                    FROM party_join
                    WHERE party_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // save message ID for a party 인베드 전송 후, messageId를 party 테이블에 저장
    public static void saveMessageId(int partyId, String messageId) {

        String sql = """
                    UPDATE party
                    SET message_id = ?
                    WHERE id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, String.valueOf(messageId));
            ps.setInt(2, partyId);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // get message ID by party ID
    public static String getMessageId(int partyId) {

        String sql = """
                    SELECT message_id
                    FROM party
                    WHERE id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("message_id");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // get owner ID by party ID
    public static long getOwnerId(int partyId) {

        String sql = """
                    SELECT owner_id
                    FROM party
                    WHERE id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getLong("owner_id");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    // 파티 참여자 여부 확인
    public static boolean isMember(int partyId, long discordId) {

        String sql = """
                    SELECT COUNT(*)
                    FROM party_join
                    WHERE party_id = ? AND discord_id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ps.setLong(2, discordId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // 파티 취소
    public static void cancelParty(int partyId) {

        String sql = """
                DELETE FROM party
                WHERE id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 만료된 파티 ID 목록 조회
    public static List<Integer> getExpiredPartyIds() {

        List<Integer> partyIds = new ArrayList<>();

        String sql = """
                SELECT id
                FROM party
                WHERE deleted_at <= DATETIME('now', '+9 hours')
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                partyIds.add(rs.getInt("id"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return partyIds;
    }

    // 파티 삭제
    public static boolean deleteParty(int partyId) {

        String sql = """
                DELETE FROM party
                WHERE id = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, partyId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // 특정 파티에서 참여자의 디스코드 ID 조회 (kickNumber 순서 기준)
    public static long getParticipantDiscordId(
            int partyId,
            int kickNumber) {

        String sql = """
                SELECT discord_id
                FROM (
                    SELECT discord_id,
                           ROW_NUMBER() OVER (
                               ORDER BY joined_at ASC
                           ) AS number
                    FROM party_join
                    WHERE party_id = ?
                )
                WHERE number = ?
                """;

        try (Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partyId);
            ps.setInt(2, kickNumber);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getLong("discord_id");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }
}