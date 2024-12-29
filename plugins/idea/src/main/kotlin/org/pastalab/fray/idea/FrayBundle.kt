package org.pastalab.fray.idea

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls

object FrayBundle {
  @NonNls
  val BUNDLE = "messages.FrayBundle";
  val INSTANCE = DynamicBundle(FrayBundle::class.java, BUNDLE);
}


//public final class XDebuggerBundle {
//  public static final @NonNls String BUNDLE = "messages.XDebuggerBundle";
//  private static final DynamicBundle INSTANCE = new DynamicBundle(XDebuggerBundle.class, BUNDLE);
//
//  private XDebuggerBundle() {}
//
//  public static @NotNull @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
//    return INSTANCE.getMessage(key, params);
//  }
//
//  public static @NotNull Supplier<@Nls String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
//    return INSTANCE.getLazyMessage(key, params);
//  }
//}
