package com.dudu.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dudu.dao.PartyDAO;

@Component
public class PartyScheduler {

    // 1시간마다 마감된 파티 삭제
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void deleteExpiredParties() {

        List<Integer> partyIds =
                PartyDAO.getExpiredPartyIds();

        for (int partyId : partyIds) {
            PartyDAO.deleteParty(partyId);
        }
    }
}