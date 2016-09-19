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
package org.sonatype.nexus.repository.manager.internal;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.MultipleFailures;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.event.EventBus;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuard;
import org.sonatype.nexus.common.stateguard.StateGuardAware;
import org.sonatype.nexus.common.stateguard.Transitions;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.MissingFacetException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryDestroyedEvent;
import org.sonatype.nexus.repository.RepositoryStartedEvent;
import org.sonatype.nexus.repository.RepositoryStoppedEvent;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;

import com.google.inject.assistedinject.Assisted;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.manager.internal.RepositoryImpl.State.DELETED;
import static org.sonatype.nexus.repository.manager.internal.RepositoryImpl.State.DESTROYED;
import static org.sonatype.nexus.repository.manager.internal.RepositoryImpl.State.FAILED;
import static org.sonatype.nexus.repository.manager.internal.RepositoryImpl.State.INITIALISED;
import static org.sonatype.nexus.repository.manager.internal.RepositoryImpl.State.NEW;
import static org.sonatype.nexus.repository.manager.internal.RepositoryImpl.State.STARTED;
import static org.sonatype.nexus.repository.manager.internal.RepositoryImpl.State.STOPPED;

/**
 * {@link Repository} implementation.
 *
 * @since 3.0
 */
public class RepositoryImpl
    extends ComponentSupport
    implements Repository, StateGuardAware
{
  private final EventBus eventBus;

  private final Type type;

  private final Format format;

  private final FacetLookup facets = new FacetLookup();

  private Configuration configuration;

  private String name;

  @Inject
  public RepositoryImpl(final EventBus eventBus,
                        @Assisted final Type type,
                        @Assisted final Format format)
  {
    this.eventBus = checkNotNull(eventBus);
    this.type = checkNotNull(type);
    this.format = checkNotNull(format);
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public Format getFormat() {
    return format;
  }

  @Override
  public String getName() {
    return checkNotNull(name);
  }

  @Override
  public Configuration getConfiguration() {
    return checkNotNull(configuration);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "type=" + type +
        ", format=" + format +
        ", name='" + name + '\'' +
        '}';
  }

  //
  // State
  //

  static final class State
  {
    public static final String NEW = "NEW";

    public static final String INITIALISED = "INITIALISED";

    public static final String STARTED = "STARTED";

    public static final String STOPPED = "STOPPED";

    public static final String DELETED = "DELETED";

    public static final String DESTROYED = "DESTROYED";

    public static final String FAILED = "FAILED";
  }

  private final StateGuard states = new StateGuard.Builder()
      .logger(createLogger())
      .initial(NEW)
      .failure(FAILED)
      .create();

  @Override
  @Nonnull
  public StateGuard getStateGuard() {
    return states;
  }

  //
  // Lifecycle
  //

  @Override
  public void validate(final Configuration configuration) throws Exception {
    checkNotNull(configuration);

    Set<ConstraintViolation<?>> violations = new HashSet<>();
    MultipleFailures failures = new MultipleFailures();

    for (Facet facet : facets) {
      log.debug("Validating facet: {}", facet);
      try {
        facet.validate(configuration);
      }
      catch (ConstraintViolationException e) {
        log.debug("Facet validation produced violations: {}", facet, e);
        violations.addAll(e.getConstraintViolations());
      }
      catch (Throwable t) {
        log.error("Failed to validate facet: {}", facet, t);
        failures.add(t);
      }
    }
    failures.maybePropagate("Failed to validate facets");

    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  @Override
  @Transitions(from = NEW, to = INITIALISED)
  public void init(final Configuration configuration) throws Exception {
    this.configuration = checkNotNull(configuration);
    this.name = configuration.getRepositoryName();

    MultipleFailures failures = new MultipleFailures();
    for (Facet facet : facets) {
      try {
        log.debug("Initializing facet: {}", facet);
        facet.init();
      }
      catch (Throwable t) {
        log.error("Failed to initialize facet: {}", facet, t);
        failures.add(t);
      }
    }
    failures.maybePropagate("Failed to initialize facets");
  }

  @Override
  @Guarded(by = STOPPED)
  public void update(final Configuration configuration) throws Exception {
    checkNotNull(configuration);

    // Ensure configuration sanity
    validate(configuration);
    this.configuration = configuration;

    MultipleFailures failures = new MultipleFailures();
    for (Facet facet : facets) {
      try {
        log.debug("Updating facet: {}", facet);
        facet.update();
      }
      catch (Throwable t) {
        log.error("Failed to update facet: {}", facet, t);
        failures.add(t);
      }
    }
    failures.maybePropagate("Failed to update facets");
  }

  @Override
  @Transitions(from = {INITIALISED, STOPPED}, to = STARTED)
  public void start() throws Exception {
    MultipleFailures failures = new MultipleFailures();
    for (Facet facet : facets) {
      try {
        log.debug("Starting facet: {}", facet);
        facet.start();
      }
      catch (Throwable t) {
        log.error("Failed to start facet: {}", facet, t);
        failures.add(t);
      }
    }
    failures.maybePropagate("Failed to start facets");

    eventBus.post(new RepositoryStartedEvent(this));
  }

  @Override
  @Transitions(from = STARTED, to = STOPPED)
  public void stop() throws Exception {
    MultipleFailures failures = new MultipleFailures();

    for (Facet facet : facets.reverse()) {
      try {
        log.debug("Stopping facet: {}", facet);
        facet.stop();
      }
      catch (Throwable t) {
        log.error("Failed to stop facet: {}", facet, t);
        failures.add(t);
      }
    }
    failures.maybePropagate("Failed to stop facets");

    eventBus.post(new RepositoryStoppedEvent(this));
  }

  @Override
  @Transitions(from = STOPPED, to = DELETED)
  public void delete() throws Exception {
    MultipleFailures failures = new MultipleFailures();

    for (Facet facet : facets.reverse()) {
      try {
        log.debug("Deleting facet: {}", facet);
        facet.delete();
      }
      catch (Throwable t) {
        log.error("Failed to delete facet: {}", facet, t);
        failures.add(t);
      }
    }
    failures.maybePropagate("Failed to delete facets");
  }

  @Override
  @Transitions(to = DESTROYED)
  public void destroy() throws Exception {
    if (states.is(STARTED)) {
      stop();
    }

    MultipleFailures failures = new MultipleFailures();
    for (Facet facet : facets.reverse()) {
      try {
        log.debug("Destroying facet: {}", facet);
        facet.destroy();
      }
      catch (Throwable t) {
        log.error("Failed to destroy facet: {}", facet, t);
        failures.add(t);
      }
    }
    failures.maybePropagate("Failed to destroy facets");

    facets.clear();
    configuration = null;

    eventBus.post(new RepositoryDestroyedEvent(this));
  }

  //
  // Facets
  //

  @Override
  @Guarded(by = NEW)
  public void attach(final Facet facet) throws Exception {
    checkNotNull(facet);
    log.debug("Attaching facet: {}", facet);
    facet.attach(this);
    facets.add(facet);
  }

  @Override
  @Nonnull
  public <T extends Facet> T facet(final Class<T> type) throws MissingFacetException {
    T facet = facets.get(type);
    if (facet == null) {
      throw new MissingFacetException(this, type);
    }

    return facet;
  }

  @Override
  @Nonnull
  public <T extends Facet> Optional<T> optionalFacet(final Class<T> type) {
    return Optional.ofNullable(facets.get(type));
  }

  @Override
  public String getUrl() {
    return BaseUrlHolder.get() + "/repository/" + getName();
  }
}
