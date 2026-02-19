package red.gaius.brightbronze.versioned.mc12110;

import net.minecraft.world.InteractionResult;
import red.gaius.brightbronze.versioned.InteractionResultHelper;

/**
 * MC 1.21.10 implementation of InteractionResultHelper.
 */
public class InteractionResultHelperImpl implements InteractionResultHelper {
    
    @Override
    public InteractionResult success() {
        return InteractionResult.SUCCESS;
    }
    
    @Override
    public InteractionResult consume() {
        return InteractionResult.CONSUME;
    }
    
    @Override
    public InteractionResult fail() {
        return InteractionResult.FAIL;
    }
    
    @Override
    public InteractionResult pass() {
        return InteractionResult.PASS;
    }
}
