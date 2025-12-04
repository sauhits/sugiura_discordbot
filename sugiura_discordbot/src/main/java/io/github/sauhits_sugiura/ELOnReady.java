package io.github.sauhits_sugiura;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;


public class ELOnReady extends ListenerAdapter{
    private static final Logger logger = LoggerFactory.getLogger(ELOnReady.class);

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        // 起動確認ログ
        logger.info("{} is online!", event.getJDA().getSelfUser().getAsTag());
    }
}
