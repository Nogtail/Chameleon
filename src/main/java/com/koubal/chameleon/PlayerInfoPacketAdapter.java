package com.koubal.chameleon;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.google.common.collect.Multimap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayerInfoPacketAdapter extends PacketAdapter {
	private final Chameleon chameleon;

	public PlayerInfoPacketAdapter() {
		super(Chameleon.getInstance(), PacketType.Play.Server.PLAYER_INFO);

		chameleon = Chameleon.getInstance();
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		PacketContainer packet = event.getPacket();

		if (packet.getPlayerInfoAction().read(0) != EnumWrappers.PlayerInfoAction.ADD_PLAYER) {
			return;
		}

		Player viewer = event.getPlayer();
		List<PlayerInfoData> playerInfoDataList = new ArrayList<PlayerInfoData>();

		for (PlayerInfoData playerInfoData : packet.getPlayerInfoDataLists().read(0)) {
			WrappedGameProfile profile = playerInfoData.getProfile();
			Player target = Bukkit.getPlayer(profile.getUUID());

			AsyncPlayerTagEvent tagEvent = new AsyncPlayerTagEvent(viewer, target);
			Bukkit.getPluginManager().callEvent(tagEvent);

			String tag = tagEvent.getTag();
			String texturesName = tagEvent.getTextures();

			if (tag == null && texturesName == null) {
				playerInfoDataList.add(playerInfoData);
				continue;
			}

			if (tag == null) {
				tag = profile.getName();
			}

			Collection<WrappedSignedProperty> textures = chameleon.getTextures(texturesName);

			profile = profile.withName(tag);

			Multimap<String, WrappedSignedProperty> properties = profile.getProperties();
			properties.removeAll(Chameleon.TEXTURES_KEY);
			properties.putAll(Chameleon.TEXTURES_KEY, textures);

			playerInfoDataList.add(new PlayerInfoData(profile, playerInfoData.getPing(), playerInfoData.getGameMode(), playerInfoData.getDisplayName()));
		}

		packet.getPlayerInfoDataLists().write(0, playerInfoDataList);
	}
}
