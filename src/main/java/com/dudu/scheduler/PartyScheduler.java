package com.dudu.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dudu.DuduBot;
import com.dudu.dao.PartyDAO;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

@Component
public class PartyScheduler {

    // 1시간마다 마감된 파티 삭제
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void deleteExpiredParties() {

        List<Integer> partyIds =
                PartyDAO.getExpiredPartyIds();

        for (int partyId : partyIds) {

            String messageId =
                    PartyDAO.getMessageId(partyId);

            String channelId =
                    PartyDAO.getChannelId(partyId);

            // Discord 메시지 정보가 없는 경우
            if (messageId == null || channelId == null) {

                System.out.println(
                        "Discord 메시지 정보를 찾을 수 없습니다. partyId: "
                                + partyId);

                PartyDAO.deleteParty(partyId);
                continue;
            }

            TextChannel channel =
                    DuduBot.api.getTextChannelById(channelId);

            // 채널을 찾을 수 없는 경우
            if (channel == null) {

                System.out.println(
                        "Discord 채널을 찾을 수 없습니다. channelId: "
                                + channelId
                                + ", partyId: "
                                + partyId);

                PartyDAO.deleteParty(partyId);
                continue;
            }

            // Discord 메시지 삭제
            channel.deleteMessageById(messageId).queue(
                    success -> {

                        System.out.println(
                                "만료된 파티 임베드 삭제 완료. partyId: "
                                        + partyId);

                        // Discord 메시지 삭제 성공 후 DB에서도 삭제
                        PartyDAO.deleteParty(partyId);
                    },
                    error -> {

                        System.out.println(
                                "만료된 파티 임베드 삭제 실패. partyId: "
                                        + partyId);

                        error.printStackTrace();
                    });
        }
    }
}