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
package org.sonatype.nexus.proxy.maven.routing.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.routing.PrefixSource;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Carries the results of a discovery.
 *
 * @author cstamas
 * @since 2.4
 */
public class DiscoveryResult<R extends MavenRepository>
{
  /**
   * The outcome of discovery of a single strategy.
   */
  public static interface Outcome
  {
    /**
     * Returns the ID of the {@link RemoteStrategy} that this outcome belongs to.
     *
     * @return the strategy ID, never {@code null}.
     */
    String getStrategyId();

    /**
     * Returns {@code true} if this outcome is a successful one.
     *
     * @return {@code true} if this outcome is a successful one.
     */
    boolean isSuccessful();

    /**
     * Message of the outcome, meant for {@link RemoteStrategy} to pass some strategy specific information.
     *
     * @return the message of the outcome.
     */
    String getMessage();

    /**
     * Throwable associated with outcome, if any, of {@code null}.
     *
     * @return throwable
     */
    Throwable getThrowable();
  }

  private static class OutcomeImpl
      implements Outcome
  {
    private final String strategyId;

    private final boolean successful;

    private final String message;

    private final Throwable throwable;

    private OutcomeImpl(final String strategyId, final boolean successful, final String message,
                        final Throwable throwable)
    {
      this.strategyId = checkNotNull(strategyId);
      this.successful = successful;
      this.message = checkNotNull(message);
      this.throwable = throwable;
    }

    /**
     * Returns the ID of the {@link RemoteStrategy} that this outcome belongs to.
     *
     * @return the strategy ID, never {@code null}.
     */
    public String getStrategyId() {
      return strategyId;
    }

    /**
     * Returns {@code true} if this outcome is a successful one.
     *
     * @return {@code true} if this outcome is a successful one.
     */
    public boolean isSuccessful() {
      return successful;
    }

    /**
     * Message of the outcome, meant for {@link RemoteStrategy} to pass some strategy specific information.
     *
     * @return the message of the outcome.
     */
    public String getMessage() {
      return message;
    }

    /**
     * Throwable belonging to outcome.
     *
     * @return the Throwable if any.
     */
    public Throwable getThrowable() {
      return throwable;
    }

    @Override
    public String toString() {
      return "Outcome[strategyId=" + strategyId + ", successful=" + successful + ", message=" + message + "]";
    }
  }

  private final R mavenRepository;

  private final List<Outcome> outcomes;

  private PrefixSource prefixSource;

  /**
   * Constructor.
   *
   * @param mavenRepository the repository having been discovered.
   */
  public DiscoveryResult(final R mavenRepository) {
    this.mavenRepository = checkNotNull(mavenRepository);
    this.outcomes = new ArrayList<Outcome>();
    this.prefixSource = null;
  }

  /**
   * Returns the repository being discovered.
   *
   * @return the discovered repository.
   */
  public R getMavenRepository() {
    return mavenRepository;
  }

  /**
   * Returns {@code true} if discovery was successful.
   *
   * @return {@code true} if discovery was successful.
   */
  public boolean isSuccessful() {
    return getLastSuccessOutcome() != null;
  }

  /**
   * Returns the succeeded strategy instance.
   *
   * @return strategy that succeeded.
   */
  public Outcome getLastResult() {
    return getLastOutcome();
  }

  /**
   * Returns all the outcomes.
   *
   * @return strategy that succeeded.
   */
  public List<Outcome> getAllResults() {
    return Collections.unmodifiableList(outcomes);
  }

  /**
   * Returns the {@link PrefixSource} that was provided by successful strategy.
   *
   * @return entry source built by successful strategy.
   */
  public PrefixSource getPrefixSource() {
    return prefixSource;
  }

  /**
   * Records a success on behalf of a strategy, if this has not yet recorded a success, in which case this method
   * will
   * do nothing.
   */
  public void recordSuccess(final String usedStrategyId, final String message, final PrefixSource prefixSource) {
    if (!isSuccessful()) {
      checkNotNull(usedStrategyId);
      checkNotNull(message);
      checkNotNull(prefixSource);
      final OutcomeImpl success = new OutcomeImpl(usedStrategyId, true, message, null);
      this.outcomes.add(success);
      this.prefixSource = prefixSource;
    }
  }

  /**
   * Records a failure on behalf of a strategy, if this has not yet recorded a success, in which case this method
   * will
   * do nothing. A failure simply means "this strategy failed to get remote prefix list".
   */
  public void recordFailure(final String usedStrategyId, final String message) {
    if (!isSuccessful()) {
      checkNotNull(usedStrategyId);
      checkNotNull(message);
      final OutcomeImpl failure = new OutcomeImpl(usedStrategyId, false, message, null);
      this.outcomes.add(failure);
    }
  }

  /**
   * Records an error on behalf of a strategy, if this has not yet recorded a success, in which case this method will
   * do nothing.
   */
  public void recordError(final String usedStrategyId, final Throwable errorCause) {
    if (!isSuccessful()) {
      checkNotNull(usedStrategyId);
      checkNotNull(errorCause);
      final OutcomeImpl failure = new OutcomeImpl(usedStrategyId, false, errorCause.getMessage(), errorCause);
      this.outcomes.add(failure);
    }
  }

  // ==

  protected Outcome getLastOutcome() {
    if (outcomes.size() > 0) {
      final Outcome outcome = outcomes.get(outcomes.size() - 1);
      return outcome;
    }
    return null;
  }

  protected Outcome getLastSuccessOutcome() {
    final Outcome outcome = getLastOutcome();
    if (outcome != null && outcome.isSuccessful()) {
      return outcome;
    }
    return null;
  }
}
