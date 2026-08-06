package net.timothaty.timothatystrinkets.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.material.PushReaction;

public class DebtlordsHeadBlock extends SkullBlock {
	public static final SkullBlock.Type DEBTLORD_TYPE = new DebtlordSkullType("debtlord");

	public DebtlordsHeadBlock() {
		super(DEBTLORD_TYPE, skullProperties());
	}

	public static BlockBehaviour.Properties skullProperties() {
		return BlockBehaviour.Properties.of()
				.instrument(NoteBlockInstrument.CUSTOM_HEAD)
				.strength(1.0F)
				.pushReaction(PushReaction.DESTROY);
	}

	private record DebtlordSkullType(String name) implements SkullBlock.Type {
		private DebtlordSkullType {
			SkullBlock.Type.TYPES.put(name, this);
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}
}
