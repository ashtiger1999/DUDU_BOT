package com.dudu.util;

import com.dudu.handler.PartyHandler;
import com.dudu.handler.InHouseHandler;
import com.dudu.handler.MemberHandler;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

// 모달 분기 처리
public class ModalRouter {

    public void route(ModalInteractionEvent event) {

        String id = event.getModalId();

        if (id.startsWith("party:create:")) {
            // 파티 생성 처리 핸들러 매핑
            new PartyHandler().handleCreate(event);
        } else if (id.startsWith("party:join:new:")) {
            // 신규 사용자 등록 처리 핸들러 매핑
            new PartyHandler().handleNewUserJoin(event);
        } else if (id.startsWith("party:kick:")) {
            // 파티 강퇴 처리 핸들러 매핑
            new PartyHandler().handleKick(event);
        } else if (id.startsWith("inHousePartyCreate:")) {
            // 내전 생성 처리 핸들러 매핑
            new InHouseHandler().handleInHouseCreate(event);
        } else if (id.startsWith("inHouse:datetime")) {
            // 내전 시작 처리 핸들러 매핑
            new InHouseHandler().handleInHouseDateTime(event);
        } else if (id.startsWith("inHouse:manage:startTime:")) {
            // 내전 시작시간 변경 처리 핸들러 매핑
            new InHouseHandler().handleInHouseManageStartTimeModal(event);
        } else if (id.startsWith("inHouse:manage:kick:")) {
            // 내전 참여자 추방 처리 핸들러 매핑
            new InHouseHandler().handleInHouseManageKickModal(event);
        } else if (id.startsWith("summonerName:change")) {
            // 소환사 이름 변경 처리 핸들러 매핑
            MemberHandler.handleChangeSummonerName(event);
        }
    }
}
