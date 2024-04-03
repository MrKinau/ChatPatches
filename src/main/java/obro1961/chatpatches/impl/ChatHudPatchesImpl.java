package obro1961.chatpatches.impl;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.util.CommandHistoryManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import obro1961.chatpatches.ChatPatches;
import obro1961.chatpatches.accessor.ChatHudAccessor;
import obro1961.chatpatches.api.ChatHudPatches;
import obro1961.chatpatches.chatlog.ChatLog;
import obro1961.chatpatches.config.Config;
import obro1961.chatpatches.mixin.gui.ChatHudMixin;
import obro1961.chatpatches.util.ChatUtils;
import obro1961.chatpatches.util.Flags;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static obro1961.chatpatches.ChatPatches.config;
import static obro1961.chatpatches.util.ChatUtils.MESSAGE_INDEX;
import static obro1961.chatpatches.util.ChatUtils.getPart;

/**
 * Patch implementations for the {@link ChatHud}.
 * Javadocs contain detailed implementation specs,
 * whereas the {@link ChatHudPatches} interface
 * contains vague intentions of the patches.
 */
public class ChatHudPatchesImpl implements ChatHudPatches {
	private final ChatHud ths;
	private final List<ChatHudLine> messages; // Shadow

	public ChatHudPatchesImpl(ChatHud ths) {
		this.ths = ths;
		this.messages = access().getMessages();
	}

	@NotNull
	public ChatHud me() {
		return ths;
	}

	@NotNull
	public ChatHudAccessor access() {
		return ChatHudAccessor.from(me());
	}


	/**
	 * Clears the entire chat of all messages and history.
	 * Vanilla triggers this method when leaving a world or disconnecting
	 * (clearHistory = false), or when pressing F3+D (clearHistory = true).
	 * In either case, however, the method clears the entire visible chat,
	 * which should only be allowed if {@link Config#vanillaClearing} is true.
	 *
	 * @implNote Since Minecraft 1.20.2, the vanilla method is also called
	 * in between switching worlds (client reconfigure phase), so it also
	 * needs to prevent that as well, unconditionally. todo impl this, see #147
	 *
	 * @see ChatHudMixin#clear(boolean, CallbackInfo)
	 */
	@Override
	public void clearHud(boolean clearHistory, CallbackInfo ci) {
		if(!config.vanillaClearing) {
			// Clear message using F3+D
			if(!clearHistory) {
				client.getMessageHandler().processAll();
				access().getRemovalQueue().clear();
				messages.clear();
				access().getVisibleMessages().clear();
				// empties the message cache (which on save clears chatlog.json)
				ChatLog.clearMessages();
				ChatLog.clearHistory();
			}

			ci.cancel();
		}
	}

	/**
	 * Increases the maximum amount of messages that can be displayed
	 * on the chat HUD. This method simply returns the configured amount,
	 * stored in {@link Config#chatMaxMessages}.
	 *
	 * @see ChatHudMixin#moreMessages(int)
	 */
	@Override
	public int getMaxMessageAmount(int defaultMax) {
		return config.chatMaxMessages;
	}

	/**
	 * Increases the maximum width of the chat HUD. This method returns the
	 * configured with stored in {@link Config#chatWidth} only if it is set
	 * to a value greater than 0. Otherwise, it returns the default width.
	 *
	 * @see ChatHudMixin#moreWidth(int)
	 */
	@Override
	public int getChatHudWidth(int defaultWidth) {
		return config.chatWidth > 0 ? config.chatWidth : defaultWidth;
	}

	/**
	 * Shifts the chat HUD to the configured amount stored in
	 * {@link Config#shiftChat}. This method accomplishes this by adding
	 * the configured amount to the old shift value, scaled appropriately.
	 *
	 * @see #getChatHudShiftNeg(int) The inverse of this method, #getChatHudShiftNeg(int)
	 * @see ChatHudMixin#moveINDHoverText(double)
	 */
	@Override
	public double getChatHudShiftPos(double oldShift) {
		return oldShift + (config.shiftChat * me().getChatScale());
	}

	/**
	 * Shifts the chat HUD to the configured amount stored in
	 * {@link Config#shiftChat}. This method accomplishes this by subtracting
	 * the configured amount to the old shift value, scaled appropriately.
	 *
	 * @see #getChatHudShiftPos(double) The inverse of this method, #getChatHudShiftPos(double)
	 * @see ChatHudMixin#moveChat(int)
	 */
	@Override
	public int getChatHudShiftNeg(int oldShift) {
		return oldShift - MathHelper.floor(config.shiftChat / me().getChatScale());
	}

	/**
	 * Modifies the incoming message, restructures it, and adds configured information.
	 * Implementation currently is written in {@link ChatUtils}, todo move the code here
	 * and update/copy the javadocs here too. also i need to merge it with #addCounter
	 *
	 * @see ChatUtils#modifyMessage(Text)
	 * @see ChatHudMixin#modifyMessage(Text, boolean)
	 */
	@Override
	public Text modifyMessage(Text original, boolean refreshingHud) {
		return refreshingHud ? original : addCounter(ChatUtils.modifyMessage(original));
	}

