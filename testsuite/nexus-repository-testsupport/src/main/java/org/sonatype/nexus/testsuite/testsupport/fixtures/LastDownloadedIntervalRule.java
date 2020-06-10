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
package org.sonatype.nexus.testsuite.testsupport.fixtures;

import java.time.Duration;

import javax.inject.Provider;

import org.sonatype.nexus.repository.capability.GlobalRepositorySettings;

import org.junit.rules.ExternalResource;

public class LastDownloadedIntervalRule
    extends ExternalResource
{
  private final Provider<GlobalRepositorySettings> repositorySettings;

  private Duration lastDownloadedInterval;

  public LastDownloadedIntervalRule(final Provider<GlobalRepositorySettings> repositorySettings) {
    this.repositorySettings = repositorySettings;
  }

  @Override
  protected void before() {
    lastDownloadedInterval = repositorySettings.get().getLastDownloadedInterval();
  }

  @Override
  protected void after() {
    repositorySettings.get().setLastDownloadedInterval(lastDownloadedInterval);
  }

  public void setLastDownloadedInterval(final Duration duration) {
    repositorySettings.get().setLastDownloadedInterval(duration);
  }
}
