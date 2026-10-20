package com.talhanation.recruits.entities;
public abstract class AbstractRecruitEntity extends net.minecraft.world.entity.Mob {
  public boolean getFleeing() { return false; }
  public boolean getShouldMovePos() { return false; }
  public boolean getShouldHoldPos() { return false; }
}
