package org.brahypno.esotericismtinker.smeltery.block.entity.component;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.brahypno.esotericismtinker.smeltery.EsotericismTinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryComponentBlockEntity;

public class TransmuteComponentBlockEntity extends SmelteryComponentBlockEntity {
    public TransmuteComponentBlockEntity(BlockPos pos, BlockState state) {
        super(EsotericismTinkerSmeltery.transmuteComponent.get(), pos, state);
    }
}
