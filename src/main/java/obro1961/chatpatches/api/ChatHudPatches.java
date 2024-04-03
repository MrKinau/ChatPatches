package obro1961.chatpatches.api;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.util.CommandHistoryManager;
import net.minecraft.text.Text;
import obro1961.chatpatches.accessor.ChatHudAccessor;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public interface ChatHudPatches extends BasePatch.Accessor<ChatHud, ChatHudAccessor> {
	void clearHud(boolean clearHistory, CallbackInfo ci);

	int getMaxMessageAmount(int oldMax);

	int getChatHudWidth(int oldWidth);

	double getChatHudShiftPos(double oldShift);
	int getChatHudShiftNeg(int oldShift);

	Text modifyMessage(Text originalMessage, boolean refreshingHud);

	void addHistory(String message, CallbackInfo ci);

	boolean toggleCommandLog(CommandHistoryManager manager, String message);

	void ignoreRestoredMessages(Text message, MessageIndicator indicator, CallbackInfo ci);

	Text addCounter(Text originalMessage);
}