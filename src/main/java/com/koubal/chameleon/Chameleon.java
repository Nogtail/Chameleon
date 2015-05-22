package com.koubal.chameleon;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Chameleon extends JavaPlugin {
	protected static final String TEXTURES_KEY = "textures";

	private static Chameleon instance;
	private ProtocolManager protocolManager;
	private LoadingCache<String, Collection<WrappedSignedProperty>> cachedTextures;

	@Override
	public void onEnable() {
		instance = this;
		protocolManager = ProtocolLibrary.getProtocolManager();

		cachedTextures = CacheBuilder.newBuilder()
				.expireAfterWrite(1, TimeUnit.HOURS)
				.build(new CacheLoader<String, Collection<WrappedSignedProperty>>() {
					@Override
					public Collection<WrappedSignedProperty> load(String name) throws ReflectiveOperationException {
						WrappedGameProfile profile;
						Player player = Bukkit.getPlayer(name);

						if (player != null) {
							profile = WrappedGameProfile.fromPlayer(player);
						} else {
							OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
							profile = WrappedGameProfile.fromOfflinePlayer(offlinePlayer);
							profile = Util.fillProfileProperties(profile);
						}

						return profile.getProperties().get(TEXTURES_KEY);
					}
				});

		protocolManager.getAsynchronousManager().registerAsyncHandler(new PlayerInfoPacketAdapter()).start();

		protocolManager.getAsynchronousManager().registerAsyncHandler(new PacketAdapter(this, ListenerPriority.HIGH,
				PacketType.Play.Server.PLAYER_INFO,
				PacketType.Play.Server.SPAWN_POSITION,
				PacketType.Play.Server.RESPAWN,
				PacketType.Play.Server.POSITION,
				PacketType.Play.Server.HELD_ITEM_SLOT,
				PacketType.Play.Server.NAMED_ENTITY_SPAWN,
				PacketType.Play.Server.ENTITY_HEAD_ROTATION,
				PacketType.Play.Server.EXPERIENCE,
				PacketType.Play.Server.MAP_CHUNK,
				PacketType.Play.Server.MAP_CHUNK_BULK,
				PacketType.Play.Server.WINDOW_ITEMS,
				PacketType.Play.Server.UPDATE_SIGN,
				PacketType.Play.Server.TILE_ENTITY_DATA,
				PacketType.Play.Server.WORLD_BORDER) {
			@Override
			public void onPacketSending(PacketEvent event) {
				if (event.getPacket().getType() == PacketType.Play.Server.ENTITY_DESTROY) {
					event.setCancelled(true);
				}
			}
		}).syncStart();

		getLogger().info("Chameleon version " + getDescription().getVersion() + " enabled!");
	}

	@Override
	public void onDisable() {
		cachedTextures.invalidateAll();
		getLogger().info("Chameleon version " + getDescription().getVersion() + " disabled!");
	}

	public void updateViewers(Player target) {
		for (Player viewer : protocolManager.getEntityTrackers(target)) {
			viewer.hidePlayer(target);
			viewer.showPlayer(target);
		}
	}

	public void update(Player player) {
		try {
			Util.sendPlayerInfoPacket(player);
			Util.reloadSkin(player);
		} catch (ReflectiveOperationException exception) {
			getLogger().info("Unable to update player!");
		}
	}

	public boolean isCached(String name) {
		return cachedTextures.getIfPresent(name) != null;
	}

	public void loadTextures(String name) {
		getTextures(name);
	}

	protected Collection<WrappedSignedProperty> getTextures(String name) {
		try {
			return cachedTextures.get(name);
		} catch (ExecutionException exception) {
			getLogger().severe("Unable to load textures for " + name + "!");
			return null;
		}
	}

	public static Chameleon getInstance() {
		return instance;
	}

	protected ProtocolManager getProtocolManager() {
		return protocolManager;
	}
}
