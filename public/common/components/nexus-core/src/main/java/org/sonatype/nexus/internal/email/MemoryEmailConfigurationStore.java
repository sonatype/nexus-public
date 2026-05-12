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
package org.sonatype.nexus.internal.email;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import jakarta.inject.Singleton;

import org.sonatype.nexus.email.EmailConfiguration;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * In-memory {@link EmailConfigurationStore}.
 *
 * @since 3.0
 */
@Component
@Qualifier("memory")
@Singleton
@Priority(Integer.MIN_VALUE)
@Order(Ordered.LOWEST_PRECEDENCE)
@VisibleForTesting
public class MemoryEmailConfigurationStore
    implements EmailConfigurationStore
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private EmailConfiguration model;

  @Nullable
  @Override
  public synchronized EmailConfiguration load() {
    return model;
  }

  @Override
  public synchronized void save(final EmailConfiguration configuration) {
    this.model = checkNotNull(configuration);
  }

  @Override
  public EmailConfiguration newConfiguration() {
    return new MemoryEmailConfiguration();
  }
}
