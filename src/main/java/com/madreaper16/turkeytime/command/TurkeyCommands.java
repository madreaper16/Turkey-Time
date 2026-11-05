package com.madreaper16.turkeytime.command;

import com.madreaper16.turkeytime.entity.Turkey;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

/** Operator-only animation preview and entity inspection commands. */
public final class TurkeyCommands {
    private TurkeyCommands() {}
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> {
            var targets = Commands.argument("targets", EntityArgument.entities());
            String[] names = {"idle", "forage", "strut", "flutter", "startled"};
            for (int index = 0; index < names.length; index++) {
                final int display = index;
                targets.then(Commands.literal(names[index]).executes(context -> {
                    int count = 0;
                    for (var entity : EntityArgument.getEntities(context, "targets")) {
                        if (entity instanceof Turkey turkey && turkey.startDisplay(display)) count++;
                    }
                    final int changed = count;
                    context.getSource().sendSuccess(() -> Component.translatable(
                            "commands.turkeytime.animate", changed), false);
                    return count;
                }));
            }
            dispatcher.register(Commands.literal("turkeytime")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.literal("animate").then(targets))
                    .then(Commands.literal("inspect").then(Commands.argument("targets", EntityArgument.entities())
                            .executes(context -> {
                                int count = 0;
                                for (var entity : EntityArgument.getEntities(context, "targets")) {
                                    if (entity instanceof Turkey turkey) {
                                        count++;
                                        context.getSource().sendSuccess(() -> Component.translatable(
                                                "commands.turkeytime.inspect", turkey.getUUID().toString(),
                                                turkey.isMale(), turkey.getPlumage(), turkey.isBaby(),
                                                turkey.getDisplay(), turkey.getBbWidth(), turkey.getBbHeight()), false);
                                    }
                                }
                                return count;
                            }))));
        });
    }
}
