package io.github.sauhits_sugiura;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
// 必要なimportの追加
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import java.util.concurrent.TimeUnit;

public class ELMemberJoin extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ELMemberJoin.class);

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();

        List<Role> roles = guild.getRolesByName("unknown", true);

        if (!roles.isEmpty()) {
            Role unknownRole = roles.get(0);
            guild.addRoleToMember(member, unknownRole).queue(
                    success -> logger.info("Role added to {}", member.getUser().getName()),
                    error -> logger.error("Failed to add role: {}", error.getMessage()));
        } else {
            logger.warn("Role 'unknown' not found in guild.");
        }

        String channelName = "entry-" + member.getUser().getName();

        guild.createTextChannel(channelName)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
                .addPermissionOverride(guild.getSelfMember(),
                        EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)

                .queue(channel -> {
                    logger.info("Created private channel for {}", member.getUser().getName());

                    Button registerButton = Button.primary("btn_register", "初期登録を行う");
                    channel.sendMessage(member.getAsMention() + " さん、ようこそ！\n下のボタンを押して、氏名・学籍番号等の情報を入力してください。")
                            .setActionRow(registerButton) // ボタンを配置
                            .queue();
                });
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getComponentId().equals("btn_register")) {

            TextInput idInput = TextInput.create("input_student_id", "学籍番号", TextInputStyle.SHORT)
                    .setPlaceholder("例: 70310018")
                    .setMinLength(8)
                    .setMaxLength(8) // 必要に応じて調整
                    .setRequired(true)
                    .build();

            // 2. 氏名の入力欄
            TextInput nameInput = TextInput.create("input_name", "氏名 (漢字)", TextInputStyle.SHORT)
                    .setPlaceholder("例: 杉浦彰彦")
                    .setMinLength(1)
                    .setMaxLength(10)
                    .setRequired(true)
                    .build();

            // 3. ニックネームの入力欄
            TextInput nickInput = TextInput.create("input_nickname", "ニックネーム(氏名も可)", TextInputStyle.SHORT)
                    .setPlaceholder("例: おとのさま")
                    .setMinLength(1)
                    .setMaxLength(10)
                    .setRequired(true)
                    .build();

            // フォーム(Modal)全体の構築
            Modal modal = Modal.create("modal_register", "初期登録フォーム")
                    .addActionRow(idInput)
                    .addActionRow(nameInput)
                    .addActionRow(nickInput)
                    .build();

            // ユーザーにフォームを表示
            event.replyModal(modal).queue();
        }
    }

    /**
     * フォームが送信された時の処理
     * (ニックネームを変更する)
     */
    // @Override
    // public void onModalInteraction(@NotNull ModalInteractionEvent event) {
    //     if (event.getModalId().equals("modal_register")) {

    //         // 入力値の取得
    //         String studentId = event.getValue("input_student_id").getAsString();
    //         String fullName = event.getValue("input_name").getAsString();
    //         String nickname = event.getValue("input_nickname").getAsString();

    //         // 書式の生成: [学籍番号]-[ニックネーム]
    //         String newDisplayName = String.format("%s/%s",nickname,  fullName);

    //         Member member = event.getMember();
    //         Guild guild = event.getGuild();

    //         if (member != null && guild != null) {
    //             // ニックネーム変更の実行
    //             member.modifyNickname(newDisplayName).queue(
    //                     success -> {
    //                         // 成功時のメッセージ
    //                         event.reply("登録が完了しました！\n表示名: " + newDisplayName).setEphemeral(true).queue();
    //                         logger.info("Renamed user {} to {}", member.getUser().getName(), newDisplayName);

    //                         Role unknownRole = guild.getRolesByName("unknown", true).stream().findFirst().orElse(null);

    //                         // ★修正箇所: "Authorized" ロールを取得
    //                         Role authorizedRole = guild.getRolesByName("Authorized", true).stream().findFirst()
    //                                 .orElse(null);

    //                         if (unknownRole != null && authorizedRole != null) {
    //                             // ロールの入れ替え処理
    //                             guild.removeRoleFromMember(member, unknownRole).queue();
    //                             guild.addRoleToMember(member, authorizedRole).queue();

    //                             // 完了メッセージ
    //                             event.reply("登録が完了しました！正規メンバー(Authorized)として登録されました。\nこのチャンネルは5秒後に自動削除されます。")
    //                                     .setEphemeral(true)
    //                                     .queue();

    //                             // チャンネルの削除 (5秒後)
    //                             if (event.getChannel() instanceof TextChannel) {
    //                                 ((TextChannel) event.getChannel()).delete().queueAfter(5, TimeUnit.SECONDS);
    //                             }

    //                             logger.info("Registration completed for {}. Role 'Authorized' granted.",
    //                                     newDisplayName);

    //                         } else {
    //                             // ロールが見つからない場合
    //                             event.reply(
    //                                     "登録自体は完了しましたが、ロール 'Authorized' または 'unknown' が見つからず自動付与に失敗しました。\n管理者にお問い合わせください。")
    //                                     .setEphemeral(true)
    //                                     .queue();
    //                             logger.error("Role 'Authorized' or 'unknown' not found.");
    //                         }
    //                     },
    //                     error -> {
    //                         // 失敗時 (権限不足や文字数オーバーなど)
    //                         event.reply("エラーが発生しました: " + error.getMessage()).setEphemeral(true).queue();
    //                         logger.error("Failed to rename user: {}", error.getMessage());
    //                     });
    //         }
    //     }
    // }
}
