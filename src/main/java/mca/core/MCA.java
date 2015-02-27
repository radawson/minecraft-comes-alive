package mca.core;

import java.util.HashMap;
import java.util.Map;

import mca.api.APICoreMCA;
import mca.command.CommandMCA;
import mca.core.forge.EventHooksFML;
import mca.core.forge.EventHooksForge;
import mca.core.forge.GuiHandler;
import mca.core.forge.ServerProxy;
import mca.core.minecraft.Achievements;
import mca.core.minecraft.Blocks;
import mca.core.minecraft.Items;
import mca.core.radix.LanguageParser;
import mca.data.PlayerData;
import mca.entity.EntityHuman;
import mca.network.MCAPacketHandler;
import mca.tile.TileVillagerBed;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.Logger;

import radixcore.ModMetadataEx;
import radixcore.RadixCore;
import radixcore.data.AbstractPlayerData;
import radixcore.data.DataContainer;
import radixcore.helpers.StartupHelper;
import radixcore.lang.LanguageManager;
import radixcore.math.Point3D;
import radixcore.update.RDXUpdateProtocol;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(modid = MCA.ID, name = MCA.NAME, version = MCA.VERSION, dependencies = "required-after:RadixCore@[2.0.0,)", acceptedMinecraftVersions = "[1.7.10]",
		guiFactory = "mca.core.forge.client.MCAGuiFactory")
public class MCA
{
	public static final String ID = "MCA";
	public static final String NAME = "Minecraft Comes Alive";
	public static final String VERSION = "5.0.0";

	@Instance(ID)
	private static MCA instance;
	private static ModMetadata metadata;
	private static Items items;
	private static Blocks blocks;
	private static Achievements achievements;
	private static CreativeTabs creativeTabMain;
	private static CreativeTabs creativeTabGemCutting;
	private static Config config;
	private static LanguageManager languageManager;
	private static MCAPacketHandler packetHandler;
	
	private static Logger logger;
	
	@SidedProxy(clientSide = "mca.core.forge.ClientProxy", serverSide = "mca.core.forge.ServerProxy")
	public static ServerProxy proxy;
	
	public static Map<String, AbstractPlayerData> playerDataMap;
	
	@SideOnly(Side.CLIENT)
	public static DataContainer playerDataContainer;
	@SideOnly(Side.CLIENT)
	public static Point3D destinyCenterPoint;
	
