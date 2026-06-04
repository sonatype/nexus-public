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
package org.sonatype.nexus.common.i18n;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.sonatype.nexus.common.i18n.MessageBundle.DefaultMessage;
import org.sonatype.nexus.common.i18n.MessageBundle.Key;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Internationalization access.
 *
 */
public class I18N
{
  private static final Logger log = LoggerFactory.getLogger(I18N.class);

  @VisibleForTesting
  static final String MISSING_MESSAGE_FORMAT = "ERROR_MISSING_MESSAGE[%s]"; // NON-NLS

  private I18N() {
    super();
  }

  // TODO: Need to hook up dynamic access to locale so this stuff can be useful in reality
  // TODO: Need to abstract access to the current threads Locale (could be gotten from servlet api, or from local jvm
  // etc)
  // TODO: Need to dynamically build/cache/whatever the resource bundles, not sure how expensive it is to look it up
  // each time

  /**
   * Returns a {@link MessageSource} for the given types.
   *
   * @param types One or more classes
   * @return {@link MessageSource} instance; never null
   */
  public static MessageSource of(final Class<?>... types) {
    checkNotNull(types);
    checkArgument(types.length > 0);
    return new ResourceBundleMessageSource().add(/* bundle is not required */ false, types);
  }

  /**
   * Returns a proxy to the given {@link MessageBundle} type.
   *
   * @return {@link MessageBundle} proxy; never null
   */
  @SuppressWarnings({"unchecked"})
  public static <T extends MessageBundle> T create(final Class<T> type) {
    checkNotNull(type);
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, new Handler(type));
  }

  /**
   * Proxy invocation handler to convert method calls into message lookup/format.
   */
  private static class Handler
      implements InvocationHandler
  {
    private final Class<? extends MessageBundle> type;

    private final MessageSource messages;

    public Handler(final Class<? extends MessageBundle> type) {
      this.type = checkNotNull(type);
      this.messages = I18N.of(type);
    }

    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      if (method.getDeclaringClass() == Object.class) {
        return method.invoke(this, args);
      }
      else if (method.getReturnType() != String.class) {
        throw new Error("Illegal MessageBundle method: " + method);
      }

      // TODO: Optimize/cache

      String key = getKey(method);
      String format = getFormat(key);

      if (format == null) {
        DefaultMessage defaultMessage = method.getAnnotation(DefaultMessage.class);
        if (defaultMessage != null) {
          format = defaultMessage.value();
        }
      }

      if (format == null) {
        log.warn("Missing message for: {}, key: {}", type, key);
        return String.format(MISSING_MESSAGE_FORMAT, key);
      }

      if (args != null) {
        // TODO: Support annotation-configuration of formatting method?
        return String.format(format, args);
      }
      return format;
    }

    private String getFormat(final String key) {
      try {
        return messages.getMessage(key);
      }
      catch (ResourceNotFoundException e) {
        log.trace("Missing resource for: {}, key: {}", type, key);
        return null;
      }
    }

    private String getKey(final Method method) {
      Key key = method.getAnnotation(Key.class);
      if (key != null) {
        return key.value();
      }
      return method.getName();
    }
  }
}
