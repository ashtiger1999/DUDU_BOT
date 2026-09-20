package com.dudu.util;

import com.dudu.handler.PartyHandler;
import com.dudu.handler.InHouseHandler;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

// 버튼 분기 처리
public class ButtonRouter {

    public void route(ButtonInteractionEvent event) {

        String btnId = event.getComponentId();

        if (btnId.startsWith("party:join:")) {
            // 참여하기 버튼
            new PartyHandler().handleJoin(event);
        } else if (btnId.startsWith("party:leave:")) {
            // 참여취소 버튼
            new PartyHandler().handleLeave(event);
        } else if (btnId.startsWith("party:kick:")) {
            // 추방하기 버튼
            new PartyHandler().handleKick(event);
        } else if (btnId.startsWith("party:extend:")) {
            // 파티 마감 연장 버튼
            new PartyHandler().handleExtend(event);
        } else if (btnId.startsWith("party:gameMode:h:")) {
            // 게임 모드 선택 버튼
            new PartyHandler().handleGameModeSelection(event);
        } else if (btnId.startsWith("inHouse:gameType:")) {
            // 내전 게임 타입 선택 버튼
            new InHouseHandler().handleInHouseGameTypeSelection(event);
        } else if (btnId.startsWith("inHouse:startAt:")) {
            // 내전 시작 방식 선택 버튼
            new InHouseHandler().handleInHouseStartAt(event);
        } else if (btnId.startsWith("inHouse:manage:open:")) {
            // 내전 관리 메뉴 열기 버튼
            new InHouseHandler().handleInHouseManage(event);
        } else if (btnId.startsWith("inHouse:manage:cancel:")) {
            // 내전 취소 버튼
            new InHouseHandler().handleInHouseManageCancel(event);
        } else if (btnId.startsWith("inHouse:manage:status:")) {
            // 내전 상태 변경 버튼
            new InHouseHandler().handleInHouseManageStatus(event);
        } else if (btnId.startsWith("inHouse:joinTeam:")) {
            // 팀에 참여 버튼
            new InHouseHandler().handleInHouseJoinTeam(event);
        } else if (btnId.startsWith("inHouse:leaveTeam:")) {
            // 팀 나가기 버튼
            new InHouseHandler().handleInHouseLeaveTeam(event);
        } else if (btnId.startsWith("inHouse:setWin:")) {
            // 세트 승리 팀 선택 버튼
            new InHouseHandler().handleSetWin(event);
        } else if (btnId.startsWith("inHouse:start:")) {
            // 내전 시작 버튼
            new InHouseHandler().handleInHouseStart(event);
        } else if (btnId.startsWith("inHouse:decreaseScore:")) {

            String[] parts = btnId.split(":");

            if (parts.length == 3) {

                // 세트 승 취소 → 팀 선택창
                new InHouseHandler().handleDecreaseScore(event);

            } else if (parts.length == 4) {

                // 선택한 팀의 세트 승 실제 취소
                new InHouseHandler().handleDecreaseScoreTeam(event);
            }
        } else if (btnId.startsWith("inHouse:nextMatch:")) {
            // 매치 종료 버튼
            new InHouseHandler().handleNextMatch(event);
        } else if (btnId.startsWith("inHouse:manage:startTime:")) {
            // 내전 시작시간 변경 버튼
            new InHouseHandler().handleInHouseManageStartTime(event);
        } else if (btnId.startsWith("inHouse:manage:kick:")) {
            // 추방하기 버튼
            new InHouseHandler().handleInHouseManageKick(event);
        } else if (btnId.startsWith("inHouse:manage:kickConfirm:")) {
            // 추방 확인 버튼
            new InHouseHandler().handleInHouseManageKickConfirm(event);
        } else if (btnId.equals("inHouse:manage:kickCancel")) {
            event.editMessage("❌ 추방을 취소했습니다.")
                    .setComponents()
                    .queue(hook->{
                        // 60초 뒤에 메시지 삭제
                        hook.deleteOriginal().queueAfter(60, java.util.concurrent.TimeUnit.SECONDS);
                    });
        }
    }

}
