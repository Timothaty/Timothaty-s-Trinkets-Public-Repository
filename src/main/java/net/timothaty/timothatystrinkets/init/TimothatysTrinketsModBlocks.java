package net.timothaty.timothatystrinkets.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import net.timothaty.timothatystrinkets.block.IncenseTrailBlock;
import net.timothaty.timothatystrinkets.block.AromaticOlibanumBlock;
import net.timothaty.timothatystrinkets.block.DebtlordsHeadBlock;
import net.timothaty.timothatystrinkets.block.DamnationAltarBlock;
import net.timothaty.timothatystrinkets.block.DormantSphereBlock;
import net.timothaty.timothatystrinkets.block.EchoSphereBlock;
import net.timothaty.timothatystrinkets.block.BlockofBlightBlock;
import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

public class TimothatysTrinketsModBlocks {
	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(TimothatysTrinketsMod.MODID);
	public static final DeferredBlock<Block> BLOCK_OF_BLIGHT;
	public static final DeferredBlock<Block> DAMNATION_ALTAR;
	public static final DeferredBlock<Block> DORMANT_SPHERE;
	public static final DeferredBlock<Block> ECHO_SPHERE;
	public static final DeferredBlock<Block> DEBTLORDS_HEAD;
	public static final DeferredBlock<IncenseTrailBlock> INCENSE;
	public static final DeferredBlock<AromaticOlibanumBlock> AROMATIC_OLIBANUM;
	static {
		BLOCK_OF_BLIGHT = REGISTRY.register("block_of_blight", BlockofBlightBlock::new);
		DAMNATION_ALTAR = REGISTRY.register("damnation_altar", DamnationAltarBlock::new);
		DORMANT_SPHERE = REGISTRY.register("dormant_sphere", DormantSphereBlock::new);
		ECHO_SPHERE = REGISTRY.register("echo_sphere", EchoSphereBlock::new);
		DEBTLORDS_HEAD = REGISTRY.register("debtlords_head", DebtlordsHeadBlock::new);
		INCENSE = REGISTRY.register("incense", () -> new IncenseTrailBlock(BlockBehaviour.Properties.of()
				.noCollission().noOcclusion().instabreak().replaceable().sound(SoundType.SAND)
				.pushReaction(PushReaction.DESTROY)));
		AROMATIC_OLIBANUM = REGISTRY.register("aromatic_olibanum", () -> new AromaticOlibanumBlock(
				BlockBehaviour.Properties.of().noCollission().noOcclusion().strength(0.1F)
						.randomTicks().sound(SoundType.HONEY_BLOCK).pushReaction(PushReaction.DESTROY)));
	}
	public static final net.neoforged.neoforge.registries.DeferredBlock<Block> DEBTLORDS_WALL_HEAD = REGISTRY.register("debtlords_wall_head",
			() -> new net.minecraft.world.level.block.WallSkullBlock(DebtlordsHeadBlock.DEBTLORD_TYPE, DebtlordsHeadBlock.skullProperties().dropsLike(DEBTLORDS_HEAD.get())));
}
