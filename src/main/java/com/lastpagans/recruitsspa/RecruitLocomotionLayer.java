package com.lastpagans.recruitsspa;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;

/**
 * Base full-body locomotion for human Recruits.
 *
 * V0.3 keeps the proven 0.1.3 walk/run hysteresis and adds swimming, falling,
 * horse riding, and a blank passenger state for non-horse vehicles.
 */
final class RecruitLocomotionLayer extends LodAwareModifierLayer {
    private static final ResourceLocation IDLE_ANIM = spa("idle_standing");
    private static final ResourceLocation WALK_ANIM = spa("walking");
    private static final ResourceLocation RUN_ANIM = spa("running");
    private static final ResourceLocation FALL_ANIM = spa("falling");
    private static final ResourceLocation SWIM_ANIM = spa("swimming");
    private static final ResourceLocation WATER_IDLE_ANIM = spa("idle_in_water");
    private static final ResourceLocation WATER_MOVE_ANIM = spa("forward_in_water");
    private static final ResourceLocation HORSE_IDLE_ANIM = spa("horse_idle");
    private static final ResourceLocation HORSE_RUN_ANIM = ours("horse_running_recruits");
    private static final ResourceLocation BLANK_ANIM = spa("blank_loop");

    private static final double RUN_ENTER_THRESHOLD = 0.23D;
    private static final double RUN_EXIT_THRESHOLD = 0.16D;
    private static final int RUN_EXIT_GRACE_TICKS = 8;
    private static final double IDLE_EPSILON = 0.004D;
    private static final double MOUNT_MOVE_EPSILON = 0.012D;
    private static final double WATER_MOVE_EPSILON = 0.010D;
    private static final double FALL_SPEED_THRESHOLD = -0.60D;
    private static final double SPEED_SMOOTHING = 0.30D;

    private final AbstractRecruitEntity recruit;
    private final SpeedModifier speedModifier;

    private State state;
    private double lastX;
    private double lastY;
    private double lastZ;
    private boolean haveLastPosition;
    private boolean haveSmoothedSpeed;
    private double smoothedSpeed;
    private int runExitTicks;

    RecruitLocomotionLayer(AbstractRecruitEntity recruit) {
        super(recruit);
        this.recruit = recruit;
        this.speedModifier = new SpeedModifier(1.0F);
        addModifierLast(this.speedModifier);
    }

    @Override
    public boolean isActive() {
        // Required for bootstrap: AnimationStack does not tick inactive layers.
        return true;
    }

    @Override
    public Vec3f get3DTransform(String partName, TransformType type, float tickDelta, Vec3f value0) {
        // Recruits computes a target-aware vanilla BOW_AND_ARROW pose before
        // Mob Player Animator applies this layer. SPA locomotion also contains
        // arm/head keys, which would otherwise overwrite that correct aim pose.
        // While actively drawing a bow, pass those parts through untouched so
        // Recruits keeps control of target yaw/pitch while SPA still animates
        // the torso and legs underneath. No entity rotation is changed here.
        // Betsy Ross sets its own standard-bearing arm pose in HumanoidModel.
        // Preserve it exactly instead of letting SPA locomotion overwrite the arms.
        if (BetsyRossCompat.isArmPart(partName) && BetsyRossCompat.isHoldingStandard(recruit)) {
            return value0;
        }

        if (isActivelyDrawingBow() && isBowAimPart(partName)) {
            return value0;
        }
        return super.get3DTransform(partName, type, tickDelta, value0);
    }

    private boolean isActivelyDrawingBow() {
        if (!recruit.isUsingItem()) {
            return false;
        }
        ItemStack active = recruit.getUseItem();
        if (active == null || active.isEmpty()) {
            return false;
        }
        UseAnim useAnim = active.getUseAnimation();
        return useAnim != null && "BOW".equals(useAnim.name());
    }

    private static boolean isBowAimPart(String partName) {
        return "head".equals(partName)
                || "leftArm".equals(partName)
                || "rightArm".equals(partName)
                || "left_arm".equals(partName)
                || "right_arm".equals(partName);
    }

    @Override
    public void tick() {
        Vec3 pos = recruit.position();
        double instantSpeed = 0.0D;
        double verticalSpeed = 0.0D;

        if (haveLastPosition) {
            double dx = pos.x - lastX;
            double dz = pos.z - lastZ;
            instantSpeed = Math.sqrt(dx * dx + dz * dz);
            verticalSpeed = pos.y - lastY;
        } else {
            haveLastPosition = true;
        }

        lastX = pos.x;
        lastY = pos.y;
        lastZ = pos.z;

        if (!haveSmoothedSpeed) {
            smoothedSpeed = instantSpeed;
            haveSmoothedSpeed = true;
        } else {
            smoothedSpeed += (instantSpeed - smoothedSpeed) * SPEED_SMOOTHING;
        }

        State next = chooseState(instantSpeed, smoothedSpeed, verticalSpeed);
        speedModifier.speed = animationSpeed(next, smoothedSpeed);

        if (next != state || getAnimation() == null || !getAnimation().isActive()) {
            switchAnimation(next);
        }

        super.tick();
    }

