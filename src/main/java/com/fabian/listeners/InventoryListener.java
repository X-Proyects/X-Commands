package com.fabian.listeners;

import com.fabian.XCommands;
import com.fabian.executors.CustomCommandExecutor;
import com.fabian.managers.menus.TitleMenu;
import com.fabian.managers.menus.TeleportMenu;
import com.fabian.managers.menus.EffectMenu;
import com.fabian.managers.menus.GiveMenu;
import com.fabian.managers.menus.ParticleMenu;
import com.fabian.managers.menus.ActionReorderMenu;
import com.fabian.managers.menus.NumericActionMenu;
import com.fabian.managers.menus.AliasMenu;
import com.fabian.utils.ColorUtils;
import com.fabian.utils.MenuHolder;
import com.fabian.utils.MenuHolder.MenuType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.fabian.utils.SchedulerUtils;

public class InventoryListener implements Listener {

    private final XCommands plugin;
    private final Map<UUID, ChatInputRequest> chatInputs = new ConcurrentHashMap<>();
    private final Map<UUID, Long> closeWarningTimestamps = new HashMap<>();
    private final Map<UUID, Long> scheduledTransitions = new HashMap<>();

    // Cached Keys
    private final NamespacedKey keyCommandName;
    private final NamespacedKey keyActionIndex;
    private final NamespacedKey keyActionType;

