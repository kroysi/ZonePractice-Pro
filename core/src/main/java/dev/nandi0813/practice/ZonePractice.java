package dev.nandi0813.practice;

import com.github.retrooper.packetevents.PacketEvents;
import dev.nandi0813.practice.Command.Accept.AcceptCommand;
import dev.nandi0813.practice.Command.Arena.ArenaCommand;
import dev.nandi0813.practice.Command.Division.DivisionsCommand;
import dev.nandi0813.practice.Command.Duel.DuelCommand;
import dev.nandi0813.practice.Command.Event.EventCommand;
import dev.nandi0813.practice.Command.FFA.FFACommand;
import dev.nandi0813.practice.Command.Hologram.HologramCommand;
import dev.nandi0813.practice.Command.Ladder.LadderCommand;
import dev.nandi0813.practice.Command.MatchStats.MatchStatsCommand;
import dev.nandi0813.practice.Command.Party.PartyCommand;
import dev.nandi0813.practice.Command.Practice.PracticeCommand;
import dev.nandi0813.practice.Command.Preview.PreviewCommand;
import dev.nandi0813.practice.Command.PrivateMessage.MessageCommand;
import dev.nandi0813.practice.Command.PrivateMessage.ReplyCommand;
import dev.nandi0813.practice.Command.Settings.SettingsCommand;
import dev.nandi0813.practice.Command.Setup.SetupCommand;
import dev.nandi0813.practice.Command.SingleCommands.*;
import dev.nandi0813.practice.Command.Spectate.SpectateCommand;
import dev.nandi0813.practice.Command.Staff.StaffCommand;
import dev.nandi0813.practice.Command.Statistics.StatisticsCommand;
import dev.nandi0813.practice.Listener.*;
import dev.nandi0813.practice.Manager.Arena.ArenaListener;
import dev.nandi0813.practice.Manager.Arena.ArenaManager;
import dev.nandi0813.practice.Manager.Arena.Util.ArenaWorldUtil;
import dev.nandi0813.practice.Manager.Backend.*;
import dev.nandi0813.practice.Manager.Division.DivisionManager;
import dev.nandi0813.practice.Manager.Fight.Event.EventManager;
import dev.nandi0813.practice.Manager.Fight.FFA.FFAManager;
import dev.nandi0813.practice.Manager.Fight.Match.MatchManager;
import dev.nandi0813.practice.Manager.Fight.Util.EntityHiderListener;
import dev.nandi0813.practice.Manager.GUI.Setup.Arena.ArenaSetupManager;
import dev.nandi0813.practice.Manager.Inventory.InventoryManager;
import dev.nandi0813.practice.Manager.Ladder.Abstraction.Ladder;
import dev.nandi0813.practice.Manager.Ladder.LadderManager;
import dev.nandi0813.practice.Manager.Leaderboard.Hologram.HologramManager;
import dev.nandi0813.practice.Manager.Leaderboard.LeaderboardManager;
import dev.nandi0813.practice.Manager.PlayerDisplay.Tab.TabListManager;
import dev.nandi0813.practice.Manager.PlayerKit.PlayerKitManager;
import dev.nandi0813.practice.Manager.Profile.ProfileManager;
import dev.nandi0813.practice.Manager.Server.ServerManager;
import dev.nandi0813.practice.Manager.Sidebar.SidebarManager;
import dev.nandi0813.practice.Module.Util.VersionChecker;
import dev.nandi0813.practice.Util.*;
import dev.nandi0813.practice.Util.PlaceholderAPI.PlayerExpansion;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

public final class ZonePractice extends JavaPlugin {

    @Getter
    private Map<StartUpTypes, Boolean> startUpProgress = new EnumMap<>(StartUpTypes.class);

    @Getter
    private static ZonePractice instance;
    @Getter
    private static BukkitAudiences adventure;

    private Metrics metrics;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        instance = this;
        adventure = BukkitAudiences.create(this);
        PacketEvents.getAPI().init();
        metrics = new Metrics(this, 16055);

