package io.github.sauhits_sugiura;

import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class Rename extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("reauth")) {

            // フォームの中身は「新規登録」と同じものを作成
            TextInput idInput = TextInput.create("input_student_id", "学籍番号", TextInputStyle.SHORT)
                    .setPlaceholder("例: b1023001")
                    .setRequired(true)
                    .build();

            TextInput nameInput = TextInput.create("input_name", "氏名 (漢字)", TextInputStyle.SHORT)
                    .setPlaceholder("例: 山田太郎")
                    .setRequired(true)
                    .build();

            TextInput nickInput = TextInput.create("input_nickname", "ニックネーム", TextInputStyle.SHORT)
                    .setPlaceholder("例: ヤマダ")
                    .setRequired(true)
                    .build();

            // ★重要: ModalのIDを "modal_reauth" にして、新規登録と区別する
            Modal modal = Modal.create("modal_reauth", "登録情報の変更")
                    .addActionRow(idInput)
                    .addActionRow(nameInput)
                    .addActionRow(nickInput)
                    .build();

            event.replyModal(modal).queue();
        }
    }

    /**
     * フォーム送信時の処理 (修正版)
     */
    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String modalId = event.getModalId();

        // 新規登録(modal_register) または 再設定(modal_reauth) の場合
        if (modalId.equals("modal_register") || modalId.equals("modal_reauth")) {

            // 入力値の取得とフォーマット (共通処理)
            String studentId = event.getValue("input_student_id").getAsString();
            String fullName = event.getValue("input_name").getAsString();
            String nickname = event.getValue("input_nickname").getAsString();
            String newDisplayName = String.format("%s/%s", nickname, fullName);

            Member member = event.getMember();
            Guild guild = event.getGuild();

            if (member != null && guild != null) {
                // リネーム実行
                member.modifyNickname(newDisplayName).queue(
                        success -> {
                            // --------------------------------------------------
                            // 分岐処理: IDによって動作を変える
                            // --------------------------------------------------

                            if (modalId.equals("modal_register")) {
                                // === 新規登録の場合 (ロール付与＋チャンネル削除) ===

                                Role unknownRole = guild.getRolesByName("unknown", true).stream().findFirst()
                                        .orElse(null);
                                Role authorizedRole = guild.getRolesByName("Authorized", true).stream().findFirst()
                                        .orElse(null);

                                if (unknownRole != null && authorizedRole != null) {
                                    guild.removeRoleFromMember(member, unknownRole).queue();
                                    guild.addRoleToMember(member, authorizedRole).queue();

                                    event.reply("登録が完了しました！正規メンバー(Authorized)として登録されました。\nこのチャンネルは5秒後に自動削除されます。")
                                            .setEphemeral(true).queue();

                                    if (event.getChannel() instanceof TextChannel) {
                                        ((TextChannel) event.getChannel()).delete().queueAfter(5, TimeUnit.SECONDS);
                                    }
                                } else {
                                    event.reply("登録完了しましたが、ロール付与に失敗しました。").setEphemeral(true).queue();
                                }

                            } else if (modalId.equals("modal_reauth")) {
                                // === 再設定(/reauth)の場合 (リネームのみ通知) ===

                                event.reply("登録情報を更新しました。\n新しい表示名: " + newDisplayName)
                                        .setEphemeral(true)
                                        .queue();
                            }
                        },
                        error -> {
                            event.reply("エラーが発生しました: " + error.getMessage()).setEphemeral(true).queue();
                        });
            }
        }
    }
}