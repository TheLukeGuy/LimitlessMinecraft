package com.lukepc.limitlessminecraft;

import com.lukepc.limitlessminecraft.copilot.CopilotAuth;
import com.lukepc.limitlessminecraft.copilot.CopilotRequest;
import com.lukepc.limitlessminecraft.copilot.CopilotToken;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class LimitlessMinecraft extends JavaPlugin implements Listener {
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

    public void startAction(String classId, String prompt, Player player) {
        String fullCode = null;
        do {
            CopilotRequest copilotRequest = new CopilotRequest();
            copilotRequest.setMaxTokens(prompt.length() + 500);
            copilotRequest.setCount(1);
            copilotRequest.setStop(List.of("\n}"));

            if (fullCode == null) {
                player.sendMessage(ChatColor.GREEN + "Waiting for a response from Copilot...");
            } else {
                player.sendMessage(ChatColor.GREEN + "Useless code, waiting for another response...");
            }

            List<String> choices = copilotRequest.send(copilotToken, prompt);
            if (choices == null) {
                return;
            }
            fullCode = (prompt + choices.get(0)).trim();
        } while (!fullCode.endsWith("}"));

        String fullCodeFlat = fullCode
                .replace("\n", "")
                .replace(" ", "");
        if (!fullCodeFlat.endsWith("}}")) {
            fullCode += "\n}";
        }

        getLogger().info(ChatColor.DARK_GRAY + "\n" + fullCode);
        ActionRunner runner = new ActionRunner(classId, fullCode, tempDir);

        player.sendMessage(ChatColor.GREEN + "Compiling the generated code...");
        if (!runner.compile()) {
            player.sendMessage(ChatColor.RED + "Something went wrong while compiling the code! :(");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "Running the generated code...");
        if (!runner.run(player, this)) {
            player.sendMessage(ChatColor.RED + "Something went wrong while running the code! :(");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "Successfully ran the code!");
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
        String classId = timeId + randomId;

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
                startAction(classId, prompt, event.getPlayer()));
    }
}
