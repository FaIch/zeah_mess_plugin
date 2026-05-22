package com.zeahMess;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.Text;

class ZeahMessOverlay extends Overlay
{
	private static final Color CYAN    = Color.CYAN;
	private static final Color GREEN   = Color.GREEN;
	private static final Color PURPLE  = Color.MAGENTA;

	private static final Map<Integer, Color> ITEM_COLORS = Map.of(
		ItemID.HOSIDIUS_SERVERY_PINEAPPLE_CHUNKS, GREEN,
		ItemID.KNIFE, PURPLE
	);

	private final ZeahMessPlugin plugin;
	private final Client client;

	@Inject
	ZeahMessOverlay(ZeahMessPlugin plugin, Client client)
	{
		this.plugin = plugin;
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		switch (plugin.getState())
		{
			case FILL_BOWLS:
				highlightObject(graphics, plugin.getSink());
				break;
			case GET_FLOUR:
			case GET_TOMATO:
			case GET_CHEESE:
			case GET_PINEAPPLE:
				highlightObject(graphics, plugin.getCupboard());
				highlightServeryIngredient(graphics, plugin.serveryItemForState());
				break;
			case COOK:
				highlightObject(graphics, plugin.getStove());
				break;
			case CUT_PINEAPPLE:
				highlightInventoryItems(graphics, ItemID.KNIFE, ItemID.HOSIDIUS_SERVERY_PINEAPPLE);
				break;
			case COMBINE_PIZZA:
				highlightInventoryItems(graphics, ItemID.HOSIDIUS_SERVERY_PLAIN_PIZZA, ItemID.HOSIDIUS_SERVERY_PINEAPPLE_CHUNKS);
				break;
			default:
				break;
		}
		return null;
	}

	private void highlightObject(Graphics2D graphics, TileObject obj)
	{
		if (obj == null)
		{
			return;
		}
		Shape hull;
		if (obj instanceof WallObject)
		{
			hull = ((WallObject) obj).getConvexHull();
		}
		else if (obj instanceof GameObject)
		{
			hull = ((GameObject) obj).getConvexHull();
		}
		else if (obj instanceof DecorativeObject)
		{
			hull = ((DecorativeObject) obj).getConvexHull();
		}
		else if (obj instanceof GroundObject)
		{
			hull = ((GroundObject) obj).getConvexHull();
		}
		else
		{
			hull = null;
		}
		if (hull != null)
		{
			OverlayUtil.renderPolygon(graphics, hull, CYAN);
		}
	}

	private void highlightServeryIngredient(Graphics2D graphics, int targetItemId)
	{
		if (targetItemId == -1)
		{
			return;
		}
		for (int i = 0; i < 30; i++)
		{
			Widget w = client.getWidget(InterfaceID.HOSIDIUS_SERVERY, i);
			if (w == null || w.isHidden())
			{
				continue;
			}
			searchAndHighlight(graphics, w, targetItemId);
		}
	}

	private void searchAndHighlight(Graphics2D graphics, Widget widget, int targetItemId)
	{
		if (matchesIngredient(widget, targetItemId))
		{
			drawWidgetHighlight(graphics, widget.getBounds(), CYAN);
			return;
		}

		Widget[] children = widget.getChildren();
		if (children != null)
		{
			for (Widget child : children)
			{
				searchAndHighlight(graphics, child, targetItemId);
			}
		}

		Widget[] dynChildren = widget.getDynamicChildren();
		if (dynChildren != null)
		{
			for (Widget child : dynChildren)
			{
				searchAndHighlight(graphics, child, targetItemId);
			}
		}
	}

	private boolean matchesIngredient(Widget widget, int targetItemId)
	{
		if (widget.getItemId() == targetItemId)
		{
			return true;
		}
		String name = Text.removeTags(widget.getName()).toLowerCase();
		String text = Text.removeTags(widget.getText()).toLowerCase();
		String keyword = keywordForItemId(targetItemId);
		return keyword != null && (name.contains(keyword) || text.contains(keyword));
	}

	private String keywordForItemId(int itemId)
	{
		if (itemId == ItemID.HOSIDIUS_SERVERY_POT_FLOUR)  return "flour";
		if (itemId == ItemID.HOSIDIUS_SERVERY_TOMATO)     return "tomato";
		if (itemId == ItemID.HOSIDIUS_SERVERY_CHEESE)     return "cheese";
		if (itemId == ItemID.HOSIDIUS_SERVERY_PINEAPPLE)  return "pineapple";
		return null;
	}

	private void highlightInventoryItems(Graphics2D graphics, int... targetItemIds)
	{
		Widget inv = client.getWidget(InterfaceID.INVENTORY, 0);
		if (inv == null || inv.isHidden())
		{
			return;
		}
		Widget[] slots = inv.getDynamicChildren();
		if (slots == null)
		{
			return;
		}
		for (Widget slot : slots)
		{
			for (int targetId : targetItemIds)
			{
				if (slot.getItemId() == targetId)
				{
					Color color = ITEM_COLORS.getOrDefault(targetId, CYAN);
					drawWidgetHighlight(graphics, slot.getBounds(), color);
					break;
				}
			}
		}
	}

	private void drawWidgetHighlight(Graphics2D graphics, Rectangle bounds, Color color)
	{
		if (bounds == null || bounds.isEmpty())
		{
			return;
		}
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
		graphics.fill(bounds);
		graphics.setColor(color);
		graphics.draw(bounds);
	}
}
