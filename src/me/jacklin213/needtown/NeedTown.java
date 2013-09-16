package me.jacklin213.needtown;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import me.jacklin213.needtown.utils.UpdateChecker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class NeedTown extends JavaPlugin {

	public static NeedTown plugin;
	
	public final Logger logger = Logger.getLogger("Minecraft");	
	public String chatPluginPrefix;
	public UpdateChecker updateChecker;
	private Towny towny = null;
	private File colorFile;
	public PluginManager pm;
	
	private List<String> cantDoCommand = new ArrayList<String>();

	public void onDisable() {
		logger.info(String.format("[%s] 비활성화됨, 버전 %s", getDescription()
				.getName(), getDescription().getVersion()));
	}

	public void onEnable() {
		
		pm = getServer().getPluginManager();
		
		Boolean useTowny = getConfig().getBoolean("TownyIntegration");
		
		// Update Check
		Boolean updateCheck = Boolean.valueOf(getConfig().getBoolean("UpdateCheck"));
		 
		this.updateChecker = new UpdateChecker(this, "http://dev.bukkit.org/server-mods/needtown/files.rss");

		if ((updateCheck) && (this.updateChecker.updateNeeded())) {
			this.logger.info(String.format("[%s] A new update is avalible, Version: %s", getDescription().getName(), this.updateChecker.getVersion()));
			this.logger.info(String.format("[%s] Get it now from: %s", getDescription().getName(), this.updateChecker.getLink()));
		}
		
		// Checks for towny
		if (useTowny){
			checkPlugins();
			if ((towny == null) || (getServer().getScheduler().scheduleSyncDelayedTask(this, new onLoadedTask(this),1) == -1)){
				/*
				 *  We either failed to find Towny
				 *  or the Scheduler failed to register the task.
				 */
				logger.severe(String.format("[%s] Towny를 발견하지 못했습니다, 플러그인 비활성화 중...", getDescription().getName()));
				logger.severe(String.format("[%s] 비활성화되었습니다.", getDescription().getName()));;
				pm.disablePlugin(this);
				return;
			}
		}
		
		logger.info(String.format("[%s] 활성화됨, 버전 %s by jacklin213",
				getDescription().getName(), getDescription().getVersion()));
		
		// Creates Config.yml + Colors.yml
		createfiles();
	}

	public void createfiles() {
		// Creates config.yml
		File configfile = new File(getDataFolder() + File.separator
				+ "config.yml");
		this.colorFile = new File(getDataFolder() + File.separator
				+ "colors.yml");
		// If config.yml doesnt exit
		if (!configfile.exists() || !colorFile.exists()){
			if (!configfile.exists()) {
				this.getConfig().options().copyDefaults(true);
				this.saveDefaultConfig();
			}
			if (!colorFile.exists()) {
				try {
					this.logger.info("[NeedTown] colors.yml 을 생성합니다");
					PrintStream out = new PrintStream(new FileOutputStream(
							this.colorFile));
					out.println("# ======= Color.yml ======= #");
					out.println("# Do not edit any thing in here or else you won't know the colors");
					out.println("# This is a Color.yml for NeedTown");
					out.println("List of colors:");
					out.println("<red> - Color Red");
					out.println("<dark_red> - Color DarkRed");
					out.println("<green> - Color Green");
					out.println("<dark_green> - Color Dark-Green");
					out.println("<aqua> - Color Aqua");
					out.println("<dark_aqua> - Color Dark-Aqua");
					out.println("<blue> - Color Gold");
					out.println("<dark_blue> - Color Dark-Blue");
					out.println("<yellow> - Color Yellow");
					out.println("<gold> - Color Gold");
					out.println("<white> - Color White");
					out.println("<black> - Color Black");
					out.println("<light_purple> - Color Light-Purple");
					out.println("<dark_purple> - Color Dark-Purple");
					out.println("<gray> - Color Gray");
					out.println("<dark_gray> - Color Dark-Grey");
					out.println("# These are the only ones tested so far, feel free too try them yourself");
					out.println();
					out.println("# Copyright BMX_ATVMAN14,jacklin213,LinCraft,LinProdutions 2012");
					out.close();
				} catch (IOException e) {
					this.logger.severe(String.format("[%s] 파일 생성 실패 !", getDescription().getName()));
				}
				
			}
			
			this.getLogger().info("필요한 파일 생성 ");
		}
	}

	  public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		  // Gets things needed from Config
		  boolean useCooldown = getConfig().getBoolean("Cooldowns");
		  boolean useTowny = getConfig().getBoolean("TownyIntegration");
		  int cdTime = (getCooldownTime() * 20);
		  chatPluginPrefix = format(getConfig().getString("PluginPrefix")) + " ";
		  // Check if sender is a player
		  if (commandLabel.equalsIgnoreCase("니드타운리로드") || commandLabel.equalsIgnoreCase("니타리로드")){
			  if (sender.hasPermission("needtown.reload")){
				  if (sender instanceof Player){
					  sender.sendMessage(chatPluginPrefix + "콘솔에서만 사용가능합니다!"); 
					  return true;
				  }
				  reloadNTConsole(sender);
				  return true;
			  }
		  }
		  if (sender instanceof Player) {
			  Player player = (Player)sender;
			  final String playerName = player.getName();
			  if (player.hasPermission("needtown.use")) {
				  if ((cmd.getName().equalsIgnoreCase("마을구함")) || (cmd.getName().equalsIgnoreCase("nt"))) {
					  
					  if (args.length == 1) {
						  if (player.hasPermission("needtown.reload")) {
							  if (args[0].equalsIgnoreCase("리로드")) {
								  reloadNT(sender);
								  return true;
							  }
						  } else {
							  player.sendMessage(ChatColor.RED + "오류: 이 명령어를 사용할 권한이 없습니다!");
							  return true;
						  }
					  }
					  
					  // Using Towny (begin)
					  if (useTowny){
						  List<Resident> residentList = TownyUniverse.getDataSource().getResidentsWithoutTown();
						  if (!residentList.contains(player)){
							  player.sendMessage(chatPluginPrefix + "이미 마을에 소속되어 있으므로 이 명령어를 사용할 수 없습니다!"); 
							  return true;
						  } else {
							  if (useCooldown){
								  runCommandWithCD(playerName, player, cdTime);
								  return true;
							  } else {
								  runCommand(player);
								  return true;
							  }
						  }
					  } else {
					// Using Towny (end)
						  //Without towny
						  if (useCooldown){
							  runCommandWithCD(playerName, player, cdTime);
							  return true;
						  } else {
							 runCommand(player);
							 return true;
						  }
					  }
				  }
				 
			  } else {
				  player.sendMessage(ChatColor.RED + "오류: 이 명령어를 사용할 권한이 없습니다!");
				  return true;
			  }
		  } else {
			  sender.sendMessage("플레이어만 이 명령어를 사용할 수 있습니다");
			  return true;
		  }
		  return false;
	  }
		  
	/**
	 * Gets message from config and formats it (Grabs colors)
	 * @param string - The message
	 * @return Formatted Message
	 */
	public static String format(String string) {
		String s = string;
		for (ChatColor color : ChatColor.values()) {
			s = s.replaceAll("(?i)<" + color.name() + ">", "" + color);
		}
		return s;
	}

	/**
	 * The default NeedTown message if none is specified in the config or if the config is broken
	 * @param player who issued the command
	 * @return Default NeedTown Message
	 */
	public boolean defaultmessage(Player player) {
		chatPluginPrefix = format(getConfig().getString("PluginPrefix")) + " ";
		Bukkit.broadcastMessage(chatPluginPrefix + ChatColor.GOLD + player.getDisplayName()
				+ ChatColor.AQUA + " 님이 마을에 들어가고 싶어합니다! "
				+ ChatColor.RED + "촌장님께서는" + ChatColor.AQUA
				+ "" + ChatColor.GOLD
				+ player.getDisplayName() + ChatColor.AQUA + "님을 초대해주세요!");

		return true;
	}
	
	/**
	 * Gets the cooldown time from config
	 * @return Cooldown time
	 */
	
	public int getCooldownTime(){
		int cdTime;
		try {
			cdTime = Integer.parseInt(getConfig().getString("Cooldown-time"));
			return cdTime;
		} catch (NumberFormatException e){
			this.logger.info(String.format("[%s] 설정값에서 CoolDown 값을 읽어오지 못했습니다.", getDescription().getName()));
			this.logger.info(String.format("[%s] 수정하신 후 플러그인을 리로드해 주시기 바랍니다", getDescription().getName()));
		}	
		return 0;
	}
	
	
	/**
	 * Runs the NeedTown command with cooldown.
	 * @param playerName who issued the command
	 * @param player who issued the command 
	 * @param cdTime from config
	 * @return Command issued
	 */
	
	public boolean runCommandWithCD(final String playerName, Player player, int cdTime){
		chatPluginPrefix = format(getConfig().getString("PluginPrefix")) + " ";
		if (!cantDoCommand.contains(playerName)){
			  if (getConfig().getBoolean("CustomNeedTownMessage", true)) {
				  String message = chatPluginPrefix + (getConfig().getString("Message"));
				  message = message.replace("%p", player.getName());
				  Bukkit.broadcastMessage(format(message));
				  cantDoCommand.add(playerName);
				  Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
					  public void run(){
						  cantDoCommand.remove(playerName);
					  }
				  }, cdTime);
				  return true;
			  } else {
				  cantDoCommand.add(playerName);
				  Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
					  public void run(){
						  cantDoCommand.remove(playerName);
					  }
				  }, cdTime);
				  return defaultmessage(player); 
			  }
		  } else {
			  String message = getConfig().getString("Cooldown-Message");
			  player.sendMessage(chatPluginPrefix + format(message));
			  return true;
		  }
	}
	
	/**
	 * Runs the Needtown command with no cooldown
	 * @param player who issued the command
	 * @return Command issued
	 */
	
	public boolean runCommand(Player player){
		chatPluginPrefix = format(getConfig().getString("PluginPrefix")) + " ";
		 if (getConfig().getBoolean("CustomNeedTownMessage", true)) {
			  String message = chatPluginPrefix + (getConfig().getString("Message"));
			  message = message.replace("%p", player.getName());
			  Bukkit.broadcastMessage(format(message));
			  return true;
		  } else {
			  return defaultmessage(player); 
		  }
	}
	
	private void checkPlugins() {
        Plugin test;
        
        pm = getServer().getPluginManager();
        test = pm.getPlugin("Towny");
        if (test != null && test instanceof Towny)
        	towny = (Towny)test;

	}
	
	protected Towny getTowny() {
		return towny;
	}
	
	/**
	 * Runs the reload for player
	 * @param useTowny - After config is reloaded, check if they enabled towny
	 * @param sender - CommandSender (Should be player)
	 */
	
	public void reloadNT(CommandSender sender){
		chatPluginPrefix = format(getConfig().getString("PluginPrefix")) + " ";
		reloadConfig();
		boolean useTowny = getConfig().getBoolean("TownyIntegration");
		  String string = "<green>설정값이 리로드되었습니다!";
		  if (useTowny){
				checkPlugins();
				if ((towny == null) || (getServer().getScheduler().scheduleSyncDelayedTask(this, new onLoadedTask(this),1) == -1)){
					/*
					 *  We either failed to find Towny
					 *  or the Scheduler failed to register the task.
					 */
					logger.severe(String.format("[%s] Towny를 발견하지 못했습니다. 플러그인 비활성화 중...", getDescription().getName()));
					logger.severe(String.format("[%s] 비활성화되었습니다.", getDescription().getName()));;
					pm.disablePlugin(this);
				}
			}
		  sender.sendMessage(chatPluginPrefix + format(string));
	}
	
	/**
	 * Runs the reload for console
	 * @param useTowny - After config is reloaded, check if they enabled towny
	 * @param sender - CommandSender (Should be console)
	 */
	public void reloadNTConsole(CommandSender sender){
		chatPluginPrefix = format(getConfig().getString("PluginPrefix")) + " ";
		reloadConfig();
		boolean useTowny = getConfig().getBoolean("TownyIntegration");
		  if (useTowny){
				checkPlugins();
				if ((towny == null) || (getServer().getScheduler().scheduleSyncDelayedTask(this, new onLoadedTask(this),1) == -1)){
					/*
					 *  We either failed to find Towny
					 *  or the Scheduler failed to register the task.
					 */
					logger.severe(String.format("[%s] Towny를 발견하지 못했습니다. 플러그인 비활성화 중...", getDescription().getName()));
					logger.severe(String.format("[%s] 비활성화되었습니다", getDescription().getName()));;
					pm.disablePlugin(this);
				}
			}
		  logger.info(String.format("[%s] 설정값이 리로드되었습니다!", getDescription().getName()));
	}

	/*
	 * For String format usage String message =
	 * this.getConfig().getString("Message"); this.getConfig().set("Message",
	 * format(message));
	 */

}
