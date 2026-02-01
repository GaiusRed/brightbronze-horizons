package red.gaius.brightbronze.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.world.dimension.SourceDimensionManager;

import java.util.Set;

public final class BbhDebugCommands {

    private BbhDebugCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("bbh:tpSource")
                .requires(source -> source.hasPermission(2))
                .then(
                    Commands.argument("biome", ResourceLocationArgument.id())
                        .executes(ctx -> {
                            ResourceLocation biomeId = ResourceLocationArgument.getId(ctx, "biome");
                            return tpSource(ctx.getSource(), biomeId, 0.0, 120.0, 0.0);
                        })
                        .then(
                            Commands.argument("x", DoubleArgumentType.doubleArg())
                                .then(
                                    Commands.argument("y", DoubleArgumentType.doubleArg())
                                        .then(
                                            Commands.argument("z", DoubleArgumentType.doubleArg())
                                                .executes(ctx -> {
                                                    ResourceLocation biomeId = ResourceLocationArgument.getId(ctx, "biome");
                                                    double x = DoubleArgumentType.getDouble(ctx, "x");
                                                    double y = DoubleArgumentType.getDouble(ctx, "y");
                                                    double z = DoubleArgumentType.getDouble(ctx, "z");
                                                    return tpSource(ctx.getSource(), biomeId, x, y, z);
                                                })
                                        )
                                )
                        )
                )
        );
    }

    private static int tpSource(CommandSourceStack source, ResourceLocation biomeId, double x, double y, double z) {
        MinecraftServer server = source.getServer();

        ServerLevel level = SourceDimensionManager.getOrCreateSourceDimension(server, biomeId);
        if (level == null) {
            source.sendFailure(Component.literal("Failed to create source dimension for biome " + biomeId));
            return 0;
        }

        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        // Ensure the destination chunk is loaded before teleport.
        level.getChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);

        boolean ok = player.teleportTo(level, x, y, z, Set.of(), player.getYRot(), player.getXRot(), false);
        if (!ok) {
            source.sendFailure(Component.literal("Teleport failed."));
            return 0;
        }

        source.sendSuccess(
            () -> Component.literal("Teleported to source dimension for biome " + biomeId + " (" + level.dimension().location() + ")"),
            false
        );

        BrightbronzeHorizons.LOGGER.info(
            "Debug TP: {} -> source dimension {} (biome {}) at ({}, {}, {})",
            player.getName().getString(), level.dimension().location(), biomeId, x, y, z
        );

        return 1;
    }
}
