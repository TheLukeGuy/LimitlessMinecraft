package com.lukepc.limitlessminecraft;

import com.lukepc.limitlessminecraft.copilot.CopilotAuth;
import com.lukepc.limitlessminecraft.copilot.CopilotRequest;
import com.lukepc.limitlessminecraft.copilot.CopilotToken;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class LimitlessMinecraft extends JavaPlugin implements Listener {
    private static final int CANDIDATES_PER_ACTION = 20;

    private static final Random random = new Random();
    private static File tempDir;

    private CopilotToken copilotToken;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        tempDir = new File(getDataFolder(), "temp");
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            getLogger().severe("Failed to create the temporary directory.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        for (int i = 0; i < 5; i++) {
            getLogger().severe("!!!!! YOU SHOULD NEVER ACTUALLY USE THIS PLUGIN !!!!!");
        }
        getLogger().info("Seriously, bad idea. Very bad.");
        getLogger().info("Unless, of course, you want to maximize the happiness of your players. ;)");
        getLogger().info("In which case, please authenticate with GitHub with the link below:");
        getLogger().info(ChatColor.DARK_GRAY + CopilotAuth.AUTH_URL);
        getLogger().info("Paste the authorization token URL in the console when you're done.");
        getLogger().warning("Oh, and you're on your own for sandboxing. Use this at your own risk!");

        String vsCodeUrl = new Scanner(System.in).nextLine();
        copilotToken = CopilotAuth.getToken(vsCodeUrl);
        if (copilotToken == null) {
            getLogger().severe("Failed to obtain a Copilot access token.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info(ChatColor.GREEN + "Successfully obtained a Copilot access token!");
    }

    public void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    public void generateCode(List<ActionCandidate> candidates, String classId, String prompt) {
        ActionCandidate candidate = new ActionCandidate(classId);
        synchronized (candidates) {
            candidates.add(candidate);
        }

        CopilotRequest copilotRequest = new CopilotRequest();
        copilotRequest.setMaxTokens(prompt.length() + 500);
        copilotRequest.setCount(1);
        copilotRequest.setStop(List.of("\n}"));

        List<String> choices = copilotRequest.send(copilotToken, prompt);
        if (choices == null) {
            candidate.setStatus(CandidateStatus.GENERATING_FAILED);
            return;
        }
        String fullCode = (prompt + choices.get(0)).trim();
        if (!fullCode.endsWith("}")) {
            candidate.setCode(fullCode);
            candidate.setStatus(CandidateStatus.BAD_CODE);
            return;
        }

        String fullCodeFlat = fullCode
                .replace("\n", "")
                .replace(" ", "");
        if (!fullCodeFlat.endsWith("}}")) {
            fullCode += "\n}";
        }

        candidate.setCode(fullCode);
        candidate.setStatus(CandidateStatus.AWAITING_COMPILATION);
    }

    public void runAction(List<ActionRunner> compiledRunners, Player player) {
        for (ActionRunner runner : compiledRunners) {
            getLogger().info("The following code is being executed:\n" + ChatColor.DARK_GRAY + runner.code());
            sendActionBar(player, ChatColor.GREEN + "Attempting to run the next action candidate...");

            AtomicBoolean finishedRunning = new AtomicBoolean(false);
            AtomicBoolean runSuccess = new AtomicBoolean();
            boolean startSuccess = runner.run(player, this, tempDir, success -> {
                runSuccess.set(success);
                finishedRunning.set(true);
            });
            if (!startSuccess) {
                sendActionBar(player, ChatColor.RED + "Failed to start running the action candidate. :(");
                continue;
            }

            long startTime = System.currentTimeMillis();
            while (!finishedRunning.get()) {
                long timeElapsed = System.currentTimeMillis() - startTime;
                if (timeElapsed < 3000) {
                    continue;
                }

                // TODO: Somehow stop the action forcefully
                getLogger().warning("A running action has been running for longer than 3 seconds!");
            }
            if (runSuccess.get()) {
                sendActionBar(player, ChatColor.GREEN +
                        "Successfully ran the action candidate!");
                break;
            }
            sendActionBar(player, ChatColor.RED +
                    "Something went wrong while running the action candidate. :(");
        }
    }

    public void prepareAction(String baseId, String message, Player player) {
        List<ActionCandidate> candidates = new ArrayList<>();

        for (int i = 0; i < CANDIDATES_PER_ACTION; i++) {
            String classId = baseId + i;
            String prompt = String.format("""
                import org.bukkit.*;
                import org.bukkit.entity.*;
                import org.bukkit.inventory.*;
                import org.bukkit.plugin.*;
                import org.bukkit.plugin.java.*;
                
                import java.time.*;
                import java.util.*;
                
                public class Action%s {
                    // Action: %s
                    public static void runAction(Player me) {""", classId, message);

            getServer().getScheduler().runTaskAsynchronously(this, task ->
                    generateCode(candidates, classId, prompt));
        }

        getServer().getScheduler().runTaskAsynchronously(this, task -> {
            List<ActionRunner> compiledRunners = new ArrayList<>();

            BossBar bossBar = getServer().createBossBar("", BarColor.GREEN, BarStyle.SOLID);
            bossBar.addPlayer(player);

            while (true) {
                Map<CandidateStatus, List<ActionCandidate>> candidatesByStatus = new HashMap<>();
                for (CandidateStatus status : CandidateStatus.values()) {
                    List<ActionCandidate> statusList = new ArrayList<>();
                    candidatesByStatus.put(status, statusList);
                }

                int completeCandidates = 0;
                synchronized (candidates) {
                    for (ActionCandidate candidate : candidates) {
                        CandidateStatus status = candidate.getStatus();
                        if (status.isComplete()) {
                            completeCandidates++;
                        }
                        candidatesByStatus.get(status).add(candidate);
                    }
                }

                StringBuilder bossBarText = new StringBuilder();
                for (Map.Entry<CandidateStatus, List<ActionCandidate>> entry : candidatesByStatus.entrySet()) {
                    if (!bossBarText.toString().equals("")) {
                        bossBarText.append(", ");
                    }

                    CandidateStatus status = entry.getKey();
                    int count = entry.getValue().size();
                    bossBarText.append(status.getIdentifierColor())
                            .append(status.getIdentifier())
                            .append(": ")
                            .append(ChatColor.RESET)
                            .append(count);
                }
                bossBar.setTitle(bossBarText.toString());

                double percentDone = (double) completeCandidates / CANDIDATES_PER_ACTION;
                bossBar.setProgress(percentDone);

                if (completeCandidates >= CANDIDATES_PER_ACTION) {
                    if (compiledRunners.isEmpty()) {
                        sendActionBar(player, ChatColor.RED +
                                "Copilot didn't generate anything runnable. Try rephrasing your message. :(");
                    } else {
                        runAction(compiledRunners, player);
                    }

                    bossBar.removeAll();
                    break;
                }

                sendActionBar(player, ChatColor.GREEN + "Preparing action candidates...");

                List<ActionCandidate> awaitingCompilation =
                        candidatesByStatus.get(CandidateStatus.AWAITING_COMPILATION);
                if (!awaitingCompilation.isEmpty()) {
                    ActionCandidate candidate = awaitingCompilation.get(0);
                    ActionRunner runner = candidate.getRunner();
                    if (runner.compile(tempDir)) {
                        compiledRunners.add(runner);
                        candidate.setStatus(CandidateStatus.READY_TO_RUN);
                    } else {
                        candidate.setStatus(CandidateStatus.COMPILATION_FAILED);
                    }
                }
            }
        });
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        if (message.startsWith("!")) {
            String newMessage = message.replaceFirst("!", ChatColor.RED + "!" + ChatColor.GRAY);
            event.setMessage(newMessage);
            return;
        }

        String timeId = Long.toString(System.currentTimeMillis());
        String randomId = Long.toString(Math.abs(random.nextLong()));
        String baseId = timeId + randomId;

        prepareAction(baseId, message, event.getPlayer());
    }
}
