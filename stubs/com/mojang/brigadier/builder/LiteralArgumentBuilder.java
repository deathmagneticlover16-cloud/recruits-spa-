package com.mojang.brigadier.builder;
import com.mojang.brigadier.Command;
public class LiteralArgumentBuilder<S> {
    public LiteralArgumentBuilder<S> then(LiteralArgumentBuilder<S> child) { return this; }
    public LiteralArgumentBuilder<S> executes(Command<S> command) { return this; }
}
