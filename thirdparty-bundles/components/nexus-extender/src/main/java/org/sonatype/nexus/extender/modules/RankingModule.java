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
package org.sonatype.nexus.extender.modules;

import java.util.concurrent.atomic.AtomicInteger;

import org.sonatype.nexus.common.log.LogManager;

import com.google.inject.AbstractModule;
import org.eclipse.sisu.inject.DefaultRankingFunction;
import org.eclipse.sisu.inject.RankingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides ranking policy that gives more recent plugins priority over older plugins.
 * 
 * @since 3.0
 */
public class RankingModule
    extends AbstractModule
{
  private final AtomicInteger rank = new AtomicInteger(1);

  @Override
  protected void configure() {
    bind(RankingFunction.class).toInstance(new DefaultRankingFunction(rank.incrementAndGet()));

    // TEMP: placeholder to satisfy a few types that expect to inject loggers
    bind(Logger.class).toInstance(LoggerFactory.getLogger(LogManager.class));
  }
}
