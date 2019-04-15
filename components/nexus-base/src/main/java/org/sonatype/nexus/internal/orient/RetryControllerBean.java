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
package org.sonatype.nexus.internal.orient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.jmx.reflect.ManagedAttribute;
import org.sonatype.nexus.jmx.reflect.ManagedObject;
import org.sonatype.nexus.transaction.RetryController;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * JMX management of the {@link RetryController}.
 *
 * @since 3.16
 */
@Named
@Singleton
@ManagedObject
public class RetryControllerBean
{
  private final RetryController retryController;

  @Inject
  public RetryControllerBean(final RetryController retryController) {
    this.retryController = checkNotNull(retryController);
  }

  @ManagedAttribute
  public int getRetryLimit() {
    return retryController.getRetryLimit();
  }

  @ManagedAttribute
  public void setRetryLimit(final int retryLimit) {
    retryController.setRetryLimit(retryLimit);
  }

  @ManagedAttribute
  public int getMinSlots() {
    return retryController.getMinSlots();
  }

  @ManagedAttribute
  public void setMinSlots(final int minSlots) {
    retryController.setMinSlots(minSlots);
  }

  @ManagedAttribute
  public int getMaxSlots() {
    return retryController.getMaxSlots();
  }

  @ManagedAttribute
  public void setMaxSlots(final int maxSlots) {
    retryController.setMaxSlots(maxSlots);
  }

  @ManagedAttribute
  public int getMinorDelayMillis() {
    return retryController.getMinorDelayMillis();
  }

  @ManagedAttribute
  public void setMinorDelayMillis(final int minorDelayMillis) {
    retryController.setMinorDelayMillis(minorDelayMillis);
  }

  @ManagedAttribute
  public int getMajorDelayMillis() {
    return retryController.getMajorDelayMillis();
  }

  @ManagedAttribute
  public void setMajorDelayMillis(final int majorDelayMillis) {
    retryController.setMajorDelayMillis(majorDelayMillis);
  }

  @ManagedAttribute
  public String getMajorExceptionFilter() {
    return retryController.getMajorExceptionFilter();
  }

  @ManagedAttribute
  public void setMajorExceptionFilter(final String majorExceptionFilter) {
    retryController.setMajorExceptionFilter(majorExceptionFilter);
  }
}
