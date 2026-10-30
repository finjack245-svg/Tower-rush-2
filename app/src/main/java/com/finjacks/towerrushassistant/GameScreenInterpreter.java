package com.finjacks.towerrushassistant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GameScreenInterpreter {
    private GameScreenInterpreter() { }

    public static String makeReport(String rawText) {
        String text = rawText == null ? "" : rawText.trim();
        if (text.isEmpty()) {
            return "No readable game text detected yet. Keep the game interface clear and wait for the next scan.";
        }

        String lower = text.toLowerCase(Locale.ROOT);
        List<String> observations = new ArrayList<>();

        Matcher balance = Pattern.compile("(?i)(?:ZAR\\s*)?([0-9]+[.,][0-9]{1,2})\\s*(ZAR|R)\\b")
                .matcher(text);
        if (balance.find()) {
            observations.add("Visible balance text: " + balance.group(1) + " " + balance.group(2).toUpperCase(Locale.ROOT));
        }

        if (containsWord(lower, "build")) observations.add("BUILD control is visible (not pressed).");
        if (containsWord(lower, "auto")) observations.add("AUTO control is visible (not activated).");
        if (containsWord(lower, "all in") || containsWord(lower, "allin")) observations.add("ALL IN control is visible (not activated).");
        if (containsWord(lower, "buy bonus")) observations.add("BUY BONUS control is visible (not activated).");
        if (containsWord(lower, "cashout") || containsWord(lower, "cash out")) observations.add("A cashout label appears on screen.");
        if (lower.contains("2x chance") || lower.contains("2 x chance")) observations.add("2X CHANCE feature label is visible.");

        Matcher multiplier = Pattern.compile("(?i)\\b([0-9]+(?:[.,][0-9]+)?\\s*x)\\b").matcher(text);
        if (multiplier.find()) observations.add("Visible multiplier-like text: " + multiplier.group(1));

        Matcher floor = Pattern.compile("(?i)\\b(?:floor|level)\\s*[:#-]?\\s*([0-9]+)\\b").matcher(text);
        if (floor.find()) observations.add("Visible floor/level label: " + floor.group(1));

        if (lower.contains("tower rush")) observations.add("Tower Rush title text detected.");

        StringBuilder report = new StringBuilder();
        if (lower.contains("build") && lower.contains("buy bonus")) {
            report.append("SCREEN ESTIMATE: likely the pre-build/game-ready view.\n");
        } else if (lower.contains("cashout") || lower.contains("cash out")) {
            report.append("SCREEN ESTIMATE: a cashout-related screen or control may be visible.\n");
        } else {
            report.append("SCREEN ESTIMATE: game screen text detected; round state is not confirmed.\n");
        }

        if (observations.isEmpty()) {
            report.append("No known Tower Rush 2 labels were confidently recognised.\n");
        } else {
            for (String item : observations) report.append("• ").append(item).append('\n');
        }

        report.append("\nImportant: this reads visible text only. It cannot see hidden game logic, forecast random outcomes, or tell you that a move will win. All game actions remain manual.");
        return report.toString();
    }

    private static boolean containsWord(String text, String word) {
        return Pattern.compile("(?i)(^|[^a-z])" + Pattern.quote(word) + "([^a-z]|$)").matcher(text).find();
    }
}
