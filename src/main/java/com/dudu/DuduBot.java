package com.dudu;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import com.dudu.db.DB;
import java.util.Properties;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.scheduling.annotation.EnableScheduling;

import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

@SpringBootApplication
@EnableScheduling
public class DuduBot {

        public static void main(String[] args) throws Exception {

                DB.init();

                Properties props = PropertiesLoaderUtils.loadProperties(
                                new ClassPathResource("application.properties"));

                String BOT_TOKEN = System.getenv("DISCORD_TOKEN");

                if (BOT_TOKEN == null || BOT_TOKEN.isBlank()) {
                        throw new IllegalStateException(
                                        "DISCORD_TOKEN 환경변수가 설정되지 않았습니다.");
                }

                // JDA 객체 설정 및 생성
                JDA api = JDABuilder.createDefault(BOT_TOKEN)
                                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                                .setMemberCachePolicy(MemberCachePolicy.ALL)
                                .addEventListeners(new BotListener())
                                .build();

                api.updateCommands()
                                .addCommands(
                                                Commands.slash("증바람", "아수라장 파티 모집"))
                                .addCommands(Commands.slash("협곡", "소환사의 협곡 파티 모집"))
                                .addCommands(Commands.slash("아레나", "필요시 추가 요청바람"))
                                .addCommands(Commands.slash("내전생성", "아수라장/협곡 내전 생성"))
                                .addCommands(Commands.slash("소환사명변경", "소환사의 이름을 변경합니다."))
                                .queue(command -> {
                                        System.out.println("Commands registered!");
                                });

                System.out.println(
                                "//////////////////////////////////////////////////////////////\nBot connected! ver.01.03.19\n//////////////////////////////////////////////////////////////");

        }
}