	/**
	 * Adds a previously sent message to the {@link ChatLog}
	 * if it wasn't already added, and if the ChatLog isn't loading. This method is called
	 * when a message is sent.
	 *
	 * @see ChatHudMixin#addHistory(String, CallbackInfo)
	 */
	@Override
	public void addHistory(String message, CallbackInfo ci) {
		if(!Flags.LOADING_CHATLOG.isRaised())
			ChatLog.addHistory(message);
	}

	/**
	 * Disables the vanilla command log, which is a feature that logs up to 50 commands,
	 * if the ChatLog is already enabled as specified by {@link Config#chatlog}.
	 *
	 * @see ChatHudMixin#disableCommandLog(CommandHistoryManager, String)
	 */
	@Override
	public boolean toggleCommandLog(CommandHistoryManager manager, String message) {
		return !config.chatlog;
	}

	/**
	 * Cancels logging chat messages if the ChatLog is loading and the indicator isn't null,
	 * meaning it's a restored message. This method is called before a message is logged.
	 *
	 * @see ChatHudMixin#ignoreRestoredMessages(Text, MessageIndicator, CallbackInfo)
	 */
	@Override
	public void ignoreRestoredMessages(Text message, MessageIndicator indicator, CallbackInfo ci) {
		if(Flags.LOADING_CHATLOG.isRaised() && indicator != null)
			ci.cancel();
	}

	/**
	 * Adds a counter to the chat message, indicating how many times the same
	 * message has been sent. Can check only the last message, or
	 * {@link Config#counterCompactDistance} times back. Slightly more
	 * efficient than the older method, although it's still is quite slow.
	 *
	 * @implNote
	 * <ol>
	 *     <li>IF {@code COUNTER} is enabled AND the message count >0 AND the message isn't a boundary line, continue.</li>
	 *     <li>Cache the result of trying to condense the incoming message with the last message received.</li>
	 *     <li>IF the counter should use the CompactChat method and the message wasn't already condensed:</li>
	 *     <ol>
	 *         <li>Calculate the adjusted distance to attempt comparing, depending on the amount of messages already in the chat.</li>
	 *         <li>Filter all the messages within the target range that are case-insensitively equal to the incoming message.</li>
	 *         <li>If a message was the same, call {@link ChatUtils#getCondensedMessage(Text, int)},
	 *         which ultimately removes that message and its visibles.</li>
	 *     </ol>
	 *     <li>Return the (potentially) condensed message, to later be formatted further in {@link #modifyMessage(Text, boolean)}</li>
	 * </ol>
	 * (Wraps the entire method in a try-catch to prevent any errors accidentally disabling the chat.)
	 *
	 * @apiNote This injector is pretty ugly and could definitely be cleaner and more concise, but I'm going to deal with it
	 * in the future when I API-ify the rest of the mod. When that happens, this flag-add-flag-cancel method will be replaced
	 * with a simple (enormous) method call alongside
	 * {@link #modifyMessage(Text, boolean)} in a @{@link ModifyVariable}
	 * handler. (NOTE: as of v202.6.0, this is partially done already thanks to #132)
	 * TODO use notes above to merge this into #modifyMessage(Text, boolean)
	 *
	 * @see ChatUtils#getCondensedMessage(Text, int)
	 */
	@Override
	public Text addCounter(Text incoming) {
		try {
			if( config.counter && !messages.isEmpty() ) {
				// condenses the incoming message into the last message if it is the same
				Text condensedLastMessage = ChatUtils.getCondensedMessage(incoming, 0);

				// if the counterCompact option is true but the last message received was not condensed, look for
				// any dupes in the last counterCompactDistance messages and if any are found condense them
				if( config.counterCompact && condensedLastMessage.equals(incoming) ) {
					// ensures {0 <= attemptDistance <= messages.size()} is true
					int attemptDistance = MathHelper.clamp((
						(config.counterCompactDistance == -1)
							? messages.size()
							: (config.counterCompactDistance == 0)
								? me().getVisibleLineCount()
								: config.counterCompactDistance
					), 0, messages.size());

					// exclude the first message, already checked above
					messages.subList(1, attemptDistance)
						.stream()
						.filter( hudLine -> getPart(hudLine.content(), MESSAGE_INDEX).getString().equalsIgnoreCase( getPart(incoming, MESSAGE_INDEX).getString() ) )
						.findFirst()
						.ifPresent( hudLine -> ChatUtils.getCondensedMessage(incoming, messages.indexOf(hudLine)) );
				}

				// this result is used in #modifyMessage(...)
				return condensedLastMessage;
			}
		} catch(IndexOutOfBoundsException e) {
			ChatPatches.LOGGER.error("[ChatHudMixin.addCounter] Couldn't add duplicate counter because message '{}' ({} parts) was not constructed properly.", incoming.getString(), incoming.getSiblings().size());
			ChatPatches.LOGGER.error("[ChatHudMixin.addCounter] This could have also been caused by an issue with the new CompactChat dupe-condensing method. Either way,");
			ChatPatches.logInfoReportMessage(e);
		} catch(Exception e) {
			ChatPatches.LOGGER.error("[ChatHudMixin.addCounter] /!\\ Couldn't add duplicate counter because of an unexpected error! /!\\");
			ChatPatches.logInfoReportMessage(e);
		}

		return incoming;
	}
}