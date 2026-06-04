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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A message source which prefixes message codes.
 *
 */
public class PrefixingMessageSource
    implements MessageSource
{
  private final MessageSource messages;

  private final String prefix;

  public PrefixingMessageSource(final MessageSource messages, final String prefix) {
    this.messages = checkNotNull(messages);
    this.prefix = checkNotNull(prefix);
  }

  protected String createCode(final String code) {
    checkNotNull(code);
    return prefix + code;
  }

  @Override
  public String getMessage(final String code) {
    return messages.getMessage(createCode(code));
  }

  @Override
  public String getMessage(final String code, final String defaultValue) {
    return messages.getMessage(createCode(code), defaultValue);
  }

  @Override
  public String format(final String code, final Object... args) {
    return messages.format(createCode(code), args);
  }
}
