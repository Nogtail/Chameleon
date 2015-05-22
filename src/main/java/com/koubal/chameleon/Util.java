package com.koubal.chameleon;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class Util {
	private static Object sessionServiceObject;
	private static Method fillProfilePropertiesMethod;

	private static Method getHandleMethod;
	private static Field pingField;

	private static Object playerListObject;
	private static Method moveToWorldMethod;

	private Util() {
	}

	protected static WrappedGameProfile fillProfileProperties(WrappedGameProfile profile) throws ReflectiveOperationException {
		if (fillProfilePropertiesMethod == null) {
			Server server = Bukkit.getServer();
			Object minecraftServerObject = server.getClass().getDeclaredMethod("getServer").invoke(server);

			for (Method method : minecraftServerObject.getClass().getMethods()) {
				if (method.getReturnType().getSimpleName().equals("MinecraftSessionService")) {
					sessionServiceObject = method.invoke(minecraftServerObject);
					break;
				}
			}

			for (Method method : sessionServiceObject.getClass().getMethods()) {
				if (method.getName().equals("fillProfileProperties")) {
					fillProfilePropertiesMethod = method;
					break;
				}
			}
		}

		return WrappedGameProfile.fromHandle(fillProfilePropertiesMethod.invoke(sessionServiceObject, profile.getHandle(), true));
	}

	protected static void sendPlayerInfoPacket(Player player) throws ReflectiveOperationException {
		WrappedGameProfile profile = WrappedGameProfile.fromPlayer(player);
		Object entityPlayer;

		if (pingField == null) {
			if (getHandleMethod == null) {
				initialiseGetHandleMethod(player);
			}

			entityPlayer = getHandleMethod.invoke(player);
			pingField = entityPlayer.getClass().getField("ping");
		} else {
			entityPlayer = getHandleMethod.invoke(player);
		}

		int ping = (Integer) pingField.get(entityPlayer);
		EnumWrappers.NativeGameMode gameMode = EnumWrappers.NativeGameMode.fromBukkit(player.getGameMode());
		WrappedChatComponent displayName = WrappedChatComponent.fromText(player.getPlayerListName());

		PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
		packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
		packet.getPlayerInfoDataLists().write(0, Arrays.asList(new PlayerInfoData(profile, ping, gameMode, displayName)));

		Chameleon.getInstance().getProtocolManager().sendServerPacket(player, packet);
	}

	protected static void reloadSkin(Player player) throws ReflectiveOperationException {
		if (moveToWorldMethod == null) {
			Server server = Bukkit.getServer();
			Object minecraftServerObject = server.getClass().getMethod("getServer").invoke(server);
			playerListObject = minecraftServerObject.getClass().getMethod("getPlayerList").invoke(minecraftServerObject);

			for (Method method : playerListObject.getClass().getMethods()) {
				if (method.getName().equals("moveToWorld") && method.getParameterTypes().length == 5) {
					moveToWorldMethod = method;
					break;
				}
			}
		}

		if (getHandleMethod == null) {
			initialiseGetHandleMethod(player);
		}

		Object entityHumanObject = getHandleMethod.invoke(player);
		moveToWorldMethod.invoke(playerListObject, entityHumanObject, 0, true, player.getLocation(), false);
	}

	private static void initialiseGetHandleMethod(Player player) throws NoSuchMethodException {
		getHandleMethod = player.getClass().getMethod("getHandle");
	}
}
