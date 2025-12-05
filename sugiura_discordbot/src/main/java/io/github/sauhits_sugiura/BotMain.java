package io.github.sauhits_sugiura;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class BotMain {
    public static void main(String[] args) {
        // .envからTOKENを取得
        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("TOKEN");

        // Botの構築
        JDA jda = JDABuilder.createDefault(token)
                // メンバー情報のキャッシュと操作に必須
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                // メッセージ内容の読み取りが必要な場合（今回はSlash Command前提なら必須ではないが念のため）
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                // イベントリスナーの登録
                .addEventListeners(new ELOnReady(), new ELMemberJoin(),new ReAuth())
                .build();

        jda.updateCommands().addCommands(
                Commands.slash("reauth", "登録情報（氏名・学籍番号）を変更します")).queue();
    }
}

// mvn clean package
// java -jar .\target\sugiura_discordbot-1.0-SNAPSHOT.jar