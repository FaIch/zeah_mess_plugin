package com.zeahMess;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameState;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Zeah Mess Hall",
	description = "Highlights objects and menu options for Zeah Mess Hall pineapple pizza making",
	tags = {"zeah", "mess", "hall", "cooking", "pineapple", "pizza", "hosidius"}
)
public class ZeahMessPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(ZeahMessPlugin.class);

	static final int SINK_OBJECT_ID = 9684;
	static final int CUPBOARD_OBJECT_ID = 27375;
	static final int STOVE_OBJECT_ID = 21302;

	@Inject
	private Client client;

	@Inject
	private ZeahMessConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ZeahMessOverlay overlay;

	private MessState state = MessState.IDLE;
	private TileObject sink;
	private TileObject cupboard;
	private TileObject stove;

	MessState getState()   { return state; }
	TileObject getSink()   { return sink; }
	TileObject getCupboard() { return cupboard; }
	TileObject getStove()  { return stove; }

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		scanScene();
		log.debug("Zeah Mess Hall started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		clearTrackedObjects();
		state = MessState.IDLE;
		log.debug("Zeah Mess Hall stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			clearTrackedObjects();
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		trackObject(event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		untrackObject(event.getGameObject());
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		trackObject(event.getWallObject());
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned event)
	{
		untrackObject(event.getWallObject());
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		trackObject(event.getDecorativeObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		untrackObject(event.getDecorativeObject());
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		trackObject(event.getGroundObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		untrackObject(event.getGroundObject());
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INVENTORY.getId())
		{
			return;
		}
		updateState(event.getItemContainer());
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() != InterfaceID.HOSIDIUS_SERVERY)
		{
			return;
		}
		log.debug("Hosidius servery interface loaded — dumping widget tree:");
		for (int i = 0; i < 30; i++)
		{
			Widget w = client.getWidget(InterfaceID.HOSIDIUS_SERVERY, i);
			if (w == null)
			{
				continue;
			}
			log.debug("  [{}] itemId={} name='{}' text='{}' hidden={} spriteId={}",
				i, w.getItemId(), w.getName(), w.getText(), w.isHidden(), w.getSpriteId());
			Widget[] dyn = w.getDynamicChildren();
			if (dyn != null)
			{
				for (int j = 0; j < dyn.length; j++)
				{
					log.debug("    dyn[{}] itemId={} name='{}' text='{}' hidden={} spriteId={}",
						j, dyn[j].getItemId(), dyn[j].getName(), dyn[j].getText(), dyn[j].isHidden(), dyn[j].getSpriteId());
				}
			}
		}
	}

	@Provides
	ZeahMessConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ZeahMessConfig.class);
	}

	private void updateState(ItemContainer inventory)
	{
		Item[] items = inventory.getItems();

		boolean hasPineapple = hasItem(items, ItemID.HOSIDIUS_SERVERY_PINEAPPLE);
		boolean hasChunks = hasItem(items, ItemID.HOSIDIUS_SERVERY_PINEAPPLE_CHUNKS);
		boolean hasUncookedPizza = hasItem(items, ItemID.HOSIDIUS_SERVERY_UNCOOKED_PIZZA);
		boolean hasPlainPizza = hasItem(items, ItemID.HOSIDIUS_SERVERY_PLAIN_PIZZA);

		if (hasItem(items, ItemID.BOWL_EMPTY))
		{
			state = MessState.FILL_BOWLS;
		}
		else if (hasItem(items, ItemID.BOWL_WATER))
		{
			state = MessState.GET_FLOUR;
		}
		else if (hasItem(items, ItemID.HOSIDIUS_SERVERY_PIZZA_BASE))
		{
			state = MessState.GET_TOMATO;
		}
		else if (hasItem(items, ItemID.HOSIDIUS_SERVERY_INCOMPLETE_PIZZA))
		{
			state = MessState.GET_CHEESE;
		}
		else if (hasPineapple)
		{
			state = MessState.CUT_PINEAPPLE;
		}
		else if (hasChunks && hasUncookedPizza)
		{
			state = MessState.COOK;
		}
		else if (hasChunks && hasPlainPizza)
		{
			state = MessState.COMBINE_PIZZA;
		}
		else if (hasUncookedPizza)
		{
			state = MessState.GET_PINEAPPLE;
		}
		else
		{
			state = MessState.IDLE;
		}
	}

	private boolean hasItem(Item[] items, int itemId)
	{
		for (Item item : items)
		{
			if (item.getId() == itemId)
			{
				return true;
			}
		}
		return false;
	}

	int serveryItemForState()
	{
		switch (state)
		{
			case GET_FLOUR:    return ItemID.HOSIDIUS_SERVERY_POT_FLOUR;
			case GET_TOMATO:   return ItemID.HOSIDIUS_SERVERY_TOMATO;
			case GET_CHEESE:   return ItemID.HOSIDIUS_SERVERY_CHEESE;
			case GET_PINEAPPLE: return ItemID.HOSIDIUS_SERVERY_PINEAPPLE;
			default:           return -1;
		}
	}

	private void trackObject(TileObject obj)
	{
		if (obj == null)
		{
			return;
		}
		int id = obj.getId();
		if (id == SINK_OBJECT_ID)
		{
			sink = obj;
		}
		else if (id == CUPBOARD_OBJECT_ID)
		{
			cupboard = obj;
		}
		else if (id == STOVE_OBJECT_ID)
		{
			stove = obj;
		}
	}

	private void untrackObject(TileObject obj)
	{
		if (obj == sink)        sink = null;
		else if (obj == cupboard) cupboard = null;
		else if (obj == stove)  stove = null;
	}

	private void scanScene()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		Tile[][][] tiles = client.getScene().getTiles();
		for (Tile[][] plane : tiles)
		{
			for (Tile[] col : plane)
			{
				for (Tile tile : col)
				{
					if (tile == null)
					{
						continue;
					}
					for (GameObject obj : tile.getGameObjects())
					{
						trackObject(obj);
					}
					trackObject(tile.getWallObject());
					trackObject(tile.getDecorativeObject());
					trackObject(tile.getGroundObject());
				}
			}
		}
	}

	private void clearTrackedObjects()
	{
		sink = null;
		cupboard = null;
		stove = null;
	}
}
