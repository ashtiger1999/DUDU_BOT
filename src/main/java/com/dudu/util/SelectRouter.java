package com.dudu.util;

import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

public class SelectRouter {

    public void route(StringSelectInteractionEvent event) {

        String componentId = event.getComponentId();

        if (componentId.startsWith("inHouse:status:")) {
            // 내전 상태 선택 처리 라우팅
            new com.dudu.handler.InHouseHandler().handleInHouseStatusSelect(event);
        } else if (componentId.startsWith("inHouse:joinTeam:")) {
            // 내전 팀 참여 선택 처리 라우팅
            new com.dudu.handler.InHouseHandler().handleInHouseJoinTeamSelect(event);
        }
    }

}
