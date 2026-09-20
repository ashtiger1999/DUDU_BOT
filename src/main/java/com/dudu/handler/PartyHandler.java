package com.dudu.handler;

import com.dudu.util.PartyEmbedBuilder;
import com.dudu.dao.MemberDAO;
import com.dudu.dao.PartyDAO;

import java.time.LocalDateTime;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.EmbedBuilder;

// 파티 처리 핸들러
public class PartyHandler {

        // InteractionHook를 사용하여 일정 시간 후 메시지를 삭제하는 유틸리티 메서드
        private static void deleteAfterDelay(
                        InteractionHook hook,
                        long delaySeconds) {

                hook.deleteOriginal()
                                .queueAfter(
                                                delaySeconds,
                                                java.util.concurrent.TimeUnit.SECONDS);
        }

        // 파티 참여 핸들러
        public void handleJoin(ButtonInteractionEvent event) {

                // 버튼 ID에서 파티 ID 추출
                String btnId = event.getComponentId();
                String[] parts = btnId.split(":");
                int partyId = Integer.parseInt(parts[2]);

                long discordId = event.getUser().getIdLong();

                // 신규 사용자 여부 확인
                boolean isNewUser = !MemberDAO.isUserExists(discordId);

                // 신규 사용자라면 소환사명 등록 모달 출력
                if (isNewUser) {

                        Modal modal = Modal.create("party:join:new:" + partyId, "소환사명 등록")
                                        .addActionRow(TextInput.create("gameNickname", "소환사명", TextInputStyle.SHORT)
                                                        .setPlaceholder("예: 두두두리#KR1").setRequired(true).build())
                                        .build();

                        event.replyModal(modal).queue();
                        return;
                }

                // 기존 회원의 파티 참여 처리
                handleExistingUserJoin(event, partyId, discordId);
        }

        // 기존 사용자의 파티 참여 처리
        public static void handleExistingUserJoin(
                        ButtonInteractionEvent event,
                        int partyId,
                        long discordId) {

                boolean joined = joinParty(partyId, discordId);

                if (!joined) {
                        event.reply("❌ 이미 파티에 참여한 상태입니다.")
                                        .setEphemeral(true)
                                        .queue(hook -> {
                                                // 60초 후 자동 삭제
                                                deleteAfterDelay(hook, 60);
                                        });
                        return;
                }

                // 내전/비내전 구분하여 임베드 생성
                EmbedBuilder embed;
                if (PartyDAO.getGameMode(partyId) != 3) {
                        embed = PartyEmbedBuilder.buildDefaultEmbed(
                                        event.getGuild(), partyId);
                } else {
                        embed = PartyEmbedBuilder.buildInHouseEmbed(
                                        event.getGuild(), partyId);
                }

                event.getMessage()
                                .editMessageEmbeds(embed.build())
                                .queue();

                event.reply("✅ 참여 완료")
                                .setEphemeral(true)
                                .queue(hook -> {
                                        // 60초 후 자동 삭제
                                        deleteAfterDelay(hook, 60);
                                });
        }

        // 신규 사용자의 파티 참여 처리 핸들러
        public void handleNewUserJoin(ModalInteractionEvent event) {

                String[] parts = event.getModalId().split(":");
                int partyId = Integer.parseInt(parts[3]);

                long discordId = event.getUser().getIdLong();

                String gameNickname = event.getValue("gameNickname").getAsString();

                if (!MemberHandler.isValidGameNickname(gameNickname)) {
                        event.reply("❌ 소환사명 형식이 올바르지 않습니다.")
                                        .setEphemeral(true)
                                        .queue(hook -> {
                                                // 60초 후 자동 삭제
                                                deleteAfterDelay(hook, 60);
                                        });
                        return;
                }

                MemberHandler.registerOrUpdateMember(
                                discordId,
                                gameNickname);

                boolean joined = joinParty(partyId, discordId);

                if (!joined) {
                        event.reply("❌ 이미 파티에 참여한 상태입니다.")
                                        .setEphemeral(true)
                                        .queue(hook -> {
                                                // 60초 후 자동 삭제
                                                deleteAfterDelay(hook, 60);
                                        });
                        return;
                }

                // 비내전/내전을 구분하여 결과 임베드를 생성
                EmbedBuilder embed;
                if (PartyDAO.getGameMode(partyId) != 3) {

                        embed = PartyEmbedBuilder.buildDefaultEmbed(
                                        event.getGuild(), partyId);
                } else {
                        embed = PartyEmbedBuilder.buildInHouseEmbed(
                                        event.getGuild(), partyId);
                }

                String messageId = PartyDAO.getMessageId(partyId);

                event.getChannel()
                                .editMessageEmbedsById(messageId, embed.build())
                                .queue();

                event.reply("✅ 소환사명 등록 및 파티 참여 완료")
                                .setEphemeral(true)
                                .queue(hook -> {
                                        // 60초 후 자동 삭제
                                        deleteAfterDelay(hook, 60);
                                });
        }