        if (VersionChecker.getBukkitVersion() == null) {
            Common.sendConsoleMMMessage("<red>Unsupported server version! Please use 1.8.8 or 1.8.9 or 1.20.6 or 1.21.4");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        new SaveResource().saveResources(this);

        ConfigManager.createFile();
        LanguageManager.createFile(this);
        GUIFile.createFile(this);
        MysqlManager.openConnection();
        DivisionManager.getInstance().getData();
        ArenaWorldUtil.createArenaWorld();
        BackendManager.createFile(this);

        ZonePracticeApiImpl.setup();
        StartUpUtil.loadStartUpProgressMap();

        this.registerCommands(Bukkit.getServer());
        this.registerListeners(Bukkit.getPluginManager());

        ServerManager.getInstance().loadLobby();
        InventoryManager.getInstance().loadInventories();
        PlayerKitManager.getInstance().load();
        TabListManager.getInstance().start();

        LadderManager.getInstance().loadLadders(() ->
        {
            LadderManager.getInstance().getLadders().sort(Comparator.comparing(Ladder::getName));
            startUpProgress.replace(StartUpTypes.LADDER_LOADING, true);

            ArenaManager.getInstance().loadArenas(() ->
            {
                ArenaSetupManager.getInstance().loadGUIs();
                startUpProgress.replace(StartUpTypes.ARENA_LOADING, true);
            });

            ProfileManager.getInstance().loadProfiles(() ->
            {
                ProfileManager.getInstance().loadAllProfileInformations();
                startUpProgress.replace(StartUpTypes.PROFILE_LOADING, true);

                LeaderboardManager.getInstance().createAllLB(() ->
                {
                    startUpProgress.replace(StartUpTypes.LEADERBOARD_LOADING, true);
                    LadderManager.getInstance().loadGUIs();

                    HologramManager.getInstance().loadHolograms();
                    startUpProgress.replace(StartUpTypes.HOLOGRAM_LOADING, true);

                    SidebarManager.getInstance().load();
                    startUpProgress.replace(StartUpTypes.SIDEBAR_LOADING, true);

                    this.loadPlaceholderAPI();
                });
            });
        });

        EventManager.getInstance().loadEventData(() ->
        {
            EventManager.getInstance().loadGUIs();
            startUpProgress.replace(StartUpTypes.EVENT_LOADING, true);
        });

        FFAManager.getInstance();
        EntityHiderListener.getInstance();
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
        MatchManager.getInstance().endMatches();
        FFAManager.getInstance().endFFAs();
        HologramManager.getInstance().saveHolograms();
        EventManager.getInstance().endEvents();
        EventManager.getInstance().saveEventData();
        ArenaManager.getInstance().saveArenas();
        ProfileManager.getInstance().saveProfiles();
        LadderManager.getInstance().saveLadders();
        SidebarManager.getInstance().close();
        InventoryManager.getInstance().setData();
        if (adventure != null) adventure.close();
        if (metrics != null) metrics.shutdown();
        MysqlManager.closeConnection();
        BackendManager.save();
    }

