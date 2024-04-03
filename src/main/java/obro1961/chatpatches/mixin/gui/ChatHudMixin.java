package obro1961.chatpatches.mixin.gui;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.util.CommandHistoryManager;
import net.minecraft.text.Text;
import obro1961.chatpatches.accessor.ChatHudAccessor;
import obro1961.chatpatches.api.ChatHudPatches;
import obro1961.chatpatches.config.Config;
import obro1961.chatpatches.impl.ChatHudPatchesImpl;
import obro1961.chatpatches.util.ChatUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * The main entrypoint mixin for most chat modifications.
 * Implements {@link ChatHudAccessor} to widen access to
 * extra fields and methods used elsewhere.
 */
@Environment(EnvType.CLIENT)
@Mixin(value = ChatHud.class, priority = 500)
public abstract class ChatHudMixin implements ChatHudAccessor {
    @Unique private ChatHudPatches patcher;

    @Shadow @Final private List<ChatHudLine> messages;
    @Shadow @Final private List<ChatHudLine.Visible> visibleMessages;
    @Shadow @Final private List<?> removalQueue;
    @Shadow private int scrolledLines;


    @Shadow protected abstract double toChatLineX(double x);
    @Shadow protected abstract double toChatLineY(double y);
    @Shadow protected abstract int getLineHeight();
    @Shadow protected abstract int getMessageLineIndex(double x, double y);
    // ChatHudAccessor methods used outside this mixin
    @Unique public List<ChatHudLine> getMessages() { return messages; }
    @Unique public List<ChatHudLine.Visible> getVisibleMessages() { return visibleMessages; }
    @Unique public List<?> getRemovalQueue() { return removalQueue; }
    @Unique public int getScrolledLines() { return scrolledLines; }
    @Unique public int chatpatches$getMessageLineIndex(double x, double y) { return getMessageLineIndex(x, y); }
    @Unique public double chatpatches$toChatLineX(double x) { return toChatLineX(x); }
    @Unique public double chatpatches$toChatLineY(double y) { return toChatLineY(y); }
    @Unique public int chatpatches$getLineHeight() { return getLineHeight(); }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void loadPatches(MinecraftClient client, CallbackInfo ci) {
        patcher = new ChatHudPatchesImpl((ChatHud) (Object) this);
    }


    /** Prevents the game from actually clearing chat history */
    @Inject(method = "clear", at = @At("HEAD"), cancellable = true)
    private void clear(boolean clearHistory, CallbackInfo ci) {
        patcher.clearHud(clearHistory, ci);
    }

    @ModifyExpressionValue(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V",
        at = @At(value = "CONSTANT", args = "intValue=100")
    )
    private int moreMessages(int hundred) {
        return patcher.getMaxMessageAmount(hundred);
    }

    /** Allows for a chat width larger than 320px */
    @ModifyReturnValue(method = "getWidth()I", at = @At("RETURN"))
    private int moreWidth(int defaultWidth) {
        return patcher.getChatHudWidth(defaultWidth);
    }

    /**
     * These methods shift most of the chat hud by
     * {@link Config#shiftChat}, including the text
     * and scroll bar, by shifting the y position of the chat.
     *
     * @implNote Target: <br>{@code int m = MathHelper.floor((float)(l - 40) / f);}
     */
    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 7)
    private int moveChat(int m) {
        return patcher.getChatHudShiftNeg(m);
    }

    /**
     * Moves the message indicator and hover tooltip
     * by {@link Config#shiftChat} to correctly shift
     * the chat with the other components.
     * Targets two methods because the first part of both
     * methods are identical.
     */
    @ModifyVariable(method = {"getIndicatorAt", "getTextStyleAt"}, argsOnly = true, at = @At("HEAD"), ordinal = 1)
    private double moveINDHoverText(double e) {
        return patcher.getChatHudShiftPos(e);
    }


    /**
     * Modifies the incoming message by adding timestamps, nicer
     * player names, hover events, and duplicate counters in conjunction with
     * {@link ChatHudPatches#addCounter(Text)}.
     * <br>
     * See {@link ChatUtils#modifyMessage(Text)} for detailed
     * implementation specifications.
     */
    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Text modifyMessage(Text m, @Local(argsOnly = true) boolean refreshing) {
        return patcher.modifyMessage(m, refreshing);
    }

    @Inject(method = "addToMessageHistory", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/collection/ArrayListDeque;size()I"))
    private void addHistory(String message, CallbackInfo ci) {
        patcher.addHistory(message, ci);
    }

    /** Disables logging commands to the vanilla command log if the Chat Patches' ChatLog is enabled. */
    @WrapWithCondition(method = "addToMessageHistory", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/CommandHistoryManager;add(Ljava/lang/String;)V"))
    private boolean disableCommandLog(CommandHistoryManager manager, String message) {
        return patcher.toggleCommandLog(manager, message);
    }

    @Inject(method = "logChatMessage", at = @At("HEAD"), cancellable = true)
    private void ignoreRestoredMessages(Text message, @Nullable MessageIndicator indicator, CallbackInfo ci) {
        patcher.ignoreRestoredMessages(message, indicator, ci);
    }
}