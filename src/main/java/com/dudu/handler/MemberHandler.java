package com.dudu.handler;

import com.dudu.dao.MemberDAO;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

// 사용자 정보 처리 모달 핸들러
public class MemberHandler {

    // 소환사명 형식 검증
    public static boolean isValidGameNickname(
            String gameNickname) {

        return gameNickname != null
                && gameNickname.equals(gameNickname.trim())
                && gameNickname.matches(
                        "^[a-zA-Z0-9가-힣 ]{3,16}#[a-zA-Z0-9가-힣]{3,5}$");
    }

    // 회원 등록/갱신 메서드
    public static void registerOrUpdateMember(
            long discordId,
            String gameNickname) {

        // 신규 회원이면 회원 등록
        if (!MemberDAO.isUserExists(discordId)) {

            MemberDAO.join(
                    discordId,
                    gameNickname);

            return;
        }

        // 기존 회원이면 기존 소환사명 조회
        String savedGameNickname =
                MemberDAO.getGameNickname(discordId);

        // 기존 소환사명과 다를 경우 변경
        if (!savedGameNickname.equals(gameNickname)) {

            MemberDAO.updateName(
                    discordId,
                    gameNickname);
        }
    }

    // 소환사 이름 변경 처리
    public static void handleChangeSummonerName(
            ModalInteractionEvent event) {

        // 모달에서 입력된 새 소환사 이름 가져오기
        String newSummonerName =
                event.getValue("summonerName")
                        .getAsString();

        long discordId =
                event.getUser().getIdLong();

        // 소환사명 형식 검증
        if (!isValidGameNickname(newSummonerName)) {

            event.reply(
                    "❌ 유효하지 않은 소환사 이름 형식입니다.")
                    .setEphemeral(true)
                    .queue(hook->{
                        // 60초 후에 메시지 삭제
                        hook.deleteOriginal().queueAfter(60, java.util.concurrent.TimeUnit.SECONDS);
                    });

            return;
        }

        // 기존 회원인지 확인
        boolean isExistingMember =
                MemberDAO.isUserExists(discordId);

        // 신규 회원이면 회원 등록
        // 기존 회원이면 소환사명 변경
        registerOrUpdateMember(
                discordId,
                newSummonerName);

        if (isExistingMember) {

            event.reply(
                    "✅ 소환사 이름이 성공적으로 변경되었습니다.")
                    .setEphemeral(true)
                    .queue(hook->{
                        // 60초 후에 메시지 삭제
                        hook.deleteOriginal().queueAfter(60, java.util.concurrent.TimeUnit.SECONDS);
                    });

        } else {

            event.reply(
                    "✅ 회원 등록 및 소환사 이름 설정이 완료되었습니다.")
                    .setEphemeral(true)
                    .queue(hook->{
                        // 60초 후에 메시지 삭제
                        hook.deleteOriginal().queueAfter(60, java.util.concurrent.TimeUnit.SECONDS);
                    });
        }
    }
}
