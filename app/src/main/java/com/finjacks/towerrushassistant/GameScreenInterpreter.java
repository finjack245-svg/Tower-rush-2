package com.finjacks.towerrushassistant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GameScreenInterpreter {
    private GameScreenInterpreter() { }

    public static String getBuildSignal(String rawText) {
        String lower = rawText == null ? "" : rawText.toLowerCase(Locale.ROOT);
        boolean buildVisible = containsWord(lower, "build");
        boolean bonusVisible = lower.contains("buy bonus");
        boolean cashoutVisible = lower.contains("cashout") || lower.contains("cash out");

        if (cashoutVisible) return "WAIT — cashout/round text detected";
        if (buildVisible && bonusVisible) return "READY TO DECIDE — BUILD control visible";
        if (buildVisible) return "UNCLEAR — BUILD seen, but pre-build screen not confirmed";
        return "WAIT — BUILD control not detected";
    }

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
        if (containsWord(lower, "cashout") || containsWord(lower, "cash out")) observations.add("Cashout-related text is visible.");
        if (lower.contains("2x chance") || lower.contains("2 x chance")) observations.add("2X CHANCE feature label is visible.");

        Matcher multiplier = Pattern.compile("(?i)\\b([0-9]+(?:[.,][0-9]+)?\\s*x)\\b").matcher(text);
        if (multiplier.find()) observations.add("Visible multiplier-like text: " + multiplier.group(1));

        Matcher floor = Pattern.compile("(?i)\\b(?:floor|level)\\s*[:#-]?\\s*([0-9]+)\\b").matcher(text);
        if (floor.find()) observations.add("Visible floor/level label: " + floor.group(1));

        StringBuilder report = new StringBuilder();
        report.append("INTERFACE STATUS: ").append(getBuildSignal(text)).append("\\n");
        for (String item : observations) report.append("• ").append(item).append('\\n');
        if (observations.isEmpty()) report.append("No known Tower Rush 2 labels were confidently recognised.\\n");

        report.append("\\nThis is only a screen-state check. READY TO DECIDE means the interface resembles the pre-build screen; it does NOT identify a lucky, safe, or profitable moment. Outcomes cannot be predicted from this screen. No game controls are pressed.");
        return report.toString();
    }

    private static boolean containsWord(String text, String word) {
        return Pattern.compile("(?i)(^|[^a-z])" + Pattern.quote(word) + "([^a-z]|$)").matcher(text).find();
    }
}
