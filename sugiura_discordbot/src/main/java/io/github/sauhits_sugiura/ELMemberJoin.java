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
				.addPermissionOverride(member,
						EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
				.addPermissionOverride(guild.getSelfMember(),
						EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)

				.queue(channel -> {
					logger.info("Created private channel for {}", member.getUser().getName());

					Button registerButton = Button.primary("btn_register", "初期登録を行う");
					channel.sendMessage(member.getAsMention()
							+ " さん、ようこそ！\n下のボタンを押して、氏名・学籍番号等の情報を入力してください。")
							.setActionRow(registerButton) // ボタンを配置
							.queue();
				});
	}

	@Override
	public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
		if (event.getComponentId().equals("btn_register")) {
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
			Modal modal = Modal.create("modal_register", "登録情報の変更")
					.addActionRow(idInput)
					.addActionRow(nameInput)
					.addActionRow(nickInput)
					.addActionRow(teamInput)
					.build();
			// ユーザーにフォームを表示
			event.replyModal(modal).queue();
		}
	}
}