        // 파티 참여 처리
        public static boolean joinParty(int partyId, long discordId) {

                // 이미 참여했는지 확인
                if (PartyDAO.isMember(partyId, discordId)) {
                        return false;
                }

                // 파티 참여자 정보 저장
                PartyDAO.join(partyId, discordId);

                return true;
        }

        // 파티 참여 취소 핸들러
        public void handleLeave(ButtonInteractionEvent event) {

                // 버튼 ID에서 파티 ID와 Discord ID 추출
                String btnId = event.getComponentId();
                String[] parts = btnId.split(":");

                int partyId = Integer.parseInt(parts[2]);
                long discordId = event.getUser().getIdLong();

                // 내전일 경우 InHouseHandler를 통해 처리
                if (PartyDAO.getGameMode(partyId) == 3) {

                        InHouseHandler inHouseHandler = new InHouseHandler();
                        inHouseHandler.handleInHouseLeave(event);

                        return;
                }

                // 이미 파티에 참여하지 않은 상태인지 확인
                if (!PartyDAO.isMember(partyId, discordId)) {

                        event.reply("❌ 파티에 참여하지 않은 상태입니다.")
                                        .setEphemeral(true)
                                        .queue(hook -> {
                                                deleteAfterDelay(hook, 60);
                                        });

                        return;
                }

                // 파티에서 참여자 정보 삭제
                PartyDAO.leave(partyId, discordId);

                // 일반 파티 임베드 갱신
                EmbedBuilder embed = PartyEmbedBuilder.buildDefaultEmbed(
                                event.getGuild(),
                                partyId);

                event.getMessage()
                                .editMessageEmbeds(embed.build())
                                .queue();

                // 버튼 클릭 응답
                event.reply("참여 취소 완료")
                                .setEphemeral(true)
                                .queue(hook -> {
                                        deleteAfterDelay(hook, 60);
                                });
        }

        // 파티 강퇴 핸들러
        public void handleKick(ButtonInteractionEvent event) {

                // 버튼 ID에서 파티 ID와 Discord ID 추출
                String btnId = event.getComponentId();
                String[] parts = btnId.split(":");
                int partyId = Integer.parseInt(parts[2]);

                // 추방 대상을 입력받는 모달을 출력
                TextInput numberInput = TextInput.create("kickNumber", "강퇴할 참여자의 번호", TextInputStyle.SHORT)
                                .setPlaceholder("번호를 입력하세요").setRequired(true).build();

                Modal modal = Modal.create("party:kick:" + partyId, "강퇴할 참여자 선택")
                                .addActionRow(numberInput)
                                .build();
                event.replyModal(modal).queue();

                // 파티에서 참여자 정보 삭제는 모달에서 입력받은 번호를 처리한 후 수행해야 함
                // 따라서 여기서는 강퇴 로직을 바로 수행하지 않고 모달 응답을 기다림
        }

