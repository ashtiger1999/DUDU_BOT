package com.dudu;

import com.dudu.dao.MemberDAO;
import com.dudu.util.ModalRouter;
import com.dudu.util.ButtonRouter;
import com.dudu.util.SelectRouter;

import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

// 봇 이벤트 리스너
public class BotListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        // 명령어 이름 가져오기
        String commandName = event.getName();

        // Discord 사용자 ID 가져오기
        long discordId = event.getUser().getIdLong();

        // 명령어별 분기
        switch (commandName) {
            case "증바람":
                handleKPartyCreateCommand(event, discordId);
                break;
            case "협곡":
                handleHPartyCreateCommand(event, discordId);
                break;
            case "아레나":
                event.reply("❌ 아레나 명령어는 아직 구현되지 않았습니다. 요청시 추후에 개발").setEphemeral(true).queue(hook -> {
                    // 180초 후에 버튼 메시지를 삭제
                    hook.deleteOriginal().queueAfter(180, TimeUnit.SECONDS);
                });
                break;
            case "내전생성":
                handleInHousePartyCreateCommand(event, discordId);
                break;
            case "소환사명변경":
                handleChangeSummonerNameCommand(event);
                break;
            default:
                event.reply("❌ 알 수 없는 명령어입니다.").setEphemeral(true).queue(hook -> {
                    // 180초 후에 버튼 메시지를 삭제
                    hook.deleteOriginal().queueAfter(180, TimeUnit.SECONDS);
                });
                break;
        }
    }

    // /증바람 명령어 처리
    private void handleKPartyCreateCommand(SlashCommandInteractionEvent event, long discordId) {
        // 사용자의 신규 여부 확인
        boolean isNewUser = !MemberDAO.isUserExists(discordId);

        // 기존 사용자라면 DB에 저장된 소환사명 조회
        String gameNickname = "";
        if (!isNewUser) {
            gameNickname = MemberDAO.getGameNickname(discordId);
        }

        // 소환사명 입력
        TextInput.Builder gameNicknameInput = TextInput.create(
                "gameNickname",
                "소환사명 입력",
                TextInputStyle.SHORT)
                .setPlaceholder("예: 두두두리#KR1")
                .setMaxLength(50)
                .setRequired(isNewUser);

        if (!isNewUser) {
            gameNicknameInput.setValue(gameNickname);
        }

        // 파티 비고 입력하기
        TextInput descriptionInput = TextInput.create("description", "파티 비고 입력(선택)", TextInputStyle.SHORT)
                .setPlaceholder("예: 즐겜용 파티입니다.")
                .setRequired(false)
                .setMaxLength(200)
                .build();

        Modal modal = Modal.create(
                "party:create:k:1",
                "파티 비고 입력")
                .addActionRow(gameNicknameInput.build())
                .addActionRow(descriptionInput)
                .build();

        event.replyModal(modal).queue();
    }

    // /협곡 명령어 처리
    private void handleHPartyCreateCommand(SlashCommandInteractionEvent event, long discordId) {

        // 게임 모드 선택 버튼
        Button gameModeBtn = Button.primary("party:gameMode:h:1", "일반 게임");
        Button rankedGameModeBtn = Button.primary("party:gameMode:h:2", "랭크 게임");

        event.reply("게임 모드를 선택하세요.").addActionRow(gameModeBtn, rankedGameModeBtn).setEphemeral(true).queue(hook -> {
            // 180초 후에 버튼 메시지를 삭제
            hook.deleteOriginal().queueAfter(180, TimeUnit.SECONDS);
        });
    }

    // /내전 생성 명령어 처리
    private void handleInHousePartyCreateCommand(SlashCommandInteractionEvent event, long discordId) {

        // 내전 게임 타입 선택(아수라장/협곡/!아레나-미구현)
        Button kGameModeBtn = Button.primary("inHouse:gameType:1", "아수라장");
        Button hGameModeBtn = Button.primary("inHouse:gameType:2", "협곡");

        event.reply("게임 모드를 선택하세요.")
                .addActionRow(kGameModeBtn, hGameModeBtn)
                .setEphemeral(true)
                .queue(hook -> {

                    // 180초 후 버튼 메시지 삭제
                    hook.deleteOriginal()
                            .queueAfter(
                                    180,
                                    TimeUnit.SECONDS,
                                    success -> {
                                        // 삭제 성공
                                    },
                                    error -> {
                                        // 이미 삭제된 메시지는 무시
                                    });
                });
    }

    // /소환사명변경 명령어 처리
    private void handleChangeSummonerNameCommand(
            SlashCommandInteractionEvent event) {

        // 모달 생성
        TextInput summonerNameInput = TextInput.create(
                "summonerName",
                "새 소환사 이름",
                TextInputStyle.SHORT)
                .setPlaceholder("예: 새로운소환사명#KR1")
                .setRequired(true)
                .setMaxLength(22)
                .build();

        Modal modal = Modal.create(
                "summonerName:change",
                "소환사 이름 변경")
                .addActionRow(summonerNameInput)
                .build();

        event.replyModal(modal).queue();
    }

    // 모달 분기 처리
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        new ModalRouter().route(event);
    }

    // 버튼 클릭 이벤트 분기 처리
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        new ButtonRouter().route(event);
    }

    // 선택 분기
    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        new SelectRouter().route(event);
    }

}