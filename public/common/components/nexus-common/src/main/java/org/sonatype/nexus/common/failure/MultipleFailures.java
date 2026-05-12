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
package org.sonatype.nexus.common.failure;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper to collect and throw multiple {@link Throwable}s.
 */
public class MultipleFailures
{
  private static final Logger log = LoggerFactory.getLogger(MultipleFailures.class);

  private final List<Throwable> failures;

  public MultipleFailures(final int initialCapacity) {
    this.failures = new ArrayList<>(initialCapacity);
  }

  public MultipleFailures() {
    this.failures = new ArrayList<>();
  }

  public List<Throwable> getFailures() {
    return failures;
  }

  public void add(final Throwable failure) {
    log.trace("Adding: {}", failure);
    failures.add(checkNotNull(failure));
  }

  public int size() {
    return failures.size();
  }

  public boolean isEmpty() {
    return failures.isEmpty();
  }

  public class MultipleFailuresException
      extends Exception
  {
    private MultipleFailuresException(@Nullable final String message) {
      super(message);
      for (Throwable failure : failures) {
        addSuppressed(failure);
      }
    }

    public List<Throwable> getFailures() {
      return ImmutableList.copyOf(failures);
    }

    @Override
    public String getMessage() {
      StringBuilder buff = new StringBuilder();

      String message = super.getMessage();
      if (message != null) {
        buff.append(message).append("; ");
      }

      int size = failures.size();
      buff.append(size).append(" ").append(size == 1 ? "failure" : "failures");

      return buff.toString();
    }
  }

  /**
   * Maybe throw {@link MultipleFailuresException} if there are any failures with optional message.
   */
  public void maybePropagate(@Nullable final String message) throws MultipleFailuresException {
    if (!failures.isEmpty()) {
      log.trace("Propagating: {}", failures);
      throw new MultipleFailuresException(message);
    }
  }

  /**
   * Maybe throw {@link MultipleFailuresException} if there are any failures.
   */
  public void maybePropagate() throws MultipleFailuresException {
    maybePropagate(null);
  }
}