    public InventoryListener(XCommands plugin) {
        this.plugin = plugin;

        // Initialize keys once
        this.keyCommandName = new NamespacedKey(plugin, "command_name");
        this.keyActionIndex = new NamespacedKey(plugin, "action_index");
        this.keyActionType = new NamespacedKey(plugin, "action_type");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder)) {
            return;
        }

        // Clear cursor to prevent ghost items
        if (event.getPlayer().getItemOnCursor() != null
                && event.getPlayer().getItemOnCursor().getType() != Material.AIR) {
            event.getPlayer().setItemOnCursor(null);
        }

        UUID uuid = event.getPlayer().getUniqueId();

        // Check if there's a scheduled transition (within last 100ms)
        if (scheduledTransitions.containsKey(uuid)) {
            long transitionTime = scheduledTransitions.get(uuid);
            if (System.currentTimeMillis() - transitionTime < 100) {
                // This is a scheduled transition, allow it
                return;
            }
            // Clean up old transition
            scheduledTransitions.remove(uuid);
        }

        MenuHolder holder = (MenuHolder) event.getInventory().getHolder();
        if (holder.getType() == MenuType.CONFIRM_DELETE) {
            return;
        }

        String cmdName = holder.getCommandName();

        if (cmdName != null && plugin.getCommandManager().isDirty(cmdName)) {
            long currentTime = System.currentTimeMillis();

            if (closeWarningTimestamps.containsKey(uuid)) {
                long lastTime = closeWarningTimestamps.get(uuid);
                if (currentTime - lastTime < 5000) {
                    // User confirmed exit
                    closeWarningTimestamps.remove(uuid);
                    event.getPlayer().sendMessage(plugin.getLanguageManager().getMessage("changes-discarded"));
                    plugin.getCommandManager().reloadCommand(cmdName);
                    return;
                }
            }

            // First attempt or expired time: Re-open and Warn
            closeWarningTimestamps.put(uuid, currentTime);

            // Re-open inventory to cancel close
            Bukkit.getScheduler().runTask(plugin, () -> {
                event.getPlayer().openInventory(event.getInventory());
            });

            event.getPlayer().sendMessage(plugin.getLanguageManager().getMessage("unsaved-changes-warning"));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder)) {
            return;
        }

        MenuHolder holder = (MenuHolder) event.getInventory().getHolder();
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        String cmdName = holder.getCommandName();

        // 🟢 FIX: Allow REORDER menu to handle clicks on empty slots (for placing items)
        if (holder.getType() == MenuType.ACTION_REORDER) {
            handleActionReorderMenu(event, player, clicked, cmdName);
            return;
        }

        if (clicked == null || clicked.getType() == Material.AIR || clicked.getItemMeta() == null)
            return;

        ItemMeta meta = clicked.getItemMeta();

        switch (holder.getType()) {
            case MAIN:
                handleMainMenu(event, player, clicked, meta, holder);
                break;

            case EDIT:
                handleEditMenu(event, player, clicked, meta, cmdName);
                break;

            case CONFIRM_DELETE:
                handleConfirmDelete(player, clicked, cmdName);
                break;

            case ACTIONS:
                handleActionsMenu(event, player, clicked, meta, cmdName);
                break;

            case ACTION_EDIT:
                handleActionEditMenu(player, clicked, cmdName, holder.getActionIndex());
                break;

            case ACTION_TYPE_SELECTION:
                handleActionTypeSelection(player, clicked, meta, cmdName, holder.getActionIndex());
                break;

            case NUMERIC_ACTION:
                handleNumericActionMenu(event, player, clicked, cmdName, holder.getActionIndex());
                break;

            case TITLE_MENU:
                handleTitleMenu(event, player, clicked, cmdName, holder.getActionIndex());
                break;

            case ALIAS_MENU:
                handleAliasMenu(event, player, clicked, cmdName);
                break;

            case GIVE_MENU:
                handleGiveMenu(event, player, clicked, cmdName, holder.getActionIndex());
                break;
            case TELEPORT_MENU:
            case EFFECT_MENU:
            case PARTICLE_MENU:
                // These menus don't have handlers yet, just cancel the event
                event.setCancelled(true);
                break;
        }
    }

    private void handleMainMenu(InventoryClickEvent event, Player player, ItemStack clicked, ItemMeta meta,
            MenuHolder holder) {
        int page = holder.getPage();

        // Navigation
        if (clicked.getType() == Material.ARROW) {
            String itemName = ColorUtils.stripColor(meta.getDisplayName());
            if (itemName.equals(plugin.getLanguageManager().getMessage("gui-main-prev"))
                    || itemName.equalsIgnoreCase("Anterior")) {
                plugin.getInventoryManager().openMainMenu(player, page - 1);
            } else if (itemName.equals(plugin.getLanguageManager().getMessage("gui-main-next"))
                    || itemName.equalsIgnoreCase("Siguiente")) {
                plugin.getInventoryManager().openMainMenu(player, page + 1);
            }
            return;
        }

        // Get command name from PersistentDataContainer
        String targetCmd = meta.getPersistentDataContainer().get(keyCommandName, PersistentDataType.STRING);

        if (targetCmd == null) {
            // Fallback for Create button or others
            if (clicked.getType() == Material.EMERALD) {
                if (!player.isOp() && !player.hasPermission("xcommands.admin.create")) {
                    player.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return;
                }
                requestChatInput(player, "NEW", InputType.CREATE_COMMAND, -1);
            }
            return;
        }

        if (event.getClick() == ClickType.RIGHT) {
            showActionsInChat(player, targetCmd);
            player.closeInventory();
        } else {
            plugin.getInventoryManager().openCommandEditMenu(player, targetCmd);
        }
    }

    private void handleEditMenu(InventoryClickEvent event, Player player, ItemStack clicked, ItemMeta meta,
            String cmdName) {
        if (clicked.getType() == Material.ARROW) {
            scheduleTransition(player, () -> {
                plugin.getInventoryManager().openMainMenu(player);
            });
            return;
        }

        switch (clicked.getType()) {
            case NAME_TAG:
                if (!hasPermission(player, "xcommands.admin.edit"))
                    return;
                requestChatInput(player, cmdName, InputType.COMMAND_NAME, -1);
                break;

            case BOOK:
                if (!hasPermission(player, "xcommands.admin.edit"))
                    return;
                requestChatInput(player, cmdName, InputType.COMMAND_DESCRIPTION, -1);
                break;

            case BARRIER:
                if (!hasPermission(player, "xcommands.admin.edit"))
                    return;
                requestChatInput(player, cmdName, InputType.COMMAND_PERMISSION, -1);
                break;

            case REPEATER:
            case LEVER:
                scheduleTransition(player, () -> {
                    plugin.getCommandManager().toggleCommandRegistration(cmdName);
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1, 1);
                    plugin.getInventoryManager().openCommandEditMenu(player, cmdName);
                });
                break;

            case CLOCK:
                if (!hasPermission(player, "xcommands.admin.edit"))
                    return;
                requestChatInput(player, cmdName, InputType.COMMAND_COOLDOWN, -1);
                break;

            case COMPASS:
                if (!hasPermission(player, "xcommands.admin.edit"))
                    return;
                requestChatInput(player, cmdName, InputType.COMMAND_INTERVAL, -1);
                break;

            case PAPER:
                if (!hasPermission(player, "xcommands.admin.edit"))
                    return;
                scheduleTransition(player, () -> {
                    new AliasMenu(plugin).open(player, cmdName);
                });
                break;

            case KNOWLEDGE_BOOK:
                scheduleTransition(player, () -> {
                    plugin.getInventoryManager().openActionsMenu(player, cmdName);
                });
                break;

            case COMPARATOR:
                scheduleTransition(player, () -> {
                    new ActionReorderMenu(plugin).open(player, cmdName);
                });
                break;

            case LIME_DYE:
                scheduleTransition(player, () -> {
                    plugin.getCommandManager().saveCommand(cmdName);
                    player.sendMessage(plugin.getLanguageManager().getMessage("command-saved"));
                    plugin.getInventoryManager().openMainMenu(player);
                });
                break;

            case REDSTONE_BLOCK:
                if (!hasPermission(player, "xcommands.admin.delete"))
                    return;
                scheduleTransition(player, () -> {
                    plugin.getInventoryManager().openConfirmationMenu(player, cmdName);
                });
                break;

            default:
                break;
        }
    }

    private void handleConfirmDelete(Player player, ItemStack clicked, String cmdName) {
        if (clicked.getType() == Material.LIME_CONCRETE) {
            if (!hasPermission(player, "xcommands.admin.delete")) {
                player.closeInventory();
                return;
            }
            if (plugin.getCommandManager().deleteCommand(cmdName)) {
                player.sendMessage(plugin.getLanguageManager().getMessage("command-deleted-success"));
                plugin.getInventoryManager().openMainMenu(player);
            }
        } else if (clicked.getType() == Material.RED_CONCRETE) {
            // Cancel deletion - don't show unsaved changes warning
            scheduleTransition(player, () -> {
                plugin.getInventoryManager().openCommandEditMenu(player, cmdName);
            });
        }
    }

    private void handleActionsMenu(InventoryClickEvent event, Player player, ItemStack clicked, ItemMeta meta,
            String cmdName) {
        if (clicked.getType() == Material.ARROW) {
            scheduleTransition(player, () -> {
                plugin.getInventoryManager().openCommandEditMenu(player, cmdName);
            });
            return;
        }

        if (clicked.getType() == Material.HOPPER) {
            scheduleTransition(player, () -> {
                new ActionReorderMenu(plugin).open(player, cmdName);
            });
            return;
        }

        if (clicked.getType() == Material.NETHER_STAR) {
            if (!hasPermission(player, "xcommands.admin.edit"))
                return;

            CustomCommandExecutor exec = plugin.getCommandManager().getCustomCommands().get(cmdName.toLowerCase());
            if (exec != null) {
                List<String> actions = exec.getActions();
                actions.add("[MESSAGE] Nueva acción");
                plugin.getCommandManager().markDirty(cmdName);
                // Folia/Standard compatible delay for inventory closing/opening
                SchedulerUtils.runTaskLater(plugin, () -> {
                    player.closeInventory();
                    plugin.getInventoryManager().openActionTypeSelectionMenu(player, cmdName, actions.size() - 1);
                }, 1L);
            }
            return;
        }

        if (clicked.getType() == Material.PAPER) {
            // Try to get index from PersistentDataContainer first
            Integer index = null;

            if (meta.getPersistentDataContainer().has(keyActionIndex, PersistentDataType.INTEGER)) {
                index = meta.getPersistentDataContainer().get(keyActionIndex, PersistentDataType.INTEGER);
            } else {
                // Fallback for legacy/other items (though should not happen with new menu)
                String displayName = ColorUtils.stripColor(meta.getDisplayName());
                if (displayName.startsWith("Acción #") || displayName.startsWith("Action #")) {
                    try {
                        // Simple heuristic for fallback
                        String numberPart = displayName.substring(displayName.lastIndexOf("#") + 1).trim();
                        index = Integer.parseInt(numberPart) - 1;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            if (index == null) {
                // Debug log
                plugin.logWarning("Could not determine action index for command " + cmdName);
                return;
            }

            if (event.getClick() == ClickType.SHIFT_RIGHT) {
                if (!hasPermission(player, "xcommands.admin.edit"))
                    return;

                CustomCommandExecutor exec = plugin.getCommandManager().getCustomCommands().get(cmdName.toLowerCase());
                if (exec != null) {
                    List<String> actions = exec.getActions();
                    if (index >= 0 && index < actions.size()) {
                        actions.remove((int) index); // Cast to int to ensure index removal, not object
                        plugin.getCommandManager().markDirty(cmdName);
                        scheduleTransition(player, () -> {
                            plugin.getInventoryManager().openActionsMenu(player, cmdName);
                        });
                    }
                }
            } else {
                if (!hasPermission(player, "xcommands.admin.edit"))
                    return;

                final int finalIndex = index;
                scheduleTransition(player, () -> {
                    // Add debug log if it fails to open
                    plugin.getInventoryManager().openActionEditMenu(player, cmdName, finalIndex);
                });
            }
        }
    }

    private void handleActionEditMenu(Player player, ItemStack clicked, String cmdName, int actionIdx) {
        if (clicked.getType() == Material.ARROW) {
            scheduleTransition(player, () -> {
                plugin.getInventoryManager().openActionsMenu(player, cmdName);
            });
            return;
        }

        if (clicked.getType() == Material.COMPARATOR) {
            if (!hasPermission(player, "xcommands.admin.edit"))
                return;

            scheduleTransition(player, () -> {
                plugin.getInventoryManager().openActionTypeSelectionMenu(player, cmdName, actionIdx);
            });
        } else if (clicked.getType() == Material.PAPER) {
            if (!hasPermission(player, "xcommands.admin.edit"))
                return;

            // Get current action to determine which menu to open
            CustomCommandExecutor exec = plugin.getCommandManager().getCustomCommands().get(cmdName.toLowerCase());
            if (exec != null && actionIdx >= 0 && actionIdx < exec.getActions().size()) {
                String action = exec.getActions().get(actionIdx);
                String actionType = "MESSAGE";

                if (action.startsWith("[")) {
                    int end = action.indexOf("]");
                    if (end != -1) {
                        actionType = action.substring(1, end);
                    }
                }

                // Route to specialized menu based on action type
                switch (actionType.toUpperCase()) {
                    case "GIVE_MONEY":
                    case "TAKE_MONEY":
                    case "DAMAGE":
                    case "DELAY":
                        // Open numeric menu
                        double currentValue = 0;
                        try {
                            String valueStr = action.substring(action.indexOf("]") + 1).trim();
                            currentValue = Double.parseDouble(valueStr);
                        } catch (Exception ignored) {
                        }

                        final double finalValue = currentValue;
                        final String finalType = actionType;
                        scheduleTransition(player, () -> {
                            new NumericActionMenu(plugin).open(player, cmdName, actionIdx, finalType, finalValue);
                        });
                        break;

                    case "TITLE":
                        // Open title menu
                        scheduleTransition(player, () -> {
                            new TitleMenu(plugin).open(player, cmdName, actionIdx);
                        });
                        break;

                    case "TELEPORT":
                        // Open teleport menu
                        scheduleTransition(player, () -> {
                            new TeleportMenu(plugin).open(player, cmdName, actionIdx);
                        });
                        break;

                    case "EFFECT":
                        // Open effect menu
                        scheduleTransition(player, () -> {
                            new EffectMenu(plugin).open(player, cmdName, actionIdx);
                        });
                        break;

                    case "GIVE":
                        // Open give menu
                        scheduleTransition(player, () -> {
                            new GiveMenu(plugin).open(player, cmdName, actionIdx);
                        });
                        break;

                    case "PARTICLE":
                        // Open particle menu
                        scheduleTransition(player, () -> {
                            new ParticleMenu(plugin).open(player, cmdName, actionIdx);
                        });
                        break;

                    // For simple text actions, use chat input
                    case "MESSAGE":
                    case "BROADCAST":
                    case "ACTIONBAR":
                    case "CONSOLE":
                    case "PLAYER":
                    case "KICK":
                    case "BUNGEE":
                    default:
                        requestChatInput(player, cmdName, InputType.ACTION_CONTENT, actionIdx);
                        break;
                }
            }
        }
    }

    private void handleActionTypeSelection(Player player, ItemStack clicked, ItemMeta meta, String cmdName,
            int actionIndex) {
        if (clicked.getType() == Material.ARROW) {
            scheduleTransition(player, () -> {
                plugin.getInventoryManager().openActionEditMenu(player, cmdName, actionIndex);
            });
            return;
        }

        // Try to get action tag from PDC first (Robust method)
        String tag = null;
        if (meta.getPersistentDataContainer().has(keyActionType, PersistentDataType.STRING)) {
            tag = meta.getPersistentDataContainer().get(keyActionType, PersistentDataType.STRING);
        }

        // Fallback to Display Name parsing (Legacy method)
        if (tag == null) {
            tag = ColorUtils.stripColor(meta.getDisplayName()).toUpperCase().replace(" ", "_");
            // Map legacy localized names to tags
            if (tag.equals("DAR_ITEM"))
                tag = "GIVE";
            if (tag.equals("TELETRANSPORTE"))
                tag = "TELEPORT";
            if (tag.equals("ESPERA_(TICKS)"))
                tag = "DELAY";
            if (tag.equals("CONSOLA"))
                tag = "CONSOLE";
            if (tag.equals("JUGADOR"))
                tag = "PLAYER";
            if (tag.equals("EXPULSAR"))
                tag = "KICK";
            if (tag.equals("CERRAR_INV"))
                tag = "CLOSE_INVENTORY";
        }

        CustomCommandExecutor exec = plugin.getCommandManager().getCustomCommands().get(cmdName.toLowerCase());
        if (exec != null && actionIndex >= 0 && actionIndex < exec.getActions().size()) {
            String oldAction = exec.getActions().get(actionIndex);
            String params = "";
            if (oldAction.contains("]")) {
                params = oldAction.substring(oldAction.indexOf("]") + 1).trim();
            } else {
                // If old action was plain text (MESSAGE), allow keeping it as params if
                // plausible
                if (!oldAction.startsWith("[")) {
                    params = oldAction;
                }
            }

            String newAction = "[" + tag + "] " + params;
            plugin.getCommandManager().editAction(cmdName, actionIndex, newAction);
            plugin.getCommandManager().markDirty(cmdName);
            player.sendMessage(plugin.getLanguageManager().getMessage("action-type-changed", tag));

            scheduleTransition(player, () -> {
                plugin.getInventoryManager().openActionEditMenu(player, cmdName, actionIndex);
            });
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatInputRequest request = chatInputs.get(player.getUniqueId());

        if (request == null)
            return;

        event.setCancelled(true);
        String input = event.getMessage();
        chatInputs.remove(player.getUniqueId());

        if (input.equalsIgnoreCase("cancelar") || input.equalsIgnoreCase("cancel")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("input-cancelled"));
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getInventoryManager().openCommandEditMenu(player, request.commandName);
            });
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (request.type) {
                case COMMAND_NAME:
                    String newName = plugin.getCommandManager().updateConfigValue(request.commandName, "name", input);
                    player.sendMessage(plugin.getLanguageManager().getMessage("command-name-updated", input));
                    plugin.getInventoryManager().openCommandEditMenu(player, newName);
                    return;

                case COMMAND_DESCRIPTION:
                    plugin.getCommandManager().updateConfigValue(request.commandName, "description", input);
                    player.sendMessage(plugin.getLanguageManager().getMessage("command-description-updated"));
                    break;

                case COMMAND_PERMISSION:
                    plugin.getCommandManager().updateConfigValue(request.commandName, "permission", input);
                    player.sendMessage(plugin.getLanguageManager().getMessage("command-permission-updated", input));
                    break;

                case COMMAND_COOLDOWN:
                    plugin.getCommandManager().updateConfigValue(request.commandName, "cooldown", input);
                    plugin.getCommandManager().markDirty(request.commandName);
                    player.sendMessage(plugin.getLanguageManager().getMessage("command-cooldown-updated", input));
                    break;

                case COMMAND_INTERVAL:
                    plugin.getCommandManager().updateConfigValue(request.commandName, "interval", input);
                    plugin.getCommandManager().markDirty(request.commandName);
                    player.sendMessage(plugin.getLanguageManager().getMessage("command-interval-updated", input));
                    break;

                case COMMAND_ALIAS:
                    plugin.getCommandManager().updateConfigValue(request.commandName, "aliases", input);
                    plugin.getCommandManager().markDirty(request.commandName);
                    player.sendMessage(plugin.getLanguageManager().getMessage("command-alias-updated"));
                    break;

                case ALIAS_NEW:
                    CustomCommandExecutor aliasExecNew = plugin.getCommandManager().getCustomCommands()
                            .get(request.commandName.toLowerCase());
                    if (aliasExecNew != null) {
                        aliasExecNew.getAliases().add(input);
                        plugin.getCommandManager().markDirty(request.commandName);
                        player.sendMessage(plugin.getLanguageManager().getMessage("alias-added"));
                    }
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        new AliasMenu(plugin).open(player, request.commandName);
                    }, 1L);
                    return;

                case ALIAS_EDIT:
                    CustomCommandExecutor aliasExecEdit = plugin.getCommandManager().getCustomCommands()
                            .get(request.commandName.toLowerCase());
                    if (aliasExecEdit != null && request.actionIndex >= 0
                            && request.actionIndex < aliasExecEdit.getAliases().size()) {
                        aliasExecEdit.getAliases().set(request.actionIndex, input);
                        plugin.getCommandManager().markDirty(request.commandName);
                        player.sendMessage(plugin.getLanguageManager().getMessage("alias-updated"));
                    }
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        new AliasMenu(plugin).open(player, request.commandName);
                    }, 1L);
                    return;

                case ACTION_CONTENT:
                    // Fix: Preserve Action Type
                    CustomCommandExecutor exec = plugin.getCommandManager().getCustomCommands()
                            .get(request.commandName.toLowerCase());
                    String currentAction = "";
                    if (exec != null && request.actionIndex >= 0 && request.actionIndex < exec.getActions().size()) {
                        currentAction = exec.getActions().get(request.actionIndex);
                    }

                    String newContent = input;
                    // Scan for [TYPE]
                    if (currentAction.startsWith("[")) {
                        int endIdx = currentAction.indexOf("]");
                        if (endIdx != -1) {
                            String type = currentAction.substring(0, endIdx + 1);
                            newContent = type + " " + input;
                        }
                    }

                    plugin.getCommandManager().editAction(request.commandName, request.actionIndex, newContent);
                    plugin.getCommandManager().markDirty(request.commandName);
                    player.sendMessage(plugin.getLanguageManager().getMessage("action-updated"));
                    plugin.getInventoryManager().openActionEditMenu(player, request.commandName, request.actionIndex);
                    return;

                case CREATE_COMMAND:
                    if (!input.matches("[a-zA-Z0-9_]+")) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("input-invalid-name"));
                        return;
                    }
                    if (plugin.getCommandManager().createCommand(input)) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("create-success-msg"));
                        Bukkit.getScheduler().runTask(plugin,
                                () -> plugin.getInventoryManager().openCommandEditMenu(player, input));
                        return;
                    } else {
                        player.sendMessage(plugin.getLanguageManager().getMessage("input-command-exists"));
                    }
                    Bukkit.getScheduler().runTask(plugin, () -> plugin.getInventoryManager().openMainMenu(player));
                    return;

                case TITLE_MAIN:
                case TITLE_SUB:
                case TITLE_FADEIN:
                case TITLE_STAY:
                case TITLE_FADEOUT:
                    // Update title action with new value
                    CustomCommandExecutor titleExecutor = plugin.getCommandManager().getCustomCommands()
                            .get(request.commandName.toLowerCase());
                    if (titleExecutor != null && request.actionIndex >= 0
                            && request.actionIndex < titleExecutor.getActions().size()) {
                        String titleAction = titleExecutor.getActions().get(request.actionIndex);
                        String[] parts = parseTitleAction(titleAction);

                        // Update the appropriate field
                        switch (request.type) {
                            case TITLE_MAIN:
                                parts[0] = input;
                                break;
                            case TITLE_SUB:
                                parts[1] = input;
                                break;
                            case TITLE_FADEIN:
                                parts[2] = input;
                                break;
                            case TITLE_STAY:
                                parts[3] = input;
                                break;
                            case TITLE_FADEOUT:
                                parts[4] = input;
                                break;
                            default:
                                break;
                        }

                        // Rebuild action string
                        String newAction = "[TITLE] " + parts[0] + ";" + parts[1] + ";" + parts[2] + ";" + parts[3]
                                + ";" + parts[4];
                        plugin.getCommandManager().editAction(request.commandName, request.actionIndex, newAction);
                        plugin.getCommandManager().markDirty(request.commandName);
                        player.sendMessage(plugin.getLanguageManager().getMessage("action-updated"));
                    }

                    // Reopen title menu
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        new TitleMenu(plugin).open(player, request.commandName, request.actionIndex);
                    }, 1L);
                    return;

                default:
                    break;
            }
            plugin.getInventoryManager().openCommandEditMenu(player, request.commandName);
        });
    }

    private String[] parseTitleAction(String action) {
        String[] result = { "", "", "10", "70", "20" }; // defaults

        if (action.contains("]")) {
            String content = action.substring(action.indexOf("]") + 1).trim();
            String[] parts = content.split(";");
            for (int i = 0; i < Math.min(parts.length, 5); i++) {
                result[i] = parts[i];
            }
        }

        return result;
    }

    private void showActionsInChat(Player player, String commandName) {
        CustomCommandExecutor executor = plugin.getCommandManager().getCustomCommands().get(commandName.toLowerCase());
        if (executor == null)
            return;

        player.sendMessage(plugin.getLanguageManager().getMessage("help-header")); // Reusing help header for
                                                                                   // consistency
        List<String> actions = executor.getActions();
        for (int i = 0; i < actions.size(); i++) {
            player.sendMessage(ColorUtils.translate("&e" + (i + 1) + ". &f" + actions.get(i)));
        }
        player.sendMessage(plugin.getLanguageManager().getMessage("help-footer"));
    }

    private void requestChatInput(Player player, String commandName, InputType type, int actionIndex) {
        scheduledTransitions.put(player.getUniqueId(), System.currentTimeMillis());
        chatInputs.put(player.getUniqueId(), new ChatInputRequest(commandName, type, actionIndex));
        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(plugin.getLanguageManager().getMessage("input-wait"));
        player.sendMessage(plugin.getLanguageManager().getMessage("input-instructions"));
        player.sendMessage("");
    }

    private void scheduleTransition(Player player, Runnable action) {
        scheduledTransitions.put(player.getUniqueId(), System.currentTimeMillis());
        player.closeInventory();
        SchedulerUtils.runTaskLater(plugin, action, 1L);
    }

    private void handleActionReorderMenu(InventoryClickEvent event, Player player, ItemStack clicked, String cmdName) {
        // Allow drag and drop in the top inventory
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            if (event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
            return;
        }

        int slot = event.getSlot();

        // 1. Handle Trash Can (Slot 49)
        if (slot == 49) {
            event.setCancelled(true);
            // If cursor has an item, delete it
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                event.getView().setCursor(null); // Delete item using non-deprecated method
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_LAVA_EXTINGUISH, 1, 1);
                player.sendMessage(plugin.getLanguageManager().getMessage("action-deleted"));
            }
            return;
        }

        // 2. Handle Back Button (Slot 45)
        if (slot == 45) {
            event.setCancelled(true);
            // Clear cursor if user is holding an item while clicking back
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                event.getView().setCursor(null);
            }
            saveReorderedActions(player, event.getView().getTopInventory(), cmdName);
            scheduleTransition(player, () -> {
                plugin.getInventoryManager().openCommandEditMenu(player, cmdName);
            });
            return;
        }

        // 3. Block other functional slots
        if (slot >= 36) {
            event.setCancelled(true);
            return;
        }

        // 4. Update 'action_index' in lore/PDC if necessary?
        // Actually we don't need to update index on every move.
        // We only care about the final order when saving.
        // So allow default behavior for slots 0-35
        event.setCancelled(false);
    }

    private void handleNumericActionMenu(InventoryClickEvent event, Player player, ItemStack clicked, String cmdName,
            int actionIndex) {
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        // Back button
        if (clicked.getType() == Material.ARROW) {
            scheduleTransition(player, () -> {
                plugin.getInventoryManager().openActionEditMenu(player, cmdName, actionIndex);
            });
            return;
        }

        // Confirm button
        if (clicked.getType() == Material.LIME_DYE) {
            // Get current value from center item
            ItemStack centerItem = event.getView().getTopInventory().getItem(13);
            if (centerItem != null && centerItem.getItemMeta() != null) {
                ItemMeta meta = centerItem.getItemMeta();
                if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "numeric_value"),
                        PersistentDataType.DOUBLE)) {
                    double value = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "numeric_value"),
                            PersistentDataType.DOUBLE);
                    String actionType = meta.getPersistentDataContainer()
                            .get(new NamespacedKey(plugin, "action_type_tag"), PersistentDataType.STRING);

                    // Update action
                    String newAction;
                    if (actionType.equals("GIVE_AMOUNT")) {
                        // Special case for GiveMenu: [GIVE] material;amount;customName
                        CustomCommandExecutor exec = plugin.getCommandManager().getCustomCommands()
                                .get(cmdName.toLowerCase());
                        String current = exec.getActions().get(actionIndex);
                        String content = current.substring(current.indexOf("]") + 1).trim();
                        String[] parts = content.split(";");
                        String material = parts.length > 0 ? parts[0] : "DIAMOND";
                        String customName = parts.length > 2 ? parts[2] : "";
                        newAction = "[GIVE] " + material + ";" + (int) value + ";" + customName;
                    } else {
                        newAction = "[" + actionType + "] " + (int) value;
                    }

                    plugin.getCommandManager().editAction(cmdName, actionIndex, newAction);
                    plugin.getCommandManager().markDirty(cmdName);
                    player.sendMessage(plugin.getLanguageManager().getMessage("action-updated"));

                    scheduleTransition(player, () -> {
                        if (actionType.equals("GIVE_AMOUNT")) {
                            new GiveMenu(plugin).open(player, cmdName, actionIndex);
                        } else {
                            plugin.getInventoryManager().openActionEditMenu(player, cmdName, actionIndex);
                        }
                    });
                }
            }
            return;
        }

        // Reset button
        if (clicked.getType() == Material.BARRIER) {
            ItemStack centerItem = event.getView().getTopInventory().getItem(13);
            if (centerItem != null && centerItem.getItemMeta() != null) {
                ItemMeta meta = centerItem.getItemMeta();
                String actionType = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "action_type_tag"),
                        PersistentDataType.STRING);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "numeric_value"),
                        PersistentDataType.DOUBLE, 0.0);
                meta.setDisplayName(ColorUtils.translate("&e" + actionType + ": &f0"));
                centerItem.setItemMeta(meta);
                event.getView().getTopInventory().setItem(13, centerItem);
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1, 1);
            }
            return;
        }

        // Add buttons
        if (clicked.getType() == Material.EMERALD) {
            String displayName = ColorUtils.stripColor(clicked.getItemMeta().getDisplayName());
            if (displayName.startsWith("+")) {
                try {
                    int addValue = Integer.parseInt(displayName.substring(1));
                    ItemStack centerItem = event.getView().getTopInventory().getItem(13);
                    if (centerItem != null && centerItem.getItemMeta() != null) {
                        ItemMeta meta = centerItem.getItemMeta();
                        double currentValue = meta.getPersistentDataContainer()
                                .get(new NamespacedKey(plugin, "numeric_value"), PersistentDataType.DOUBLE);
                        String actionType = meta.getPersistentDataContainer()
                                .get(new NamespacedKey(plugin, "action_type_tag"), PersistentDataType.STRING);
                        double newValue = currentValue + addValue;
                        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "numeric_value"),
                                PersistentDataType.DOUBLE, newValue);
                        meta.setDisplayName(ColorUtils.translate("&e" + actionType + ": &f" + (int) newValue));
                        centerItem.setItemMeta(meta);
                        event.getView().getTopInventory().setItem(13, centerItem);
                        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1, 1);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return;
        }
    }

    private void handleAliasMenu(InventoryClickEvent event, Player player, ItemStack clicked, String cmdName) {
        event.setCancelled(true);
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        int slot = event.getRawSlot();
        CustomCommandExecutor executor = plugin.getCommandManager().getCustomCommands().get(cmdName.toLowerCase());
        if (executor == null)
            return;

        // Back button
        if (slot == 27) {
            scheduleTransition(player, () -> {
                plugin.getInventoryManager().openCommandEditMenu(player, cmdName);
            });
            return;
        }

        // Add alias button
        if (slot == 31) {
            requestChatInput(player, cmdName, InputType.ALIAS_NEW, -1);
            return;
        }

        // Alias item interaction
        for (int i = 0; i < AliasMenu.ALIAS_SLOTS.length; i++) {
            if (slot == AliasMenu.ALIAS_SLOTS[i] && i < executor.getAliases().size()) {
                if (event.isLeftClick() && event.isShiftClick()) {
                    // Delete
                    executor.getAliases().remove(i);
                    plugin.getCommandManager().markDirty(cmdName);
                    player.sendMessage(plugin.getLanguageManager().getMessage("alias-deleted"));
                    new AliasMenu(plugin).open(player, cmdName);
                } else if (event.isRightClick()) {
                    // Edit
                    requestChatInput(player, cmdName, InputType.ALIAS_EDIT, i);
                }
                return;
            }
        }
    }

    private void handleTitleMenu(InventoryClickEvent event, Player player, ItemStack clicked, String cmdName,
            int actionIndex) {
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        event.setCancelled(true);

        if (clicked == null || !clicked.hasItemMeta())
            return;

        int slot = event.getSlot();

        // Slot 11: Main Title
        if (slot == 11) {
            requestChatInput(player, cmdName, InputType.TITLE_MAIN, actionIndex);
            return;
        }

        // Slot 13: Subtitle
        if (slot == 13) {
            requestChatInput(player, cmdName, InputType.TITLE_SUB, actionIndex);
            return;
        }

        // Slot 15: Fade In
        if (slot == 15) {
            requestChatInput(player, cmdName, InputType.TITLE_FADEIN, actionIndex);
            return;
        }

        // Slot 20: Stay
        if (slot == 20) {
            requestChatInput(player, cmdName, InputType.TITLE_STAY, actionIndex);
            return;
        }

        // Slot 22: Fade Out
        if (slot == 22) {
            requestChatInput(player, cmdName, InputType.TITLE_FADEOUT, actionIndex);
            return;
        }

        // Slot 25: Confirm
        if (slot == 25) {
            scheduleTransition(player, () -> {
                plugin.getInventoryManager().openActionsMenu(player, cmdName);
            });
            return;
        }

        // Slot 18: Back
        if (slot == 18) {
            scheduleTransition(player, () -> {
                plugin.getInventoryManager().openActionEditMenu(player, cmdName, actionIndex);
            });
        }
    }

    private void saveReorderedActions(Player player, org.bukkit.inventory.Inventory inv, String cmdName) {
        CustomCommandExecutor executor = plugin.getCommandManager().getCustomCommands().get(cmdName.toLowerCase());
        if (executor == null)
            return;

        List<String> newActions = new java.util.ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                // Recover action string from PDC
                if (item.getItemMeta() != null && item.getItemMeta().getPersistentDataContainer().has(
                        new NamespacedKey(plugin, "action_content"), PersistentDataType.STRING)) {
                    String action = item.getItemMeta().getPersistentDataContainer().get(
                            new NamespacedKey(plugin, "action_content"), PersistentDataType.STRING);
                    newActions.add(action);
                } else {
                    // Fallback to display name stripping if PDC fails
                    ColorUtils.stripColor(item.getItemMeta().getDisplayName());
                }
            }
        }

        plugin.getCommandManager().saveActions(cmdName, newActions);
        plugin.getCommandManager().markDirty(cmdName);
        player.sendMessage(plugin.getLanguageManager().getMessage("actions-reordered"));
    }

    private boolean hasPermission(Player player, String permission) {
        if (!player.isOp() && !player.hasPermission(permission)) {
            player.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
            return false;
        }
        return true;
    }

    private enum InputType {
        COMMAND_NAME, COMMAND_DESCRIPTION, COMMAND_PERMISSION, COMMAND_COOLDOWN, COMMAND_INTERVAL, COMMAND_ALIAS,
        ALIAS_NEW, ALIAS_EDIT,
        ACTION_CONTENT, CREATE_COMMAND,
        TITLE_MAIN, TITLE_SUB, TITLE_FADEIN, TITLE_STAY, TITLE_FADEOUT
    }

    private void handleGiveMenu(InventoryClickEvent event, Player player, ItemStack clicked, String cmdName,
            int actionIndex) {
        event.setCancelled(true);
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        int slot = event.getSlot();

        // Back button
        if (slot == 18) {
            scheduleTransition(player, () -> {
                plugin.getInventoryManager().openActionEditMenu(player, cmdName, actionIndex);
            });
            return;
        }

        // Amount button (Slot 13)
        if (slot == 13) {
            // Get current amount from action
            CustomCommandExecutor exec = plugin.getCommandManager().getCustomCommands().get(cmdName.toLowerCase());
            if (exec != null && actionIndex >= 0 && actionIndex < exec.getActions().size()) {
                String action = exec.getActions().get(actionIndex);
                String amountStr = "1";
                if (action.contains("]")) {
                    String content = action.substring(action.indexOf("]") + 1).trim();
                    String[] parts = content.split(";");
                    if (parts.length > 1)
                        amountStr = parts[1];
                }

                double currentAmount = 1;
                try {
                    currentAmount = Double.parseDouble(amountStr);
                } catch (Exception ignored) {
                }

                final double finalAmount = currentAmount;
                scheduleTransition(player, () -> {
                    new NumericActionMenu(plugin).open(player, cmdName, actionIndex, "GIVE_AMOUNT", finalAmount);
                });
            }
            return;
        }

        // Material (Slot 11) or Name (Slot 15)
        if (slot == 11 || slot == 15) {
            requestChatInput(player, cmdName, InputType.ACTION_CONTENT, actionIndex);
            return;
        }

        // Confirm
        if (slot == 25) {
            scheduleTransition(player, () -> {
                plugin.getInventoryManager().openActionEditMenu(player, cmdName, actionIndex);
            });
        }
    }

    private static class ChatInputRequest {
        String commandName;
        InputType type;
        int actionIndex;

        ChatInputRequest(String commandName, InputType type, int actionIndex) {
            this.commandName = commandName;
            this.type = type;
            this.actionIndex = actionIndex;
        }
    }
}
