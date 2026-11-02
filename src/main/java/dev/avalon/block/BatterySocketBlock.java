package dev.avalon.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;

/** One of the four sockets of a pillar. Interaction is handled (and validated) on the server in MissionManager. */
public class BatterySocketBlock extends Block {
    public static final MapCodec<BatterySocketBlock> CODEC = createCodec(BatterySocketBlock::new);
    public static final BooleanProperty INSTALLED = BooleanProperty.of("installed");

    public BatterySocketBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(INSTALLED, false));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(INSTALLED);
    }
}