        // 파티 강퇴 모달 응답 핸들러
        public void handleKick(ModalInteractionEvent event) {

                // 모달 ID에서 파티 ID 추출
                String modalId = event.getModalId();
                String[] parts = modalId.split(":");
                int partyId = Integer.parseInt(parts[2]);

                // 입력된 강퇴할 참여자 번호 가져오기
                if (event.getValue("kickNumber") == null) {
                        event.reply("❌ 강퇴할 참여자의 번호를 입력해주세요.")
                                        .setEphemeral(true)
                                        .queue(hook -> {
                                                // 60초 후 자동 삭제
                                                deleteAfterDelay(hook, 60);
                                        });
                        return;
                }
                int kickNumber = Integer.parseInt(event.getValue("kickNumber").getAsString());

                // 강퇴할 참여자 번호 유효성 검증
                if (kickNumber <= 0 || kickNumber > PartyDAO.getParticipantCount(partyId)) {
                        event.reply("❌ 강퇴할 참여자의 번호가 올바르지 않습니다.")
                                        .setEphemeral(true)
                                        .queue(hook -> {
                                                // 60초 후 자동 삭제
                                                deleteAfterDelay(hook, 60);
                                        });
                        return;
                }

                // 파티에서 해당 번호의 참여자 정보 삭제
                PartyDAO.kick(partyId, kickNumber);

                // 임베드 업데이트 처리
                EmbedBuilder embed = PartyEmbedBuilder.buildDefaultEmbed(event.getGuild(), partyId);

                String messageId = PartyDAO.getMessageId(partyId);

                event.getChannel()
                                .editMessageEmbedsById(messageId, embed.build())
                                .queue();

                // 모달 응답
                event.reply("강퇴 완료")
                                .setEphemeral(true)
                                .queue(hook -> {
                                        // 60초 후 자동 삭제
                                        deleteAfterDelay(hook, 60);
                                });
        }

        // 파티 연장 핸들러
        public void handleExtend(ButtonInteractionEvent event) {

                // 버튼 ID에서 파티 ID 추출
                String btnId = event.getComponentId();
                String[] parts = btnId.split(":");
                int partyId = Integer.parseInt(parts[2]);

                // 파티 마감 시간 조회
                LocalDateTime deletedAt = PartyDAO.getDeletedAt(partyId);
                // 마감시간이 6시간 미만일 때만 연장
                if (deletedAt != null && deletedAt.isAfter(LocalDateTime.now().plusHours(6))) {
                        event.reply("파티 마감 시간이 6시간 이상 남아 있어 연장할 수 없습니다.")
                                        .setEphemeral(true)
                                        .queue(hook -> {
                                                // 60초 후 자동 삭제
                                                deleteAfterDelay(hook, 60);
                                        });
                        return;
                }

                // 파티 연장 로직 처리 (예: DB 업데이트)
                PartyDAO.extendParty(partyId);

                // 임베드 업데이트 처리
                EmbedBuilder embed = PartyEmbedBuilder.buildDefaultEmbed(event.getGuild(), partyId);
                event.getMessage().editMessageEmbeds(embed.build()).queue();

                // 버튼 클릭 응답
                event.reply("파티 연장 완료")
                                .setEphemeral(true)
                                .queue(hook -> {
                                        // 60초 후 자동 삭제
                                        deleteAfterDelay(hook, 60);
                                });
        }

