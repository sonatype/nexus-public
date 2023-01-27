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
package org.sonatype.nexus.repository.task;

/**
 * Tracks deletion progress.
 *
 * @since 3.25
 */
public class DeletionProgress
{
  private long componentCount = 0L;

  private long assetCount = 0L;

  private boolean failed;

  private int attempts = 0;

  private int retryLimit = 0;

  public DeletionProgress() {
    // use defaults
  }

  public DeletionProgress(final int retryLimit) {
    this.retryLimit = retryLimit;
  }

  public long getComponentCount() {
    return componentCount;
  }

  public void addComponentCount(final long count) {
    this.componentCount += count;
  }

  public long getAssetCount() {
    return assetCount;
  }

  public void addAssetCount(final long count) {
    this.assetCount += count;
  }

  public boolean isFailed() {
    return failed;
  }

  public void setFailed(final boolean completed) {
    this.failed = completed;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(final int attempts) {
    this.attempts = attempts;
  }

  public void update(final DeletionProgress progress) {
    failed = progress.isFailed();
    componentCount += progress.getComponentCount();
    assetCount += progress.getAssetCount();
    if (progress.isFailed()) {
      attempts++;
    }
  }

  public boolean isFinished() {
    return !isFailed() || getAttempts() >= retryLimit;
  }
}