    private State chooseState(double instantSpeed, double filteredSpeed, double verticalSpeed) {
        if (recruit.isPassenger()) {
            runExitTicks = 0;
            Entity vehicle = recruit.getVehicle();
            if (isHorseLike(vehicle)) {
                // Icy animates the horse and applies its full gait/turn/jump motion
                // to every rider at render time. Keep a stable riding pose here so
                // our old running bob does not double those authored movements.
                if (EntityCompat.isIcysHorse(vehicle)) {
                    return State.HORSE_IDLE;
                }
                return (instantSpeed > MOUNT_MOVE_EPSILON || filteredSpeed > MOUNT_MOVE_EPSILON)
                        ? State.HORSE_RUN : State.HORSE_IDLE;
            }
            // Do not make a recruit walk/run while seated in a boat, ship, cart, etc.
            return State.PASSENGER;
        }

        if (recruit.isFallFlying()) {
            runExitTicks = 0;
            return State.SWIM;
        }

        if (recruit.isInWater()) {
            runExitTicks = 0;
            return (instantSpeed > WATER_MOVE_EPSILON || filteredSpeed > WATER_MOVE_EPSILON)
                    ? State.WATER_MOVE : State.WATER_IDLE;
        }

        if (!recruit.onGround() && verticalSpeed < FALL_SPEED_THRESHOLD) {
            runExitTicks = 0;
            return State.FALL;
        }

        // Preserve the 0.1.3 anti-jitter run hysteresis.
        if (state == State.RUN) {
            if (instantSpeed >= RUN_EXIT_THRESHOLD || filteredSpeed >= RUN_EXIT_THRESHOLD) {
                runExitTicks = 0;
                return State.RUN;
            }
            if (++runExitTicks < RUN_EXIT_GRACE_TICKS) {
                return State.RUN;
            }
        } else {
            runExitTicks = 0;
        }

        if (instantSpeed >= RUN_ENTER_THRESHOLD || filteredSpeed >= RUN_ENTER_THRESHOLD) {
            runExitTicks = 0;
            return State.RUN;
        }

        if (filteredSpeed <= IDLE_EPSILON) {
            return State.IDLE;
        }
        return State.WALK;
    }

    private float animationSpeed(State target, double moveSpeed) {
        return switch (target) {
            case WALK -> (float) Math.max(2.0D, 40.90909090909091D * moveSpeed);
            case RUN -> (float) Math.max(1.0D, 10.714285714285714D * moveSpeed);
            default -> 1.0F;
        };
    }

    private void switchAnimation(State target) {
        KeyframeAnimation data = PlayerAnimationRegistry.getAnimation(target.animation);
        if (data == null) {
            return;
        }

        KeyframeAnimationPlayer player = new KeyframeAnimationPlayer(data);
        int fadeTicks = switch (target) {
            case IDLE -> 10;
            case WALK -> 7;
            case RUN -> 2;
            case HORSE_IDLE -> 5;
            case HORSE_RUN -> 2;
            case SWIM, WATER_IDLE, WATER_MOVE -> 5;
            case FALL -> 6;
            case PASSENGER -> 2;
        };

        if (getAnimation() == null) {
            setAnimation(player);
        } else {
            replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(fadeTicks, Ease.INOUTSINE), player);
        }
        state = target;
    }

    private static boolean isHorseLike(Entity vehicle) {
        return EntityCompat.isHorseLike(vehicle);
    }

    private static ResourceLocation spa(String path) {
        return new ResourceLocation("seriousplayeranimations", path);
    }

    private static ResourceLocation ours(String path) {
        return new ResourceLocation(RecruitsSeriousAnimations.MOD_ID, path);
    }

    private enum State {
        IDLE(IDLE_ANIM),
        WALK(WALK_ANIM),
        RUN(RUN_ANIM),
        FALL(FALL_ANIM),
        SWIM(SWIM_ANIM),
        WATER_IDLE(WATER_IDLE_ANIM),
        WATER_MOVE(WATER_MOVE_ANIM),
        HORSE_IDLE(HORSE_IDLE_ANIM),
        HORSE_RUN(HORSE_RUN_ANIM),
        PASSENGER(BLANK_ANIM);

        private final ResourceLocation animation;

        State(ResourceLocation animation) {
            this.animation = animation;
        }
    }
}
