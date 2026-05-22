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
import com.fabian.managers.menus.SoundMenu;
import com.fabian.utils.MenuHolder;
import com.fabian.utils.MenuHolder.MenuType;
import com.fabian.utils.SchedulerUtils;
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
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        chatInputs.remove(uuid);
        closeWarningTimestamps.remove(uuid);
        scheduledTransitions.remove(uuid);
        plugin.getCommandManager().clearPlayerCooldowns(uuid);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder)) {
            return;
        }

        // Clear cursor to prevent ghost items
        if (event.getPlayer().getItemOnCursor() != null
                && event.getPlayer().getItemOnCursor().getType() != Material.AIR) {
            
            MenuHolder holder = (MenuHolder) event.getInventory().getHolder();
            // If in ACTION_REORDER, handle cursor item BEFORE clearing it
            if (holder.getMenuType() == MenuType.ACTION_REORDER) {
                ItemStack cursor = event.getPlayer().getItemOnCursor();
                org.bukkit.inventory.Inventory topInv = event.getInventory();
                for (int i = 0; i < 36; i++) {
                    if (topInv.getItem(i) == null || topInv.getItem(i).getType() == Material.AIR) {
                        topInv.setItem(i, cursor);
                        break;
                    }
                }
            }
            
            event.getPlayer().setItemOnCursor(null);
        }

        UUID uuid = event.getPlayer().getUniqueId();

        // Check if there's a scheduled transition (within last 1500ms)
        if (scheduledTransitions.containsKey(uuid)) {
            long transitionTime = scheduledTransitions.remove(uuid); // Consume the transition
            if (System.currentTimeMillis() - transitionTime < 1500) {
                // This is a scheduled transition, allow it
                return;
            }
        }

        if (chatInputs.containsKey(uuid)) {
            return; // Player is typing in chat, do not warn about unsaved changes
        }

        MenuHolder holder = (MenuHolder) event.getInventory().getHolder();
        if (holder.getMenuType() == MenuType.CONFIRM_DELETE) {
            return;
        }

        String cmdName = holder.getCommandName();

        // Automatically save reordered actions on close, so that changes are tracked
        if (holder.getMenuType() == MenuType.ACTION_REORDER) {
            saveReorderedActions((Player) event.getPlayer(), event.getInventory(), cmdName);
        }

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
            SchedulerUtils.runForPlayer(plugin, event.getPlayer(), () -> {
                event.getPlayer().openInventory(event.getInventory());
            });
            
            event.getPlayer().sendMessage(plugin.getLanguageManager().getMessage("unsaved-changes-warning"));
        }
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Block clicks during transitions (with 500ms safety timeout)
        if (scheduledTransitions.containsKey(player.getUniqueId())) {
            long time = scheduledTransitions.get(player.getUniqueId());
            if (System.currentTimeMillis() - time > 500) {
                scheduledTransitions.remove(player.getUniqueId());
            } else {
                event.setCancelled(true);
                return;
            }
        }

        MenuHolder holder = (MenuHolder) event.getInventory().getHolder();
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        String cmdName = holder.getCommandName();

        // Allow REORDER menu to handle clicks on empty slots (for placing items)
        if (holder.getMenuType() == MenuType.ACTION_REORDER) {
            handleActionReorderMenu(event, player, clicked, cmdName);
            return;
        }

        if (clicked == null || clicked.getType() == Material.AIR || clicked.getItemMeta() == null)
            return;

        ItemMeta meta = clicked.getItemMeta();
        // Ignore filler items with empty name " "
        if (com.fabian.utils.ColorUtils.stripColor(com.fabian.utils.CompatibilityUtils.getDisplayName(meta)).trim().isEmpty()) {
            return;
        }

        switch (holder.getMenuType()) {
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
                handleActionTypeSelection(event, player, clicked, meta, cmdName, holder.getActionIndex());
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
                handleTeleportMenu(event, player, clicked, cmdName, holder.getActionIndex());
                break;
            case EFFECT_MENU:
                handleEffectMenu(event, player, clicked, cmdName, holder.getActionIndex());
                break;
            case PARTICLE_MENU:
                handleParticleMenu(event, player, clicked, cmdName, holder.getActionIndex());
                break;
            case SOUND_MENU:
                handleSoundMenu(event, player, clicked, cmdName, holder.getActionIndex());
                break;
            default:
                break;
        }
    }

    private void handleMainMenu(InventoryClickEvent event, Player player, ItemStack clicked, ItemMeta meta,
            MenuHolder holder) {
        int page = holder.getPage();

        // Help button
        if (clicked.getType() == Material.BOOK) {
            player.closeInventory();
            player.performCommand("xc");
            return;
        }

        // Navigation
        if (clicked.getType() == Material.ARROW) {
            String itemName = com.fabian.utils.ColorUtils.stripColor(com.fabian.utils.CompatibilityUtils.getDisplayName(meta));
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
            if (clicked.getType() == Material.NETHER_STAR) {
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
        int slot = event.getSlot();

        // Check permission once for most edit actions
        if (slot != 36 && slot != 40 && !hasPermission(player, "xcommands.admin.edit")) {
            if (slot != 44 || !hasPermission(player, "xcommands.admin.delete")) {
                return;
            }
        }

        switch (slot) {
            case 10: // Name
                requestChatInput(player, cmdName, InputType.COMMAND_NAME, -1);
                break;

            case 11: // Description
                requestChatInput(player, cmdName, InputType.COMMAND_DESCRIPTION, -1);
                break;

            case 12: // Permission
                requestChatInput(player, cmdName, InputType.COMMAND_PERMISSION, -1);
                break;

            case 13: // Registration Toggle
                scheduleTransition(player, () -> {
                    plugin.getCommandManager().toggleCommandRegistration(cmdName);
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1, 1);
                    plugin.getInventoryManager().openCommandEditMenu(player, cmdName);
                });
                break;

            case 15: // Actions Menu
                scheduleTransition(player, () -> {
                    plugin.getInventoryManager().openActionsMenu(player, cmdName);
                });
                break;

            case 16: // Reorder Menu
                scheduleTransition(player, () -> {
                    new ActionReorderMenu(plugin).open(player, cmdName);
                });
                break;

            case 19: // Cooldown
                requestChatInput(player, cmdName, InputType.COMMAND_COOLDOWN, -1);
                break;

            case 20: // Aliases
                scheduleTransition(player, () -> {
                    new AliasMenu(plugin).open(player, cmdName);
                });
                break;

            case 21: // Interval
                requestChatInput(player, cmdName, InputType.COMMAND_INTERVAL, -1);
                break;

            case 22: // Material/Icon Change
                requestChatInput(player, cmdName, InputType.COMMAND_MATERIAL, -1);
                break;

            case 36: // Back Button
                scheduleTransition(player, () -> {
                    plugin.getInventoryManager().openMainMenu(player);
                });
                break;

            case 40: // Save Changes
                scheduleTransition(player, () -> {
                    plugin.getCommandManager().saveCommand(cmdName);
                    player.sendMessage(plugin.getLanguageManager().getMessage("command-saved"));
                    plugin.getInventoryManager().openMainMenu(player);
                });
                break;

            case 44: // Delete Command
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

        if (clicked.getType() == Material.NETHER_STAR && !meta.getPersistentDataContainer().has(keyActionIndex, PersistentDataType.INTEGER)) {
            if (!hasPermission(player, "xcommands.admin.edit"))
                return;

            CustomCommandExecutor exec = plugin.getCommandManager().getCustomCommands().get(cmdName.toLowerCase());
            if (exec != null) {
                scheduleTransition(player, () -> {
                    plugin.getInventoryManager().openActionTypeSelectionMenu(player, cmdName, -1);
                });
            }
            return;
        }

        // Identify if it's an action item by checking for the action_index key
        if (meta.getPersistentDataContainer().has(keyActionIndex, PersistentDataType.INTEGER)) {
            int index = meta.getPersistentDataContainer().get(keyActionIndex, PersistentDataType.INTEGER);
            
            if (event.getClick() == ClickType.SHIFT_RIGHT) {
                if (!hasPermission(player, "xcommands.admin.edit"))
                    return;

                CustomCommandExecutor exec = plugin.getCommandManager().getCustomCommands().get(cmdName.toLowerCase());
                if (exec != null) {
                    List<String> actions = exec.getActions();
                    if (index >= 0 && index < actions.size()) {
                        actions.remove(index);
                        plugin.getCommandManager().markDirty(cmdName);
                        scheduleTransition(player, () -> {
                            plugin.getInventoryManager().openActionsMenu(player, cmdName);
                        });
                    }
                }
            } else {
                if (!hasPermission(player, "xcommands.admin.edit"))
                    return;

                scheduleTransition(player, () -> {
                    plugin.getInventoryManager().openActionEditMenu(player, cmdName, index);
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

                if (action.trim().equals("[") || action.trim().equals("]")) {
                    actionType = action.trim();
                } else if (action.startsWith("[") || action.startsWith("![")) {
                    int start = action.indexOf("[") + 1;
                    int end = action.indexOf("]");
                    if (end != -1) {
                        actionType = action.substring(start, end);
                    }
                }

                // Route to specialized menu based on action type
                switch (actionType.toUpperCase()) {
                    case "GIVE_MONEY":
                    case "TAKE_MONEY":
                    case "DAMAGE":
                    case "DELAY":
                    case "HEAL":
                    case "FEED":
                    case "IF_CHANCE":
                    case "IF_MONEY":
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

                    case "SOUND":
                        // Open sound menu
                        scheduleTransition(player, () -> {
                            new SoundMenu(plugin).open(player, cmdName, actionIdx);
                        });
                        break;

                    case "IF_OP":
                    case "CLOSE":
                    case "[":
                    case "]":
                        // IF_OP and CLOSE do not take values
                        player.sendMessage(plugin.getLanguageManager().getMessage("action-no-value-needed"));
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

    private void handleActionTypeSelection(org.bukkit.event.inventory.InventoryClickEvent event, Player player, ItemStack clicked, ItemMeta meta, String cmdName,
            int actionIndex) {
        if (clicked.getType() == Material.ARROW) {
            scheduleTransition(player, () -> {
                if (actionIndex == -1) {
                    plugin.getInventoryManager().openActionsMenu(player, cmdName);
                } else {
                    CustomCommandExecutor executor = plugin.getCommandManager().getCustomCommands().get(cmdName.toLowerCase());
                    if (executor != null && actionIndex >= executor.getActions().size()) {
                        plugin.getInventoryManager().openActionsMenu(player, cmdName);
                    } else {
                        plugin.getInventoryManager().openActionEditMenu(player, cmdName, actionIndex);
                    }
                }
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
            tag = com.fabian.utils.ColorUtils.stripColor(com.fabian.utils.CompatibilityUtils.getDisplayName(meta)).toUpperCase().replace(" ", "_");
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
        if (exec != null) {
            if ("BRACKET".equals(tag)) {
                String newAction = event.isLeftClick() ? "[" : "]";
                if (actionIndex == -1) {
                    exec.getActions().add(newAction);
                } else if (actionIndex >= 0 && actionIndex < exec.getActions().size()) {
                    plugin.getCommandManager().editAction(cmdName, actionIndex, newAction);
                }
                plugin.getCommandManager().markDirty(cmdName);
                scheduleTransition(player, () -> {
                    plugin.getInventoryManager().openActionsMenu(player, cmdName);
                });
                return;
            }

            String newAction;
            int finalActionIndex;

            if (actionIndex == -1) {
                // Adding a new action
                newAction = "[" + tag + "] ";
                if (tag.startsWith("IF_") && event.isRightClick()) {
                    newAction = "!" + newAction;
                }
                exec.getActions().add(newAction.trim());
                finalActionIndex = exec.getActions().size() - 1;
                plugin.getCommandManager().markDirty(cmdName);
                // We don't have a specific language key for action added in this context, but dirty is marked
            } else if (actionIndex >= 0 && actionIndex < exec.getActions().size()) {
                // Editing existing action
                String oldAction = exec.getActions().get(actionIndex);
                String params = "";
                if (oldAction.contains("]")) {
                    params = oldAction.substring(oldAction.indexOf("]") + 1).trim();
                } else {
                    if (!oldAction.startsWith("[") && !oldAction.startsWith("![")) {
                        params = oldAction;
                    }
                }

                if (tag.equalsIgnoreCase("IF_OP") || tag.equalsIgnoreCase("CLOSE")) {
                    params = "";
                }

                newAction = "[" + tag + "] " + params;
                
                // Negate condition if right-clicked
                if (tag.startsWith("IF_") && event.isRightClick()) {
                    newAction = "!" + newAction;
                }
                
                plugin.getCommandManager().editAction(cmdName, actionIndex, newAction.trim());
                plugin.getCommandManager().markDirty(cmdName);
                player.sendMessage(plugin.getLanguageManager().getMessage("action-type-changed", tag));
                finalActionIndex = actionIndex;
            } else {
                return;
            }

            scheduleTransition(player, () -> {
                plugin.getInventoryManager().openActionEditMenu(player, cmdName, finalActionIndex);
            });
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    @SuppressWarnings("deprecation")
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
            SchedulerUtils.runForPlayer(plugin, player, () -> {
                plugin.getInventoryManager().openCommandEditMenu(player, request.commandName);
            });
            return;
        }

        SchedulerUtils.runForPlayer(plugin, player, () -> {
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
                    String permInput = input;
                    if (permInput.equalsIgnoreCase("none") || permInput.equalsIgnoreCase(plugin.getLanguageManager().getMessage("gui-none"))) {
                        permInput = "";
                    }
                    plugin.getCommandManager().updateConfigValue(request.commandName, "permission", permInput);
                    player.sendMessage(plugin.getLanguageManager().getMessage("command-permission-updated",
                            permInput.isEmpty() ? plugin.getLanguageManager().getMessage("gui-none") : permInput));
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

                case COMMAND_MATERIAL:
                    plugin.getCommandManager().updateConfigValue(request.commandName, "material", input);
                    plugin.getCommandManager().markDirty(request.commandName);
                    player.sendMessage(plugin.getLanguageManager().getMessage("command-material-updated", input));
                    break;

                case ALIAS_NEW:
                    CustomCommandExecutor aliasExecNew = plugin.getCommandManager().getCustomCommands()
                            .get(request.commandName.toLowerCase());
                    if (aliasExecNew != null) {
                        aliasExecNew.getAliases().add(input);
                        plugin.getCommandManager().markDirty(request.commandName);
                        player.sendMessage(plugin.getLanguageManager().getMessage("alias-added"));
                    }
                    SchedulerUtils.runTaskLater(plugin, () -> {
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
                    SchedulerUtils.runTaskLater(plugin, () -> {
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
                    if (currentAction.startsWith("[") || currentAction.startsWith("![")) {
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
                        SchedulerUtils.runTask(plugin,
                                () -> plugin.getInventoryManager().openCommandEditMenu(player, input));
                        return;
                    } else {
                        player.sendMessage(plugin.getLanguageManager().getMessage("input-command-exists"));
                    }
                    SchedulerUtils.runTask(plugin, () -> plugin.getInventoryManager().openMainMenu(player));
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
                    SchedulerUtils.runTaskLater(plugin, () -> {
                        new TitleMenu(plugin).open(player, request.commandName, request.actionIndex);
                    }, 1L);
                    return;

                case TELEPORT_WORLD:
                case TELEPORT_COORDS:
                    CustomCommandExecutor tpExec = plugin.getCommandManager().getCustomCommands()
                            .get(request.commandName.toLowerCase());
                    if (tpExec != null && request.actionIndex >= 0
                            && request.actionIndex < tpExec.getActions().size()) {
                        String action = tpExec.getActions().get(request.actionIndex);
                        String content = action.contains("]") ? action.substring(action.indexOf("]") + 1).trim() : "";
                        String[] parts = content.split(";");
                        String world = parts.length > 0 ? parts[0] : "world";
                        String x = parts.length > 1 ? parts[1] : "0";
                        String y = parts.length > 2 ? parts[2] : "64";
                        String z = parts.length > 3 ? parts[3] : "0";

                        if (request.type == InputType.TELEPORT_WORLD) {
                            world = input;
                        } else if (request.type == InputType.TELEPORT_COORDS) {
                            String[] coords = input.split(" ");
                            if (coords.length >= 3) {
                                x = coords[0];
                                y = coords[1];
                                z = coords[2];
                            }
                        }

                        String newAction = "[TELEPORT] " + world + ";" + x + ";" + y + ";" + z;
                        plugin.getCommandManager().editAction(request.commandName, request.actionIndex, newAction);
                        plugin.getCommandManager().markDirty(request.commandName);
                        player.sendMessage(plugin.getLanguageManager().getMessage("action-updated"));
                    }
                    SchedulerUtils.runTaskLater(plugin, () -> {
                        new TeleportMenu(plugin).open(player, request.commandName, request.actionIndex);
                    }, 1L);
                    return;

                case EFFECT_TYPE:
                case EFFECT_DURATION:
                case EFFECT_AMPLIFIER:
                    CustomCommandExecutor effExec = plugin.getCommandManager().getCustomCommands()
                            .get(request.commandName.toLowerCase());
                    if (effExec != null && request.actionIndex >= 0
                            && request.actionIndex < effExec.getActions().size()) {
                        String action = effExec.getActions().get(request.actionIndex);
                        String content = action.contains("]") ? action.substring(action.indexOf("]") + 1).trim() : "";
                        String[] parts = content.split(";");
                        String type = parts.length > 0 ? parts[0] : "SPEED";
                        String duration = parts.length > 1 ? parts[1] : "60";
                        String amplifier = parts.length > 2 ? parts[2] : "1";

                        if (request.type == InputType.EFFECT_TYPE) {
                            type = input.toUpperCase().replace(" ", "_");
                        } else if (request.type == InputType.EFFECT_DURATION) {
                            duration = input;
                        } else if (request.type == InputType.EFFECT_AMPLIFIER) {
                            amplifier = input;
                        }

                        String newAction = "[EFFECT] " + type + ";" + duration + ";" + amplifier;
                        plugin.getCommandManager().editAction(request.commandName, request.actionIndex, newAction);
                        plugin.getCommandManager().markDirty(request.commandName);
                        player.sendMessage(plugin.getLanguageManager().getMessage("action-updated"));
                    }
                    SchedulerUtils.runTaskLater(plugin, () -> {
                        new EffectMenu(plugin).open(player, request.commandName, request.actionIndex);
                    }, 1L);
                    return;

                case SOUND_TYPE:
                    CustomCommandExecutor sndExec = plugin.getCommandManager().getCustomCommands()
                            .get(request.commandName.toLowerCase());
                    if (sndExec != null && request.actionIndex >= 0
                            && request.actionIndex < sndExec.getActions().size()) {
                        String action = sndExec.getActions().get(request.actionIndex);
                        String content = action.contains("]") ? action.substring(action.indexOf("]") + 1).trim() : "";
                        String[] parts = content.split(";");
                        String sound = input.toUpperCase().replace(" ", "_");
                        String volume = parts.length > 1 ? parts[1] : "1.0";
                        String pitch = parts.length > 2 ? parts[2] : "1.0";

                        String newAction = "[SOUND] " + sound + ";" + volume + ";" + pitch;
                        plugin.getCommandManager().editAction(request.commandName, request.actionIndex, newAction);
                        plugin.getCommandManager().markDirty(request.commandName);
                        player.sendMessage(plugin.getLanguageManager().getMessage("action-updated"));
                    }
                    SchedulerUtils.runTaskLater(plugin, () -> {
                        new SoundMenu(plugin).open(player, request.commandName, request.actionIndex);
                    }, 1L);
                    return;

                case PARTICLE_TYPE:
                case PARTICLE_COUNT:
                    CustomCommandExecutor partExec = plugin.getCommandManager().getCustomCommands()
                            .get(request.commandName.toLowerCase());
                    if (partExec != null && request.actionIndex >= 0
                            && request.actionIndex < partExec.getActions().size()) {
                        String action = partExec.getActions().get(request.actionIndex);
                        String content = action.contains("]") ? action.substring(action.indexOf("]") + 1).trim() : "";
                        String[] parts = content.split(";");
                        String type = parts.length > 0 ? parts[0] : "FLAME";
                        String amount = parts.length > 1 ? parts[1] : "10";

                        if (request.type == InputType.PARTICLE_TYPE) {
                            type = input.toUpperCase().replace(" ", "_");
                        } else if (request.type == InputType.PARTICLE_COUNT) {
                            amount = input;
                        }

                        String newAction = "[PARTICLE] " + type + ";" + amount;
                        plugin.getCommandManager().editAction(request.commandName, request.actionIndex, newAction);
                        plugin.getCommandManager().markDirty(request.commandName);
                        player.sendMessage(plugin.getLanguageManager().getMessage("action-updated"));
                    }
                    SchedulerUtils.runTaskLater(plugin, () -> {
                        new ParticleMenu(plugin).open(player, request.commandName, request.actionIndex);
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
            player.sendMessage(com.fabian.utils.ColorUtils.translate("&e" + (i + 1) + ". &f" + actions.get(i)));
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
        SchedulerUtils.runTaskLater(plugin, () -> {
            try {
                action.run();
            } finally {
                scheduledTransitions.remove(player.getUniqueId());
            }
        }, 1L);
    }

    @SuppressWarnings("deprecation")
    private void handleActionReorderMenu(InventoryClickEvent event, Player player, ItemStack clicked, String cmdName) {
        // Block interaction with anything other than the top inventory
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            event.setCancelled(true);
            return;
        }

        // Prevent taking items out using Hotbar keys, Drop, shift-click, etc.
        org.bukkit.event.inventory.InventoryAction action = event.getAction();
        if (action == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY || 
            action == org.bukkit.event.inventory.InventoryAction.HOTBAR_SWAP || 
            action == org.bukkit.event.inventory.InventoryAction.HOTBAR_MOVE_AND_READD ||
            action == org.bukkit.event.inventory.InventoryAction.DROP_ALL_SLOT ||
            action == org.bukkit.event.inventory.InventoryAction.DROP_ONE_SLOT ||
            action == org.bukkit.event.inventory.InventoryAction.DROP_ALL_CURSOR ||
            action == org.bukkit.event.inventory.InventoryAction.DROP_ONE_CURSOR) {
            event.setCancelled(true);
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

        // 2. Handle Back Button (Slot 45) → CommandEditMenu
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

        // Handle Hopper (Slot 53) → ActionsMenu
        if (slot == 53) {
            event.setCancelled(true);
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                event.getView().setCursor(null);
            }
            saveReorderedActions(player, event.getView().getTopInventory(), cmdName);
            scheduleTransition(player, () -> {
                plugin.getInventoryManager().openActionsMenu(player, cmdName);
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
                    CustomCommandExecutor exec = plugin.getCommandManager().getCustomCommands().get(cmdName.toLowerCase());
                    if (exec != null && actionIndex >= 0 && actionIndex < exec.getActions().size()) {
                        String action = exec.getActions().get(actionIndex);
                        String newAction;

                        if (actionType.equals("GIVE_AMOUNT")) {
                            String content = action.contains("]") ? action.substring(action.indexOf("]") + 1).trim() : "";
                            String[] parts = content.split(";");
                            String material = parts.length > 0 ? parts[0] : "DIAMOND";
                            String customName = parts.length > 2 ? parts[2] : "";
                            newAction = "[GIVE] " + material + ";" + (int) value + ";" + customName;
                        } else if (actionType.equals("SOUND_VOLUME")) {
                            String content = action.contains("]") ? action.substring(action.indexOf("]") + 1).trim() : "";
                            String[] parts = content.split(";");
                            String sound = parts.length > 0 ? parts[0] : "BLOCK_NOTE_BLOCK_PLING";
                            String pitch = parts.length > 2 ? parts[2] : "1.0";
                            newAction = "[SOUND] " + sound + ";" + value + ";" + pitch;
                        } else if (actionType.equals("SOUND_PITCH")) {
                            String content = action.contains("]") ? action.substring(action.indexOf("]") + 1).trim() : "";
                            String[] parts = content.split(";");
                            String sound = parts.length > 0 ? parts[0] : "BLOCK_NOTE_BLOCK_PLING";
                            String volume = parts.length > 1 ? parts[1] : "1.0";
                            newAction = "[SOUND] " + sound + ";" + volume + ";" + value;
                        } else {
                            boolean isDecimal = actionType.equals("HEAL") || actionType.equals("FEED") || 
                                              actionType.equals("IF_CHANCE") || actionType.equals("IF_MONEY") ||
                                              actionType.startsWith("SOUND_");
                            newAction = "[" + actionType + "] " + (isDecimal ? value : (int) value);
                        }

                        plugin.getCommandManager().editAction(cmdName, actionIndex, newAction);
                        plugin.getCommandManager().markDirty(cmdName);
                        player.sendMessage(plugin.getLanguageManager().getMessage("action-updated"));
                    }

                    scheduleTransition(player, () -> {
                        if (actionType.equals("GIVE_AMOUNT")) {
                            new GiveMenu(plugin).open(player, cmdName, actionIndex);
                        } else if (actionType.startsWith("SOUND_")) {
                            new SoundMenu(plugin).open(player, cmdName, actionIndex);
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
                com.fabian.utils.CompatibilityUtils.setDisplayName(meta, "&e" + actionType + ": &f0");
                centerItem.setItemMeta(meta);
                event.getView().getTopInventory().setItem(13, centerItem);
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1, 1);
            }
            return;
        }

        // Add buttons (LIME_WOOL)
        if (clicked.getType() == Material.LIME_WOOL) {
            String displayName = com.fabian.utils.ColorUtils.stripColor(com.fabian.utils.CompatibilityUtils.getDisplayName(clicked.getItemMeta()));
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
                        boolean isDecimal = actionType.startsWith("SOUND_") || actionType.equals("HEAL")
                                || actionType.equals("FEED") || actionType.equals("IF_CHANCE")
                                || actionType.equals("IF_MONEY");
                        String displayValue = isDecimal
                                ? String.format("%.1f", newValue)
                                : String.valueOf((int) newValue);
                        com.fabian.utils.CompatibilityUtils.setDisplayName(meta, "&e" + actionType + ": &f" + displayValue);
                        centerItem.setItemMeta(meta);
                        event.getView().getTopInventory().setItem(13, centerItem);
                        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1, 1);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return;
        }

        // Subtract buttons (RED_WOOL)
        if (clicked.getType() == Material.RED_WOOL) {
            String displayName = com.fabian.utils.ColorUtils.stripColor(com.fabian.utils.CompatibilityUtils.getDisplayName(clicked.getItemMeta()));
            if (displayName.startsWith("-")) {
                try {
                    int subValue = Integer.parseInt(displayName.substring(1));
                    ItemStack centerItem = event.getView().getTopInventory().getItem(13);
                    if (centerItem != null && centerItem.getItemMeta() != null) {
                        ItemMeta meta = centerItem.getItemMeta();
                        double currentValue = meta.getPersistentDataContainer()
                                .get(new NamespacedKey(plugin, "numeric_value"), PersistentDataType.DOUBLE);
                        String actionType = meta.getPersistentDataContainer()
                                .get(new NamespacedKey(plugin, "action_type_tag"), PersistentDataType.STRING);
                        double newValue = currentValue - subValue;
                        // DELAY cannot go below 0
                        if ("DELAY".equals(actionType)) {
                            newValue = Math.max(0, newValue);
                        }
                        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "numeric_value"),
                                PersistentDataType.DOUBLE, newValue);
                        boolean isDecimal = actionType.startsWith("SOUND_") || actionType.equals("HEAL")
                                || actionType.equals("FEED") || actionType.equals("IF_CHANCE")
                                || actionType.equals("IF_MONEY");
                        String displayValue = isDecimal
                                ? String.format("%.1f", newValue)
                                : String.valueOf((int) newValue);
                        com.fabian.utils.CompatibilityUtils.setDisplayName(meta, "&e" + actionType + ": &f" + displayValue);
                        centerItem.setItemMeta(meta);
                        event.getView().getTopInventory().setItem(13, centerItem);
                        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1, 0.8f);
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
                    // Delete with transition to avoid close warning
                    executor.getAliases().remove(i);
                    plugin.getCommandManager().markDirty(cmdName);
                    player.sendMessage(plugin.getLanguageManager().getMessage("alias-deleted"));
                    scheduleTransition(player, () -> new AliasMenu(plugin).open(player, cmdName));
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

        // Slot 10: Main Title
        if (slot == 10) {
            requestChatInput(player, cmdName, InputType.TITLE_MAIN, actionIndex);
            return;
        }

        // Slot 11: Subtitle
        if (slot == 11) {
            requestChatInput(player, cmdName, InputType.TITLE_SUB, actionIndex);
            return;
        }

        // Slot 13: Fade In
        if (slot == 13) {
            requestChatInput(player, cmdName, InputType.TITLE_FADEIN, actionIndex);
            return;
        }

        // Slot 14: Stay
        if (slot == 14) {
            requestChatInput(player, cmdName, InputType.TITLE_STAY, actionIndex);
            return;
        }

        // Slot 15: Fade Out
        if (slot == 15) {
            requestChatInput(player, cmdName, InputType.TITLE_FADEOUT, actionIndex);
            return;
        }

        // Slot 26: Confirm
        if (slot == 26) {
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

        List<String> oldActions = executor.getActions();
        List<String> newActions = new java.util.ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                if (item.getItemMeta() != null && item.getItemMeta().getPersistentDataContainer().has(
                        new NamespacedKey(plugin, "action_content"), PersistentDataType.STRING)) {
                    String action = item.getItemMeta().getPersistentDataContainer().get(
                            new NamespacedKey(plugin, "action_content"), PersistentDataType.STRING);
                    newActions.add(action);
                } else {
                    String strippedName = com.fabian.utils.ColorUtils.stripColor(com.fabian.utils.CompatibilityUtils.getDisplayName(item.getItemMeta()));
                    newActions.add(strippedName);
                }
            }
        }

        // Only mark dirty if something actually changed
        if (!newActions.equals(oldActions)) {
            plugin.getCommandManager().saveActions(cmdName, newActions);
            plugin.getCommandManager().markDirty(cmdName);
            player.sendMessage(plugin.getLanguageManager().getMessage("actions-reordered"));
        }
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
        COMMAND_MATERIAL,
        ALIAS_NEW, ALIAS_EDIT,
        ACTION_CONTENT, CREATE_COMMAND,
        TITLE_MAIN, TITLE_SUB, TITLE_FADEIN, TITLE_STAY, TITLE_FADEOUT,
        TELEPORT_WORLD, TELEPORT_COORDS,
        EFFECT_TYPE, EFFECT_DURATION, EFFECT_AMPLIFIER,
        PARTICLE_TYPE, PARTICLE_COORDS, PARTICLE_COUNT,
        SOUND_TYPE
    }

    private void handleTeleportMenu(InventoryClickEvent event, Player player, ItemStack clicked, String cmdName, int actionIndex) {
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        event.setCancelled(true);
        if (clicked == null || !clicked.hasItemMeta()) return;
        int slot = event.getSlot();

        if (slot == 11) {
            // Current location
            String world = player.getLocation().getWorld().getName();
            int x = player.getLocation().getBlockX();
            int y = player.getLocation().getBlockY();
            int z = player.getLocation().getBlockZ();
            String newAction = "[TELEPORT] " + world + ";" + x + ";" + y + ";" + z;
            plugin.getCommandManager().editAction(cmdName, actionIndex, newAction);
            plugin.getCommandManager().markDirty(cmdName);
            player.sendMessage(plugin.getLanguageManager().getMessage("action-updated"));
            scheduleTransition(player, () -> new TeleportMenu(plugin).open(player, cmdName, actionIndex));
        } else if (slot == 13) {
            requestChatInput(player, cmdName, InputType.TELEPORT_COORDS, actionIndex);
        } else if (slot == 15) {
            requestChatInput(player, cmdName, InputType.TELEPORT_WORLD, actionIndex);
        } else if (slot == 26) {
            scheduleTransition(player, () -> plugin.getInventoryManager().openActionsMenu(player, cmdName));
        } else if (slot == 18) {
            scheduleTransition(player, () -> plugin.getInventoryManager().openActionEditMenu(player, cmdName, actionIndex));
        }
    }

    private void handleEffectMenu(InventoryClickEvent event, Player player, ItemStack clicked, String cmdName, int actionIndex) {
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        event.setCancelled(true);
        if (clicked == null || !clicked.hasItemMeta()) return;
        int slot = event.getSlot();

        if (slot == 11) {
            requestChatInput(player, cmdName, InputType.EFFECT_TYPE, actionIndex);
        } else if (slot == 13) {
            requestChatInput(player, cmdName, InputType.EFFECT_DURATION, actionIndex);
        } else if (slot == 15) {
            requestChatInput(player, cmdName, InputType.EFFECT_AMPLIFIER, actionIndex);
        } else if (slot == 26) {
            scheduleTransition(player, () -> plugin.getInventoryManager().openActionsMenu(player, cmdName));
        } else if (slot == 18) {
            scheduleTransition(player, () -> plugin.getInventoryManager().openActionEditMenu(player, cmdName, actionIndex));
        }
    }

    private void handleParticleMenu(InventoryClickEvent event, Player player, ItemStack clicked, String cmdName, int actionIndex) {
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        event.setCancelled(true);
        if (clicked == null || !clicked.hasItemMeta()) return;
        int slot = event.getSlot();

        if (slot == 11) {
            requestChatInput(player, cmdName, InputType.PARTICLE_TYPE, actionIndex);
        } else if (slot == 15) {
            requestChatInput(player, cmdName, InputType.PARTICLE_COUNT, actionIndex);
        } else if (slot == 26) {
            scheduleTransition(player, () -> plugin.getInventoryManager().openActionsMenu(player, cmdName));
        } else if (slot == 18) {
            scheduleTransition(player, () -> plugin.getInventoryManager().openActionEditMenu(player, cmdName, actionIndex));
        }
    }

    private void handleSoundMenu(InventoryClickEvent event, Player player, ItemStack clicked, String cmdName, int actionIndex) {
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        event.setCancelled(true);
        if (clicked == null || clicked.getType() == Material.AIR) return;
        int slot = event.getSlot();

        // Get current values
        com.fabian.executors.CustomCommandExecutor exec = plugin.getCommandManager().getCustomCommands().get(cmdName.toLowerCase());
        if (exec == null || actionIndex < 0 || actionIndex >= exec.getActions().size()) return;

        String action = exec.getActions().get(actionIndex);
        String[] parts = action.substring(action.indexOf("]") + 1).trim().split(";");
        
        double volume = parts.length > 1 ? Double.parseDouble(parts[1]) : 1.0;
        double pitch = parts.length > 2 ? Double.parseDouble(parts[2]) : 1.0;

        if (slot == 11) { // Sound Select
            requestChatInput(player, cmdName, InputType.SOUND_TYPE, actionIndex);
        } else if (slot == 13) { // Volume
            scheduleTransition(player, () -> {
                new NumericActionMenu(plugin).open(player, cmdName, actionIndex, "SOUND_VOLUME", volume);
            });
        } else if (slot == 15) { // Pitch
            scheduleTransition(player, () -> {
                new NumericActionMenu(plugin).open(player, cmdName, actionIndex, "SOUND_PITCH", pitch);
            });
        } else if (slot == 18) { // Back
            scheduleTransition(player, () -> {
                plugin.getInventoryManager().openActionEditMenu(player, cmdName, actionIndex);
            });
        }
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
