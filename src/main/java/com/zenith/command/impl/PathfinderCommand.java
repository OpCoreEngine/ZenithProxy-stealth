package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.feature.player.World;
import com.zenith.mc.block.Block;
import com.zenith.mc.block.BlockPos;
import com.zenith.mc.entity.EntityData;
import com.zenith.mc.entity.EntityRegistry;
import com.zenith.util.math.MathHelper;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.*;
import static com.zenith.command.brigadier.BlockArgument.block;
import static com.zenith.command.brigadier.BlockArgument.getBlock;
import static com.zenith.command.brigadier.BlockPosArgument.blockPos;
import static com.zenith.command.brigadier.BlockPosArgument.getBlockPos;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ItemArgument.getItem;
import static com.zenith.command.brigadier.ItemArgument.item;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static com.zenith.command.brigadier.Vec2Argument.getVec2;
import static com.zenith.command.brigadier.Vec2Argument.vec2;
import static com.zenith.discord.DiscordBot.escape;

public class PathfinderCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("pathfinder")
            .category(CommandCategory.MODULE)
            .description("""
            Baritone pathfinder
            """)
            .usageLines(
                "goto <x> <z>",
                "goto <x> <y> <z>",
                "goto <waypointId>",
                "stop",
                "follow",
                "follow <playerName>",
                "thisway <blocks>",
                "getTo <block>",
                "mine <block>",
                "click <left/right> <x> <y> <z>",
                "click <left/right> <waypointId>",
                "click <left/right> entity <type>",
                "break <x> <y> <z>",
                "pickup",
                "pickup <item>",
                "clearArea <pos1> <pos2>",
                "status",
                "settings"
            )
            .aliases(
                "path",
                "b"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("pathfinder")
            .then(literal("goto")
                .then(argument("xz", vec2()).executes(c -> {
                    var vec2 = getVec2(c, "xz");
                    int x = MathHelper.floorI(vec2.getX());
                    int z = MathHelper.floorI(vec2.getY());
                    BARITONE.pathTo(x, z)
                        .addExecutedListener(f -> {
                            c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                                .title("Pathing Completed!")
                                .addField("Pos", CONFIG.discord.reportCoords
                                    ? "||[" + x + ", " + z + "]||"
                                    : "Coords disabled")
                                .primaryColor());
                        });
                    c.getSource().getEmbed()
                        .title("Pathing")
                        .addField("Goal", CONFIG.discord.reportCoords
                            ? "||[" + x + ", " + z + "]||"
                            : "Coords disabled")
                        .primaryColor();
                }))
                .then(argument("xyz", blockPos()).executes(c -> {
                    var pos = getBlockPos(c, "xyz");
                    int x = pos.x();
                    int y = pos.y();
                    int z = pos.z();
                    BARITONE.pathTo(x, y, z)
                        .addExecutedListener(f -> {
                            c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                                .title("Pathing Completed!")
                                .addField("Pos", CONFIG.discord.reportCoords
                                    ? "||[" + x + ", " + y + ", " + z + "]||"
                                    : "Coords disabled")
                                .primaryColor());
                        });
                    c.getSource().getEmbed()
                        .title("Pathing")
                        .addField("Goal", CONFIG.discord.reportCoords
                            ? "||[" + x + ", " + y + ", " + z + "]||"
                            : "Coords disabled")
                        .primaryColor();
                }))
                .then(argument("waypoint", wordWithChars()).executes(c -> {
                    String id = getString(c, "waypoint");
                    var wpOptional = CONFIG.client.extra.waypoints.waypoints.stream()
                        .filter(w -> w.id().equalsIgnoreCase(id))
                        .findFirst();
                    if (wpOptional.isEmpty()) {
                        c.getSource().getEmbed()
                            .title("Waypoint Not Found");
                        return ERROR;
                    }
                    var wp = wpOptional.get();
                    if (wp.dimensionData() != World.getCurrentDimension()) {
                        c.getSource().getEmbed()
                            .title("Waypoint Dimension Mismatch")
                            .addField("Waypoint Dimension", wp.dimension())
                            .addField("Current Dimension", World.getCurrentDimension().name());
                        return ERROR;
                    }
                    int x = wp.x();
                    int y = wp.y();
                    int z = wp.z();
                    BARITONE.pathTo(x, y, z)
                        .addExecutedListener(f -> {
                            c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                                .title("Pathing Completed!")
                                .addField("Pos", CONFIG.discord.reportCoords
                                    ? "||[" + x + ", " + y + ", " + z + "]||"
                                    : "Coords disabled")
                                .primaryColor());
                        });
                    c.getSource().getEmbed()
                        .title("Pathing")
                        .addField("Goal", CONFIG.discord.reportCoords
                            ? "||[" + x + ", " + y + ", " + z + "]||"
                            : "Coords disabled")
                        .primaryColor();
                    return OK;
                })))
            .then(literal("stop").executes(c -> {
                BARITONE.stop();
                c.getSource().getEmbed()
                    .title("Pathing Stopped")
                    .addField("Status", "Stopped", false)
                    .primaryColor();
                return OK;
            }))
            .then(literal("follow")
                .executes(c -> {
                    BARITONE.follow((e) -> e instanceof EntityPlayer);
                    c.getSource().getEmbed()
                        .title("Following")
                        .primaryColor();
                })
                .then(argument("playerName", wordWithChars()).executes(c -> {
                    String playerName = getString(c, "playerName");
                    CACHE.getEntityCache().getPlayers().values().stream()
                        .filter(e -> CACHE.getTabListCache()
                            .get(e.getUuid())
                            .filter(p -> p.getName().equalsIgnoreCase(playerName))
                            .isPresent())
                        .findFirst()
                        .ifPresentOrElse(player -> {
                                BARITONE.follow(player);
                                c.getSource().getEmbed()
                                    .title("Following")
                                    .addField("Player", escape(playerName), false)
                                    .primaryColor();
                            },
                            () -> c.getSource().getEmbed()
                                .title("Error")
                                .description("Player not found: " + playerName)
                                .errorColor());
                    return OK;
                }))
                .then(literal("radius").then(argument("radius", integer()).executes(c -> {
                    int radius = getInteger(c, "radius");
                    CONFIG.client.extra.pathfinder.followRadius = radius;
                    c.getSource().getEmbed()
                        .title("Following")
                        .addField("Radius", radius, false)
                        .primaryColor();
                    return OK;
                }))))
            .then(literal("pickup")
                .executes(c -> {
                    BARITONE.pickup()
                        .addExecutedListener(f -> {
                            c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                                .title("Items Picked Up!")
                                .primaryColor());
                        });
                    c.getSource().getEmbed()
                        .title("Picking up all items")
                        .primaryColor();
                })
                .then(argument("item", item()).executes(c -> {
                var item = getItem(c, "item");
                if (item == null) {
                    c.getSource().getEmbed()
                        .title("Item Not found");
                    return ERROR;
                }
                BARITONE.pickup(item)
                    .addExecutedListener(f -> {
                        c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                            .title("Item Picked Up!")
                            .primaryColor());
                    });
                c.getSource().getEmbed()
                    .title("Picking up item")
                    .addField("Item", escape(item.name()))
                    .primaryColor();
                return OK;
            })))
            .then(literal("clearArea").then(argument("pos1", blockPos()).then(argument("pos2", blockPos()).executes(c -> {
                var pos1 = getBlockPos(c, "pos1");
                var pos2 = getBlockPos(c, "pos2");
                BARITONE.clearArea(pos1, pos2)
                    .addExecutedListener(f -> {
                        c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                            .title("Area Cleared!")
                            .addField("Area", CONFIG.discord.reportCoords
                                ? "||[" + pos1.x() + ", " + pos1.y() + ", " + pos1.z() + "] <> [" + pos2.x() + ", " + pos2.y() + ", " + pos2.z() + "]||"
                                : "Coords disabled")
                            .primaryColor());
                    });
                c.getSource().getEmbed()
                    .title("Clearing Area")
                    .addField("Area", CONFIG.discord.reportCoords
                        ? "||[" + pos1.x() + ", " + pos1.y() + ", " + pos1.z() + "] <> [" + pos2.x() + ", " + pos2.y() + ", " + pos2.z() + "]||"
                        : "Coords disabled")
                    .primaryColor();
            }))))
            .then(literal("thisway").then(argument("dist", integer()).executes(c -> {
                int dist = getInteger(c, "dist");
                BARITONE.thisWay(dist)
                    .addExecutedListener(f -> {
                        BlockPos pos = CACHE.getPlayerCache().getThePlayer().blockPos();
                        c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                            .title("Pathing Completed!")
                            .addField("Pos", CONFIG.discord.reportCoords
                                ? "||[" + pos.x() + ", " + pos.y() + ", " + pos.z() + "]||"
                                : "Coords disabled")
                            .primaryColor());
                    });
                c.getSource().getEmbed()
                    .title("Pathing")
                    .addField("This Way", dist, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("getTo").then(argument("block", block()).executes(c -> {
                Block block = getBlock(c, "block");
                BARITONE.getTo(block)
                    .addExecutedListener(f -> {
                        BlockPos pos = CACHE.getPlayerCache().getThePlayer().blockPos();
                        c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                            .title("At Block!")
                            .addField("Player Pos", CONFIG.discord.reportCoords
                                ? "||[" + pos.x() + ", " + pos.y() + ", " + pos.z() + "]||"
                                : "Coords disabled")
                            .primaryColor());
                    });
                c.getSource().getEmbed()
                    .title("Pathing")
                    .addField("Get To", block.name())
                    .primaryColor();
                return OK;
            })))
            .then(literal("mine").then(argument("block", block()).executes(c -> {
                Block block = getBlock(c, "block");
                BARITONE.mine(block);
                c.getSource().getEmbed()
                    .title("Pathing")
                    .addField("Mine", block.name())
                    .primaryColor();
                return OK;
            })))
            .then(literal("click")
                .then(literal("left")
                    .then(argument("pos", blockPos()).executes(c -> {
                        var pos = getBlockPos(c, "pos");
                        int x = pos.x();
                        int y = pos.y();
                        int z = pos.z();
                        BARITONE.leftClickBlock(x, y, z)
                            .addExecutedListener(f -> {
                                c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                                    .title("Block Left Clicked!")
                                    .addField("Target", CONFIG.discord.reportCoords
                                        ? "||[" + x + ", " + y + ", " + z + "]||"
                                        : "Coords disabled")
                                    .primaryColor());
                            });
                        c.getSource().getEmbed()
                            .title("Pathing")
                            .addField("Left Click", CONFIG.discord.reportCoords
                                ? "||[" + x + ", " + y + ", " + z + "]||"
                                : "Coords disabled")
                            .primaryColor();
                    }))
                    .then(literal("entity")
                        .then(argument("type", wordWithChars()).executes(c -> {
                            String entityType = getString(c, "type");
                            EntityData entityData = EntityRegistry.REGISTRY.get(entityType.toLowerCase().trim());
                            if (entityData == null) {
                                c.getSource().getEmbed()
                                    .title("Error")
                                    .description("Entity not found: " + entityType)
                                    .errorColor();
                                return OK;
                            }
                            var entityOptional = CACHE.getEntityCache().getEntities().values().stream()
                                .filter(e -> e instanceof EntityLiving)
                                .map(e -> (EntityLiving) e)
                                .filter(e -> !(e instanceof EntityPlayer player) || !player.isSelfPlayer())
                                .filter(e -> e.getEntityType() == entityData.mcplType())
                                .min((a, b) -> (int) (a.distanceSqTo(CACHE.getPlayerCache().getThePlayer()) - b.distanceSqTo(CACHE.getPlayerCache().getThePlayer())));
                            if (entityOptional.isEmpty()) {
                                c.getSource().getEmbed()
                                    .title("Error")
                                    .description("Entity not found: " + entityType)
                                    .errorColor();
                                return OK;
                            }
                            BARITONE.leftClickEntity(entityOptional.get())
                                .addExecutedListener(f -> {
                                    c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                                        .title("Entity Left Clicked!")
                                        .addField("Target", entityOptional.get().getEntityType()
                                            + (CONFIG.discord.reportCoords
                                            ? " ||[" + entityOptional.get().position() + "]||"
                                            : ""))
                                        .primaryColor());
                                });
                            c.getSource().getEmbed()
                                .title("Pathing")
                                .addField("Left Click", entityOptional.get().getEntityType()
                                    + (CONFIG.discord.reportCoords
                                    ? " ||[" + entityOptional.get().position() + "]||"
                                    : ""))
                                .primaryColor();
                            return OK;
                        })))
                    .then(argument("waypoint", wordWithChars()).executes(c -> {
                        String id = getString(c, "waypoint");
                        var wpOptional = CONFIG.client.extra.waypoints.waypoints.stream()
                            .filter(w -> w.id().equalsIgnoreCase(id))
                            .findFirst();
                        if (wpOptional.isEmpty()) {
                            c.getSource().getEmbed()
                                .title("Waypoint Not Found");
                            return ERROR;
                        }
                        var wp = wpOptional.get();
                        if (wp.dimensionData() != World.getCurrentDimension()) {
                            c.getSource().getEmbed()
                                .title("Waypoint Dimension Mismatch")
                                .addField("Waypoint Dimension", wp.dimension())
                                .addField("Current Dimension", World.getCurrentDimension().name());
                            return ERROR;
                        }
                        int x = wp.x();
                        int y = wp.y();
                        int z = wp.z();
                        BARITONE.leftClickBlock(x, y, z)
                            .addExecutedListener(f -> {
                                c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                                    .title("Pathing Completed!")
                                    .addField("Pos", CONFIG.discord.reportCoords
                                        ? "||[" + x + ", " + y + ", " + z + "]||"
                                        : "Coords disabled")
                                    .primaryColor());
                            });
                        c.getSource().getEmbed()
                            .title("Pathing")
                            .addField("Goal", CONFIG.discord.reportCoords
                                ? "||[" + x + ", " + y + ", " + z + "]||"
                                : "Coords disabled")
                            .primaryColor();
                        return OK;
                    })))
                .then(literal("right")
                    .then(argument("pos", blockPos()).executes(c -> {
                        var pos = getBlockPos(c, "pos");
                        int x = pos.x();
                        int y = pos.y();
                        int z = pos.z();
                        BARITONE.rightClickBlock(x, y, z)
                            .addExecutedListener(f -> {
                                c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                                    .title("Block Right Clicked!")
                                    .addField("Target", CONFIG.discord.reportCoords
                                        ? "||[" + x + ", " + y + ", " + z + "]||"
                                        : "Coords disabled")
                                    .primaryColor());
                            });
                        c.getSource().getEmbed()
                            .title("Pathing")
                            .addField("Right Click", CONFIG.discord.reportCoords
                                ? "||[" + x + ", " + y + ", " + z + "]||"
                                : "Coords disabled")
                            .primaryColor();
                    }))
                    .then(literal("entity")
                        .then(argument("type", wordWithChars()).executes(c -> {
                            String entityType = getString(c, "type");
                            EntityData entityData = EntityRegistry.REGISTRY.get(entityType.toLowerCase().trim());
                            if (entityData == null) {
                                c.getSource().getEmbed()
                                    .title("Error")
                                    .description("Entity not found: " + entityType)
                                    .errorColor();
                                return OK;
                            }
                            var entityOptional = CACHE.getEntityCache().getEntities().values().stream()
                                .filter(e -> e instanceof EntityLiving)
                                .map(e -> (EntityLiving) e)
                                .filter(e -> !(e instanceof EntityPlayer player) || !player.isSelfPlayer())
                                .filter(e -> e.getEntityType() == entityData.mcplType())
                                .min((a, b) -> (int) (a.distanceSqTo(CACHE.getPlayerCache().getThePlayer()) - b.distanceSqTo(CACHE.getPlayerCache().getThePlayer())));
                            if (entityOptional.isEmpty()) {
                                c.getSource().getEmbed()
                                    .title("Error")
                                    .description("Entity not found: " + entityType)
                                    .errorColor();
                                return OK;
                            }
                            BARITONE.rightClickEntity(entityOptional.get())
                                .addExecutedListener(f -> {
                                    c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                                        .title("Entity Right Clicked!")
                                        .addField("Target", entityOptional.get().getEntityType()
                                            + (CONFIG.discord.reportCoords
                                            ? " ||[" + entityOptional.get().position() + "]||"
                                            : ""))
                                        .primaryColor());
                                });
                            c.getSource().getEmbed()
                                .title("Pathing")
                                .addField("Right Click", entityOptional.get().getEntityType()
                                    + (CONFIG.discord.reportCoords
                                    ? " ||[" + entityOptional.get().position() + "]||"
                                    : ""))
                                .primaryColor();
                            return OK;
                        })))
                    .then(argument("waypoint", wordWithChars()).executes(c -> {
                        String id = getString(c, "waypoint");
                        var wpOptional = CONFIG.client.extra.waypoints.waypoints.stream()
                            .filter(w -> w.id().equalsIgnoreCase(id))
                            .findFirst();
                        if (wpOptional.isEmpty()) {
                            c.getSource().getEmbed()
                                .title("Waypoint Not Found");
                            return ERROR;
                        }
                        var wp = wpOptional.get();
                        if (wp.dimensionData() != World.getCurrentDimension()) {
                            c.getSource().getEmbed()
                                .title("Waypoint Dimension Mismatch")
                                .addField("Waypoint Dimension", wp.dimension())
                                .addField("Current Dimension", World.getCurrentDimension().name());
                            return ERROR;
                        }
                        int x = wp.x();
                        int y = wp.y();
                        int z = wp.z();
                        BARITONE.rightClickBlock(x, y, z)
                            .addExecutedListener(f -> {
                                c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                                    .title("Pathing Completed!")
                                    .addField("Pos", CONFIG.discord.reportCoords
                                        ? "||[" + x + ", " + y + ", " + z + "]||"
                                        : "Coords disabled")
                                    .primaryColor());
                            });
                        c.getSource().getEmbed()
                            .title("Pathing")
                            .addField("Goal", CONFIG.discord.reportCoords
                                ? "||[" + x + ", " + y + ", " + z + "]||"
                                : "Coords disabled")
                            .primaryColor();
                        return OK;
                    }))))
            .then(literal("break").then(argument("pos", blockPos()).executes(c -> {
                BlockPos pos = getBlockPos(c, "pos");
                int x = pos.x();
                int y = pos.y();
                int z = pos.z();
                BARITONE.breakBlock(x, y, z, true)
                    .addExecutedListener(f -> {
                        c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                            .title("Block Broken!")
                            .addField("Target", CONFIG.discord.reportCoords
                                ? "||[" + x + ", " + y + ", " + z + "]||"
                                : "Coords disabled")
                            .primaryColor());
                    });
                c.getSource().getEmbed()
                    .title("Pathing")
                    .addField("Breaking Block", CONFIG.discord.reportCoords
                        ? "||[" + x + ", " + y + ", " + z + "]||"
                        : "Coords disabled")
                    .primaryColor();
            })))
            .then(literal("status").executes(c -> {
                boolean isActive = BARITONE.isActive();
                c.getSource().getEmbed()
                    .title("Pathing Status")
                    .addField("Active", isActive ? "Yes" : "No");
                if (isActive) {
                    c.getSource().getEmbed().primaryColor();
                } else {
                    c.getSource().getEmbed().inQueueColor();
                }
                if (isActive) {
                    BARITONE.getPathingControlManager().mostRecentInControl().ifPresent(
                        process -> c.getSource().getEmbed()
                            .addField("Process", CONFIG.discord.reportCoords
                                ? process.displayName()
                                : "Coords disabled")
                    );
                }
            }))
            .then(literal("settings").executes(c -> {
                var map = getSettingsMap();
                StringBuilder settings = new StringBuilder();
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    settings.append("`").append(entry.getKey()).append("`: ").append(entry.getValue()).append("\n");
                }
                c.getSource().getEmbed()
                    .title("Settings")
                    .description(settings.toString())
                    .primaryColor();
            }))
            .then(literal("allowBreak").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.pathfinder.allowBreak = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Allow Break", CONFIG.client.extra.pathfinder.allowBreak, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("blockBreakAdditionalCost").then(argument("cost", floatArg(0, 1000)).executes(c -> {
                CONFIG.client.extra.pathfinder.blockBreakAdditionalCost = getFloat(c, "cost");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Block Break Additional Cost", CONFIG.client.extra.pathfinder.blockBreakAdditionalCost, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("allowSprint").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.pathfinder.allowSprint = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Allow Sprint", CONFIG.client.extra.pathfinder.allowSprint, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("allowPlace").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.pathfinder.allowPlace = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Allow Place", CONFIG.client.extra.pathfinder.allowPlace, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("allowInventory").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.pathfinder.allowInventory = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Allow Inventory", CONFIG.client.extra.pathfinder.allowInventory, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("allowDownward").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.pathfinder.allowDownward = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Allow Downward", CONFIG.client.extra.pathfinder.allowDownward, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("allowParkour").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.pathfinder.allowParkour = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Allow Parkour", CONFIG.client.extra.pathfinder.allowParkour, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("allowParkourPlace").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.pathfinder.allowParkourPlace = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Allow Parkour Place", CONFIG.client.extra.pathfinder.allowParkourPlace, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("allowParkourAscend").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.pathfinder.allowParkourAscend = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Allow Parkour Ascend", CONFIG.client.extra.pathfinder.allowParkourAscend, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("allowDiagonalDescend").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.pathfinder.allowDiagonalDescend = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Allow Diagonal Descend", CONFIG.client.extra.pathfinder.allowDiagonalDescend, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("allowDiagonalAscend").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.pathfinder.allowDiagonalAscend = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Allow Diagonal Ascend", CONFIG.client.extra.pathfinder.allowDiagonalAscend, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("maxFallHeightNoWater").then(argument("fallHeight", integer()).executes(c -> {
                CONFIG.client.extra.pathfinder.maxFallHeightNoWater = getInteger(c, "fallHeight");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Max Fall Height No Water", CONFIG.client.extra.pathfinder.maxFallHeightNoWater, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("allowLongFall").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.pathfinder.allowLongFall = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Allow Long Fall", CONFIG.client.extra.pathfinder.allowLongFall, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("longFallCostMultiplier").then(argument("multiplier", doubleArg(1.0, 1000.0)).executes(c -> {
                CONFIG.client.extra.pathfinder.longFallCostLogMultiplier = getDouble(c, "multiplier");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Long Fall Cost Log Multiplier", CONFIG.client.extra.pathfinder.longFallCostLogMultiplier, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("longFallCostAddCost").then(argument("cost", doubleArg(1.0, 10000.0)).executes(c -> {
                CONFIG.client.extra.pathfinder.longFallCostAddCost = getDouble(c, "cost");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Long Fall Cost Add Cost", CONFIG.client.extra.pathfinder.longFallCostAddCost, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("primaryTimeoutMs").then(argument("ms", integer(100, 10000)).executes(c -> {
                CONFIG.client.extra.pathfinder.primaryTimeoutMs = getInteger(c, "ms");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Primary Timeout", CONFIG.client.extra.pathfinder.primaryTimeoutMs, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("failureTimeoutMs").then(argument("ms", integer(100, 10000)).executes(c -> {
                CONFIG.client.extra.pathfinder.failureTimeoutMs = getInteger(c, "ms");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Failure Timeout", CONFIG.client.extra.pathfinder.failureTimeoutMs, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("planAheadPrimaryTimeoutMs").then(argument("ms", integer(100, 10000)).executes(c -> {
                CONFIG.client.extra.pathfinder.planAheadPrimaryTimeoutMs = getInteger(c, "ms");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Plan Ahead Primary Timeout", CONFIG.client.extra.pathfinder.planAheadPrimaryTimeoutMs, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("planAheadFailureTimeoutMs").then(argument("ms", integer(100, 10000)).executes(c -> {
                CONFIG.client.extra.pathfinder.planAheadFailureTimeoutMs = getInteger(c, "ms");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Plan Ahead Failure Timeout", CONFIG.client.extra.pathfinder.planAheadFailureTimeoutMs, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("failedPathSearchCooldownMs").then(argument("ms", integer(100, 10000)).executes(c -> {
                CONFIG.client.extra.pathfinder.failedPathSearchCooldownMs = getInteger(c, "ms");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Failed Path Search Cooldown", CONFIG.client.extra.pathfinder.failedPathSearchCooldownMs, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("renderPath").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.pathfinder.renderPath = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Render Path", CONFIG.client.extra.pathfinder.renderPath, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("renderPathIntervalTicks").then(argument("ticks", integer(1, 20)).executes(c -> {
                CONFIG.client.extra.pathfinder.pathRenderIntervalTicks = getInteger(c, "ticks");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Render Path Interval", CONFIG.client.extra.pathfinder.pathRenderIntervalTicks, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("renderPathDetailed").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.pathfinder.renderPathDetailed = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Render Path Detailed", CONFIG.client.extra.pathfinder.renderPathDetailed, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("teleportDelay").then(argument("delay", integer(1)).executes(c -> {
                CONFIG.client.extra.pathfinder.teleportDelayMs = getInteger(c, "delay");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Teleport Delay", CONFIG.client.extra.pathfinder.teleportDelayMs, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("getToBlockExploreForBlocks").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.pathfinder.getToBlockExploreForBlocks = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Get To Block Explore For Blocks", CONFIG.client.extra.pathfinder.getToBlockExploreForBlocks, false)
                    .primaryColor();
                return OK;
            })))
            .then(literal("getToBlockBlacklistClosestOnFailure").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.pathfinder.getToBlockBlacklistClosestOnFailure = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Pathfinder")
                    .addField("Get To Block Blacklist Closest On Failure", CONFIG.client.extra.pathfinder.getToBlockBlacklistClosestOnFailure, false)
                    .primaryColor();
                return OK;
            })));
//            .then(literal("diagonalCentering").then(argument("toggle", toggle()).executes(c -> {
//                CONFIG.client.extra.pathfinder.diagonalCentering = getToggle(c, "toggle");
//                c.getSource().getEmbed()
//                    .title("Pathfinder")
//                    .addField("Diagonal Centering", CONFIG.client.extra.pathfinder.diagonalCentering, false)
//                    .primaryColor();
//                return OK;
//            })))
//            .then(literal("traverseCentering").then(argument("toggle", toggle()).executes(c -> {
//                CONFIG.client.extra.pathfinder.traverseCentering = getToggle(c, "toggle");
//                c.getSource().getEmbed()
//                    .title("Pathfinder")
//                    .addField("Traverse Centering", CONFIG.client.extra.pathfinder.traverseCentering, false)
//                    .primaryColor();
//                return OK;
//            })));
    }

    private Map<String, String> getSettingsMap() {
        LinkedHashMap<String, String> settingsMap = new LinkedHashMap<>();
        settingsMap.put("allowBreak", toggleStr(CONFIG.client.extra.pathfinder.allowBreak));
        settingsMap.put("blockBreakAdditionalCost", String.valueOf(CONFIG.client.extra.pathfinder.blockBreakAdditionalCost));
        settingsMap.put("allowSprint", toggleStr(CONFIG.client.extra.pathfinder.allowSprint));
        settingsMap.put("allowPlace", toggleStr(CONFIG.client.extra.pathfinder.allowPlace));
        settingsMap.put("allowInventory", toggleStr(CONFIG.client.extra.pathfinder.allowInventory));
        settingsMap.put("allowDownward", toggleStr(CONFIG.client.extra.pathfinder.allowDownward));
        settingsMap.put("allowParkour", toggleStr(CONFIG.client.extra.pathfinder.allowParkour));
        settingsMap.put("allowParkourPlace", toggleStr(CONFIG.client.extra.pathfinder.allowParkourPlace));
        settingsMap.put("allowParkourAscend", toggleStr(CONFIG.client.extra.pathfinder.allowParkourAscend));
        settingsMap.put("allowDiagonalDescend", toggleStr(CONFIG.client.extra.pathfinder.allowDiagonalDescend));
        settingsMap.put("allowDiagonalAscend", toggleStr(CONFIG.client.extra.pathfinder.allowDiagonalAscend));
        settingsMap.put("maxFallHeightNoWater", String.valueOf(CONFIG.client.extra.pathfinder.maxFallHeightNoWater));
        settingsMap.put("allowLongFall", toggleStr(CONFIG.client.extra.pathfinder.allowLongFall));
        settingsMap.put("longFallCostMultiplier", String.valueOf(CONFIG.client.extra.pathfinder.longFallCostLogMultiplier));
        settingsMap.put("longFallCostAddCost", String.valueOf(CONFIG.client.extra.pathfinder.longFallCostAddCost));
        settingsMap.put("primaryTimeoutMs", String.valueOf(CONFIG.client.extra.pathfinder.primaryTimeoutMs));
        settingsMap.put("failureTimeoutMs", String.valueOf(CONFIG.client.extra.pathfinder.failureTimeoutMs));
        settingsMap.put("planAheadPrimaryTimeoutMs", String.valueOf(CONFIG.client.extra.pathfinder.planAheadPrimaryTimeoutMs));
        settingsMap.put("planAheadFailureTimeoutMs", String.valueOf(CONFIG.client.extra.pathfinder.planAheadFailureTimeoutMs));
        settingsMap.put("failedPathSearchCooldownMs", String.valueOf(CONFIG.client.extra.pathfinder.failedPathSearchCooldownMs));
        settingsMap.put("renderPath", toggleStr(CONFIG.client.extra.pathfinder.renderPath));
        settingsMap.put("renderPathIntervalTicks", String.valueOf(CONFIG.client.extra.pathfinder.pathRenderIntervalTicks));
        settingsMap.put("renderPathDetailed", toggleStr(CONFIG.client.extra.pathfinder.renderPathDetailed));
        return settingsMap;
    }
}