    /**
     * It registers all the commands that the plugin uses
     */
    private void registerCommands(Server server) {
        AcceptCommand acceptCommand = new AcceptCommand();
        if (server.getPluginCommand("accept") != null) {
            server.getPluginCommand("accept").setExecutor(acceptCommand);
            server.getPluginCommand("accept").setTabCompleter(acceptCommand);
        }

        ArenaCommand arenaCommand = new ArenaCommand();
        if (server.getPluginCommand("arena") != null) {
            server.getPluginCommand("arena").setExecutor(arenaCommand);
            server.getPluginCommand("arena").setTabCompleter(arenaCommand);
        }

        DuelCommand duelCommand = new DuelCommand();
        if (server.getPluginCommand("duel") != null) {
            server.getPluginCommand("duel").setExecutor(duelCommand);
            server.getPluginCommand("duel").setTabCompleter(duelCommand);
        }

        EventCommand eventCommand = new EventCommand();
        if (server.getPluginCommand("event") != null) {
            server.getPluginCommand("event").setExecutor(eventCommand);
            server.getPluginCommand("event").setTabCompleter(eventCommand);
        }

        HologramCommand hologramCommand = new HologramCommand();
        if (server.getPluginCommand("hologram") != null) {
            server.getPluginCommand("hologram").setExecutor(hologramCommand);
            server.getPluginCommand("hologram").setTabCompleter(hologramCommand);
        }

        LadderCommand ladderCommand = new LadderCommand();
        if (server.getPluginCommand("ladder") != null) {
            server.getPluginCommand("ladder").setExecutor(ladderCommand);
            server.getPluginCommand("ladder").setTabCompleter(ladderCommand);
        }

        MatchStatsCommand matchStatsCommand = new MatchStatsCommand();
        if (server.getPluginCommand("matchinv") != null) {
            server.getPluginCommand("matchinv").setExecutor(matchStatsCommand);
        }

        PartyCommand partyCommand = new PartyCommand();
        if (server.getPluginCommand("party") != null) {
            server.getPluginCommand("party").setExecutor(partyCommand);
            server.getPluginCommand("party").setTabCompleter(partyCommand);
        }

        PracticeCommand practiceCommand = new PracticeCommand();
        if (server.getPluginCommand("practice") != null) {
            server.getPluginCommand("practice").setExecutor(practiceCommand);
            server.getPluginCommand("practice").setTabCompleter(practiceCommand);
        }

        PreviewCommand previewCommand = new PreviewCommand();
        if (server.getPluginCommand("preview") != null) {
            server.getPluginCommand("preview").setExecutor(previewCommand);
            server.getPluginCommand("preview").setTabCompleter(previewCommand);
        }

        DivisionsCommand divisionsCommand = new DivisionsCommand();
        if (server.getPluginCommand("divisions") != null) {
            server.getPluginCommand("divisions").setExecutor(divisionsCommand);
        }

        SettingsCommand settingsCommand = new SettingsCommand();
        if (server.getPluginCommand("settings") != null) {
            server.getPluginCommand("settings").setExecutor(settingsCommand);
        }

        SetupCommand setupCommand = new SetupCommand();
        if (server.getPluginCommand("setup") != null) {
            server.getPluginCommand("setup").setExecutor(setupCommand);
        }

        SpectateCommand spectateCommand = new SpectateCommand();
        if (server.getPluginCommand("spectate") != null) {
            server.getPluginCommand("spectate").setExecutor(spectateCommand);
            server.getPluginCommand("spectate").setTabCompleter(spectateCommand);
        }

        StaffCommand staffCommand = new StaffCommand();
        if (server.getPluginCommand("staff") != null) {
            server.getPluginCommand("staff").setExecutor(staffCommand);
            server.getPluginCommand("staff").setTabCompleter(staffCommand);
        }

        StatisticsCommand statisticsCommand = new StatisticsCommand();
        if (server.getPluginCommand("statistics") != null) {
            server.getPluginCommand("statistics").setExecutor(statisticsCommand);
            server.getPluginCommand("statistics").setTabCompleter(statisticsCommand);
        }

        UnrankedCommand unrankedCommand = new UnrankedCommand();
        if (server.getPluginCommand("unranked") != null) {
            server.getPluginCommand("unranked").setExecutor(unrankedCommand);
        }

        RankedCommand rankedCommand = new RankedCommand();
        if (server.getPluginCommand("ranked") != null) {
            server.getPluginCommand("ranked").setExecutor(rankedCommand);
        }

        EditorCommand editorCommand = new EditorCommand();
        if (server.getPluginCommand("editor") != null) {
            server.getPluginCommand("editor").setExecutor(editorCommand);
        }

        CopyKitCommand copyKitCommand = new CopyKitCommand();
        if (server.getPluginCommand("copykit") != null) {
            server.getPluginCommand("copykit").setExecutor(copyKitCommand);
        }

        FFACommand ffaCommand = new FFACommand();
        if (server.getPluginCommand("ffa") != null) {
            server.getPluginCommand("ffa").setExecutor(ffaCommand);
            server.getPluginCommand("ffa").setTabCompleter(ffaCommand);
        }

        IgnoreQueueCommand ignoreQueueCommand = new IgnoreQueueCommand();
        if (server.getPluginCommand("ignorequeue") != null) {
            server.getPluginCommand("ignorequeue").setExecutor(ignoreQueueCommand);
            server.getPluginCommand("ignorequeue").setTabCompleter(ignoreQueueCommand);
        }

        if (ConfigManager.getBoolean("CHAT.PRIVATE-CHAT-ENABLED")) {
            new MessageCommand();
            new ReplyCommand();
        }

        if (ConfigManager.getBoolean("MATCH-SETTINGS.LEAVE-COMMAND.ENABLED")) {
            new LeaveCommand();
        }
    }

    private void loadPlaceholderAPI() {
        if (SoftDependUtil.isPAPI_ENABLED) {
            PlayerExpansion playerExpansion = new PlayerExpansion();
            playerExpansion.register();
        }
    }

    /**
     * It registers all the events that are used in the plugin
     */
    private void registerListeners(PluginManager pm) {
        pm.registerEvents(new PlayerJoin(), this);
        pm.registerEvents(new PlayerQuit(), this);
        pm.registerEvents(new PlayerInteract(), this);
        pm.registerEvents(new WeatherChange(), this);
        pm.registerEvents(new ItemConsume(), this);
        pm.registerEvents(new ProjectileLaunch(), this);
        pm.registerEvents(new PlayerCommandPreprocess(), this);
        pm.registerEvents(new PlayerChat(), this);
        pm.registerEvents(new EntityDamage(), this);
        pm.registerEvents(new ArenaListener(), this);
    }

}
