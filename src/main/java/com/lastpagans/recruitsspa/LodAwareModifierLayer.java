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
    private final AbstractRecruitEntity lodRecruit;
    private final Map<String, Vec3f> lastTransforms = new HashMap<>();

    protected LodAwareModifierLayer(AbstractRecruitEntity recruit) {
        super();
        lodRecruit = recruit;
    }

    @Override
    public Vec3f get3DTransform(String partName, TransformType type, float tickDelta, Vec3f value0) {
        String key = partName + '\u0000' + type.name();
        if (EmfAnimationLodCompat.shouldSkipTick(lodRecruit)) {
            return lastTransforms.getOrDefault(key, value0);
        }
        Vec3f value = super.get3DTransform(partName, type, tickDelta, value0);
        lastTransforms.put(key, value);
        return value;
    }
}
