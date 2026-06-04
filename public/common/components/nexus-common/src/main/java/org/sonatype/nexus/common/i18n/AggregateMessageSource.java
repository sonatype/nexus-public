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

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A message source which aggregates messages sources in order.
 *
 */
public class AggregateMessageSource
    implements MessageSource
{
  private static final Logger log = LoggerFactory.getLogger(AggregateMessageSource.class);

  private final List<MessageSource> sources = Lists.newArrayList();

  public AggregateMessageSource(final List<MessageSource> sources) {
    checkNotNull(sources);
    this.sources.addAll(sources);
  }

  public AggregateMessageSource(final MessageSource... sources) {
    this(Arrays.asList(sources));
  }

  public List<MessageSource> getSources() {
    return sources;
  }

  @Override
  public String getMessage(final String code) {
    String result = null;

    for (MessageSource source : sources) {
      try {
        result = source.getMessage(code);
        if (result != null) {
          break;
        }
      }
      catch (ResourceNotFoundException e) {
        log.trace(e.toString(), e);
      }
    }

    if (result == null) {
      throw new ResourceNotFoundException(code);
    }

    return result;
  }

  @Override
  public String getMessage(final String code, final String defaultValue) {
    try {
      return getMessage(code);
    }
    catch (ResourceNotFoundException e) {
      return defaultValue;
    }
  }

  @Override
  public String format(final String code, final Object... args) {
    String result = null;

    for (MessageSource source : sources) {
      try {
        result = source.format(code, args);
        if (result != null) {
          break;
        }
      }
      catch (ResourceNotFoundException e) {
        log.trace(e.toString(), e);
      }
    }

    if (result == null) {
      throw new ResourceNotFoundException(code);
    }

    return result;
  }
}
