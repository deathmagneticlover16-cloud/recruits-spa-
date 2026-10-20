package net.minecraftforge.event.entity.living;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
public class LivingDamageEvent {
    public LivingEntity getEntity() { return null; }
    public DamageSource getSource() { return null; }
    public float getAmount() { return 0.0F; }
    public void setCanceled(boolean canceled) {}
    public boolean isCanceled() { return false; }
}
