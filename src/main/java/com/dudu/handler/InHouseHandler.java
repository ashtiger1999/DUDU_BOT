package com.dudu.handler;

import com.dudu.dao.PartyDAO;
import com.dudu.dto.InHouseMatchDTO;
import com.dudu.dto.InHouseTeamDTO;
import com.dudu.util.PartyEmbedBuilder;
import com.dudu.dao.InHouseDAO;
import com.dudu.dao.InHouseMatchDAO;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class InHouseHandler {

        // InteractionHook를 사용하여 일정 시간 후 메시지를 삭제하는 유틸리티 메서드
        private void deleteAfterDelay(
                        InteractionHook hook,
                        long delaySeconds) {

                hook.deleteOriginal()
                                .queueAfter(
                                                delaySeconds,
                                                java.util.concurrent.TimeUnit.SECONDS);
        }

        // 내전 드래프트 정보를 담는 내부 클래스
        private static class InHouseDraft {

                String gameNickname;
                int maxPlayers;
                int teamCount;
                String description;
                long discordId;
                int gameType;

                public InHouseDraft(
                                String gameNickname,
                                int maxPlayers,
                                int teamCount,
                                String description,
                                long discordId,
                                int gameType) {

                        this.gameNickname = gameNickname;
                        this.maxPlayers = maxPlayers;
                        this.teamCount = teamCount;
                        this.description = description;
                        this.discordId = discordId;
                        this.gameType = gameType;
                }
        }

        // 내전 드래프트 정보를 저장하는 맵 (Discord ID를 키로 사용)
        private static final Map<Long, InHouseDraft> inHouseDrafts = new ConcurrentHashMap<>();

        // 내전 소유자 권한 확인 메서드
        private boolean checkInHouseOwner(
                        ButtonInteractionEvent event,
                        int partyId) {

                if (String.valueOf(PartyDAO.getOwnerId(partyId))
                                .equals(String.valueOf(event.getUser().getIdLong()))) {

                        return true;
                }

                event.reply("❌ 내전을 관리할 권한이 없습니다.")
                                .setEphemeral(true)
                                .queue(hook -> deleteAfterDelay(hook, 60));

                return false;
        }

        // 내전 게임 타입 선택 버튼 클릭 핸들러
        public void handleInHouseGameTypeSelection(ButtonInteractionEvent event) {
                String btnId = event.getComponentId();
                String[] parts = btnId.split(":");
                int gameType = Integer.parseInt(parts[2]); // 1 = 아수라장, 2 = 협곡
                long discordId = event.getUser().getIdLong();

                // 내전 게임 타입 선택 버튼 삭제
                event.getMessage().delete().queue();

                // 사용자의 신규 여부 확인
                TextInput.Builder gameNicknameInput = PartyHandler.createGameNicknameInput(discordId);

                // 내전 참여 인원 입력
                TextInput maxPlayersInput = TextInput.create("maxPlayers", "내전 참여 인원", TextInputStyle.SHORT)
                                .setPlaceholder("예: 10")
                                .setRequired(true)
                                .setMaxLength(3)
                                .build();

                // 내전 팀 수 입력
                TextInput teamCountInput = TextInput.create("teamCount", "내전 팀 수", TextInputStyle.SHORT)
                                .setPlaceholder("예: 2")
                                .setRequired(true)
                                .setMaxLength(3)
                                .build();

                // 파티 비고 입력
                TextInput descriptionInput = TextInput.create("description", "파티 비고 입력(선택)", TextInputStyle.SHORT)
                                .setPlaceholder("예: 자유롭게 참여 가능")
                                .setRequired(false)
                                .setMaxLength(200)
                                .build();

                Modal modal = Modal.create(
                                "inHousePartyCreate:" + gameType,
                                "내전 생성")
                                .addActionRow(gameNicknameInput.build())
                                .addActionRow(maxPlayersInput)
                                .addActionRow(teamCountInput)
                                .addActionRow(descriptionInput)
                                .build();

                event.replyModal(modal).queue();
        }

        // 내전 시작 방식 선택 버튼 핸들러
        public void handleInHouseStartAt(ButtonInteractionEvent event) {
                String btnId = event.getComponentId();
                String[] parts = btnId.split(":");
                String startType = parts[2]; // now = 바로 시작, time = 시작시간 입력

                if (startType.equals("now")) {
                        // 바로 시작 처리
                        long discordId = event.getUser().getIdLong();
                        InHouseDraft draft = inHouseDrafts.get(discordId);

                        if (draft == null) {
                                event.reply("내전 생성 정보가 만료되었습니다. 다시 생성해주세요.")
                                                .setEphemeral(true)
                                                .queue(hook -> {
                                                        // 60초 후 자동 삭제
                                                        deleteAfterDelay(hook, 60);
                                                });
                                return;
                        }

                        LocalDateTime startAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

                        // 내전 생성 요청 및 partyId 반환
                        int partyId = createInHouse(draft, startAt);
                        // 생성 실패 처리
                        if (partyId < 0) {

                                event.reply(
                                                "내전 생성에 실패했습니다.")
                                                .setEphemeral(true)
                                                .queue(hook -> {
                                                        // 60초 후 자동 삭제
                                                        deleteAfterDelay(hook, 60);
                                                });

                                return;
                        }
                        // 생성 성공 알림
                        event.reply("내전이 생성되었습니다.")
                                        .setEphemeral(true)
                                        .queue(hook -> {
                                                // 60초 후 자동 삭제
                                                deleteAfterDelay(hook, 60);
                                        });

                        // 내전 모집 임베드 전송
                        sendInHouseEmbed(
                                        event.getChannel(),
                                        event.getGuild(),
                                        partyId);

                } else if (startType.equals("time")) {
                        // 시작 시간 입력 처리
                        ZoneId koreaZone = ZoneId.of("Asia/Seoul"); // 한국 시간대 설정
                        LocalDateTime now = LocalDateTime.now(koreaZone); // 현재 한국 시간 가져오기

                        // 현재 날짜와 시간을 문자열로 포맷팅
                        String currentDate = now.format(
                                        DateTimeFormatter.ofPattern("yyyyMMdd"));
                        String currentTime = now.format(
                                        DateTimeFormatter.ofPattern("HHmm"));

                        // 현재 시간을 시(hour)와 분(minute)으로 분리
                        int hour = Integer.parseInt(
                                        currentTime.substring(0, 2));
                        int minute = Integer.parseInt(
                                        currentTime.substring(2, 4));

                        // 12시간제 표시를 위한 오전/오후 계산
                        String period = hour < 12 ? "오전" : "오후";
                        int displayHour = hour % 12;
                        if (displayHour == 0) {
                                displayHour = 12;
                        }

                        String timePlaceholder = String.format(
                                        "%s (%s %d시 %02d분)",
                                        currentTime,
                                        period,
                                        displayHour,
                                        minute);

                        Modal dateModal = Modal.create(
                                        "inHouse:datetime",
                                        "내전 시작 일시")
                                        .addActionRow(
                                                        TextInput.create(
                                                                        "startDate",
                                                                        "시작 날짜",
                                                                        TextInputStyle.SHORT)
                                                                        .setPlaceholder(currentDate)
                                                                        .setValue(currentDate)
                                                                        .setMinLength(8)
                                                                        .setMaxLength(8)
                                                                        .setRequired(true)
                                                                        .build())
                                        .addActionRow(
                                                        TextInput.create(
                                                                        "startTime",
                                                                        "시작 시간",
                                                                        TextInputStyle.SHORT)
                                                                        .setPlaceholder(timePlaceholder)
                                                                        .setValue(currentTime)
                                                                        .setMinLength(4)
                                                                        .setMaxLength(4)
                                                                        .setRequired(true)
                                                                        .build())
                                        .build();

                        event.replyModal(dateModal).queue();
                }
        }

        // 내전 시작 일시 처리
        public void handleInHouseDateTime(ModalInteractionEvent event) {

                long discordId = event.getUser().getIdLong();

                // 임시 저장된 내전 생성 정보 가져오기
                InHouseDraft draft = inHouseDrafts.get(discordId);

                if (draft == null) {
                        event.reply("내전 생성 정보가 만료되었습니다. 다시 생성해주세요.")
                                        .setEphemeral(true)
                                        .queue(hook -> {
                                                // 60초 후 자동 삭제
                                                deleteAfterDelay(hook, 60);
                                        });
                        return;
                }

                String startDate = event.getValue("startDate").getAsString();
                String startTime = event.getValue("startTime").getAsString();

                try {
                        // 날짜 + 시간을 하나로 합침
                        String dateTimeString = startDate + startTime;

                        // yyyyMMddHHmm → LocalDateTime
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

                        LocalDateTime startAt = LocalDateTime.parse(dateTimeString, formatter);

                        // 한국 시간 기준 현재 시간
                        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

                        // 과거 시간 입력 방지
                        if (startAt.isBefore(now)) {
                                event.reply("내전 시작 시간은 현재 시간 이후로 입력해주세요.")
                                                .setEphemeral(true)
                                                .queue(hook -> {
                                                        // 60초 후 자동 삭제
                                                        deleteAfterDelay(hook, 60);
                                                });
                                return;
                        }

                        // 내전 생성 요청 및 partyId 반환
                        int partyId = createInHouse(draft, startAt);

                        // 생성 실패 처리
                        if (partyId < 0) {
                                event.reply("내전 생성에 실패했습니다.")
                                                .setEphemeral(true)
                                                .queue(hook -> {
                                                        // 60초 후 자동 삭제
                                                        deleteAfterDelay(hook, 60);
                                                });
                                return;
                        }

                        // 생성 성공 알림
                        event.reply("내전이 생성되었습니다.")
                                        .setEphemeral(true)
                                        .queue(hook -> {
                                                // 60초 후 자동 삭제
                                                deleteAfterDelay(hook, 60);
                                        });

                        // 내전 모집 임베드 전송
                        sendInHouseEmbed(
                                        event.getChannel(),
                                        event.getGuild(),
                                        partyId);

                } catch (DateTimeParseException e) {
                        event.reply(
                                        "날짜 또는 시간 형식이 올바르지 않습니다.\n"
                                                        + "날짜: 20260917\n"
                                                        + "시간: 1340")
                                        .setEphemeral(true)
                                        .queue(hook -> {
                                                // 60초 후 자동 삭제
                                                deleteAfterDelay(hook, 60);
                                        });
                }
        }

        // 내전 생성 처리
        private int createInHouse(
                        InHouseDraft draft,
                        LocalDateTime startedAt) {

                // 1. 회원 정보 등록 또는 소환사명 갱신
                MemberHandler.registerOrUpdateMember(
                                draft.discordId,
                                draft.gameNickname);

                // 2. party 테이블 생성
                if (draft.description.isEmpty()) {
                        // description이 비어있으면 기본값 설정
                        draft.description = "비고 없음";
                }
                int partyId = PartyDAO.createParty(
                                draft.discordId,
                                draft.description,
                                draft.gameType,
                                3);

                if (partyId == -1) {
                        return -1;
                }

                // 3. in_house 테이블 생성
                boolean created = InHouseDAO.createInHouseParty(
                                partyId,
                                startedAt,
                                draft.maxPlayers,
                                draft.teamCount);

                if (!created) {
                        InHouseDAO.deleteInHouseParty(partyId);
                        return -2;
                }

                // 3-2. party 정보에서 마감시간을 시작시간으로부터 6시간 후로 설정
                boolean deadlineUpdated = InHouseDAO.updateDeadline(
                                partyId,
                                startedAt.plusHours(6));

                if (!deadlineUpdated) {
                        InHouseDAO.deleteInHouseParty(partyId);
                        return -4;
                }

                // 4. 생성자 자동 참여
                boolean joined = PartyDAO.join(
                                partyId,
                                draft.discordId);

                if (!joined) {
                        InHouseDAO.deleteInHouseParty(partyId);
                        return -3;
                }

                // 5. 내전 팀 생성
                boolean teamsCreated = InHouseDAO.createInHouseTeams(partyId, draft.teamCount);

                if (!teamsCreated) {
                        InHouseDAO.deleteInHouseParty(partyId);
                        return -5;
                }

                // 6. 임시 데이터 삭제
                inHouseDrafts.remove(draft.discordId);

                return partyId;
        }

        // 내전 생성 모달 처리 핸들러
        public void handleInHouseCreate(ModalInteractionEvent event) {

                String modalId = event.getModalId();
                String[] parts = modalId.split(":");
                int gameType = Integer.parseInt(parts[1]); // 1 = 아수라장, 2 = 협곡

                String gameNickname = event.getValue("gameNickname").getAsString();

                if (!MemberHandler.isValidGameNickname(gameNickname)) {
                        event.reply("❌ 소환사명 형식이 올바르지 않습니다.\n예: 두두두리#KR1")
                                        .setEphemeral(true)
                                        .queue(hook -> {
                                                // 60초 후 자동 삭제
                                                deleteAfterDelay(hook, 60);
                                        });
                        return;
                }

                int maxPlayers = Integer.parseInt(
                                event.getValue("maxPlayers").getAsString());

                int teamCount = Integer.parseInt(
                                event.getValue("teamCount").getAsString());

                String description = event.getValue("description") != null
                                ? event.getValue("description").getAsString()
                                : "비고 없음";

                long discordId = event.getUser().getIdLong();

                InHouseDraft draft = new InHouseDraft(
                                gameNickname,
                                maxPlayers,
                                teamCount,
                                description,
                                discordId,
                                gameType);

                inHouseDrafts.put(discordId, draft);

                Button nowButton = Button.primary(
                                "inHouse:startAt:now",
                                "바로 시작");

                Button timeButton = Button.secondary(
                                "inHouse:startAt:time",
                                "시작시간 입력");

                event.reply("내전 시작 방식을 선택해주세요.")
                                .addActionRow(nowButton, timeButton)
                                .setEphemeral(true)
                                .queue(hook -> {
                                        // 60초 후 자동 삭제
                                        deleteAfterDelay(hook, 60);
                                });
        }

        // 내전 모집 임베드 전송
        private void sendInHouseEmbed(
                        MessageChannel channel,
                        Guild guild,
                        int partyId) {

                EmbedBuilder embed = PartyEmbedBuilder.buildInHouseEmbed(
                                guild,
                                partyId);

                // 조건에 따른 버튼 생성
                List<ActionRow> buttonRows = createInHouseButtons(
                                guild,
                                partyId);

                // 기존 내전 정보 임베드 메시지 ID 조회
                String messageId = PartyDAO.getMessageId(partyId);

                // 기존 메시지가 없는 경우
                if (messageId == null) {
                        channel.sendMessageEmbeds(embed.build()).setComponents(buttonRows).queue(message -> {
                                // 생성된 메시지 ID 저장
                                PartyDAO.saveMessageId(
                                                partyId,
                                                String.valueOf(message.getIdLong()),
                                                message.getChannel().getId()
                                        );
                        });
                        return;
                }

                // 기존 내전 정보 임베드 수정
                channel.retrieveMessageById(Long.parseLong(messageId))
                                .queue(message -> {
                                        message.editMessageEmbeds(embed.build()).setComponents(buttonRows).queue();
                                }, error -> {
                                        // 기존 메시지가 삭제된 경우
                                        channel.sendMessageEmbeds(embed.build()).setComponents(buttonRows)
                                                        .queue(newMessage -> {// 새 메시지 ID 저장
                                                                PartyDAO.saveMessageId(partyId,
                                                                                String.valueOf(newMessage.getIdLong()),
                                                                                newMessage.getChannel().getId()
                                                                        );
                                                        });
                                });
        }

        // 내전 참여/관리 버튼 생성
        private List<ActionRow> createInHouseButtons(
                        Guild guild,
                        int partyId) {

                List<Button> buttons = new ArrayList<>();

                // 내전 상태 조회
                int status = InHouseDAO.getInHousePartyStatus(partyId);

                // 모집중일 때만 참여 관련 버튼 표시
                if (status == 0) {
                        buttons.add(Button.primary(
                                        "party:join:" + partyId,
                                        "참여하기"));
                }

                // 내전 시작/종료 상태가 아니라면 팀 관련 버튼 표시
                if (status == 1 || status == 0) {
                        buttons.add(Button.secondary(
                                        "inHouse:joinTeam:" + partyId,
                                        "팀에 참여"));

                        buttons.add(Button.danger(
                                        "inHouse:leaveTeam:" + partyId,
                                        "팀 나가기"));
                }

                buttons.add(Button.danger(
                                "party:leave:" + partyId,
                                "참여취소"));

                buttons.add(Button.success(
                                "inHouse:start:" + partyId,
                                "내전 시작"));

                // 생성자 전용 버튼
                buttons.add(Button.secondary(
                                "inHouse:manage:open:" + partyId,
                                "내전 관리"));

                // ActionRow로 분리
                List<ActionRow> rows = new ArrayList<>();

                // 첫 번째 줄: 참여 관련 버튼 (최대 4개)
                if (!buttons.isEmpty()) {

                        int firstRowEnd = Math.min(4, buttons.size());

                        rows.add(ActionRow.of(
                                        buttons.subList(0, firstRowEnd)));
                }

                // 두 번째 줄: 내전 시작 + 내전 관리
                if (buttons.size() > 4) {

                        rows.add(ActionRow.of(
                                        buttons.subList(4, buttons.size())));
                }

                return rows;
        }

        // 내전 관리 버튼 처리 메서드
        public void handleInHouseManage(ButtonInteractionEvent event) {
                String btnId = event.getComponentId();
                String[] parts = btnId.split(":");
                int partyId = Integer.parseInt(parts[3]);

                long ownerId = PartyDAO.getOwnerId(partyId);

                if (event.getUser().getIdLong() != ownerId) {
                        event.reply("❌ 내전 생성자만 관리할 수 있습니다.")
                                        .setEphemeral(true)
                                        .queue(hook -> {
                                                // 60초 후 자동 삭제
                                                deleteAfterDelay(hook, 60);
                                        });
                        return;
                }

                // 내전 관리 버튼 생성
                ActionRow row1 = ActionRow.of(
                                Button.primary("inHouse:manage:status:" + partyId, "상태 변경"),

                                Button.primary("inHouse:manage:startTime:" + partyId, "시작시간 변경"),

                                Button.primary("party:extend:" + partyId, "마감시간 연장"));

                ActionRow row2 = ActionRow.of(
                                /*
                                 * Button.secondary("inHouse:manage:move:" + partyId, "팀원 이동"),
                                 * 
                                 * Button.secondary("inHouse:manage:expel:" + partyId, "팀원 내보내기"),
                                 */

                                Button.danger("inHouse:manage:kick:" + partyId, "추방하기"),

                                Button.danger("inHouse:manage:cancel:" + partyId, "내전 취소"));

                // 내전 관리 메뉴 표시
                event.reply("⚙️ **내전 관리 메뉴**\n"
                                + "관리할 내전 ID: `" + partyId + "`")
                                .addComponents(row1, row2)
                                .setEphemeral(true)
                                .queue();
        }

        // 내전 취소 버튼
        public void handleInHouseManageCancel(ButtonInteractionEvent event) {
                String btnId = event.getComponentId();
                String[] parts = btnId.split(":");
                int partyId = Integer.parseInt(parts[3]);

                long ownerId = PartyDAO.getOwnerId(partyId);

                if (event.getUser().getIdLong() != ownerId) {
                        event.reply("❌ 내전 생성자만 취소할 수 있습니다.")
                                        .setEphemeral(true)
                                        .queue(hook -> {
                                                // 60초 후 자동 삭제
                                                deleteAfterDelay(hook, 60);
                                        });
                        return;
                }

                // 내전 취소 처리 로직
                PartyDAO.cancelParty(partyId);

                event.reply("⚠️ 내전이 취소되었습니다.")
                                .setEphemeral(true)
                                .queue(hook -> {
                                        // 60초 후 자동 삭제
                                        deleteAfterDelay(hook, 60);
                                });
        }

        // 내전 상태 변경 버튼
        public void handleInHouseManageStatus(ButtonInteractionEvent event) {
                String btnId = event.getComponentId();
                String[] parts = btnId.split(":");
                int partyId = Integer.parseInt(parts[3]);

                long ownerId = PartyDAO.getOwnerId(partyId);

                if (event.getUser().getIdLong() != ownerId) {
                        event.reply("❌ 내전 생성자만 상태를 변경할 수 있습니다.")
                                        .setEphemeral(true)
                                        .queue(hook -> {
                                                // 60초 후 자동 삭제
                                                deleteAfterDelay(hook, 60);
                                        });
                        return;
                }

                // 현재 상태 조회
                int currentStatus = InHouseDAO.getInHousePartyStatus(partyId);

                // 내전 상태 변경 메뉴 생성
                StringSelectMenu.Builder menu = StringSelectMenu.create(
                                "inHouse:status:" + partyId)
                                .setPlaceholder("변경할 상태를 선택하세요.");

                if (currentStatus != 0) {
                        menu.addOption("모집중", "0", "참여자를 모집합니다.");
                }

                if (currentStatus != 1) {
                        menu.addOption("모집마감", "1", "참여자 모집을 마감합니다.");
                }

                if (currentStatus != 2) {
                        menu.addOption("진행중", "2", "내전을 시작합니다.");
                }

                if (currentStatus != 3) {
                        menu.addOption("종료됨", "3", "내전을 종료합니다.");
                }

                event.reply("변경할 내전 상태를 선택하세요.")
                                .setEphemeral(true)
                                .addActionRow(menu.build())
                                .queue(hook -> deleteAfterDelay(hook, 180));
        }

        // 내전 상태 선택 처리
        public void handleInHouseStatusSelect(StringSelectInteractionEvent event) {
                String[] parts = event.getComponentId().split(":");
                int partyId = Integer.parseInt(parts[2]);

                long ownerId = PartyDAO.getOwnerId(partyId);

                if (event.getUser().getIdLong() != ownerId) {
                        event.reply("❌ 내전 생성자만 상태를 변경할 수 있습니다.")
                                        .setEphemeral(true)
                                        .queue(hook -> deleteAfterDelay(hook, 60));
                        return;
                }

                int newStatus = Integer.parseInt(event.getValues().get(0));

                InHouseDAO.updateInHousePartyStatus(partyId, newStatus);

                String statusText = switch (newStatus) {
                        case 0 -> "모집중";
                        case 1 -> "모집마감";
                        case 2 -> "진행중";
                        case 3 -> "종료됨";
                        default -> "알 수 없음";
                };

                // 임베드 메시지 갱신
                sendInHouseEmbed(
                                event.getChannel(),
                                event.getGuild(),
                                partyId);

                event.reply("⚙️ 내전 상태가 **" + statusText + "**으로 변경되었습니다.")
                                .setEphemeral(true)
                                .queue(hook -> deleteAfterDelay(hook, 60));
        }

        // 내전 참여 취소 처리
        public void handleInHouseLeave(ButtonInteractionEvent event) {

                String[] parts = event.getComponentId().split(":");
                int partyId = Integer.parseInt(parts[2]);

                long userId = event.getUser().getIdLong();

                // 팀 소속 여부를 먼저 확인
                boolean wasInTeam = InHouseDAO.isUserInInHouseTeam(
                                partyId,
                                userId);

                // 사용자가 팀에 소속되어있다면 팀에서 나가기
                if (wasInTeam) {

                        InHouseDAO.leaveInHouseTeam(
                                        partyId,
                                        userId);
                }

                // 내전 참여자 정보 삭제
                PartyDAO.leave(
                                partyId,
                                userId);

                // 내전 정보 임베드 갱신
                sendInHouseEmbed(
                                event.getChannel(),
                                event.getGuild(),
                                partyId);

                // 기존에 팀에 소속되어 있었다면 팀 정보 임베드 갱신
                if (wasInTeam) {

                        updateInHouseTeamEmbed(
                                        event.getChannel(),
                                        event.getGuild(),
                                        partyId);
                }
        }

        // 사용자가 팀에 참여하거나 변경했을 때 처리
        public void handleInHouseJoinTeam(ButtonInteractionEvent event) {

                String[] parts = event.getComponentId().split(":");
                int partyId = Integer.parseInt(parts[2]);

                long userId = event.getUser().getIdLong();

                // 내전에 참여하지 않은 사용자는 팀에 참여할 수 없음
                if (!PartyDAO.isMember(partyId, userId)) {

                        event.reply("❌ 먼저 내전에 참여해주세요.")
                                        .setEphemeral(true)
                                        .queue(hook -> deleteAfterDelay(hook, 60));

                        return;
                }

                int teamCount = InHouseDAO.getInHouseTeamCount(partyId);

                StringSelectMenu.Builder menu = StringSelectMenu
                                .create("inHouse:joinTeam:" + partyId)
                                .setPlaceholder("팀을 선택하세요");

                for (int i = 0; i < teamCount; i++) {

                        String teamName = String.valueOf((char) ('A' + i));

                        menu.addOption(
                                        teamName,
                                        String.valueOf(i + 1));
                }

                event.reply("참여할 팀을 선택하세요.")
                                .setEphemeral(true)
                                .addActionRow(menu.build())
                                .queue(hook -> deleteAfterDelay(hook, 180));
        }

        // 사용자가 팀 선택 메뉴에서 팀을 선택했을 때 처리하는 핸들러
        public void handleInHouseJoinTeamSelect(StringSelectInteractionEvent event) {

                String[] parts = event.getComponentId().split(":");
                int partyId = Integer.parseInt(parts[2]);

                long userId = event.getUser().getIdLong();

                // 내전에 참여하지 않은 사용자는 팀에 참여할 수 없음
                if (!PartyDAO.isMember(partyId, userId)) {

                        event.reply("❌ 먼저 내전에 참여해주세요.")
                                        .setEphemeral(true)
                                        .queue(hook -> deleteAfterDelay(hook, 60));

                        return;
                }

                int teamNumber = Integer.parseInt(event.getValues().get(0));

                int teamId = InHouseDAO.getTeamIdByPartyIdAndTeamName(
                                partyId,
                                String.valueOf((char) ('A' + teamNumber - 1)));

                boolean success;

                // 이미 팀에 소속되어 있는 경우
                if (InHouseDAO.isUserInInHouseTeam(partyId, userId)) {

                        success = InHouseDAO.changeUserInHouseTeam(
                                        partyId,
                                        userId,
                                        teamId);

                } else {

                        // 신규 팀 참여
                        success = InHouseDAO.joinUserToInHouseTeam(
                                        partyId,
                                        userId,
                                        teamId);
                }

                String message = success
                                ? "✅ " + (char) ('A' + teamNumber - 1) + "팀에 참여하였습니다."
                                : "❌ 팀 참여 처리에 실패했습니다.";

                event.reply(message)
                                .setEphemeral(true)
                                .queue(hook -> {

                                        deleteAfterDelay(hook, 60);

                                        // 참여 처리 성공 시 기존 임베드 갱신
                                        if (success) {

                                                updateInHouseTeamEmbed(
                                                                event.getChannel(),
                                                                event.getGuild(),
                                                                partyId);
                                        }
                                });
        }

        // 팀 나가기 버튼
        public void handleInHouseLeaveTeam(ButtonInteractionEvent event) {

                String[] parts = event.getComponentId().split(":");
                int partyId = Integer.parseInt(parts[2]);

                long userId = event.getUser().getIdLong();

                boolean success = InHouseDAO.leaveInHouseTeam(
                                partyId,
                                userId);

                if (!InHouseDAO.isUserInInHouseTeam(partyId, userId)) {
                        event.reply("❌ 현재 팀에 소속되어 있지 않습니다.")
                                        .setEphemeral(true)
                                        .queue(hook -> deleteAfterDelay(hook, 60));
                        return;
                }

                String message = success
                                ? "✅ 팀에서 나갔습니다."
                                : "❌ 팀 나가기 처리에 실패했습니다.";

                event.reply(message)
                                .setEphemeral(true)
                                .queue(hook -> {

                                        deleteAfterDelay(hook, 60);

                                        // 나가기 성공 시 기존 임베드 갱신
                                        if (success) {

                                                updateInHouseTeamEmbed(
                                                                event.getChannel(),
                                                                event.getGuild(),
                                                                partyId);
                                        }
                                });
        }

        // 내전 팀 정보 임베드 업데이트
        private void updateInHouseTeamEmbed(
                        MessageChannel channel,
                        Guild guild,
                        int partyId) {

                EmbedBuilder embed = PartyEmbedBuilder.buildInHouseTeamEmbed(
                                guild,
                                partyId);

                // 팀 정보 임베드 버튼
                ActionRow buttonRow = ActionRow.of(
                                Button.secondary(
                                                "inHouse:joinTeam:" + partyId,
                                                "팀에 참여"),

                                Button.danger(
                                                "inHouse:leaveTeam:" + partyId,
                                                "팀 나가기"),

                                Button.danger(
                                                "party:leave:" + partyId,
                                                "내전 참여 취소"),

                                Button.success(
                                                "inHouse:start:" + partyId,
                                                "내전 시작"));

                Long messageId = InHouseDAO.getTeamMessageId(partyId);

                // 팀 정보 임베드가 아직 없는 경우
                if (messageId == null) {

                        channel.sendMessageEmbeds(embed.build())
                                        .setComponents(buttonRow)
                                        .queue(message -> {
                                                // 생성된 메시지 ID 저장
                                                InHouseDAO.updateTeamMessageId(partyId, message.getIdLong());
                                        });
                        return;
                }

                // 기존 팀 정보 임베드 수정
                channel.retrieveMessageById(messageId)
                                .queue(message -> {
                                        message.editMessageEmbeds(embed.build()).setComponents(buttonRow).queue();
                                }, error -> {
                                        // 기존 메시지가 삭제된 경우
                                        channel.sendMessageEmbeds(embed.build()).setComponents(buttonRow)
                                                        .queue(newMessage -> {
                                                                InHouseDAO.updateTeamMessageId(partyId,
                                                                                newMessage.getIdLong());
                                                        });
                                });
        }

        // 내전 시작 버튼 처리
        public void handleInHouseStart(ButtonInteractionEvent event) {

                String[] parts = event.getComponentId().split(":");
                int partyId = Integer.parseInt(parts[2]);

                // 내전 생성자인지 확인
                if (!checkInHouseOwner(event, partyId)) {
                        return;
                }

                // 이미 시작된 내전인지 확인
                int status = InHouseDAO.getInHousePartyStatus(partyId);

                if (status != 0) {

                        event.reply("❌ 이미 시작되었거나 종료된 내전입니다.")
                                        .setEphemeral(true)
                                        .queue(hook -> deleteAfterDelay(hook, 60));

                        return;
                }

                // 파티 ID로 내전 ID 조회
                int inHouseId = InHouseDAO.getInHouseIdByPartyId(partyId);

                // 내전 팀 목록 조회
                List<InHouseTeamDTO> teams = InHouseDAO.getTeamsByPartyId(partyId);
                // 최소 2팀 필요
                if (teams.size() < 2) {

                        event.reply("❌ 내전을 시작하려면 최소 2개의 팀이 필요합니다.").setEphemeral(true)
                                        .queue(hook -> deleteAfterDelay(hook, 60));
                        return;
                }

                // 내전 상태를 진행 중으로 변경
                boolean success = InHouseDAO.updateInHousePartyStatus(
                                partyId,
                                2);

                if (!success) {

                        event.reply("❌ 내전 시작 처리에 실패했습니다.")
                                        .setEphemeral(true)
                                        .queue(hook -> deleteAfterDelay(hook, 60));

                        return;
                }

                // 내전 정보 임베드 갱신
                sendInHouseEmbed(
                                event.getChannel(),
                                event.getGuild(),
                                partyId);

                // 팀 정보 임베드 갱신
                updateInHouseTeamEmbed(
                                event.getChannel(),
                                event.getGuild(),
                                partyId);

                // 기존 매치 초기화
                InHouseMatchDAO.deleteMatchesByInHouseId(inHouseId);

                // 팀 목록을 랜덤하게 섞음
                Collections.shuffle(teams);

                // 1라운드 매치 생성
                for (int i = 0; i + 1 < teams.size(); i += 2) {

                        InHouseTeamDTO team1 = teams.get(i);
                        InHouseTeamDTO team2 = teams.get(i + 1);

                        InHouseMatchDTO match = new InHouseMatchDTO();

                        match.setInHouseId(inHouseId);
                        match.setRoundNumber(1);
                        match.setTeam1Id(team1.getId());
                        match.setTeam2Id(team2.getId());

                        InHouseMatchDAO.insertMatch(match);
                }

                // 내전 매치 임베드를 출력
                updateInHouseMatchEmbed(
                                event.getChannel(),
                                event.getGuild(),
                                partyId);

                event.reply("✅ 내전이 시작되었습니다.")
                                .setEphemeral(true)
                                .queue(hook -> deleteAfterDelay(hook, 60));
        }

        // 내전 매치 정보 임베드를 갱신하는 메서드
        private void updateInHouseMatchEmbed(
                        MessageChannel channel,
                        Guild guild,
                        int partyId) {

                EmbedBuilder embed = PartyEmbedBuilder.buildInHouseMatchEmbed(
                                guild,
                                partyId);

                // 인하우스 매치 버튼 생성
                int inHouseId = InHouseDAO.getInHouseIdByPartyId(partyId);
                List<ActionRow> buttonRows = createInHouseMatchButtons(inHouseId);

                Long messageId = InHouseDAO.getMatchMessageId(partyId);

                // 매치 정보 임베드가 아직 없는 경우
                if (messageId == null) {

                        channel.sendMessageEmbeds(
                                        embed.build()).setComponents(buttonRows)
                                        .queue(message -> {
                                                InHouseDAO.updateMatchMessageId(partyId, message.getIdLong());
                                        });
                        return;
                }

                // 기존 매치 정보 임베드 수정
                channel.retrieveMessageById(messageId)
                                .queue(message -> {
                                        message.editMessageEmbeds(embed.build()).setComponents(buttonRows).queue();
                                }, error -> {
                                        // 기존 메시지가 삭제된 경우
                                        channel.sendMessageEmbeds(embed.build()).setComponents(buttonRows)
                                                        .queue(newMessage -> {
                                                                InHouseDAO.updateMatchMessageId(partyId,
                                                                                newMessage.getIdLong());
                                                        });
                                });
        }

        // 인하우스 매치 버튼 생성 메서드
        private List<ActionRow> createInHouseMatchButtons(
                        int inHouseId) {

                List<ActionRow> rows = new ArrayList<>();

                // 매치 목록 조회
                List<InHouseMatchDTO> matches = InHouseMatchDAO.getMatchesByInHouseId(
                                inHouseId);

                for (InHouseMatchDTO match : matches) {

                        // 이미 승자가 결정된 경기는 버튼 생성하지 않음
                        if (match.getWinnerTeamId() != 0) {
                                continue;
                        }

                        // 팀 이름 조회
                        String team1Name = InHouseDAO.getTeamNameByTeamId(
                                        match.getTeam1Id());

                        String team2Name = InHouseDAO.getTeamNameByTeamId(
                                        match.getTeam2Id());

                        Button team1Button = Button.success(
                                        "inHouse:setWin:"
                                                        + match.getId()
                                                        + ":"
                                                        + match.getTeam1Id(),
                                        team1Name + " 세트 승");

                        Button team2Button = Button.success(
                                        "inHouse:setWin:"
                                                        + match.getId()
                                                        + ":"
                                                        + match.getTeam2Id(),
                                        team2Name + " 세트 승");

                        rows.add(
                                        ActionRow.of(
                                                        team1Button,
                                                        team2Button));
                }

                Button decreaseScoreButton = Button.secondary("inHouse:decreaseScore:" + inHouseId, "세트 승 취소");
                Button nextMatchButton = Button.primary("inHouse:nextMatch:" + inHouseId, "매치 종료");

                rows.add(ActionRow.of(decreaseScoreButton, nextMatchButton));

                return rows;
        }

        // 세트 승리 처리 핸들러
        public void handleSetWin(ButtonInteractionEvent event) {

                String[] parts = event.getComponentId().split(":");

                int matchId = Integer.parseInt(parts[2]);
                int teamId = Integer.parseInt(parts[3]);

                // 내전 생성자인지 확인
                int partyId = InHouseDAO.getPartyIdByInHouseId(InHouseMatchDAO.getInHouseIdByMatchId(matchId));
                if (!checkInHouseOwner(event, partyId)) {
                        return;
                }

                int gameType = PartyDAO.getGameType(partyId);

                boolean success = InHouseMatchDAO.addTeamScore(matchId, teamId, gameType);

                if (!success) {
                        event.reply("세트 승리 처리 실패")
                                        .setEphemeral(true)
                                        .queue(hook -> deleteAfterDelay(hook, 60));
                        return;
                }

                // 경기 임베드 갱신
                updateInHouseMatchEmbed(
                                event.getChannel(),
                                event.getGuild(),
                                partyId);

                event.reply("✅ 세트 승리가 기록되었습니다.")
                                .setEphemeral(true)
                                .queue(hook -> deleteAfterDelay(hook, 60));
        }

        // 세트 승 취소 처리 핸들러
        public void handleDecreaseScore(ButtonInteractionEvent event) {

                String[] parts = event.getComponentId().split(":");
                int inHouseId = Integer.parseInt(parts[2]);

                // 내전 생성자인지 확인
                int partyId = InHouseDAO.getPartyIdByInHouseId(inHouseId);
                if (!checkInHouseOwner(event, partyId)) {
                        return;
                }

                // 세트 승지 취소할 팀을 선택하는 선택지
                List<ActionRow> rows = new ArrayList<>();

                List<InHouseMatchDTO> matches = InHouseMatchDAO.getMatchesByInHouseId(inHouseId);

                for (InHouseMatchDTO match : matches) {

                        // 이미 승자가 결정된 경기는 점수 변경 불가
                        if (match.getWinnerTeamId() != 0) {
                                continue;
                        }

                        String team1Name = InHouseDAO.getTeamNameByTeamId(match.getTeam1Id());
                        String team2Name = InHouseDAO.getTeamNameByTeamId(match.getTeam2Id());
                        Button team1Button = Button.danger(
                                        "inHouse:decreaseScore:" + inHouseId + ":" + match.getTeam1Id(),
                                        team1Name + " 세트 승 취소");
                        Button team2Button = Button.danger(
                                        "inHouse:decreaseScore:" + inHouseId + ":" + match.getTeam2Id(),
                                        team2Name + " 세트 승 취소");

                        rows.add(ActionRow.of(team1Button, team2Button));
                }
                event.reply("세트 승 취소할 팀을 선택하세요.")
                                .addComponents(rows)
                                .setEphemeral(true)
                                .queue(hook -> deleteAfterDelay(hook, 60));
        }

        // 세트 승 취소 처리 (팀 선택 후 실제 취소)
        public void handleDecreaseScoreTeam(ButtonInteractionEvent event) {

                String[] parts = event.getComponentId().split(":");

                int inHouseId = Integer.parseInt(parts[2]);
                int teamId = Integer.parseInt(parts[3]);
                int partyId = InHouseDAO.getPartyIdByInHouseId(inHouseId);

                // 세트 승 취소
                boolean success = InHouseMatchDAO.decreaseScoreAndWinCount(
                                inHouseId,
                                teamId,
                                PartyDAO.getGameType(partyId));

                if (!success) {

                        event.reply("❌ 세트 승을 취소할 수 없습니다.")
                                        .setEphemeral(true)
                                        .queue(hook -> deleteAfterDelay(hook, 60));

                        return;
                }

                // 매치 임베드 갱신
                updateInHouseMatchEmbed(
                                event.getChannel(),
                                event.getGuild(),
                                partyId);

                event.reply("✅ 세트 승을 취소했습니다.")
                                .setEphemeral(true)
                                .queue(hook -> deleteAfterDelay(hook, 60));
        }

        // 매치 종료 처리
        public void handleNextMatch(ButtonInteractionEvent event) {

                String[] parts = event.getComponentId().split(":");

                int inHouseId = Integer.parseInt(parts[2]);
                int partyId = InHouseDAO.getPartyIdByInHouseId(inHouseId);

                // 현재 매치의 모든 경기를 조회
                List<InHouseMatchDTO> matches = InHouseMatchDAO.getCurrentRoundMatches(inHouseId);

                // 동점 경기가 있다면 다음 매치로 진행 불가
                for (InHouseMatchDTO match : matches) {
                        if (match.getTeam1Score() == match.getTeam2Score()) {
                                event.reply("❌ 동점 경기가 존재하여 다음 매치로 진행할 수 없습니다.")
                                                .setEphemeral(true)
                                                .queue(hook -> deleteAfterDelay(hook, 60));
                                return;
                        }
                }

                // 각 경기의 승수가 높은 팀을 승자로 저장
                for (InHouseMatchDTO match : matches) {
                        int winnerTeamId = match.getTeam1Score() > match.getTeam2Score() ? match.getTeam1Id()
                                        : match.getTeam2Id();
                        InHouseMatchDAO.updateWinnerTeam(match.getId(), winnerTeamId);
                }

                // 승리 팀 수 조회
                int winnerTeamCount = InHouseMatchDAO.getWinnerTeamCount(inHouseId);

                if (winnerTeamCount >= 2) {
                        // 승자가 2팀 이상이라면 다음 매치를 생성
                        List<Integer> winnerTeamIds = InHouseMatchDAO.getWinnerTeamIds(inHouseId);

                        // 승리 팀들을 무작위로 섞음
                        Collections.shuffle(winnerTeamIds);

                        // 다음 라운드 번호
                        int nextRoundNumber = matches.get(0).getRoundNumber() + 1;

                        // 승리 팀들을 2팀씩 묶어서 다음 매치 생성
                        for (int i = 0; i + 1 < winnerTeamIds.size(); i += 2) {

                                int team1Id = winnerTeamIds.get(i);
                                int team2Id = winnerTeamIds.get(i + 1);

                                InHouseMatchDTO nextMatch = new InHouseMatchDTO();

                                nextMatch.setInHouseId(inHouseId);
                                nextMatch.setRoundNumber(nextRoundNumber);
                                nextMatch.setTeam1Id(team1Id);
                                nextMatch.setTeam2Id(team2Id);

                                InHouseMatchDAO.insertMatch(nextMatch);
                        }
                } else if (winnerTeamCount == 1) {
                        // 최종 승리 팀 확정 및 내전 종료 로직

                        // 최종 승리 팀 ID
                        List<Integer> winnerTeamIds = InHouseMatchDAO.getWinnerTeamIds(inHouseId);

                        int winnerTeamId = winnerTeamIds.get(0);

                        // 최종 승리 팀 이름
                        String winnerTeamName = InHouseDAO.getTeamNameByTeamId(winnerTeamId);

                        // 내전 종료 처리
                        InHouseDAO.updateInHousePartyStatus(
                                        partyId,
                                        3);

                        int gameType = PartyDAO.getGameType(partyId);
                        String gameTypeName = gameType == 1 ? "칼바람" : "협곡";

                        LocalDateTime startedAt = InHouseDAO.getStartedAt(partyId);
                        String startedAtStr = startedAt.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분"));

                        String hostName = event.getGuild().getMemberById(PartyDAO.getOwnerId(partyId))
                                        .getEffectiveName();

                        // 최종 승리 팀 안내
                        event.reply(
                                        "🏆 내전이 종료되었습니다!\n"
                                                        + "최종 우승 팀: **"
                                                        + winnerTeamName
                                                        + "**\n"
                                                        + "게임 종류: **"
                                                        + gameTypeName
                                                        + "**\n"
                                                        + "내전 시작 시간: **"
                                                        + startedAtStr
                                                        + "**\n"
                                                        + "주최자: **"
                                                        + hostName
                                                        + "**")
                                        .queue();
                }

                // 매치 임베드 갱신
                updateInHouseMatchEmbed(
                                event.getChannel(),
                                event.getGuild(),
                                partyId);

                event.reply("✅ 매치를 종료했습니다.")
                                .setEphemeral(true)
                                .queue(hook -> deleteAfterDelay(hook, 60));
        }

        // 내전 시작 시간 변경 처리
        public void handleInHouseManageStartTime(
                        ButtonInteractionEvent event) {

                String[] parts = event.getComponentId().split(":");

                int partyId = Integer.parseInt(parts[3]);

                // 내전 생성자인지 확인
                if (!checkInHouseOwner(event, partyId)) {
                        return;
                }

                // 시작시간 변경 모달
                Modal modal = Modal.create(
                                "inHouse:manage:startTime:" + partyId,
                                "내전 시작시간 변경")
                                .addActionRow(
                                                TextInput.create(
                                                                "startDate",
                                                                "시작 날짜",
                                                                TextInputStyle.SHORT)
                                                                .setPlaceholder("예: 20260921")
                                                                .setRequired(true)
                                                                .build())
                                .addActionRow(
                                                TextInput.create(
                                                                "startTime",
                                                                "시작 시간",
                                                                TextInputStyle.SHORT)
                                                                .setPlaceholder("예: 1930")
                                                                .setRequired(true)
                                                                .build())
                                .build();

                event.replyModal(modal).queue();
        }

        // 내전 시작시간 변경 모달 처리
        public void handleInHouseManageStartTimeModal(
                        ModalInteractionEvent event) {

                String[] parts = event.getModalId().split(":");

                int partyId = Integer.parseInt(parts[3]);

                String startDate = event.getValue("startDate").getAsString();

                String startTime = event.getValue("startTime").getAsString();

                LocalDateTime startedAt = LocalDateTime.parse(
                                startDate + startTime,
                                DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

                // 내전 종료 시간 = 시작 시간 + 6시간
                LocalDateTime deletedAt = startedAt.plusHours(6);

                // 내전 시작 시간 / 내전 종료 시간 변경
                boolean success = InHouseDAO.updateStartAndDeadline(
                                partyId,
                                startedAt,
                                deletedAt);

                if (!success) {
                        event.reply("❌ 내전 시작 시간 변경에 실패했습니다.")
                                        .setEphemeral(true)
                                        .queue(hook -> deleteAfterDelay(hook, 60));
                        return;
                }

                // 내전 정보 임베드 갱신
                sendInHouseEmbed(
                                event.getChannel(),
                                event.getGuild(),
                                partyId);

                event.reply("✅ 내전 시작 시간이 변경되었습니다.")
                                .setEphemeral(true)
                                .queue(hook -> deleteAfterDelay(hook, 60));
        }

        // 내전 참여자 추방 모달 처리
        public void handleInHouseManageKick(
                        ButtonInteractionEvent event) {

                String[] parts = event.getComponentId().split(":");

                int partyId = Integer.parseInt(parts[3]);

                // 내전 생성자인지 확인
                if (!checkInHouseOwner(event, partyId)) {
                        return;
                }

                TextInput numberInput = TextInput.create(
                                "kickNumber",
                                "추방할 참여자의 번호",
                                TextInputStyle.SHORT)
                                .setPlaceholder("번호를 입력하세요")
                                .setRequired(true)
                                .build();

                Modal modal = Modal.create(
                                "inHouse:manage:kick:" + partyId,
                                "추방할 참여자 선택")
                                .addActionRow(numberInput)
                                .build();

                event.replyModal(modal).queue();
        }

        // 내전 참여자 추방 모달 처리 핸들러
        public void handleInHouseManageKickModal(ModalInteractionEvent event) {

                String[] parts = event.getModalId().split(":");
                int partyId = Integer.parseInt(parts[3]);
                String kickNumberStr = event.getValue("kickNumber").getAsString();
                int kickNumber;

                try {
                        kickNumber = Integer.parseInt(kickNumberStr);

                } catch (NumberFormatException e) {

                        event.reply("❌ 올바른 번호를 입력하세요.")
                                        .setEphemeral(true)
                                        .queue();

                        return;
                }

                // 추방 대상의 Discord ID 확인
                long kickDiscordId = PartyDAO.getParticipantDiscordId(
                                partyId,
                                kickNumber);

                if (kickDiscordId == 0) {

                        event.reply("❌ 해당 번호의 참여자를 찾을 수 없습니다.")
                                        .setEphemeral(true)
                                        .queue();

                        return;
                }

                // 추방 대상의 Discord 닉네임 확인
                String kickNickname = event.getGuild()
                                .getMemberById(kickDiscordId)
                                .getEffectiveName();

                // 추방 대상 확인
                event.reply(
                                "⚠️ 추방 대상이 **" + kickNickname + "**님이 맞습니까?")
                                .setEphemeral(true)
                                .addActionRow(
                                                Button.danger(
                                                                "inHouse:manage:kickConfirm:"
                                                                                + partyId + ":"
                                                                                + kickDiscordId,
                                                                "추방하기"),
                                                Button.secondary(
                                                                "inHouse:manage:kickCancel",
                                                                "취소"))
                                .queue();
        }

        // 내전 관리 - 추방 확인 처리
        public void handleInHouseManageKickConfirm(
                        ButtonInteractionEvent event) {

                String[] parts = event.getComponentId().split(":");

                int partyId = Integer.parseInt(parts[3]);

                long kickDiscordId = Long.parseLong(parts[4]);

                // 실제 내전 참여자 추방
                boolean success = InHouseDAO.kickParticipant(
                                partyId,
                                kickDiscordId);

                if (!success) {

                        event.editMessage(
                                        "❌ 참여자 추방에 실패했습니다.")
                                        .setComponents()
                                        .queue();

                        return;
                }

                // 내전 정보 임베드 갱신
                sendInHouseEmbed(
                                event.getChannel(),
                                event.getGuild(),
                                partyId);

                // 내전 팀 정보 임베드 갱신
                updateInHouseTeamEmbed(
                                event.getChannel(),
                                event.getGuild(),
                                partyId);

                // 확인 메시지 수정
                event.editMessage(
                                "✅ 참여자가 추방되었습니다.")
                                .setComponents()
                                .queue();
        }
}