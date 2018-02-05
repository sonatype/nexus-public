/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.quartz.internal.orient;

import java.lang.reflect.Constructor;

import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.ReflectionFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper to create object instances for serialisation.
 *
 * @since 3.0
 */
class InstanceCreator
{
  private static final Logger log = LoggerFactory.getLogger(InstanceCreator.class);

  private final UnsafeStrategy unsafeStrategy;

  public InstanceCreator() {
    this.unsafeStrategy = UnsafeStrategy.create();
    log.trace("Unsafe strategy: {}", unsafeStrategy);
  }

  /**
   * Construct a new instance from the given type.
   */
  public <T> T newInstance(final Class<T> type) throws Exception {
    checkNotNull(type);
    try {
      // first try to use default mechanism with declared no-args constructor
      Constructor<T> ctor = type.getDeclaredConstructor();
      ctor.setAccessible(true);
      log.trace("New instance: {}", type);
      return ctor.newInstance();
    }
    catch (Exception e) {
      log.trace("Failed to create instance of {} using standard constructor; trying unsafe strategy", type, e);
      // otherwise fallback to unsafe mechanisms
      return unsafeStrategy.newInstance(type);
    }
  }

  // TODO: If use of ReflectionFactory is sane, could probably simplify this further
  // TODO: For the moment leaving asis for reference of sun.misc.Unsafe impl

  /**
   * Strategy for constructing object instances using unsafe mechanisms.
   */
  @IgnoreJRERequirement // we access sun.* bits here
  private abstract static class UnsafeStrategy
  {
    protected static final Logger log = LoggerFactory.getLogger(UnsafeStrategy.class);

    private final String name;

    public UnsafeStrategy(final String name) {
      this.name = name;
    }

    public abstract <T> T newInstance(final Class<T> type) throws Exception;

    public String toString() {
      return name;
    }

    public static UnsafeStrategy create() {
      // NOTE: sun.misc.Unsafe is going away (or otherwise unusable) in Java-9, so ReflectionFactory method may be better
      // try sun.misc.Unsafe
      //try {
      //  Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
      //  Field field = unsafeClass.getDeclaredField("theUnsafe");
      //  field.setAccessible(true);
      //  final Object unsafe = field.get(null);
      //  final Method factory = unsafeClass.getMethod("allocateInstance", Class.class);
      //
      //  return new UnsafeStrategy("sun.misc.Unsafe")
      //  {
      //    @Override
      //    public <T> T newInstance(final Class<T> type) throws Exception {
      //      log.trace("New instance: {}", type);
      //      Object instance = factory.invoke(unsafe, type);
      //      return type.cast(instance);
      //    }
      //  };
      //}
      //catch (Exception e) {
      //  // ignore
      //}

      // try sun.reflect.ReflectionFactory
      try {
        final ReflectionFactory rf = ReflectionFactory.getReflectionFactory();

        return new UnsafeStrategy("sun.reflect.ReflectionFactory")
        {
          @Override
          public <T> T newInstance(final Class<T> type) throws Exception {
            log.trace("New instance: {}", type);
            Constructor ctor = rf.newConstructorForSerialization(type, Object.class.getDeclaredConstructor());
            ctor.setAccessible(true);
            Object instance = ctor.newInstance();
            return type.cast(instance);
          }
        };
      }
      catch (Exception e) {
        log.trace("Failed to resolve ReflectionFactory", e);
      }

      // unsupported
      return new UnsafeStrategy("unsupported")
      {
        @Override
        public <T> T newInstance(final Class<T> type) throws Exception {
          throw new UnsupportedOperationException("Unable to create instance for: " + type);
        }
      };
    }
  }
}
