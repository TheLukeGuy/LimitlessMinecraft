package com.lukepc.limitlessminecraft;

import net.md_5.bungee.api.ChatColor;

public enum CandidateStatus {
    GENERATING(false, "G", ChatColor.GRAY),
    GENERATING_FAILED(true, "GF", ChatColor.DARK_RED),
    BAD_CODE(true, "B", ChatColor.RED),
    AWAITING_COMPILATION(false, "W", ChatColor.AQUA),
    COMPILATION_FAILED(true, "CF", ChatColor.RED),
    READY_TO_RUN(true, "R", ChatColor.GREEN);

    private final boolean complete;
    private final String identifier;
    private final ChatColor identifierColor;

    CandidateStatus(boolean complete, String identifier, ChatColor identifierColor) {
        this.complete = complete;
        this.identifier = identifier;
        this.identifierColor = identifierColor;
    }

    public boolean isComplete() {
        return complete;
    }

    public String getIdentifier() {
        return identifier;
    }

    public ChatColor getIdentifierColor() {
        return identifierColor;
    }
}
