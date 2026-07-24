/*
 * This file is part of architectury.
 * Copyright (C) 2020, 2021, 2022 architectury
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package dev.architectury.fluid;

import com.mojang.serialization.Codec;
import dev.architectury.hooks.fluid.FluidStackTemplateHooks;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FluidStackTemplate implements DataComponentHolder {
    private static final FluidStackTemplateAdapter<Object, Object> ADAPTER = adapt(FluidStackTemplate::getValue, FluidStackTemplate::new);
    public static final Codec<FluidStackTemplate> CODEC = ADAPTER.codec();
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStackTemplate> STREAM_CODEC = ADAPTER.streamCodec();
    
    private final Object value;
    
    private FluidStackTemplate(Supplier<Fluid> fluid, long amount, DataComponentPatch patch) {
        this(ADAPTER.of(fluid, amount, patch));
    }
    
    private FluidStackTemplate(Object value) {
        this.value = Objects.requireNonNull(value);
    }
    
    private Object getValue() {
        return value;
    }
    
    @ExpectPlatform
    private static FluidStackTemplateAdapter<Object, Object> adapt(Function<FluidStackTemplate, Object> toValue, Function<Object, FluidStackTemplate> fromValue) {
        throw new AssertionError();
    }
    
    @ApiStatus.Internal
    public interface FluidStackTemplateAdapter<T, R> {
        T of(Supplier<Fluid> fluid, long amount, @Nullable DataComponentPatch patch);
        
        Holder<Fluid> fluid(T object);
        
        long amount(T object);
        
        T withAmount(T object, int amount);
        
        DataComponentMap components(T value);
        
        DataComponentPatch patch(T value);
        
        R apply(T value, DataComponentPatch patch);
        
        R apply(T value, int amount, DataComponentPatch patch);
        
        @Nullable <D> D get(T value, DataComponentType<D> type);
        
        @Nullable <D> D get(T value, Supplier<DataComponentType<D>> type);
        
        T copy(T value);
        
        int hashCode(T value);
        
        Codec<FluidStackTemplate> codec();
        
        StreamCodec<RegistryFriendlyByteBuf, FluidStackTemplate> streamCodec();
    }
    
    public static Object of(FluidStackTemplate stack) {
        return new FluidStackTemplate(stack);
    }
    
    public static FluidStackTemplate of(Holder<Fluid> fluid, long amount) {
        return new FluidStackTemplate(fluid::value, amount, DataComponentPatch.EMPTY);
    }
    
    public static FluidStackTemplate of(Holder<Fluid> fluid, long amount, DataComponentPatch patch) {
        return new FluidStackTemplate(fluid::value, amount, patch);
    }
    
    public FluidStack create() {
        return FluidStack.create(fluid().value(), amount(), getPatch());
    }
    
    public Holder<Fluid> fluid() {
        return ADAPTER.fluid(value);
    }
    
    
    public long amount() {
        return ADAPTER.amount(value);
    }
    
    
    @Override
    public DataComponentMap getComponents() {
        return ADAPTER.components(value);
    }
    
    public DataComponentPatch getPatch() {
        return ADAPTER.patch(value);
    }
    
    public void apply(DataComponentPatch patch) {
        ADAPTER.apply(value, patch);
    }
    
    public void apply(int amount, DataComponentPatch patch) {
        ADAPTER.apply(value,amount, patch);
    }
    
    public String getTranslationKey() {
        return FluidStackTemplateHooks.getTranslationKey(this);
    }
    
    public FluidStackTemplate copy() {
        return new FluidStackTemplate(ADAPTER.copy(value));
    }
    
    @Override
    public int hashCode() {
        return ADAPTER.hashCode(value);
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FluidStackTemplate)) {
            return false;
        }
        return isFluidStackEqual((FluidStackTemplate) o);
    }
    
    public boolean isFluidStackEqual(FluidStackTemplate other) {
        return fluid() == other.fluid() && amount() == other.amount() && isComponentEqual(other);
    }
    
    public boolean isFluidEqual(FluidStackTemplate other) {
        return fluid() == other.fluid();
    }
    
    public boolean isComponentEqual(FluidStackTemplate other) {
        var patch = getComponents();
        var otherPatch = other.getComponents();
        return Objects.equals(patch, otherPatch);
    }
    
    public static FluidStackTemplate read(RegistryFriendlyByteBuf buf) {
        return FluidStackTemplateHooks.read(buf);
    }
    
    public static Optional<FluidStackTemplate> read(HolderLookup.Provider provider, Tag tag) {
        return FluidStackTemplateHooks.read(provider, tag);
    }
    
    public void write(RegistryFriendlyByteBuf buf) {
        FluidStackTemplateHooks.write(this, buf);
    }
    
    public Tag write(HolderLookup.Provider provider, Tag tag) {
        return FluidStackTemplateHooks.write(provider, this, tag);
    }
    
    public FluidStackTemplate copyWithAmount(long amount) {
        return new FluidStackTemplate(() -> fluid().value(), amount, getPatch());
    }
    
    @ApiStatus.Internal
    public static void init() {
        // classloading my beloved 😍
        // please don't use this by the way
    }
}
