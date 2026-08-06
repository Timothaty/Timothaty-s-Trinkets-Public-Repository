package net.timothaty.timothatystrinkets.client.dialogue;

import com.mojang.blaze3d.systems.RenderSystem;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.network.DialogueHudMessage;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class DialogueHudClient {
	private static final int CURRENT_MAX_WIDTH = 340;
	private static final int CURRENT_PADDING_X = 14;
	private static final int CURRENT_PADDING_Y = 8;
	private static final int PREVIOUS_MAX_WIDTH = 280;
	private static final int PREVIOUS_PADDING_X = 10;
	private static final int PREVIOUS_PADDING_Y = 6;
	private static final int HISTORY_LIMIT = 2;
	private static final int HISTORY_SLIDE_TICKS = 10;
	private static final int HISTORY_LIFETIME_TICKS = 140;
	private static final int CURRENT_FADE_TICKS = 24;
	private static final int CURRENT_BACKGROUND = 0xC00A070B;
	private static final int PREVIOUS_BACKGROUND = 0x960A070B;
	private static final int SPEAKER_COLOR = 0xFFFF1E1E;
	private static final int CURRENT_TEXT_COLOR = 0xFFFFFFFF;
	private static final int PREVIOUS_TEXT_COLOR = 0xFFE6E6E6;
	private static final Random RANDOM = new Random();

	private static final Deque<QueuedLine> queuedLines = new ArrayDeque<>();
	private static final Deque<RenderedLine> previousLines = new ArrayDeque<>();
	private static ActiveLine activeLine;

	private DialogueHudClient() {
	}

	public static void play(DialogueHudMessage message) {
		if (message.lineKeys().isEmpty())
			return;

		if (activeLine != null && !activeLine.visibleText.isEmpty())
			pushPrevious(activeLine.toRenderedLine());

		queuedLines.clear();
		for (String lineKey : message.lineKeys()) {
			String speakerName = Component.translatable(message.speakerKey()).getString();
			String text = Component.translatable(lineKey).getString();
			queuedLines.add(new QueuedLine(message.speakerKey(), speakerName, text, message.soundId(), message.letterDelayTicks(), message.lineHoldTicks(), message.minPitch(), message.maxPitch(), message.characterSounds()));
		}
		activeLine = null;
		startNextLine();
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		tickPreviousLines();
		if (activeLine == null) {
			startNextLine();
			return;
		}

		activeLine.age++;
		if (activeLine.isFadingOut()) {
			activeLine.fadeTicksLeft--;
			if (activeLine.fadeTicksLeft <= 0)
				activeLine = null;
			return;
		}
		if (activeLine.jitterTicks > 0)
			activeLine.jitterTicks--;

		if (activeLine.pauseTicks > 0) {
			activeLine.pauseTicks--;
			return;
		}

		if (activeLine.isFinished()) {
			if (activeLine.finishedVisibleTicks < activeLine.lineHoldTicks) {
				activeLine.finishedVisibleTicks++;
				return;
			}

			if (queuedLines.isEmpty()) {
				activeLine.startFadeOut();
			} else {
				pushPrevious(activeLine.toRenderedLine());
				activeLine = null;
				startNextLine();
			}
			return;
		}

		if (activeLine.letterDelayLeft > 0) {
			activeLine.letterDelayLeft--;
			return;
		}

		revealNextToken();
	}

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui)
			return;
		if (activeLine == null && previousLines.isEmpty())
			return;

		GuiGraphics gui = event.getGuiGraphics();
		Font font = minecraft.font;
		int screenWidth = minecraft.getWindow().getGuiScaledWidth();
		int screenHeight = minecraft.getWindow().getGuiScaledHeight();
		int currentY = screenHeight - 112;
		int currentHeight = 0;

		RenderSystem.enableBlend();
		if (activeLine != null) {
			currentHeight = renderCurrentLine(gui, font, screenWidth, currentY, activeLine);
		}
		renderPreviousLines(gui, font, screenWidth, currentY, currentHeight);
		RenderSystem.disableBlend();
	}

	private static int renderCurrentLine(GuiGraphics gui, Font font, int screenWidth, int y, ActiveLine line) {
		int textMaxWidth = Math.min(CURRENT_MAX_WIDTH, Math.max(120, screenWidth - 48));
		List<String> wrappedText = wrapText(font, line.visibleText.toString(), textMaxWidth);
		int speakerWidth = font.width(line.speakerName);
		int textWidth = maxLineWidth(font, wrappedText);
		int panelWidth = Math.max(150, Math.max(speakerWidth, textWidth) + CURRENT_PADDING_X * 2);
		int panelHeight = CURRENT_PADDING_Y * 2 + 10 + wrappedText.size() * 10;
		int x = screenWidth / 2 - panelWidth / 2;

		if (line.jitterTicks > 0) {
			int seed = line.age * 31 + line.jitterTicks * 17;
			x += seed % 5 - 2;
			y += (seed / 5) % 5 - 2;
		}

		float alpha = line.getFadeAlpha();
		gui.fill(x, y, x + panelWidth, y + panelHeight, applyAlpha(CURRENT_BACKGROUND, alpha));
		gui.drawString(font, line.speakerName, screenWidth / 2 - speakerWidth / 2, y + CURRENT_PADDING_Y, applyAlpha(SPEAKER_COLOR, alpha), true);
		int textY = y + CURRENT_PADDING_Y + 16;
		for (String wrappedLine : wrappedText) {
			gui.drawString(font, wrappedLine, screenWidth / 2 - font.width(wrappedLine) / 2, textY, applyAlpha(CURRENT_TEXT_COLOR, alpha), true);
			textY += 10;
		}
		return panelHeight;
	}

	private static void renderPreviousLines(GuiGraphics gui, Font font, int screenWidth, int currentY, int currentHeight) {
		if (previousLines.isEmpty())
			return;

		int index = 0;
		int baseY = activeLine == null ? currentY : currentY + currentHeight + 10;
		for (RenderedLine line : previousLines) {
			boolean hideSpeaker = activeLine != null && activeLine.speakerKey.equals(line.speakerKey);
			int lineHeight = renderPreviousLine(gui, font, screenWidth, baseY, index, line, hideSpeaker);
			baseY += lineHeight + 7;
			index++;
		}
	}

	private static int renderPreviousLine(GuiGraphics gui, Font font, int screenWidth, int baseY, int index, RenderedLine line, boolean hideSpeaker) {
		float scale = 0.78F;
		int textMaxWidth = Math.min(PREVIOUS_MAX_WIDTH, Math.max(100, screenWidth - 60));
		List<String> wrappedText = wrapText(font, line.text, textMaxWidth);
		int speakerWidth = hideSpeaker ? 0 : font.width(line.speakerName);
		int textWidth = maxLineWidth(font, wrappedText);
		int panelWidth = Math.max(110, Math.max(speakerWidth, textWidth) + PREVIOUS_PADDING_X * 2);
		int panelHeight = PREVIOUS_PADDING_Y * 2 + wrappedText.size() * 10 + (hideSpeaker ? 0 : 12);
		float slide = smooth01(line.age / (float) HISTORY_SLIDE_TICKS);
		float alpha = 1.0F - smooth01(Math.max(0, line.age - HISTORY_SLIDE_TICKS) / (float) Math.max(1, HISTORY_LIFETIME_TICKS - HISTORY_SLIDE_TICKS));
		int y = baseY - Math.round((1.0F - slide) * 24.0F);
		int x = Math.round(screenWidth / 2.0F - panelWidth * scale / 2.0F);

		gui.pose().pushPose();
		gui.pose().translate(x, y, 0.0F);
		gui.pose().scale(scale, scale, 1.0F);
		gui.fill(0, 0, panelWidth, panelHeight, applyAlpha(PREVIOUS_BACKGROUND, alpha));
		int textY = PREVIOUS_PADDING_Y;
		if (!hideSpeaker) {
			gui.drawString(font, line.speakerName, panelWidth / 2 - font.width(line.speakerName) / 2, textY, applyAlpha(SPEAKER_COLOR, alpha), true);
			textY += 12;
		}
		for (String wrappedLine : wrappedText) {
			gui.drawString(font, wrappedLine, panelWidth / 2 - font.width(wrappedLine) / 2, textY, applyAlpha(PREVIOUS_TEXT_COLOR, alpha), true);
			textY += 10;
		}
		gui.pose().popPose();
		return Math.round(panelHeight * scale);
	}

	private static void revealNextToken() {
		if (activeLine == null)
			return;

		while (!activeLine.isFinished()) {
			TextToken token = activeLine.tokens.get(activeLine.tokenIndex++);
			if (token.type == TextToken.PAUSE) {
				activeLine.pauseTicks = token.ticks;
				return;
			}
			if (token.type == TextToken.JITTER) {
				activeLine.jitterTicks = Math.max(activeLine.jitterTicks, token.ticks);
				continue;
			}

			activeLine.visibleText.append(token.character);
			if (activeLine.characterSounds)
				playLetterSound(activeLine, token.character);
			activeLine.letterDelayLeft = Math.max(0, activeLine.letterDelayTicks - 1);
			return;
		}
	}

	private static void startNextLine() {
		if (activeLine != null || queuedLines.isEmpty())
			return;

		activeLine = new ActiveLine(queuedLines.removeFirst());
	}

	private static void pushPrevious(RenderedLine line) {
		previousLines.addFirst(line);
		while (previousLines.size() > HISTORY_LIMIT)
			previousLines.removeLast();
	}

	private static void tickPreviousLines() {
		previousLines.removeIf(line -> ++line.age > HISTORY_LIFETIME_TICKS);
	}

	private static void playLetterSound(ActiveLine line, char character) {
		if (!Character.isLetterOrDigit(character))
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null)
			return;

		ResourceLocation soundId;
		try {
			soundId = ResourceLocation.parse(line.soundId);
		} catch (Exception ignored) {
			return;
		}

		SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundId);
		if (soundEvent == null)
			return;

		float pitchRange = Math.max(0.0F, line.maxPitch - line.minPitch);
		float pitch = line.minPitch + RANDOM.nextFloat() * pitchRange;
		minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(), soundEvent, SoundSource.VOICE, 0.72F, pitch, false);
	}

	private static List<TextToken> parseText(String text) {
		List<TextToken> tokens = new ArrayList<>();
		int index = 0;
		while (index < text.length()) {
			char character = text.charAt(index);
			if (character == '<') {
				int end = text.indexOf('>', index + 1);
				if (end > index) {
					TextToken token = parseTag(text.substring(index + 1, end));
					if (token != null) {
						tokens.add(token);
						index = end + 1;
						continue;
					}
				}
			} else if (character == '[') {
				int end = text.indexOf(']', index + 1);
				if (end > index) {
					TextToken token = parseTag(text.substring(index + 1, end));
					if (token != null) {
						tokens.add(token);
						index = end + 1;
						continue;
					}
				}
			}

			tokens.add(TextToken.character(character));
			index++;
		}
		return tokens;
	}

	private static TextToken parseTag(String rawTag) {
		String tag = rawTag.trim().toLowerCase();
		if (tag.startsWith("pause:") || tag.startsWith("wait:"))
			return TextToken.pause(parseTicks(tag.substring(tag.indexOf(':') + 1), 0));
		if (tag.startsWith("pause=") || tag.startsWith("wait="))
			return TextToken.pause(parseTicks(tag.substring(tag.indexOf('=') + 1), 0));
		if (tag.startsWith("jitter:") || tag.startsWith("shake:"))
			return TextToken.jitter(parseTicks(tag.substring(tag.indexOf(':') + 1), 12));
		if (tag.startsWith("jitter=") || tag.startsWith("shake="))
			return TextToken.jitter(parseTicks(tag.substring(tag.indexOf('=') + 1), 12));
		return null;
	}

	private static int parseTicks(String value, int fallback) {
		String trimmed = value.trim();
		try {
			if (trimmed.endsWith("s")) {
				float seconds = Float.parseFloat(trimmed.substring(0, trimmed.length() - 1));
				return Math.max(0, Math.round(seconds * 20.0F));
			}
			return Math.max(0, Integer.parseInt(trimmed));
		} catch (NumberFormatException ignored) {
			return fallback;
		}
	}

	private static List<String> wrapText(Font font, String text, int maxWidth) {
		List<String> lines = new ArrayList<>();
		if (text.isEmpty()) {
			lines.add("");
			return lines;
		}

		for (String paragraph : text.split("\\n", -1)) {
			StringBuilder currentLine = new StringBuilder();
			String[] words = paragraph.split(" ");
			for (String word : words) {
				String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
				if (font.width(candidate) <= maxWidth || currentLine.isEmpty()) {
					currentLine.setLength(0);
					currentLine.append(candidate);
				} else {
					lines.add(currentLine.toString());
					currentLine.setLength(0);
					currentLine.append(word);
				}
			}
			lines.add(currentLine.toString());
		}
		return lines;
	}

	private static int maxLineWidth(Font font, List<String> lines) {
		int width = 0;
		for (String line : lines)
			width = Math.max(width, font.width(line));
		return width;
	}

	private static float smooth01(float value) {
		float t = Mth.clamp(value, 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}

	private static int applyAlpha(int color, float alpha) {
		int baseAlpha = color >>> 24;
		int adjustedAlpha = Mth.clamp(Math.round(baseAlpha * Mth.clamp(alpha, 0.0F, 1.0F)), 0, 255);
		return (color & 0x00FFFFFF) | (adjustedAlpha << 24);
	}

	private record QueuedLine(String speakerKey, String speakerName, String text, String soundId, int letterDelayTicks, int lineHoldTicks, float minPitch, float maxPitch, boolean characterSounds) {
	}

	private static final class ActiveLine {
		private final String speakerKey;
		private final String speakerName;
		private final String soundId;
		private final int letterDelayTicks;
		private final int lineHoldTicks;
		private final float minPitch;
		private final float maxPitch;
		private final boolean characterSounds;
		private final List<TextToken> tokens;
		private final StringBuilder visibleText = new StringBuilder();
		private int tokenIndex;
		private int pauseTicks;
		private int jitterTicks;
		private int letterDelayLeft;
		private int finishedVisibleTicks;
		private int fadeTicksLeft = -1;
		private int age;

		private ActiveLine(QueuedLine line) {
			this.speakerKey = line.speakerKey();
			this.speakerName = line.speakerName();
			this.soundId = line.soundId();
			this.letterDelayTicks = Math.max(1, line.letterDelayTicks());
			this.lineHoldTicks = Math.max(0, line.lineHoldTicks());
			this.minPitch = line.minPitch();
			this.maxPitch = line.maxPitch();
			this.characterSounds = line.characterSounds();
			this.tokens = parseText(line.text());
		}

		private boolean isFinished() {
			return tokenIndex >= tokens.size();
		}

		private void startFadeOut() {
			fadeTicksLeft = CURRENT_FADE_TICKS;
		}

		private boolean isFadingOut() {
			return fadeTicksLeft >= 0;
		}

		private float getFadeAlpha() {
			if (!isFadingOut())
				return 1.0F;
			return smooth01(fadeTicksLeft / (float) CURRENT_FADE_TICKS);
		}

		private RenderedLine toRenderedLine() {
			return new RenderedLine(speakerKey, speakerName, visibleText.toString());
		}
	}

	private static final class RenderedLine {
		private final String speakerKey;
		private final String speakerName;
		private final String text;
		private int age;

		private RenderedLine(String speakerKey, String speakerName, String text) {
			this.speakerKey = speakerKey;
			this.speakerName = speakerName;
			this.text = text;
		}
	}

	private static final class TextToken {
		private static final int CHARACTER = 0;
		private static final int PAUSE = 1;
		private static final int JITTER = 2;

		private final int type;
		private final char character;
		private final int ticks;

		private TextToken(int type, char character, int ticks) {
			this.type = type;
			this.character = character;
			this.ticks = ticks;
		}

		private static TextToken character(char character) {
			return new TextToken(CHARACTER, character, 0);
		}

		private static TextToken pause(int ticks) {
			return new TextToken(PAUSE, '\0', ticks);
		}

		private static TextToken jitter(int ticks) {
			return new TextToken(JITTER, '\0', ticks);
		}
	}
}
