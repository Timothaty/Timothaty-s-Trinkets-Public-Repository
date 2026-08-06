package net.timothaty.timothatystrinkets.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class EncyclopaediaReliquiarumData {
	private static final String CATEGORIES_PATH = "encyclopaedia/categories";
	private static final String ARTICLES_PATH = "encyclopaedia/articles";
	private static final Icon FALLBACK_ICON = new Icon(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/item/encyclopaedia_reliquiarum.png"), false, 16, 16, 1, 30, 16, 16);

	private final List<Category> categories;
	private final Map<String, Category> categoriesById;
	private final Map<String, List<Article>> articlesByCategory;
	private final Map<String, Article> articlesById;

	private EncyclopaediaReliquiarumData(List<Category> categories, List<Article> articles) {
		this.categories = List.copyOf(categories);
		this.categoriesById = new HashMap<>();
		this.articlesByCategory = new HashMap<>();
		this.articlesById = new HashMap<>();

		for (Category category : categories) {
			this.categoriesById.put(category.id(), category);
		}
		for (Article article : articles) {
			this.articlesById.put(article.id(), article);
			this.articlesByCategory.computeIfAbsent(article.categoryId(), id -> new ArrayList<>()).add(article);
		}
		for (List<Article> categoryArticles : this.articlesByCategory.values()) {
			categoryArticles.sort(Comparator.comparingInt(Article::order).thenComparing(Article::id));
		}
	}

	static EncyclopaediaReliquiarumData load() {
		ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
		List<Category> categories = loadCategories(resourceManager);
		List<Article> articles = loadArticles(resourceManager);
		if (categories.isEmpty()) {
			return fallback();
		}
		return new EncyclopaediaReliquiarumData(categories, articles);
	}

	private static List<Category> loadCategories(ResourceManager resourceManager) {
		List<Category> categories = new ArrayList<>();
		Map<ResourceLocation, Resource> resources = resourceManager.listResources(CATEGORIES_PATH, location -> location.getPath().endsWith(".json"));
		resources.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString))).forEach(entry -> {
			try (Reader reader = entry.getValue().openAsReader()) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				Category category = parseCategory(json, entry.getKey());
				if (category != null) {
					categories.add(category);
				}
			} catch (Exception exception) {
				TimothatysTrinketsMod.LOGGER.warn("Failed to load encyclopaedia category {}", entry.getKey(), exception);
			}
		});
		categories.sort(Comparator.comparingInt(Category::order).thenComparing(Category::id));
		return categories;
	}

	private static List<Article> loadArticles(ResourceManager resourceManager) {
		List<Article> articles = new ArrayList<>();
		Map<ResourceLocation, Resource> resources = resourceManager.listResources(ARTICLES_PATH, location -> location.getPath().endsWith(".json"));
		resources.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString))).forEach(entry -> {
			try (Reader reader = entry.getValue().openAsReader()) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				Article article = parseArticle(json, entry.getKey());
				if (article != null) {
					articles.add(article);
				}
			} catch (Exception exception) {
				TimothatysTrinketsMod.LOGGER.warn("Failed to load encyclopaedia article {}", entry.getKey(), exception);
			}
		});
		articles.sort(Comparator.comparingInt(Article::order).thenComparing(Article::id));
		return articles;
	}

	private static Category parseCategory(JsonObject json, ResourceLocation source) {
		String id = getString(json, "id", fileName(source));
		String title = getString(json, "title", id);
		Icon icon = parseIcon(json, FALLBACK_ICON);
		int order = getInt(json, "order", 0);
		return new Category(id, title, icon, order);
	}

	private static Article parseArticle(JsonObject json, ResourceLocation source) {
		String id = getString(json, "id", fileName(source));
		String category = getString(json, "category", "");
		if (category.isBlank()) {
			TimothatysTrinketsMod.LOGGER.warn("Skipping encyclopaedia article {} because it has no category", source);
			return null;
		}

		String title = getString(json, "title", id);
		Icon icon = parseIcon(json, FALLBACK_ICON);
		int order = getInt(json, "order", 0);
		List<ArticleElement> elements = parseArticleElements(json);
		return new Article(id, category, title, icon, elements, order);
	}

	private static List<ArticleElement> parseArticleElements(JsonObject json) {
		List<ArticleElement> elements = new ArrayList<>();
		if (json.has("elements") && json.get("elements").isJsonArray()) {
			for (JsonElement element : json.getAsJsonArray("elements")) {
				if (element.isJsonObject()) {
					ArticleElement articleElement = parseArticleElement(element.getAsJsonObject());
					if (articleElement != null) {
						elements.add(articleElement);
					}
				}
			}
		}

		if (elements.isEmpty() && json.has("paragraphs") && json.get("paragraphs").isJsonArray()) {
			JsonArray paragraphs = json.getAsJsonArray("paragraphs");
			for (JsonElement paragraph : paragraphs) {
				if (paragraph.isJsonPrimitive()) {
					elements.add(new TextElement(paragraph.getAsString(), Alignment.LEFT, 0, 0, false, 5));
				}
			}
		}
		return elements;
	}

	private static ArticleElement parseArticleElement(JsonObject json) {
		String type = getString(json, "type", "text");
		if ("image".equals(type)) {
			String textureValue = getString(json, "texture", "");
			if (textureValue.isBlank()) {
				return null;
			}
			int width = getInt(json, "width", 16);
			int height = getInt(json, "height", width);
			int sourceWidth = getInt(json, "source_width", width);
			int sourceHeight = getInt(json, "source_height", height);
			Alignment alignment = parseAlignment(getString(json, "align", "center"));
			int xOffset = getInt(json, "x_offset", 0);
			int gap = getInt(json, "gap", 5);
			return new ImageElement(parseTexture(textureValue), width, height, sourceWidth, sourceHeight, alignment, xOffset, gap);
		}

		String key = getString(json, "key", "");
		if (key.isBlank()) {
			return null;
		}
		Alignment alignment = parseAlignment(getString(json, "align", "left"));
		int xOffset = getInt(json, "x_offset", 0);
		int maxWidth = getInt(json, "max_width", 0);
		boolean shadow = getBoolean(json, "shadow", false);
		int gap = getInt(json, "gap", 5);
		return new TextElement(key, alignment, xOffset, maxWidth, shadow, gap);
	}

	private static Icon parseIcon(JsonObject json, Icon fallback) {
		JsonObject iconJson = json;
		String textureValue = "";
		if (json.has("icon")) {
			JsonElement iconElement = json.get("icon");
			if (iconElement.isJsonPrimitive()) {
				textureValue = iconElement.getAsString();
			} else if (iconElement.isJsonObject()) {
				iconJson = iconElement.getAsJsonObject();
				textureValue = getString(iconJson, "texture", "");
			}
		}
		if (textureValue.isBlank()) {
			textureValue = getString(json, "icon_texture", "");
		}
		if (textureValue.isBlank()) {
			return fallback;
		}

		boolean animated = getBoolean(iconJson, "animated", getBoolean(json, "animated_icon", false));
		int frameWidth = getInt(iconJson, "frame_width", 16);
		int frameHeight = getInt(iconJson, "frame_height", 16);
		int frameCount = getInt(iconJson, "frames", animated ? 4 : 1);
		int frameTicks = getInt(iconJson, "frame_ticks", 30);
		int sourceWidth = getInt(iconJson, "source_width", frameWidth);
		int sourceHeight = getInt(iconJson, "source_height", animated ? frameHeight * frameCount : frameHeight);
		return new Icon(parseTexture(textureValue), animated, frameWidth, frameHeight, frameCount, frameTicks, sourceWidth, sourceHeight);
	}

	private static Alignment parseAlignment(String value) {
		return switch (value.toLowerCase()) {
			case "left" -> Alignment.LEFT;
			case "right" -> Alignment.RIGHT;
			default -> Alignment.CENTER;
		};
	}

	private static ResourceLocation parseTexture(String value) {
		ResourceLocation location = value.contains(":") ? ResourceLocation.parse(value) : ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, value);
		String path = location.getPath();
		if (path.endsWith(".png")) {
			return location;
		}
		if (path.startsWith("textures/")) {
			return ResourceLocation.fromNamespaceAndPath(location.getNamespace(), path + ".png");
		}
		if (path.startsWith("item/")) {
			return ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "textures/" + path + ".png");
		}
		if (path.startsWith("screen/")) {
			return ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "textures/screens/" + path.substring("screen/".length()) + ".png");
		}
		if (path.startsWith("screens/")) {
			return ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "textures/" + path + ".png");
		}
		return location;
	}

	private static String getString(JsonObject json, String key, String fallback) {
		if (json.has(key) && json.get(key).isJsonPrimitive()) {
			return json.get(key).getAsString();
		}
		return fallback;
	}

	private static int getInt(JsonObject json, String key, int fallback) {
		if (json.has(key) && json.get(key).isJsonPrimitive()) {
			return json.get(key).getAsInt();
		}
		return fallback;
	}

	private static boolean getBoolean(JsonObject json, String key, boolean fallback) {
		if (json.has(key) && json.get(key).isJsonPrimitive()) {
			return json.get(key).getAsBoolean();
		}
		return fallback;
	}

	private static String fileName(ResourceLocation source) {
		String path = source.getPath();
		int slash = path.lastIndexOf('/');
		String fileName = slash >= 0 ? path.substring(slash + 1) : path;
		return fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - ".json".length()) : fileName;
	}

	private static EncyclopaediaReliquiarumData fallback() {
		List<Category> categories = List.of(
				new Category("holiness", "guide.timothatys_trinkets.catalogue_chapters.1", new Icon(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/screens/holiness.png"), true, 16, 16, 4, 30, 16, 64), 0),
				new Category("occultism", "guide.timothatys_trinkets.catalogue_chapters.2", new Icon(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/item/necronomicon.png"), false, 16, 16, 1, 30, 16, 16), 1),
				new Category("spheres", "guide.timothatys_trinkets.catalogue_chapters.3", new Icon(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/item/void_sphere.png"), false, 16, 16, 1, 30, 16, 16), 2),
				new Category("undead", "guide.timothatys_trinkets.catalogue_chapters.4", new Icon(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/item/undead_knight_armlet.png"), false, 16, 16, 1, 30, 16, 16), 3),
				new Category("effects", "guide.timothatys_trinkets.catalogue_chapters.5", new Icon(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/item/cleansing_dust.png"), false, 16, 16, 1, 30, 16, 16), 4)
		);
		List<Article> articles = List.of(new Article(
				"holy_rosarium",
				"holiness",
				"guide.timothatys_trinkets.holiness_chapter.1",
				new Icon(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/item/holy_rosarium.png"), false, 16, 16, 1, 30, 16, 16),
				List.of(
						new TextElement("guide.timothatys_trinkets.holiness_chapter.2", Alignment.LEFT, 0, 0, false, 5),
						new TextElement("guide.timothatys_trinkets.holiness_chapter.3", Alignment.LEFT, 0, 0, false, 5),
						new TextElement("guide.timothatys_trinkets.holiness_chapter.4", Alignment.LEFT, 0, 0, false, 5)
				),
				0
		));
		return new EncyclopaediaReliquiarumData(categories, articles);
	}

	List<Category> categories() {
		return this.categories;
	}

	Category category(String id) {
		return this.categoriesById.get(id);
	}

	Article article(String id) {
		return this.articlesById.get(id);
	}

	List<Article> articlesFor(String categoryId) {
		return this.articlesByCategory.getOrDefault(categoryId, List.of());
	}

	record Category(String id, String titleKey, Icon icon, int order) {
	}

	record Article(String id, String categoryId, String titleKey, Icon icon, List<ArticleElement> elements, int order) {
	}

	record Icon(ResourceLocation texture, boolean animated, int frameWidth, int frameHeight, int frameCount, int frameTicks, int sourceWidth, int sourceHeight) {
	}

	sealed interface ArticleElement permits TextElement, ImageElement {
		int gap();
	}

	record TextElement(String key, Alignment alignment, int xOffset, int maxWidth, boolean shadow, int gap) implements ArticleElement {
	}

	record ImageElement(ResourceLocation texture, int width, int height, int sourceWidth, int sourceHeight, Alignment alignment, int xOffset, int gap) implements ArticleElement {
	}

	enum Alignment {
		LEFT,
		CENTER,
		RIGHT
	}
}
