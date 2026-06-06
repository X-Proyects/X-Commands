package com.fabian.managers;

import com.fabian.conditions.*;
import com.fabian.XCommands;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages condition registration and evaluation
 */
public class ConditionManager {

    private final XCommands plugin;
    private final Map<String, Condition> conditions;

    public ConditionManager(XCommands plugin) {
        this.plugin = plugin;
        this.conditions = new HashMap<>();
        registerDefaults();
    }

    private void registerDefaults() {
        registerCondition("IF_WORLD", new WorldCondition());
        registerCondition("IF_PERMISSION", new PermissionCondition());
        registerCondition("IF_MONEY", new MoneyCondition());
        registerCondition("IF_OP", new OpCondition());
        registerCondition("IF_CHANCE", new ChanceCondition());
    }

    /**
     * Registers a condition
     */
    public void registerCondition(String tag, Condition condition) {
        conditions.put(tag.toUpperCase(), condition);
    }

    /**
     * Checks a condition string (e.g., "[IF_OP]", "! [IF_WORLD] lobby")
     */
    public boolean check(Player player, String conditionStr, Map<String, Object> context) {
        if (player == null) return false;

        boolean negate = conditionStr.startsWith("!");
        String cleanStr = negate ? conditionStr.substring(1).trim() : conditionStr.trim();

        // Parse tag and params: [TAG] params
        if (!cleanStr.startsWith("[") || !cleanStr.contains("]")) {
            return false;
        }

        int closingBracket = cleanStr.indexOf("]");
        String tag = cleanStr.substring(1, closingBracket).toUpperCase();
        String params = cleanStr.substring(closingBracket + 1).trim();

        // Support Legacy _NOT suffix: [IF_PERMISSION_NOT] -> ![IF_PERMISSION]
        if (tag.endsWith("_NOT")) {
            tag = tag.substring(0, tag.length() - 4);
            negate = !negate; // Flip negation
        }

        Condition condition = conditions.get(tag);
        if (condition == null) {
            plugin.logWarning("Unknown condition type: " + tag);
            return false;
        }

        // Add params to context for the condition to use
        context.put("params", params);
        
        boolean result = condition.check(player, context);
        return negate ? !result : result;
    }

    public Map<String, Condition> getConditions() {
        return new HashMap<>(conditions);
    }

    /**
     * Reloads the condition manager
     */
    public void reload() {
        conditions.clear();
        registerDefaults();
    }
}
