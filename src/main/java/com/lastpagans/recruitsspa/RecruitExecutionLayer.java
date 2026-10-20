package com.lastpagans.recruitsspa;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.resources.ResourceLocation;

/** Full-body matched-combat override, deliberately above BMC's normal attack layer. */
final class RecruitExecutionLayer extends ModifierLayer<IAnimation> {
    private static final ResourceLocation SWORD_ATTACKER_FALLBACK = ours("execution_sword_attacker_01");
    private static final ResourceLocation SWORD_VICTIM_STAB = ours("execution_sword_victim_bmc_01");
    private static final ResourceLocation SWORD_VICTIM_SLASH = ours("execution_sword_victim_slash_02");
    private static final ResourceLocation SWORD_VICTIM_UPPERCUT = ours("execution_sword_victim_uppercut_03");
    private static final ResourceLocation SWORD_MATCHED_ATTACKER = ours("execution_sword_attacker_matched_04");
    private static final ResourceLocation SWORD_MATCHED_VICTIM = ours("execution_sword_victim_matched_04");
    private static final ResourceLocation SWORD_DUEL_ATTACKER = ours("execution_sword_attacker_duel_05");
    private static final ResourceLocation SWORD_DUEL_VICTIM = ours("execution_sword_victim_duel_05");
    private static final ResourceLocation SPEAR_ATTACKER_FALLBACK = ours("execution_spear_attacker_fallback_01");
    private static final ResourceLocation SPEAR_VICTIM = ours("execution_spear_victim_01");
    private static final ResourceLocation BLANK = spa("blank_loop");

    private final AbstractRecruitEntity recruit;
    private int lastSequence = Integer.MIN_VALUE;
    private boolean executing;

    RecruitExecutionLayer(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void tick() {
        ClientExecutionState.State state = ClientExecutionState.get(recruit.m_19879_());

        if (state != null) {
            if (!executing || state.sequence != lastSequence) {
                start(resolve(state));
                lastSequence = state.sequence;
                executing = true;
            }
        } else if (executing) {
            switchToBlank();
            executing = false;
        }

        super.tick();
    }

    private KeyframeAnimation resolve(ClientExecutionState.State state) {
        if (state.role == ClientExecutionState.Role.VICTIM) {
            return PlayerAnimationRegistry.getAnimation(switch (state.executionType) {
                case ExecutionTypes.SPEAR_CHEST_01 -> SPEAR_VICTIM;
                case ExecutionTypes.SWORD_SLASH_02 -> SWORD_VICTIM_SLASH;
                case ExecutionTypes.SWORD_UPPERCUT_03 -> SWORD_VICTIM_UPPERCUT;
                case ExecutionTypes.SWORD_MATCHED_04 -> SWORD_MATCHED_VICTIM;
                case ExecutionTypes.SWORD_DUEL_05 -> SWORD_DUEL_VICTIM;
                default -> SWORD_VICTIM_STAB;
            });
        }

        return switch (state.executionType) {
            case ExecutionTypes.SPEAR_CHEST_01 -> firstNonNull(
                    BetterCombatAnimationBridge.get("bettercombat:two_handed_stab_right"),
                    PlayerAnimationRegistry.getAnimation(SPEAR_ATTACKER_FALLBACK));
            case ExecutionTypes.SWORD_SLASH_02 -> firstNonNull(
                    BetterCombatAnimationBridge.get("bettercombat:one_handed_slash_horizontal_right"),
                    PlayerAnimationRegistry.getAnimation(SWORD_ATTACKER_FALLBACK));
            case ExecutionTypes.SWORD_UPPERCUT_03 -> firstNonNull(
                    BetterCombatAnimationBridge.get("bettercombat:one_handed_uppercut_right"),
                    PlayerAnimationRegistry.getAnimation(SWORD_ATTACKER_FALLBACK));
            case ExecutionTypes.SWORD_MATCHED_04 -> PlayerAnimationRegistry.getAnimation(SWORD_MATCHED_ATTACKER);
            case ExecutionTypes.SWORD_DUEL_05 -> PlayerAnimationRegistry.getAnimation(SWORD_DUEL_ATTACKER);
            default -> firstNonNull(
                    BetterCombatAnimationBridge.get("bettercombat:one_handed_stab"),
                    PlayerAnimationRegistry.getAnimation(SWORD_ATTACKER_FALLBACK));
        };
    }

    private static KeyframeAnimation firstNonNull(KeyframeAnimation preferred, KeyframeAnimation fallback) {
        return preferred != null ? preferred : fallback;
    }

    private void start(KeyframeAnimation data) {
        if (data == null) {
            return;
        }
        KeyframeAnimationPlayer player = new KeyframeAnimationPlayer(data);
        if (getAnimation() == null) {
            setAnimation(player);
        } else {
            replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(1, Ease.INOUTSINE), player);
        }
    }

    private void switchToBlank() {
        KeyframeAnimation data = PlayerAnimationRegistry.getAnimation(BLANK);
        if (data == null) {
            setAnimation(null);
            return;
        }
        KeyframeAnimationPlayer player = new KeyframeAnimationPlayer(data);
        if (getAnimation() == null) {
            setAnimation(player);
        } else {
            replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(3, Ease.INOUTSINE), player);
        }
    }

    private static ResourceLocation ours(String path) {
        return new ResourceLocation(RecruitsSeriousAnimations.MOD_ID, path);
    }

    private static ResourceLocation spa(String path) {
        return new ResourceLocation("seriousplayeranimations", path);
    }
}
