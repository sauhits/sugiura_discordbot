package io.github.sauhits_sugiura;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

import ch.qos.logback.core.model.Model;
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

public class ReAuth extends ListenerAdapter {
    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String modalId = event.getModalId();

        // 新規登録(modal_register) または 再設定(modal_reauth) の場合
        if (modalId.equals("modal_register") || modalId.equals("modal_reauth")) {

            // 入力値の取得とフォーマット (共通処理)
            String studentId = event.getValue("input_student_id").getAsString();
            String fullName = event.getValue("input_name").getAsString();
            String nickname = event.getValue("input_nickname").getAsString();
            String team = event.getValue("input_team").getAsString();
            String newDisplayName = String.format("%s/%s", nickname, fullName);

            Member member = event.getMember();
            Guild guild = event.getGuild();

            if (member != null && guild != null) {
                // リネーム実行
                member.modifyNickname(newDisplayName).queue(
                        success -> {
                            // 新規登録処理
                            if (modalId.equals("modal_register")) {
                                // ロールの作成
                                Role unknownRole = guild.getRolesByName("unknown", true).stream().findFirst()
                                        .orElse(null);
                                Role authorizedRole = guild.getRolesByName("Authorized", true).stream().findFirst()
                                        .orElse(null);

                                // ロールの存在を確認
                                if (unknownRole != null && authorizedRole != null) {
                                    guild.removeRoleFromMember(member, unknownRole).queue(); // unknownロールを剥奪する
                                    guild.addRoleToMember(member, authorizedRole).queue(); // Authorizedロールを付与する

                                    event.reply("登録が完了しました！正規メンバー(Authorized)として登録されました。")
                                            .setEphemeral(true).queue();

                                    if (event.getChannel() instanceof TextChannel) {
                                        ((TextChannel) event.getChannel()).delete().queueAfter(2, TimeUnit.SECONDS); // 2s後に初期設定チャンネルの削除
                                    }
                                } else {
                                    event.reply("登録完了しましたが、ロール付与に失敗しました。").setEphemeral(true).queue();
                                }
                                // 登録情報更新処理
                            } else if (modalId.equals("modal_reauth")) {
                                event.reply("登録情報を更新しました")
                                        .setEphemeral(true) // 関連ユーザにのみ表示
                                        .queue();
                            }
                            // チームロールの更新
                            List<Role> grantedRoles = member.getRoles();
                            Role csRole = guild.getRolesByName("cs", true).stream().findFirst().orElse(null);
                            Role imgRole = guild.getRolesByName("画像", true).stream().findFirst().orElse(null);
                            Role appRole = guild.getRolesByName("アプリ", true).stream().findFirst().orElse(null);

                            for (Role grantedRole : grantedRoles) {
                                if (grantedRole.equals(csRole) || grantedRole.equals(imgRole)
                                        || grantedRole.equals(appRole)) {
                                    guild.removeRoleFromMember(member, grantedRole).queue();
                                    break;
                                }
                            }
                            if (team.matches("^(c|C|ｃ|Ｃ)(s|S|ｓ|Ｓ)(チーム|ちーむ)$")) {
                                guild.addRoleToMember(member, csRole).queue(); // csチームロールを付与
                            } else if (team.matches("^(画像|がぞう|ガゾウ)(チーム|ちーむ)$")) {
                                guild.addRoleToMember(member, imgRole).queue(); // 画像チームロールを付与
                            } else if (team.matches("^(アプリ|あぷり|app|App)(チーム|ちーむ)$")) {
                                guild.addRoleToMember(member, appRole).queue(); // アプリチームロールを付与
                            }
                        },
                        error -> {
                            event.reply("エラーが発生しました: " + error.getMessage()).setEphemeral(true).queue();
                        });
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        // reAuthイベントについて
        if (event.getName().equals("reauth")) {
            TextInput idInput = TextInput.create("input_student_id", "学籍番号", TextInputStyle.SHORT)
                    .setPlaceholder("例: 70310018")
                    .setRequiredRange(8, 8)
                    .setRequired(true)
                    .build();

            TextInput nameInput = TextInput.create("input_name", "氏名", TextInputStyle.SHORT)
                    .setPlaceholder("例: 杉浦彰彦")
                    .setRequiredRange(1, 10)
                    .setRequired(true)
                    .build();

            TextInput nickInput = TextInput.create("input_nickname", "ニックネーム", TextInputStyle.SHORT)
                    .setPlaceholder("例: おとのさま")
                    .setRequiredRange(1, 10)
                    .setRequired(true)
                    .build();

            TextInput teamInput = TextInput.create("input_team", "所属チーム", TextInputStyle.SHORT)
                    .setPlaceholder("例: csチーム, 画像チーム, アプリチーム")
                    .setRequiredRange(5, 6)
                    .setRequired(true)
                    .build();

            // ★重要: ModalのIDを "modal_reauth" にして、新規登録と区別する
            Modal modal = Modal.create("modal_reauth", "登録情報の変更")
                    .addActionRow(idInput)
                    .addActionRow(nameInput)
                    .addActionRow(nickInput)
                    .addActionRow(teamInput)
                    .build();

            event.replyModal(modal).queue();
        }
    }
}