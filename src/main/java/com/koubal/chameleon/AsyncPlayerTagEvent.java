package com.koubal.chameleon;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AsyncPlayerTagEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	private final Player viewer;
	private final Player target;
	private String tag;
	private String textures;

	public AsyncPlayerTagEvent(Player viewer, Player target) {
		this.viewer = viewer;
		this.target = target;
	}

	public Player getViewer() {
		return viewer;
	}

	public Player getTarget() {
		return target;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getTextures() {
		return textures;
	}

	public void setTextures(String textures) {
		this.textures = textures;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
