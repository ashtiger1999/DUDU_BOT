package com.dudu.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DB {

    private static final String URL = "jdbc:sqlite:data/party.db";

    public static void init() {
        /*
         ** 멤버 정보 SQL**
         * id = 파티원 고유 ID (자동 증가)
         * discord_id = 파티원 Discord ID
         * game_nickname = 게임 내 닉네임
         * k_win_count = 칼바람 승 수 (기본 0)
         * k_lose_count = 칼바람 패 수 (기본 0)
         * h_win_count = 협곡 승 수 (기본 0)
         * h_lose_count = 협곡 패 수 (기본 0)
         * joined_at = 가입 시간 (자동 설정)
         */
        String memberSql = """
                CREATE TABLE IF NOT EXISTS member (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    discord_id INTEGER NOT NULL UNIQUE,
                    game_nickname TEXT NOT NULL,
                    k_win_count INTEGER DEFAULT 0,
                    k_lose_count INTEGER DEFAULT 0,
                    h_win_count INTEGER DEFAULT 0,
                    h_lose_count INTEGER DEFAULT 0,
                    joined_at DATETIME DEFAULT (DATETIME(CURRENT_TIMESTAMP, '+9 hours'))
                )
                """;

        /*
         ** 파티 참여 테이블 생성 SQL**
         * id = 파티 참여 고유 ID (자동 증가)
         * party_id = 파티 ID (party 테이블의 id와 외래키 관계)
         * discord_id = 참여자 Discord ID (member 테이블의 discord_id와 외래키 관계)
         * joined_at = 참여 시간 (자동 설정)
         * UNIQUE(party_id, discord_id) = 동일한 파티에 동일한 사용자가 중복 참여하지 않도록 설정
         */
        String partyJoinSql = """
                CREATE TABLE IF NOT EXISTS party_join (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    party_id INTEGER NOT NULL,
                    discord_id INTEGER NOT NULL,
                    joined_at DATETIME DEFAULT (DATETIME(CURRENT_TIMESTAMP, '+9 hours')),
                    FOREIGN KEY (party_id) REFERENCES party(id) ON DELETE CASCADE,
                    FOREIGN KEY (discord_id) REFERENCES member(discord_id),
                    UNIQUE(party_id, discord_id)
                )
                """;

        /*
         ** 일반 게임 파티 테이블 생성 SQL**
         * id = 파티 고유 ID (자동 증가)
         * owner_id = 파티장 Discord ID
         * message_id = 파티 모집 메시지 ID (파티원 갱신 시 필요)
         * channel_id = 파티 모집 메시지가 게시된 채널 ID
         * created_at = 파티 생성 시간 (자동 설정)
         * deleted_at = 파티 삭제 시간 (기본 6시간 후 자동 삭제, 수동 삭제 시 현재 시간으로 설정)
         * game_type = 파티 유형 (예: 아수라장 = 1, 협곡 = 2, 등등)
         * game_mode = 게임 모드 (예: 일반 = 1, 랭크 = 2, 3 = 내전, 등등)
         * description = 파티 설명 (최대 200자)
         */
        String partySql = """
                CREATE TABLE IF NOT EXISTS party (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner_id INTEGER NOT NULL,
                    message_id INTEGER,
                    channel_id INTEGER,
                    created_at DATETIME DEFAULT (DATETIME(CURRENT_TIMESTAMP, '+9 hours')),
                    deleted_at DATETIME DEFAULT (DATETIME(CURRENT_TIMESTAMP, '+15 hours')),
                    game_type INTEGER NOT NULL,
                    game_mode INTEGER NOT NULL DEFAULT 1,
                    description TEXT,
                    FOREIGN KEY (owner_id) REFERENCES member(discord_id)
                )
                """;

        /*
         ** 내전 게임 파티 테이블 생성 SQL**
         * id = 내전 파티 고유 ID (자동 증가)
         * party_id = 내전 파티 ID
         * started_at = 내전 시작 시간 (내전 시작 시 현재 시간으로 설정)
         * max_players = 내전 최대 참여 인원 (예: 10, 20, 등등)
         * team_count = 내전 팀 수 (예: 2, 4, 등등)
         * status = 내전 상태 (예: 0 = 모집중, 1 = 모집마감, 2 = 시작, 3 = 종료)
         * team_message_id = 팀 정보 메시지 ID (팀 정보 갱신 시 필요)
         * match_message_id = 경기 정보 메시지 ID (경기 정보 갱신 시 필요)
         */
        String inHouseSql = """
                CREATE TABLE IF NOT EXISTS in_house (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    party_id INTEGER NOT NULL,
                    started_at DATETIME DEFAULT (DATETIME(CURRENT_TIMESTAMP, '+9 hours')),
                    max_players INTEGER NOT NULL,
                    team_count INTEGER NOT NULL,
                    team_message_id INTEGER,
                    match_message_id INTEGER,
                    status INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (party_id) REFERENCES party(id) ON DELETE CASCADE
                )
                """;

        /*
         ** 내전 팀 테이블 생성 SQL**
         * id = 내전 팀 고유 ID (자동 증가)
         * in_house_id = 내전 ID (in_house 테이블의 id와 외래키 관계)
         * team_name = 팀 이름
         */
        String inHouseTeamSql = """
                CREATE TABLE IF NOT EXISTS in_house_team (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    in_house_id INTEGER NOT NULL,
                    team_name TEXT NOT NULL,
                    FOREIGN KEY (in_house_id) REFERENCES in_house(id) ON DELETE CASCADE
                )
                """;

        /*
         ** 내전 팀 멤버 테이블 SQL**
         * id = 내전 팀 멤버 고유 ID (자동 증가)
         * in_house_id = 내전 ID (in_house 테이블의 id와 외래키 관계)
         * team_id = 내전 팀 ID (in_house_team 테이블의 id와 외래키 관계)
         * user_id = 참여자 고유 ID (member 테이블의 discord_id와 외래키 관계)
         * UNIQUE(team_id, user_id) = 동일한 팀에 동일한 사용자가 중복 참여하지 않도록 설정
         * UNIQUE(in_house_id, user_id) = 동일한 내전에서 동일한 사용자가 중복 참여하지 않도록 설정
         */
        String inHouseTeamMemberSql = """
                CREATE TABLE IF NOT EXISTS in_house_team_member (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    in_house_id INTEGER NOT NULL,
                    team_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    FOREIGN KEY (in_house_id) REFERENCES in_house(id) ON DELETE CASCADE,
                    FOREIGN KEY (team_id) REFERENCES in_house_team(id) ON DELETE CASCADE,
                    FOREIGN KEY (user_id) REFERENCES member(discord_id),
                    UNIQUE(team_id, user_id),
                    UNIQUE(in_house_id, user_id)
                )
                """;

        /*
         ** in_house_match 테이블 생성 SQL**
         * id = 내전 경기 고유 ID (자동 증가)
         * in_house_id = 내전 ID (in_house 테이블의 id와 외래키 관계)
         * round_number = 경기 라운드 번호 (예: 1, 2, 3, 등등, 경기 단계를 구분하기 위해 사용)
         * team1_id = 팀 1 ID (in_house_team 테이블의 id와 외래키 관계)
         * team2_id = 팀 2 ID (in_house_team 테이블의 id와 외래키 관계)
         * team1_score = 팀 1 점수 (기본 0)
         * team2_score = 팀 2 점수 (기본 0)
         * winner_team_id = 승리 팀 ID (in_house_team 테이블의 id와 외래키 관계, 경기 종료 후 설정)
         */
        String inHouseMatchSql = """
                CREATE TABLE IF NOT EXISTS in_house_match (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    in_house_id INTEGER NOT NULL,
                    round_number INTEGER NOT NULL,
                    team1_id INTEGER NOT NULL,
                    team2_id INTEGER NOT NULL,
                    team1_score INTEGER DEFAULT 0,
                    team2_score INTEGER DEFAULT 0,
                    winner_team_id INTEGER,
                    FOREIGN KEY (in_house_id) REFERENCES in_house(id) ON DELETE CASCADE,
                    FOREIGN KEY (team1_id) REFERENCES in_house_team(id),
                    FOREIGN KEY (team2_id) REFERENCES in_house_team(id),
                    FOREIGN KEY (winner_team_id) REFERENCES in_house_team(id)
                )
                """;

        try (
                Connection conn = DriverManager.getConnection(URL);
                Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute(memberSql);
            stmt.execute(partySql);

            // 기존 DB에 channel_id 컬럼이 없는 경우 추가
            try {
                stmt.execute("ALTER TABLE party ADD COLUMN channel_id INTEGER");
            } catch (SQLException e) {
                // 이미 존재하는 경우 무시
            }
            
            stmt.execute(partyJoinSql);
            stmt.execute(inHouseSql);
            stmt.execute(inHouseTeamSql);
            stmt.execute(inHouseMatchSql);
            stmt.execute(inHouseTeamMemberSql);
            System.out.println("Connected to the database.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        conn.createStatement().execute("PRAGMA foreign_keys = ON");
        return conn;
    }
}