	@EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {	
    	instance = this;
		metadata = event.getModMetadata();
    	logger = event.getModLog();
    	config = new Config(event);
    	languageManager = new LanguageManager(ID, new LanguageParser());
    	packetHandler = new MCAPacketHandler(ID);
    	proxy.registerRenderers();
    	proxy.registerEventHandlers();
    	playerDataMap = new HashMap<String, AbstractPlayerData>();
    	
    	ModMetadataEx exData = ModMetadataEx.getFromModMetadata(metadata);
    	exData.updateProtocolClass = RDXUpdateProtocol.class;
    	exData.classContainingClientDataContainer = MCA.class;
    	exData.playerDataMap = playerDataMap;
    	
    	RadixCore.registerMod(exData);
    	
    	FMLCommonHandler.instance().bus().register(new EventHooksFML());
    	MinecraftForge.EVENT_BUS.register(new EventHooksForge());
    	NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    	creativeTabMain = StartupHelper.registerCreativeTab(Items.class, "engagementRing", metadata);
    	creativeTabGemCutting = StartupHelper.registerCreativeTab(Items.class, "diamondHeart", metadata);
    	items = new Items();
    	blocks = new Blocks();
    	achievements = new Achievements();
    	
    	SkinLoader.loadSkins();

    	//Entity registry
    	EntityRegistry.registerModEntity(EntityHuman.class, EntityHuman.class.getSimpleName(), config.baseEntityId, this, 50, 2, true);

    	//Tile registry
    	GameRegistry.registerTileEntity(TileVillagerBed.class, TileVillagerBed.class.getSimpleName());
    	
    	//Recipes
    	GameRegistry.addRecipe(new ItemStack(Items.engagementRing), 
    			"GDG", "G G", "GGG", 'D', net.minecraft.init.Items.diamond, 'G', net.minecraft.init.Items.gold_ingot);
    	GameRegistry.addRecipe(new ItemStack(Items.engagementRingRG), 
    			"GDG", "G G", "GGG", 'D', net.minecraft.init.Items.diamond, 'G', Items.roseGoldIngot);
    	GameRegistry.addRecipe(new ItemStack(Items.weddingRing),
    			"GGG", "G G", "GGG", 'G', net.minecraft.init.Items.gold_ingot);
    	GameRegistry.addRecipe(new ItemStack(Items.weddingRingRG),
    			"GGG", "G G", "GGG", 'G', Items.roseGoldIngot);
    	GameRegistry.addRecipe(new ItemStack(Blocks.roseGoldBlock),
    			"GGG", "GGG", "GGG", 'G', Items.roseGoldIngot);
    	GameRegistry.addRecipe(new ItemStack(Items.matchmakersRing),
    			"III", "I I", "III", 'I', net.minecraft.init.Items.iron_ingot);
    	
		for (int i = 0; i < 16; ++i)
		{
			ItemStack diamond =  new ItemStack(Items.coloredDiamond, 1, i);
			ItemStack engagementRing = new ItemStack(Items.coloredEngagementRing, 1, i);
			ItemStack engagementRingRG = new ItemStack(Items.coloredEngagementRingRG, 1, i);
			ItemStack dye = new ItemStack(net.minecraft.init.Items.dye, 1, i);
			
			GameRegistry.addShapelessRecipe(diamond, dye, new ItemStack(net.minecraft.init.Items.diamond));

	    	GameRegistry.addRecipe(engagementRing, 
	    			"GDG", "G G", "GGG", 'D', diamond, 'G', net.minecraft.init.Items.gold_ingot);
	    	GameRegistry.addRecipe(engagementRingRG, 
	    			"GDG", "G G", "GGG", 'D', diamond, 'G', Items.roseGoldIngot);
		}
		
    	//Smeltings
    	GameRegistry.addSmelting(Blocks.roseGoldOre, new ItemStack(Items.roseGoldIngot), 5);
    }
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
    	APICoreMCA.addObjectAsGift(net.minecraft.init.Items.wooden_sword, 3);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.wooden_axe, 3);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.wooden_hoe, 3);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.wooden_shovel, 3);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.stone_sword, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.stone_axe, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.stone_hoe, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.stone_shovel, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.wooden_pickaxe, 3);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.beef, 2);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.chicken, 2);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.porkchop, 2);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.leather, 2);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.leather_chestplate, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.leather_helmet, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.leather_leggings, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.leather_boots, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.reeds, 2);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.wheat_seeds, 2);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.wheat, 3);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.bread, 6);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.coal, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.sugar, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.clay_ball, 2);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.dye, 1);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.cooked_beef, 7);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.cooked_chicken, 7);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.cooked_porkchop, 7);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.cookie, 10);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.melon, 10);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.melon_seeds, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.iron_helmet, 10);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.iron_chestplate, 10);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.iron_leggings, 10);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.iron_boots, 10);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.cake, 12);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.iron_sword, 10);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.iron_axe, 10);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.iron_hoe, 10);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.iron_pickaxe, 10);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.iron_shovel, 10);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.fishing_rod, 3);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.bow, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.book, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.bucket, 3);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.milk_bucket, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.water_bucket, 2);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.lava_bucket, 2);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.mushroom_stew, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.pumpkin_seeds, 8);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.flint_and_steel, 4);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.redstone, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.boat, 4);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.wooden_door, 4);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.iron_door, 6);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.minecart, 7);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.flint, 2);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.gold_nugget, 4);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.gold_ingot, 20);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.iron_ingot, 10);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.diamond, 30);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.map, 10);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.clock, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.compass, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.blaze_rod, 10);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.blaze_powder, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.diamond_sword, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.diamond_axe, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.diamond_shovel, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.diamond_hoe, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.diamond_leggings, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.diamond_helmet, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.diamond_chestplate, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.diamond_leggings, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.diamond_boots, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.painting, 6);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.ender_pearl, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.ender_eye, 10);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.potionitem, 3);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.slime_ball, 3);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.saddle, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.gunpowder, 7);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.golden_apple, 25);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.record_11, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.record_13, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.record_wait, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.record_cat, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.record_chirp, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.record_far, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.record_mall, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.record_mellohi, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.record_stal, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.record_strad, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.record_ward, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Items.emerald, 25);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.red_flower, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.yellow_flower, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.planks, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.log, 3);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.pumpkin, 3);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.chest, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.wool, 2);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.iron_ore, 4);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.gold_ore, 7);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.redstone_ore, 3);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.rail, 3);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.detector_rail, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.activator_rail, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.furnace, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.crafting_table, 5);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.lapis_block, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.bookshelf, 7);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.gold_block, 50);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.iron_block, 25);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.diamond_block, 100);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.brewing_stand, 12);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.enchanting_table, 25);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.brick_block, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.obsidian, 15);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.piston, 10);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.glowstone, 10);
		APICoreMCA.addObjectAsGift(net.minecraft.init.Blocks.emerald_block, 100);
		
		APICoreMCA.addBlockToMiningAI(net.minecraft.init.Blocks.coal_ore, 1);
		APICoreMCA.addBlockToMiningAI(net.minecraft.init.Blocks.iron_ore, 2);
		APICoreMCA.addBlockToMiningAI(net.minecraft.init.Blocks.lapis_ore, 3);
		APICoreMCA.addBlockToMiningAI(net.minecraft.init.Blocks.gold_ore, 4);
		APICoreMCA.addBlockToMiningAI(net.minecraft.init.Blocks.diamond_ore, 5);
		APICoreMCA.addBlockToMiningAI(net.minecraft.init.Blocks.emerald_ore, 6);
		APICoreMCA.addBlockToMiningAI(net.minecraft.init.Blocks.quartz_ore, 7);
		APICoreMCA.addBlockToMiningAI(Blocks.roseGoldOre, 8);
    }
    
    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
    	event.registerServerCommand(new CommandMCA());
    }
    
	public static MCA getInstance()
	{
		return instance;
	}
	
	public static Logger getLog()
	{
		return logger;
	}
	
	public static Config getConfig()
	{
		return config;
	}
	
	public static ModMetadata getMetadata()
	{
		return metadata;
	}
	
	public static CreativeTabs getCreativeTabMain()
	{
		return creativeTabMain;
	}
	
	public static CreativeTabs getCreativeTabGemCutting()
	{
		return creativeTabGemCutting;
	}
	
	public static LanguageManager getLanguageManager()
	{
		return languageManager;
	}
	
	public static MCAPacketHandler getPacketHandler()
	{
		return packetHandler;
	}
	
	public static PlayerData getPlayerData(EntityPlayer player)
	{
		if (!player.worldObj.isRemote)
		{
			return (PlayerData) playerDataMap.get(player.getUniqueID().toString());
		}
		
		else
		{
			return playerDataContainer.getPlayerData(PlayerData.class);
		}
	}

	public static EntityHuman getHumanByPermanentId(int id) 
	{
		for (WorldServer world : MinecraftServer.getServer().worldServers)
		{
			for (Object obj : world.loadedEntityList)
			{
				if (obj instanceof EntityHuman)
				{
					EntityHuman human = (EntityHuman)obj;
					
					if (human.getPermanentId() == id)
					{
						return human;
					}
				}
			}
		}
		
		return null;
	}
}
