package dev.avalon.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;

/** Battery depot ("gudang"). Handing out batteries is validated on the server in MissionManager. */
public class DepotBlock extends Block {
    public static final MapCodec<DepotBlock> CODEC = createCodec(DepotBlock::new);

    public DepotBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }
}
