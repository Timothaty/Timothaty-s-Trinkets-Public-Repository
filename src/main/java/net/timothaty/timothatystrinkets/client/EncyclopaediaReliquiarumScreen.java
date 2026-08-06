package net.timothaty.timothatystrinkets.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class EncyclopaediaReliquiarumScreen extends Screen {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/screens/encyclopaedia_reliquiarum_ui.png");
	private static final ResourceLocation PAGE_FORWARD_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_forward_highlighted");
	private static final ResourceLocation PAGE_FORWARD_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_forward");
	private static final ResourceLocation PAGE_BACKWARD_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_backward_highlighted");
	private static final ResourceLocation PAGE_BACKWARD_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_backward");
	private static final ResourceLocation ENCYCLOPAEDIA_ICON = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/item/encyclopaedia_reliquiarum.png");
	private static final ResourceLocation DRAWING_TEXTURE = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/screens/drawing_256.png");
	private static final String CATALOGUE_TITLE_KEY = "guide.timothatys_trinkets.catalogue";
	private static final int TEXTURE_WIDTH = 512;
	private static final int TEXTURE_HEIGHT = 256;
	private static final int BOOK_WIDTH = 300;
	private static final int BOOK_HEIGHT = 184;
	private static final int PAGE_TOP = 10;
	private static final int PAGE_WIDTH = 130;
	private static final int PAGE_HEIGHT = 152;
	private static final int LEFT_PAGE_X = 10;
	private static final int RIGHT_PAGE_X = 158;
	private static final int LEFT_PAGE_CENTER_X = LEFT_PAGE_X + PAGE_WIDTH / 2;
	private static final int PAGE_BUTTON_WIDTH = 23;
	private static final int PAGE_BUTTON_HEIGHT = 13;
	private static final int PAGE_BUTTON_SIDE_PADDING = 4;
	private static final int PAGE_BUTTON_BOTTOM_PADDING = 4;
	private static final int PAGE_BUTTON_CONTENT_PADDING = 6;
	private static final int TITLE_Y = 39;
	private static final int SUBTITLE_Y = 58;
	private static final int ICON_Y = 79;
	private static final int TITLE_PAGE_COLOR = ChatFormatting.GOLD.getColor();
	private static final int DEFAULT_TEXT_COLOR = 0x2B2418;
	private static final int TITLE_ICON_SIZE = 32;
	private static final float TEXT_SCALE = 0.9F;
	private static final float TITLE_SCALE = 1.45F * TEXT_SCALE;
	private static final float SUBTITLE_SCALE = 1.08F * TEXT_SCALE;
	private static final int DRAWING_SOURCE_WIDTH = 1086;
	private static final int DRAWING_SOURCE_HEIGHT = 1448;
	private static final int MILLIS_PER_TICK = 50;
	private static final int ENTRY_ICON_SIZE = 18;
	private static final int ENTRY_ROW_GAP = 26;
	private static final int ENTRIES_PER_PAGE = 4;
	private static final int LIST_TITLE_Y = 4;
	private static final int LIST_ROW_START_Y = 28;
	private static final int ARTICLE_CONTENT_HEIGHT = PAGE_HEIGHT - PAGE_BUTTON_HEIGHT - PAGE_BUTTON_BOTTOM_PADDING - PAGE_BUTTON_CONTENT_PADDING;
	private static final int ARTICLE_TEXT_WIDTH = PAGE_WIDTH - 8;
	private static final int ARTICLE_TEXT_X = 4;
	private static final long OPEN_ANIMATION_MS = 120L;
	private static final long CLOSE_ANIMATION_MS = 95L;
	private static final float MIN_SCALE = 0.78F;

	private final EncyclopaediaReliquiarumData data = EncyclopaediaReliquiarumData.load();
	private final long openedAtMillis = Util.getMillis();
	private final List<ClickableText> clickables = new ArrayList<>();
	private BookView view = BookView.TITLE;
	private String selectedCategoryId;
	private String selectedArticleId;
	private int catalogueSpread;
	private int categorySpread;
	private int articleSpread;
	private boolean closing;
	private long closingAtMillis;

	public EncyclopaediaReliquiarumScreen() {
		super(Component.literal("Encyclopaedia Reliquiarum"));
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.clickables.clear();
		this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
		int x = (this.width - BOOK_WIDTH) / 2;
		int y = (this.height - BOOK_HEIGHT) / 2;
		float scale = this.getAnimationScale();

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(this.width / 2.0F, this.height / 2.0F, 0.0F);
		guiGraphics.pose().scale(scale, scale, 1.0F);
		guiGraphics.pose().translate(-this.width / 2.0F, -this.height / 2.0F, 0.0F);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		guiGraphics.blit(TEXTURE, x, y, BOOK_WIDTH, BOOK_HEIGHT, 0.0F, 0.0F, BOOK_WIDTH, BOOK_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
		switch (this.view) {
			case TITLE -> {
				this.renderTitlePage(guiGraphics, x, y);
				this.renderDrawingPage(guiGraphics, x, y);
			}
			case CATALOGUE -> this.renderCatalogue(guiGraphics, x, y, mouseX, mouseY);
			case CATEGORY -> this.renderCategory(guiGraphics, x, y, mouseX, mouseY);
			case ARTICLE -> this.renderArticle(guiGraphics, x, y);
		}
		this.renderPageButtons(guiGraphics, x, y, mouseX, mouseY);
		RenderSystem.disableBlend();
		guiGraphics.pose().popPose();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			for (ClickableText clickable : this.clickables) {
				if (clickable.contains(mouseX, mouseY)) {
					this.handleClickAction(clickable.action());
					return true;
				}
			}

			int x = (this.width - BOOK_WIDTH) / 2;
			int y = (this.height - BOOK_HEIGHT) / 2;
			if (this.canTurnNextPage() && this.isPageButtonClick(mouseX, mouseY, x, y, true)) {
				if (this.turnNextPage()) {
					this.playPageTurnSound();
				}
				return true;
			}
			if (this.canTurnPreviousPage() && this.isPageButtonClick(mouseX, mouseY, x, y, false)) {
				if (this.turnPreviousPage()) {
					this.playPageTurnSound();
				}
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void tick() {
		if (this.closing && Util.getMillis() - this.closingAtMillis >= CLOSE_ANIMATION_MS) {
			super.onClose();
		}
	}

	@Override
	public void onClose() {
		if (!this.closing) {
			this.closing = true;
			this.closingAtMillis = Util.getMillis();
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void renderTitlePage(GuiGraphics guiGraphics, int bookX, int bookY) {
		this.drawScaledCenteredString(guiGraphics, "Encyclopaedia", bookX + LEFT_PAGE_CENTER_X, bookY + TITLE_Y, TITLE_SCALE, TITLE_PAGE_COLOR);
		this.drawScaledCenteredString(guiGraphics, "Reliquarium", bookX + LEFT_PAGE_CENTER_X, bookY + SUBTITLE_Y, SUBTITLE_SCALE, TITLE_PAGE_COLOR);
		int iconX = bookX + LEFT_PAGE_CENTER_X - TITLE_ICON_SIZE / 2;
		guiGraphics.blit(ENCYCLOPAEDIA_ICON, iconX, bookY + ICON_Y, TITLE_ICON_SIZE, TITLE_ICON_SIZE, 0.0F, 0.0F, 16, 16, 16, 16);
	}

	private void renderDrawingPage(GuiGraphics guiGraphics, int bookX, int bookY) {
		int pageX = bookX + RIGHT_PAGE_X;
		int pageY = bookY + PAGE_TOP;
		this.blitTextureFit(guiGraphics, DRAWING_TEXTURE, pageX + 13, pageY + 6, PAGE_WIDTH - 26, PAGE_HEIGHT - 18, DRAWING_SOURCE_WIDTH, DRAWING_SOURCE_HEIGHT);
	}

	private void renderCatalogue(GuiGraphics guiGraphics, int bookX, int bookY, int mouseX, int mouseY) {
		List<NavigationEntry> entries = new ArrayList<>();
		for (EncyclopaediaReliquiarumData.Category category : this.data.categories()) {
			entries.add(new NavigationEntry(Component.translatable(category.titleKey()), category.icon(), new ClickAction(ClickActionType.OPEN_CATEGORY, category.id())));
		}
		this.renderNavigationList(guiGraphics, bookX, bookY, Component.translatable(CATALOGUE_TITLE_KEY), entries, this.catalogueSpread, mouseX, mouseY);
	}

	private void renderCategory(GuiGraphics guiGraphics, int bookX, int bookY, int mouseX, int mouseY) {
		EncyclopaediaReliquiarumData.Category category = this.selectedCategory();
		if (category == null) {
			return;
		}

		List<NavigationEntry> entries = new ArrayList<>();
		for (EncyclopaediaReliquiarumData.Article article : this.data.articlesFor(category.id())) {
			entries.add(new NavigationEntry(Component.translatable(article.titleKey()), article.icon(), new ClickAction(ClickActionType.OPEN_ARTICLE, article.id())));
		}
		this.renderNavigationList(guiGraphics, bookX, bookY, Component.translatable(category.titleKey()), entries, this.categorySpread, mouseX, mouseY);
	}

	private void renderNavigationList(GuiGraphics guiGraphics, int bookX, int bookY, Component title, List<NavigationEntry> entries, int spread, int mouseX, int mouseY) {
		int leftPage = spread * 2;
		int rightPage = leftPage + 1;
		this.renderNavigationPage(guiGraphics, bookX + LEFT_PAGE_X, bookY + PAGE_TOP, title, entries, leftPage, mouseX, mouseY);
		this.renderNavigationPage(guiGraphics, bookX + RIGHT_PAGE_X, bookY + PAGE_TOP, title, entries, rightPage, mouseX, mouseY);
	}

	private void renderNavigationPage(GuiGraphics guiGraphics, int pageX, int pageY, Component title, List<NavigationEntry> entries, int pageIndex, int mouseX, int mouseY) {
		int firstEntry = pageIndex * ENTRIES_PER_PAGE;
		if (firstEntry >= entries.size() && !(entries.isEmpty() && pageIndex == 0)) {
			return;
		}

		this.drawCenteredComponent(guiGraphics, title, pageX + PAGE_WIDTH / 2, pageY + LIST_TITLE_Y, false);
		for (int index = 0; index < ENTRIES_PER_PAGE; index++) {
			int entryIndex = firstEntry + index;
			if (entryIndex >= entries.size()) {
				break;
			}
			this.renderNavigationEntry(guiGraphics, pageX, pageY + LIST_ROW_START_Y + index * ENTRY_ROW_GAP, entries.get(entryIndex), mouseX, mouseY);
		}
	}

	private void renderNavigationEntry(GuiGraphics guiGraphics, int pageX, int y, NavigationEntry entry, int mouseX, int mouseY) {
		int iconX = pageX + 13;
		int textX = iconX + ENTRY_ICON_SIZE + 8;
		int textY = y + 4;
		this.drawIcon(guiGraphics, entry.icon(), iconX, y, ENTRY_ICON_SIZE, ENTRY_ICON_SIZE);
		this.drawClickableText(guiGraphics, entry.title(), textX, textY, mouseX, mouseY, entry.action());
	}

	private void renderArticle(GuiGraphics guiGraphics, int bookX, int bookY) {
		EncyclopaediaReliquiarumData.Article article = this.selectedArticle();
		if (article == null) {
			return;
		}

		List<PageContent> pages = this.buildArticlePages(article);
		int leftPage = this.articleSpread * 2;
		int rightPage = leftPage + 1;
		if (leftPage < pages.size()) {
			pages.get(leftPage).render(this, guiGraphics, bookX + LEFT_PAGE_X, bookY + PAGE_TOP);
		}
		if (rightPage < pages.size()) {
			pages.get(rightPage).render(this, guiGraphics, bookX + RIGHT_PAGE_X, bookY + PAGE_TOP);
		}
	}

	private List<PageContent> buildArticlePages(EncyclopaediaReliquiarumData.Article article) {
		List<PageContent> pages = new ArrayList<>();
		pages.add(new PageContent());
		int y = 0;
		EncyclopaediaReliquiarumData.Category category = this.data.category(article.categoryId());
		if (category != null) {
			y = this.addCenteredText(pages, y, Component.translatable(category.titleKey()), 1.55F, false, 3);
		}
		y = this.addCenteredText(pages, y, Component.translatable(article.titleKey()), 1.08F, true, 10);
		y = this.addIcon(pages, y, article.icon(), 16, 16, EncyclopaediaReliquiarumData.Alignment.CENTER, 0, 8);
		for (EncyclopaediaReliquiarumData.ArticleElement element : article.elements()) {
			if (element instanceof EncyclopaediaReliquiarumData.TextElement textElement) {
				y = this.addParagraph(pages, y, Component.translatable(textElement.key()), textElement);
			} else if (element instanceof EncyclopaediaReliquiarumData.ImageElement imageElement) {
				y = this.addImage(pages, y, imageElement);
			}
		}
		return pages;
	}

	private int addCenteredText(List<PageContent> pages, int y, Component text, float scale, boolean shadow, int gap) {
		float actualScale = scale * TEXT_SCALE;
		int height = this.scaledLineHeight(actualScale);
		y = this.ensureSpace(pages, y, height);
		PageContent page = pages.get(pages.size() - 1);
		int textY = y;
		page.elements.add((screen, guiGraphics, pageX, pageY) -> screen.drawScaledCenteredComponent(guiGraphics, text, pageX + PAGE_WIDTH / 2, pageY + textY, actualScale, shadow));
		return y + height + gap;
	}

	private int addParagraph(List<PageContent> pages, int y, Component text, EncyclopaediaReliquiarumData.TextElement textElement) {
		int textWidth = textElement.maxWidth() > 0 ? Math.min(textElement.maxWidth(), ARTICLE_TEXT_WIDTH) : ARTICLE_TEXT_WIDTH;
		List<FormattedCharSequence> lines = this.font.split(text, Math.max(1, Math.round(textWidth / TEXT_SCALE)));
		int lineHeight = this.scaledLineHeight(TEXT_SCALE);
		for (FormattedCharSequence line : lines) {
			y = this.ensureSpace(pages, y, lineHeight);
			PageContent page = pages.get(pages.size() - 1);
			int textX = this.getAlignedArticleX(this.scaledWidth(line, TEXT_SCALE), textElement.alignment(), textElement.xOffset());
			int textY = y;
			page.elements.add((screen, guiGraphics, pageX, pageY) -> screen.drawScaledFormattedText(guiGraphics, line, pageX + textX, pageY + textY, DEFAULT_TEXT_COLOR, textElement.shadow(), TEXT_SCALE));
			y += lineHeight;
		}
		return y + textElement.gap();
	}

	private int addImage(List<PageContent> pages, int y, EncyclopaediaReliquiarumData.ImageElement image) {
		y = this.ensureSpace(pages, y, image.height());
		PageContent page = pages.get(pages.size() - 1);
		int imageX = this.getAlignedArticleX(image.width(), image.alignment(), image.xOffset());
		int imageY = y;
		page.elements.add((screen, guiGraphics, pageX, pageY) -> guiGraphics.blit(image.texture(), pageX + imageX, pageY + imageY, image.width(), image.height(), 0.0F, 0.0F, image.sourceWidth(), image.sourceHeight(), image.sourceWidth(), image.sourceHeight()));
		return y + image.height() + image.gap();
	}

	private int addIcon(List<PageContent> pages, int y, EncyclopaediaReliquiarumData.Icon icon, int width, int height, EncyclopaediaReliquiarumData.Alignment alignment, int xOffset, int gap) {
		y = this.ensureSpace(pages, y, height);
		PageContent page = pages.get(pages.size() - 1);
		int iconX = this.getAlignedArticleX(width, alignment, xOffset);
		int iconY = y;
		page.elements.add((screen, guiGraphics, pageX, pageY) -> screen.drawIcon(guiGraphics, icon, pageX + iconX, pageY + iconY, width, height));
		return y + height + gap;
	}

	private int ensureSpace(List<PageContent> pages, int y, int height) {
		if (y > 0 && y + height > ARTICLE_CONTENT_HEIGHT) {
			pages.add(new PageContent());
			return 0;
		}
		return y;
	}

	private int getAlignedArticleX(int width, EncyclopaediaReliquiarumData.Alignment alignment, int xOffset) {
		int x = switch (alignment) {
			case LEFT -> ARTICLE_TEXT_X;
			case RIGHT -> ARTICLE_TEXT_X + ARTICLE_TEXT_WIDTH - width;
			case CENTER -> ARTICLE_TEXT_X + (ARTICLE_TEXT_WIDTH - width) / 2;
		};
		return x + xOffset;
	}

	private void drawClickableText(GuiGraphics guiGraphics, Component component, int x, int y, int mouseX, int mouseY, ClickAction action) {
		String plainText = this.stripFormatting(component);
		int width = this.scaledWidth(plainText, TEXT_SCALE);
		int height = this.scaledLineHeight(TEXT_SCALE);
		boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		this.drawScaledText(guiGraphics, component, x, y, DEFAULT_TEXT_COLOR, false, TEXT_SCALE);
		if (hovered) {
			this.drawScaledText(guiGraphics, "<", x + width + 4, y, DEFAULT_TEXT_COLOR, false, TEXT_SCALE);
		}
		this.clickables.add(new ClickableText(x, y, width, height, action));
	}

	private void renderPageButtons(GuiGraphics guiGraphics, int bookX, int bookY, int mouseX, int mouseY) {
		if (this.canTurnPreviousPage()) {
			this.renderPageButton(guiGraphics, bookX, bookY, mouseX, mouseY, false);
		}
		if (this.canTurnNextPage()) {
			this.renderPageButton(guiGraphics, bookX, bookY, mouseX, mouseY, true);
		}
	}

	private void renderPageButton(GuiGraphics guiGraphics, int bookX, int bookY, int mouseX, int mouseY, boolean forward) {
		int x = this.getPageButtonX(bookX, forward);
		int y = this.getPageButtonY(bookY);
		boolean hovered = mouseX >= x && mouseX < x + PAGE_BUTTON_WIDTH && mouseY >= y && mouseY < y + PAGE_BUTTON_HEIGHT;
		ResourceLocation sprite = forward
				? (hovered ? PAGE_FORWARD_HIGHLIGHTED_SPRITE : PAGE_FORWARD_SPRITE)
				: (hovered ? PAGE_BACKWARD_HIGHLIGHTED_SPRITE : PAGE_BACKWARD_SPRITE);
		guiGraphics.blitSprite(sprite, x, y, PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT);
	}

	private void drawIcon(GuiGraphics guiGraphics, EncyclopaediaReliquiarumData.Icon icon, int x, int y, int width, int height) {
		if (icon.animated()) {
			long frameLengthMs = (long) icon.frameTicks() * MILLIS_PER_TICK;
			int frame = (int) ((Util.getMillis() / frameLengthMs) % Math.max(1, icon.frameCount()));
			guiGraphics.blit(icon.texture(), x, y, width, height, 0.0F, frame * icon.frameHeight(), icon.frameWidth(), icon.frameHeight(), icon.sourceWidth(), icon.sourceHeight());
		} else {
			guiGraphics.blit(icon.texture(), x, y, width, height, 0.0F, 0.0F, icon.sourceWidth(), icon.sourceHeight(), icon.sourceWidth(), icon.sourceHeight());
		}
	}

	private void handleClickAction(ClickAction action) {
		if (action.type() == ClickActionType.OPEN_CATEGORY) {
			EncyclopaediaReliquiarumData.Category category = this.data.category(action.id());
			if (category != null) {
				this.selectedCategoryId = category.id();
				this.selectedArticleId = null;
				this.categorySpread = 0;
				this.view = BookView.CATEGORY;
				this.playPageTurnSound();
			}
		} else if (action.type() == ClickActionType.OPEN_ARTICLE) {
			EncyclopaediaReliquiarumData.Article article = this.data.article(action.id());
			if (article != null) {
				this.selectedArticleId = article.id();
				this.selectedCategoryId = article.categoryId();
				this.articleSpread = 0;
				this.view = BookView.ARTICLE;
				this.playPageTurnSound();
			}
		}
	}

	private boolean turnNextPage() {
		return switch (this.view) {
			case TITLE -> {
				this.view = BookView.CATALOGUE;
				this.catalogueSpread = 0;
				yield true;
			}
			case CATALOGUE -> {
				if (this.catalogueSpread < this.getCatalogueSpreadCount() - 1) {
					this.catalogueSpread++;
					yield true;
				}
				yield false;
			}
			case CATEGORY -> {
				if (this.categorySpread < this.getCategorySpreadCount() - 1) {
					this.categorySpread++;
					yield true;
				}
				yield false;
			}
			case ARTICLE -> {
				if (this.articleSpread < this.getArticleSpreadCount() - 1) {
					this.articleSpread++;
					yield true;
				}
				yield false;
			}
		};
	}

	private boolean turnPreviousPage() {
		return switch (this.view) {
			case TITLE -> false;
			case CATALOGUE -> {
				if (this.catalogueSpread > 0) {
					this.catalogueSpread--;
				} else {
					this.view = BookView.TITLE;
				}
				yield true;
			}
			case CATEGORY -> {
				if (this.categorySpread > 0) {
					this.categorySpread--;
				} else {
					this.view = BookView.CATALOGUE;
				}
				yield true;
			}
			case ARTICLE -> {
				if (this.articleSpread > 0) {
					this.articleSpread--;
				} else {
					this.view = this.selectedCategory() != null ? BookView.CATEGORY : BookView.CATALOGUE;
				}
				yield true;
			}
		};
	}

	private int getCatalogueSpreadCount() {
		return this.getNavigationSpreadCount(this.data.categories().size());
	}

	private int getCategorySpreadCount() {
		EncyclopaediaReliquiarumData.Category category = this.selectedCategory();
		return this.getNavigationSpreadCount(category == null ? 0 : this.data.articlesFor(category.id()).size());
	}

	private int getNavigationSpreadCount(int entryCount) {
		return Math.max(1, (this.getNavigationPageCount(entryCount) + 1) / 2);
	}

	private int getNavigationPageCount(int entryCount) {
		return Math.max(1, (entryCount + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE);
	}

	private int getArticleSpreadCount() {
		EncyclopaediaReliquiarumData.Article article = this.selectedArticle();
		if (article == null) {
			return 1;
		}
		return Math.max(1, (this.buildArticlePages(article).size() + 1) / 2);
	}

	private boolean canTurnNextPage() {
		return switch (this.view) {
			case TITLE -> true;
			case CATALOGUE -> this.catalogueSpread < this.getCatalogueSpreadCount() - 1;
			case CATEGORY -> this.categorySpread < this.getCategorySpreadCount() - 1;
			case ARTICLE -> this.articleSpread < this.getArticleSpreadCount() - 1;
		};
	}

	private boolean canTurnPreviousPage() {
		return this.view != BookView.TITLE;
	}

	private boolean isPageButtonClick(double mouseX, double mouseY, int bookX, int bookY, boolean forward) {
		int x = this.getPageButtonX(bookX, forward);
		int y = this.getPageButtonY(bookY);
		return mouseX >= x && mouseX < x + PAGE_BUTTON_WIDTH && mouseY >= y && mouseY < y + PAGE_BUTTON_HEIGHT;
	}

	private int getPageButtonX(int bookX, boolean forward) {
		if (forward) {
			return bookX + RIGHT_PAGE_X + PAGE_WIDTH - PAGE_BUTTON_WIDTH - PAGE_BUTTON_SIDE_PADDING;
		}
		return bookX + LEFT_PAGE_X + PAGE_BUTTON_SIDE_PADDING;
	}

	private int getPageButtonY(int bookY) {
		return bookY + PAGE_TOP + PAGE_HEIGHT - PAGE_BUTTON_HEIGHT - PAGE_BUTTON_BOTTOM_PADDING;
	}

	private EncyclopaediaReliquiarumData.Category selectedCategory() {
		return this.selectedCategoryId == null ? null : this.data.category(this.selectedCategoryId);
	}

	private EncyclopaediaReliquiarumData.Article selectedArticle() {
		return this.selectedArticleId == null ? null : this.data.article(this.selectedArticleId);
	}

	private void playPageTurnSound() {
		if (this.minecraft != null && this.minecraft.player != null) {
			this.minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.65F, 1.0F);
		}
	}

	private void blitTextureFit(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int maxWidth, int maxHeight, int sourceWidth, int sourceHeight) {
		float widthRatio = maxWidth / (float) sourceWidth;
		float heightRatio = maxHeight / (float) sourceHeight;
		float ratio = Math.min(widthRatio, heightRatio);
		int drawWidth = Math.max(1, Math.round(sourceWidth * ratio));
		int drawHeight = Math.max(1, Math.round(sourceHeight * ratio));
		int drawX = x + (maxWidth - drawWidth) / 2;
		int drawY = y + (maxHeight - drawHeight) / 2;
		guiGraphics.blit(texture, drawX, drawY, drawWidth, drawHeight, 0.0F, 0.0F, sourceWidth, sourceHeight, sourceWidth, sourceHeight);
	}

	private void drawScaledCenteredString(GuiGraphics guiGraphics, String text, int centerX, int y, float scale, int color) {
		this.drawScaledCenteredComponent(guiGraphics, Component.literal(text), centerX, y, scale, color, true);
	}

	private void drawCenteredComponent(GuiGraphics guiGraphics, Component text, int centerX, int y, boolean shadow) {
		this.drawScaledText(guiGraphics, text, centerX - this.scaledWidth(text, TEXT_SCALE) / 2, y, DEFAULT_TEXT_COLOR, shadow, TEXT_SCALE);
	}

	private void drawScaledCenteredComponent(GuiGraphics guiGraphics, Component text, int centerX, int y, float scale, boolean shadow) {
		this.drawScaledCenteredComponent(guiGraphics, text, centerX, y, scale, DEFAULT_TEXT_COLOR, shadow);
	}

	private void drawScaledCenteredComponent(GuiGraphics guiGraphics, Component text, int centerX, int y, float scale, int fallbackColor, boolean shadow) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(centerX, y, 0.0F);
		guiGraphics.pose().scale(scale, scale, 1.0F);
		int textX = -this.font.width(text) / 2;
		guiGraphics.drawString(this.font, text, textX, 0, fallbackColor, shadow);
		guiGraphics.pose().popPose();
	}

	private void drawScaledText(GuiGraphics guiGraphics, Component text, int x, int y, int fallbackColor, boolean shadow, float scale) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x, y, 0.0F);
		guiGraphics.pose().scale(scale, scale, 1.0F);
		guiGraphics.drawString(this.font, text, 0, 0, fallbackColor, shadow);
		guiGraphics.pose().popPose();
	}

	private void drawScaledText(GuiGraphics guiGraphics, String text, int x, int y, int fallbackColor, boolean shadow, float scale) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x, y, 0.0F);
		guiGraphics.pose().scale(scale, scale, 1.0F);
		guiGraphics.drawString(this.font, text, 0, 0, fallbackColor, shadow);
		guiGraphics.pose().popPose();
	}

	private void drawScaledFormattedText(GuiGraphics guiGraphics, FormattedCharSequence text, int x, int y, int fallbackColor, boolean shadow, float scale) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x, y, 0.0F);
		guiGraphics.pose().scale(scale, scale, 1.0F);
		guiGraphics.drawString(this.font, text, 0, 0, fallbackColor, shadow);
		guiGraphics.pose().popPose();
	}

	private int scaledWidth(Component text, float scale) {
		return (int) Math.ceil(this.font.width(text) * scale);
	}

	private int scaledWidth(String text, float scale) {
		return (int) Math.ceil(this.font.width(text) * scale);
	}

	private int scaledWidth(FormattedCharSequence text, float scale) {
		return (int) Math.ceil(this.font.width(text) * scale);
	}

	private int scaledLineHeight(float scale) {
		return (int) Math.ceil(this.font.lineHeight * scale);
	}

	private String stripFormatting(Component component) {
		String stripped = ChatFormatting.stripFormatting(component.getString());
		return stripped != null ? stripped : component.getString();
	}

	private float getAnimationScale() {
		if (this.closing) {
			float progress = this.clamp((Util.getMillis() - this.closingAtMillis) / (float) CLOSE_ANIMATION_MS);
			return 1.0F - (1.0F - MIN_SCALE) * this.easeOutCubic(progress);
		}
		float progress = this.clamp((Util.getMillis() - this.openedAtMillis) / (float) OPEN_ANIMATION_MS);
		return MIN_SCALE + (1.0F - MIN_SCALE) * this.easeOutCubic(progress);
	}

	private float easeOutCubic(float value) {
		float inverted = 1.0F - value;
		return 1.0F - inverted * inverted * inverted;
	}

	private float clamp(float value) {
		if (value < 0.0F) {
			return 0.0F;
		}
		return Math.min(value, 1.0F);
	}

	private enum BookView {
		TITLE,
		CATALOGUE,
		CATEGORY,
		ARTICLE
	}

	private enum ClickActionType {
		OPEN_CATEGORY,
		OPEN_ARTICLE
	}

	private record ClickAction(ClickActionType type, String id) {
	}

	private record ClickableText(int x, int y, int width, int height, ClickAction action) {
		private boolean contains(double mouseX, double mouseY) {
			return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
		}
	}

	private record NavigationEntry(Component title, EncyclopaediaReliquiarumData.Icon icon, ClickAction action) {
	}

	@FunctionalInterface
	private interface PageElement {
		void render(EncyclopaediaReliquiarumScreen screen, GuiGraphics guiGraphics, int pageX, int pageY);
	}

	private static class PageContent {
		private final List<PageElement> elements = new ArrayList<>();

		private void render(EncyclopaediaReliquiarumScreen screen, GuiGraphics guiGraphics, int pageX, int pageY) {
			for (PageElement element : this.elements) {
				element.render(screen, guiGraphics, pageX, pageY);
			}
		}
	}
}
