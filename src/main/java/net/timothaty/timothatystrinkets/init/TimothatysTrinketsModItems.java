package net.timothaty.timothatystrinkets.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.item.*;

public class TimothatysTrinketsModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(TimothatysTrinketsMod.MODID);
	public static final DeferredItem<Item> REFRESHING_CHALICE;
	public static final DeferredItem<Item> BLOCK_OF_BLIGHT;
	public static final DeferredItem<Item> FANGS;
	public static final DeferredItem<Item> HOLY_ROSARIUM;
	public static final DeferredItem<Item> DRUMS_OF_HASTE;
	public static final DeferredItem<Item> UNDEAD_KNIGHTS_ARMLET;
	public static final DeferredItem<Item> PAGANS_CHARM;
	public static final DeferredItem<Item> FARMERS_RING;
	public static final DeferredItem<Item> BELT_OF_OUTCAST;
	public static final DeferredItem<Item> CLEANSING_DUST;
	public static final DeferredItem<Item> INCENSE;
	public static final DeferredItem<Item> AROMATIC_OLIBANUM;
	public static final DeferredItem<Item> CHAMPIONS_GAUNTLET;
	public static final DeferredItem<Item> PILLAGERS_COIN;
	public static final DeferredItem<Item> CURSED_EMERALD;
	public static final DeferredItem<Item> INDULGENCY;
	public static final DeferredItem<Item> FLAMING_EMBER;
	public static final DeferredItem<Item> DAMNATION_ALTAR;
	public static final DeferredItem<Item> RITUAL_DAGGER;
	public static final DeferredItem<Item> NECRONOMICON;
	public static final DeferredItem<Item> VOID_SPHERE;
	public static final DeferredItem<Item> DORMANT_SPHERE;
	public static final DeferredItem<Item> ECHO_SPHERE;
	public static final DeferredItem<Item> GOLDEN_HONEY_COMB;
	public static final DeferredItem<Item> BOTTLE_OF_BLOOD;
	public static final DeferredItem<Item> BOTTLE_OF_SOUL_ORB;
	public static final DeferredItem<Item> FIRE_SPHERE;
	public static final DeferredItem<Item> VENOM_SPHERE;
	public static final DeferredItem<Item> STRIKER_OF_THE_MORNING_STAR;
	public static final DeferredItem<Item> MORGENSHTERN;
	public static final DeferredItem<Item> DEATHBRINGER;
	public static final DeferredItem<Item> NECROMANCER_SPAWN_EGG;
	public static final DeferredItem<Item> UNDEAD_KNIGHT_SPAWN_EGG;
	public static final DeferredItem<Item> RUSTY_GAUNTLET;
	public static final DeferredItem<Item> RUSTY_ARMLET;
	public static final DeferredItem<Item> UNHOLY_SHARD;
	public static final DeferredItem<Item> BEAD_OF_RESURRECTION;
	public static final DeferredItem<Item> BEAD_OF_HUMILITY;
	public static final DeferredItem<Item> BEAD_OF_THE_SACRAMENT;
	public static final DeferredItem<Item> EMPTY_CHALICE;
	public static final DeferredItem<Item> DEBTLORDS_HEAD;
	public static final DeferredItem<Item> FANG;
	public static final DeferredItem<Item> CORRUPTED_INGOT;
	public static final DeferredItem<Item> ENCYCLOPAEDIA_RELIQUIARUM;
	public static final DeferredItem<Item> BEAD_OF_REPENTANCE;
	public static final DeferredItem<Item> BEAD_OF_BLASPHEMY;
	public static final DeferredItem<Item> BEAD_OF_GNOSIS;
	public static final DeferredItem<Item> BEAD_OF_PRIDE;
	public static final DeferredItem<Item> BEAD_OF_SIN;
	public static final DeferredItem<Item> BEAD_OF_WRATH;
	public static final DeferredItem<Item> BEAD_OF_THE_SAINT;
	public static final DeferredItem<Item> WOODEN_BEAD;
	public static final DeferredItem<Item> CORRUPTED_ROSARY;
	public static final DeferredItem<Item> PACT_OF_ALLIANCE;
	public static final DeferredItem<Item> HOLY_INK;
	public static final DeferredItem<Item> DUELISTS_GAUNTLET;
	static {
		REFRESHING_CHALICE = REGISTRY.register("refreshing_chalice", RefreshingChaliceItem::new);
		BLOCK_OF_BLIGHT = block(TimothatysTrinketsModBlocks.BLOCK_OF_BLIGHT);
		FANGS = REGISTRY.register("fangs", FangsItem::new);
		HOLY_ROSARIUM = REGISTRY.register("holy_rosarium", HolyRosariumItem::new);
		DRUMS_OF_HASTE = REGISTRY.register("drums_of_haste", DrumsOfHasteItem::new);
		UNDEAD_KNIGHTS_ARMLET = REGISTRY.register("undead_knights_armlet", UndeadKnightsArmletItem::new);
		PAGANS_CHARM = REGISTRY.register("pagans_charm", PagansCharmItem::new);
		FARMERS_RING = REGISTRY.register("farmers_ring", FarmersRingItem::new);
		BELT_OF_OUTCAST = REGISTRY.register("belt_of_outcast", BeltOfOutcastItem::new);
		CLEANSING_DUST = REGISTRY.register("cleansing_dust", CleansingDustItem::new);
		INCENSE = REGISTRY.register("incense", () -> new BlockItem(TimothatysTrinketsModBlocks.INCENSE.get(), new Item.Properties()));
		AROMATIC_OLIBANUM = REGISTRY.register("aromatic_olibanum", () -> new Item(new Item.Properties()));
		CHAMPIONS_GAUNTLET = REGISTRY.register("champions_gauntlet", ChampionsGauntletItem::new);
		PILLAGERS_COIN = REGISTRY.register("pillagers_coin", PillagersCoinItem::new);
		CURSED_EMERALD = REGISTRY.register("cursed_emerald", CursedEmeraldItem::new);
		INDULGENCY = REGISTRY.register("indulgency", IndulgencyItem::new);
		FLAMING_EMBER = REGISTRY.register("flaming_ember", FlamingEmberItem::new);
		DAMNATION_ALTAR = block(TimothatysTrinketsModBlocks.DAMNATION_ALTAR);
		RITUAL_DAGGER = REGISTRY.register("ritual_dagger", RitualDaggerItem::new);
		NECRONOMICON = REGISTRY.register("necronomicon", NecronomiconItem::new);
		VOID_SPHERE = REGISTRY.register("void_sphere", VoidSphereItem::new);
		DORMANT_SPHERE = REGISTRY.register("dormant_sphere", () -> new BlockItem(
				TimothatysTrinketsModBlocks.DORMANT_SPHERE.get(),
				new Item.Properties().stacksTo(1)
		));
		ECHO_SPHERE = REGISTRY.register("echo_sphere", EchoSphereItem::new);
		GOLDEN_HONEY_COMB = REGISTRY.register("golden_honey_comb", GoldenHoneyCombItem::new);
		BOTTLE_OF_BLOOD = REGISTRY.register("bottle_of_blood", BottleOfBloodItem::new);
		BOTTLE_OF_SOUL_ORB = REGISTRY.register("bottle_of_soul_orb", BottleOfSoulOrbItem::new);
		FIRE_SPHERE = REGISTRY.register("fire_sphere", FireSphereItem::new);
		VENOM_SPHERE = REGISTRY.register("venom_sphere", VenomSphereItem::new);
		STRIKER_OF_THE_MORNING_STAR = REGISTRY.register("striker_of_the_morning_star", StrikerOfTheMorningStarItem::new);
		MORGENSHTERN = REGISTRY.register("morgenshtern", MorgenshternItem::new);
		DEATHBRINGER = REGISTRY.register("deathbringer", DeathbringerItem::new);
		NECROMANCER_SPAWN_EGG = REGISTRY.register("necromancer_spawn_egg", () -> new DeferredSpawnEggItem(TimothatysTrinketsModEntities.NECROMANCER, -11068645, -8186449, new Item.Properties()));
		UNDEAD_KNIGHT_SPAWN_EGG = REGISTRY.register("undead_knight_spawn_egg", () -> new DeferredSpawnEggItem(TimothatysTrinketsModEntities.UNDEAD_KNIGHT, -1, -1, new Item.Properties()));
		RUSTY_GAUNTLET = REGISTRY.register("rusty_gauntlet", RustyGauntletItem::new);
		RUSTY_ARMLET = REGISTRY.register("rusty_armlet", RustyArmletItem::new);
		UNHOLY_SHARD = REGISTRY.register("unholy_shard", UnholyShardItem::new);
		BEAD_OF_RESURRECTION = REGISTRY.register("bead_of_resurrection", BeadOfResurrectionItem::new);
		BEAD_OF_HUMILITY = REGISTRY.register("bead_of_humility", BeadOfHumilityItem::new);
		BEAD_OF_THE_SACRAMENT = REGISTRY.register("bead_of_the_sacrament", BeadOfTheSacramentItem::new);
		EMPTY_CHALICE = REGISTRY.register("empty_chalice", EmptyChaliceItem::new);
		DEBTLORDS_HEAD = block(TimothatysTrinketsModBlocks.DEBTLORDS_HEAD);
		FANG = REGISTRY.register("fang", FangItem::new);
		CORRUPTED_INGOT = REGISTRY.register("corrupted_ingot", CorruptedIngotItem::new);
		ENCYCLOPAEDIA_RELIQUIARUM = REGISTRY.register("encyclopaedia_reliquiarum", EncyclopaediaReliquiarumItem::new);
		BEAD_OF_REPENTANCE = REGISTRY.register("bead_of_repentance", BeadOfRepentanceItem::new);
		BEAD_OF_BLASPHEMY = REGISTRY.register("bead_of_blasphemy", () -> new Item(new Item.Properties().stacksTo(1)));
		BEAD_OF_GNOSIS = REGISTRY.register("bead_of_gnosis", () -> new Item(new Item.Properties().stacksTo(1)));
		BEAD_OF_PRIDE = REGISTRY.register("bead_of_pride", () -> new Item(new Item.Properties().stacksTo(1)));
		BEAD_OF_SIN = REGISTRY.register("bead_of_sin", () -> new Item(new Item.Properties().stacksTo(1)));
		BEAD_OF_WRATH = REGISTRY.register("bead_of_wrath", () -> new Item(new Item.Properties().stacksTo(1)));
		BEAD_OF_THE_SAINT = REGISTRY.register("bead_of_the_saint", () -> new Item(new Item.Properties().stacksTo(1)));
		WOODEN_BEAD = REGISTRY.register("wooden_bead", () -> new Item(new Item.Properties().stacksTo(1)));
		CORRUPTED_ROSARY = REGISTRY.register("corrupted_rosary", CorruptedRosariumItem::new);
		PACT_OF_ALLIANCE = REGISTRY.register("pact_of_alliance", PactOfAllianceItem::new);
		HOLY_INK = REGISTRY.register("holy_ink", HolyInkItem::new);
		DUELISTS_GAUNTLET = REGISTRY.register("duelists_gauntlet", DuelistsGauntletItem::new);
	}

	private static DeferredItem<Item> block(net.neoforged.neoforge.registries.DeferredBlock<Block> block) {
		if (block == TimothatysTrinketsModBlocks.DEBTLORDS_HEAD) {
			return REGISTRY.register(block.getId().getPath(),
					() -> new net.minecraft.world.item.StandingAndWallBlockItem(TimothatysTrinketsModBlocks.DEBTLORDS_HEAD.get(), TimothatysTrinketsModBlocks.DEBTLORDS_WALL_HEAD.get(), new Item.Properties(), net.minecraft.core.Direction.DOWN));
		}
		return block((DeferredHolder<Block, Block>) block, new Item.Properties());
	}

	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block) {
		return block(block, new Item.Properties());
	}

	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block, Item.Properties properties) {
		return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), properties));
	}
}