        // 파티 생성 모달 응답 핸들러
        public void handleCreate(ModalInteractionEvent event) {

                // 모달 ID에서 파티 종류 추출
                String modalId = event.getModalId();
                String[] parts = modalId.split(":");
                String partyType = parts[2]; // k 또는 h 등 파티 종류
                int gameMode = Integer.parseInt(parts[3]); // 1 = 일반, 2 = 랭크, 3 = 내전

                int gameType;

                switch (partyType) {
                        case "k":
                                gameType = 1; // 예: k 파티는 게임 타입 1
                                break;
                        case "h":
                                gameType = 2; // 예: h 파티는 게임 타입 2
                                break;

                        default:
                                throw new IllegalArgumentException("Unknown party type: " + partyType);
                }

                long discordId = event.getUser().getIdLong();

                // 모달에서 소환사명 가져오기
                String gameNickname = "";

                if (event.getValue("gameNickname") != null) {
                        gameNickname = event.getValue("gameNickname").getAsString();
                }

                // LOL 닉네임 형식 검증
                if (!MemberHandler.isValidGameNickname(gameNickname)) {
                        event.reply("❌ 소환사명 형식이 올바르지 않습니다.\n예: 두두두리#KR1")
                                        .setEphemeral(true)
                                        .queue(hook -> {
                                                // 60초 후 자동 삭제
                                                deleteAfterDelay(hook, 60);
                                        });
                        return;
                }

                // 파티 비고 입력
                String description = "";

                if (event.getValue("description") != null) {
                        description = event.getValue("description").getAsString();
                }

                // 비고를 입력받지 않았다면 기본값으로 표시
                if (description.isEmpty()) {
                        description = "비고 없음";
                }

                // 사용자가 신규인지 여부와 상관없이 회원 정보를 등록 또는 업데이트
                MemberHandler.registerOrUpdateMember(
                                discordId,
                                gameNickname);

                // 파티 생성 요청 처리
                int partyId = PartyDAO.createParty(
                                discordId, description, gameType, gameMode);

                // 파티 생성자의 참여정보 저장
                PartyDAO.join(partyId, discordId);

                // 파티 생성 후 파티 정보 임베드 생성
                EmbedBuilder embed = PartyEmbedBuilder.buildDefaultEmbed(event.getGuild(), partyId);

                Button joinBtn = Button.primary(
                                "party:join:" + partyId,
                                "참여하기");

                Button leaveBtn = Button.danger(
                                "party:leave:" + partyId,
                                "참여취소");

                Button kickBtn = Button.danger(
                                "party:kick:" + partyId,
                                "추방하기");

                Button extendBtn = Button.secondary(
                                "party:extend:" + partyId,
                                "마감연장");

                // 임베드와 버튼을 함께 전송
                event.replyEmbeds(embed.build())
                                .addActionRow(joinBtn, leaveBtn, kickBtn, extendBtn)
                                .queue();

                // messageId 저장하기
                event.getHook()
                                .retrieveOriginal()
                                .queue(msg -> {

                                        PartyDAO.saveMessageId(
                                                        partyId,
                                                        msg.getId());
                                });
        }

        // 게임 모드 선택 버튼 클릭 핸들러
        public void handleGameModeSelection(ButtonInteractionEvent event) {
                String btnId = event.getComponentId();
                String[] parts = btnId.split(":");
                String partyType = parts[2]; // h
                int gameMode = Integer.parseInt(parts[3]); // 1 = 일반, 2 = 랭크, 3 = 내전
                long discordId = event.getUser().getIdLong();

                // 게임 모드 선택 버튼 삭제
                event.getMessage().delete().queue();

                // 사용자의 신규 여부 확인
                TextInput.Builder gameNicknameInput = createGameNicknameInput(discordId);

                // 파티 비고 입력 모달 띄우기
                TextInput descriptionInput = TextInput.create("description", "파티 비고 입력(선택)", TextInputStyle.SHORT)
                                .setPlaceholder("예: 골드 티어 구간 랭크게임입니다.")
                                .setMaxLength(200)
                                .setRequired(false)
                                .build();

                Modal modal = Modal.create(
                                "party:create:" + partyType + ":" + gameMode,
                                "파티 비고 입력")
                                .addActionRow(gameNicknameInput.build())
                                .addActionRow(descriptionInput)
                                .build();

                event.replyModal(modal).queue();
        }

        // 소환사명 입력 필드 생성
        public static TextInput.Builder createGameNicknameInput(long discordId) {

                boolean isNewUser = !MemberDAO.isUserExists(discordId);

                TextInput.Builder input = TextInput.create(
                                "gameNickname",
                                "소환사명 입력",
                                TextInputStyle.SHORT)
                                .setPlaceholder("예: 두두두리#KR1")
                                .setMaxLength(50)
                                .setRequired(isNewUser);

                if (!isNewUser) {
                        input.setValue(
                                        MemberDAO.getGameNickname(discordId));
                }

                return input;
        }
}