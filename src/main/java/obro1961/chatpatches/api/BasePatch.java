package obro1961.chatpatches.api;

import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;

/**
 * The base interface for all API classes.
 * Used for classes that do NOT have an accessor. For
 * classes that need one, use {@link Accessor} instead.
 * <p><br>
 * The intended naming format for implementing classes
 * is {@code ClazzPatches} and {@code ClazzPatchesImpl}
 * for interface and class implementations, respectively.
 * The thought process is that the interface contains
 * signatures to share across versions, the class has
 * version-specific implementations, and the <i>mixin</i>
 * for {@code Clazz} applies the patches.
 * ({@link obro1961.chatpatches.ChatPatches} is the
 * only exception, being the mod entrypoint.)
 *
 * @param <Clazz> The class that the patch is being applied to.
 */
public interface BasePatch<Clazz> {
	MinecraftClient client = MinecraftClient.getInstance();

	/**
	 * Represents the class that the patch is being applied to.
	 * Should never return null.
	 *
	 * @apiNote This method is intended to be called by the
	 * patcher class itself, not by any other class.
	 */
	@NotNull
	Clazz me();


	/**
	 * Used for classes that DO have an accessor. See
	 * {@link BasePatch} for more details about both classes.
	 *
	 * @param <Clazz> The class that the patch is being applied to.
	 * @param <Accessor> The accessor for the class that the patch is being applied to.
	 */
	interface Accessor<Clazz, Accessor> extends BasePatch<Clazz> {
		/**
		 * Represents the accessor for the class that the patch is being applied to.
		 * If the class does not have an accessor, use {@link BasePatch} instead.
		 * Should never return null.
		 *
		 * @apiNote This method is intended to be called by the
		 * patcher class itself, not by any other class.
		 */
		Accessor access();
	}
}