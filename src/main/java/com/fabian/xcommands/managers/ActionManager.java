package com.fabian.xcommands.managers;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.actions.*;
import com.fabian.xcommands.utils.SchedulerUtil;
import com.fabian.xcommands.utils.DebugLogger;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages action registration and execution
 */
public class ActionManager {

    private final XCommands plugin;
    private final Map<String, Action> actions;
    private static final int MAX_CACHE_SIZE = 500;
    private final Map<String, ActionEntry> actionCache = Collections.synchronizedMap(new LinkedHashMap<String, ActionEntry>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ActionEntry> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    });
    private final Pattern actionPattern = Pattern.compile("(!?)\\[([a-zA-Z_]+)\\]\\s*(.*)");

    private static class ActionEntry {
        final Action action;
        final String tag;
        final String params;
        final boolean negated;
        final boolean isDelay;

        ActionEntry(Action action, String tag, String params, boolean negated) {
            this.action = action;
            this.tag = tag;
            this.params = params;
            this.negated = negated;
            this.isDelay = tag.equals("DELAY");
        }
    }

    public ActionManager(XCommands plugin) {
        this.plugin = plugin;
        this.actions = new HashMap<>();
        registerActions();
    }

    /**
     * Registers all available actions
     */
    private void registerActions() {
        DebugLogger.debug("Registering action types...");
        registerAction(new MessageAction());
        registerAction(new BroadcastAction());
        registerAction(new ActionBarAction());
        registerAction(new TitleAction());
        registerAction(new ConsoleAction());
        registerAction(new PlayerAction());
        registerAction(new SoundAction());
        registerAction(new EffectAction());
        registerAction(new HealAction());
        registerAction(new FeedAction());
        registerAction(new DamageAction());
        registerAction(new GiveAction());
        registerAction(new TeleportAction());
        registerAction(new DelayAction());
        registerAction(new CloseInventoryAction());
        registerAction(new KickAction());
        registerAction(new ParticleAction());
        registerAction(new BungeeAction());
        registerAction(new VelocityAction());
        registerAction(new SendToAction());
        registerAction(new GiveMoneyAction());
        registerAction(new TakeMoneyAction());
        DebugLogger.debug("Registered " + actions.size() + " action types");
    }

    /**
     * Registers a single action
     */
    private void registerAction(Action action) {
        actions.put(action.getTag(), action);
        DebugLogger.debug("  Registered action: " + action.getTag());
    }

    /**
     * Executes a list of action strings for a player
     * 
     * @param player        The player executing the actions (null if console)
     * @param actionStrings List of action strings to execute
     */
    public void executeActions(Player player, List<String> actionStrings) {
        executeActions(player, actionStrings, new String[0]);
    }

    /**
     * Executes a list of action strings for a player with arguments
     * 
     * @param player        The player executing the actions (null if console)
     * @param actionStrings List of action strings to execute
     * @param args          The command arguments
     */
    public void executeActions(Player player, List<String> actionStrings, String[] args) {
        DebugLogger.debug("Executing " + actionStrings.size() + " actions for " + (player != null ? player.getName() : "console"));
        executeActionsLoop(player, actionStrings, 0, args);
    }

    /**
     * Recursively executes actions via loop, scheduling only when needed
     */
    private void executeActionsLoop(Player player, List<String> actionStrings, int startIndex, String[] args) {
        if (actionStrings == null || startIndex >= actionStrings.size()) {
            return;
        }

        for (int i = startIndex; i < actionStrings.size(); i++) {
            String actionString = actionStrings.get(i).trim();

            // Ignore bracket notation when executing normally
            if (actionString.equals("[") || actionString.equals("]")) {
                continue;
            }

            ActionEntry entry = getByKeyOrParse(actionString);

            if (entry == null) {
                return; // Stop execution on invalid actions (safer)
            }

            if (entry.isDelay) {
                final int nextIndex = i + 1;
                // Resolve delay at runtime (after placeholder/arg replacement) so dynamic values work
                String delayStr = com.fabian.xcommands.utils.PlaceholderUtils.replaceArgs(entry.params, args);
                long delayTicks = 1L;
                try {
                    // Try integer first; fall back to double to handle values like "60.0"
                    delayTicks = Math.max(1L, Long.parseLong(delayStr.trim()));
                } catch (NumberFormatException e1) {
                    try {
                        delayTicks = Math.max(1L, (long) Double.parseDouble(delayStr.trim()));
                    } catch (NumberFormatException e2) {
                        plugin.logWarning("[DELAY] invalid value '" + delayStr + "' — defaulting to 1 tick.");
                    }
                }
                final long finalDelay = delayTicks;
                // Schedule remainder of the list after the delay
                SchedulerUtil.runTaskLaterForPlayer(plugin, player, () -> {
                    executeActionsLoop(player, actionStrings, nextIndex, args);
                }, finalDelay);
                return; // Break current loop, remainder handled by the scheduled task
            }

            // Execute synchronous action
            if (entry.tag.startsWith("IF_")) {
                if (!evaluateCondition(player, entry, args)) {
                    // Check if the next action is a bracket block
                    if (i + 1 < actionStrings.size() && actionStrings.get(i + 1).trim().equals("[")) {
                        int bracketCount = 1;
                        i++; // Move to the '['
                        while (i + 1 < actionStrings.size() && bracketCount > 0) {
                            i++;
                            String nextAction = actionStrings.get(i).trim();
                            if (nextAction.equals("[")) {
                                bracketCount++;
                            } else if (nextAction.equals("]")) {
                                bracketCount--;
                            }
                        }
                    } else {
                        i++; // Skip just the NEXT single action
                    }
                }
                continue;
            }

            executeEntry(entry, player, args);
        }
    }

    /**
     * Gets an ActionEntry from file or parses it
     */
    private ActionEntry getByKeyOrParse(String actionString) {
        ActionEntry entry = actionCache.get(actionString);
        if (entry != null)
            return entry;

        Matcher matcher = actionPattern.matcher(actionString);
        if (!matcher.matches()) {
            plugin.logWarning("Invalid action format: " + actionString);
            return null;
        }

        boolean negated = !matcher.group(1).isEmpty();
        String tag = matcher.group(2).toUpperCase();
        String params = matcher.group(3).trim();
        Action action = actions.get(tag);

        if (action == null && !tag.startsWith("IF_")) {
            plugin.logWarning("Unknown action type: " + tag);
            DebugLogger.debug("Unknown action type in string: " + actionString + " (tag=" + tag + ")");
            return null;
        }

        entry = new ActionEntry(action, tag, params, negated);
        actionCache.put(actionString, entry);
        return entry;
    }

    private void executeEntry(ActionEntry entry, Player player, String[] args) {
        // Pre-process params with arguments
        String params = com.fabian.xcommands.utils.PlaceholderUtils.replaceArgs(entry.params, args);

        if (player == null) {
            // Console or Global execution
            try {
                Map<String, Object> context = new HashMap<>();
                context.put("params", params);
                context.put("plugin", plugin);
                entry.action.execute(null, context);
            } catch (Exception e) {
                plugin.logError("Error executing global action " + entry.tag + ": " + e.getMessage());
            }
            return;
        }

        // For players, we MUST ensure we are on the correct region thread (especially for Folia 26.1)
        SchedulerUtil.runForPlayer(plugin, player, () -> {
            try {
                Map<String, Object> context = new HashMap<>();
                context.put("params", params);
                context.put("plugin", plugin);
                
                entry.action.execute(player, context);
            } catch (Exception e) {
                plugin.logError("Error executing action [" + entry.tag + "] for player " + player.getName() + " with params: " + params);
                DebugLogger.debug("Action execution failed: " + entry.tag + " - " + e.getMessage());
            }
        });
    }

    /**
     * Executes a single action string (Legacy public method)
     */
    public void executeSingleAction(Player player, String actionString) {
        executeSingleAction(player, actionString, new String[0]);
    }

    /**
     * Executes a single action string with arguments
     */
    public void executeSingleAction(Player player, String actionString, String[] args) {
        ActionEntry entry = getByKeyOrParse(actionString);
        if (entry != null) {
            executeEntry(entry, player, args);
        }
    }

    private boolean evaluateCondition(Player player, ActionEntry entry, String[] args) {
        Map<String, Object> context = new HashMap<>();
        // Pre-process params with arguments for conditions
        String params = com.fabian.xcommands.utils.PlaceholderUtils.replaceArgs(entry.params, args);
        // Wrap the tag in brackets so ConditionManager can parse it correctly
        String formattedCondition = (entry.negated ? "!" : "") + "[" + entry.tag + "]" + (params.isEmpty() ? "" : " " + params);
        return plugin.getConditionManager().check(player, formattedCondition, context);
    }

    /**
     * Gets all registered actions
     * 
     * @return Map of registered actions by tag
     */
    public Map<String, Action> getActions() {
        return new HashMap<>(actions);
    }

    /**
     * Reloads the action manager
     */
    public void reload() {
        DebugLogger.debug("Reloading action manager...");
        actions.clear();
        actionCache.clear();
        registerActions();
    }
}
