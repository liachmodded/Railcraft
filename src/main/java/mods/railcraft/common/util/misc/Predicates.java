/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2017
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/

package mods.railcraft.common.util.misc;

import mods.railcraft.common.util.collections.StackKey;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created by CovertJaguar on 9/9/2016 for Railcraft.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public final class Predicates {

    public static <T> Predicate<T> instanceOf(Class<? extends T> clazz) {
        return clazz::isInstance;
    }

    public static <T> Predicate<T> notInstanceOf(Class<? extends T> clazz) {
        return Predicates.<T>instanceOf(clazz).negate();
    }

    public static <T> Predicate<T> distinct(Function<? super T, ?> keyFunction) {
        Set<Object> seen = Collections.newSetFromMap(new ConcurrentHashMap<>());
        return t -> seen.add(keyFunction.apply(t));
    }

    public static Predicate<ItemStack> distinctStack() {
        return distinct(StackKey::make);
    }

    public static <T> Predicate<T> alwaysTrue() {
        return t -> true; // This returns the same instance on different method calls
    }

    public static <T> Predicate<T> alwaysFalse() {
        return t -> false;
    }
}
