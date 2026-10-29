package com.lastpagans.recruitsspa;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.util.Vec3f;

import java.util.HashMap;
import java.util.Map;

/** Freezes evaluated transforms on EMF LOD skip frames as well as layer ticks. */
abstract class LodAwareModifierLayer extends ModifierLayer<IAnimation> {
    private static final int TRANSFORM_TYPE_COUNT = TransformType.values().length;
    private final AbstractRecruitEntity lodRecruit;
    private final boolean lodEnabled;
    private Map<String, Vec3f[]> lastTransforms;

    protected LodAwareModifierLayer(AbstractRecruitEntity recruit) {
        super();
        lodRecruit = recruit;
        lodEnabled = EmfAnimationLodCompat.isEnabled();
    }

    @Override
    public Vec3f get3DTransform(String partName, TransformType type, float tickDelta, Vec3f value0) {
        if (!lodEnabled) {
            return super.get3DTransform(partName, type, tickDelta, value0);
        }

        if (lastTransforms == null) {
            lastTransforms = new HashMap<>();
        }
        Vec3f[] transforms = lastTransforms.computeIfAbsent(
                partName,
                ignored -> new Vec3f[TRANSFORM_TYPE_COUNT]
        );
        int transformIndex = type.ordinal();
        if (EmfAnimationLodCompat.shouldSkipTick(lodRecruit)) {
            Vec3f cached = transforms[transformIndex];
            return cached != null ? cached : value0;
        }
        Vec3f value = super.get3DTransform(partName, type, tickDelta, value0);
        transforms[transformIndex] = value;
        return value;
    }
}
