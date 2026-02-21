package red.gaius.brightbronze.versioned;

import net.minecraft.world.InteractionResult;

/**
 * Abstracts InteractionResult creation across Minecraft versions.
 * 
 * <p>In MC 1.21.10, InteractionResult has nested classes (Success, Fail, etc.).
 * <p>In MC 1.21.1, InteractionResult is an enum with constants.
 */
public interface InteractionResultHelper {
    
    /**
     * Returns the "success" interaction result (client-side success).
     * <p>MC 1.21.10: {@code InteractionResult.SUCCESS}
     * <p>MC 1.21.1: {@code InteractionResult.SUCCESS}
     */
    InteractionResult success();
    
    /**
     * Returns the "consume" interaction result (action consumed, client animation).
     * <p>MC 1.21.10: {@code InteractionResult.CONSUME}
     * <p>MC 1.21.1: {@code InteractionResult.CONSUME}
     */
    InteractionResult consume();
    
    /**
     * Returns the "fail" interaction result (action explicitly failed).
     * <p>MC 1.21.10: {@code InteractionResult.FAIL}
     * <p>MC 1.21.1: {@code InteractionResult.FAIL}
     */
    InteractionResult fail();
    
    /**
     * Returns the "pass" interaction result (no interaction, try next handler).
     * <p>MC 1.21.10: {@code InteractionResult.PASS}
     * <p>MC 1.21.1: {@code InteractionResult.PASS}
     */
    InteractionResult pass();
}
