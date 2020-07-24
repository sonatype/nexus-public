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
package org.sonatype.nexus.repository.routing.internal;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.repository.routing.RoutingRule;
import org.sonatype.nexus.repository.routing.RoutingRuleInvalidatedEvent;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.validation.constraint.NamePatternConstants;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.UUID.fromString;

/**
 * MyBatis {@link RoutingRuleStore} implementation.
 *
 * @since 3.21
 */
@Named("mybatis")
@Singleton
public class RoutingRuleStoreImpl
    extends ConfigStoreSupport<RoutingRuleDAO>
    implements RoutingRuleStore
{
  private static final String NAME = "name";

  private static final String DESCRIPTION = "description";

  private static final String MATCHERS = "matchers";

  private static final String MODE = "mode";

  private static final String NONE = "none";

  private final EventManager eventManager;

  @Inject
  public RoutingRuleStoreImpl(final DataSessionSupplier sessionSupplier, final EventManager eventManager) {
    super(sessionSupplier);
    this.eventManager = checkNotNull(eventManager);
  }

  @Override
  public RoutingRule newRoutingRule() {
    return new RoutingRuleData();
  }

  @Transactional
  @Override
  public List<RoutingRule> list() {
    return ImmutableList.copyOf(dao().browse());
  }

  @Transactional
  @Override
  public RoutingRule create(final RoutingRule rule) {
    try {
      dao().create((RoutingRuleData) validate(rule));
      return rule;
    }
    catch (DuplicateKeyException e) {
      throw new ValidationErrorsException("name", "A rule with the same name already exists. Name must be unique.");
    }
  }

  @Transactional
  @Override
  public RoutingRule getById(final String id) {
    return dao().read(new EntityUUID(fromString(id))).orElse(null);
  }

  @Transactional
  @Override
  public RoutingRule getByName(final String name) {
    return dao().readByName(name).orElse(null);
  }

  @Override
  public void update(final RoutingRule rule) {
    doUpdate((RoutingRuleData) validate(rule));
    postEvent(rule);
  }

  @Transactional
  protected void doUpdate(final RoutingRuleData rule) {
    try {
      dao().update(rule);
    }
    catch (DuplicateKeyException e) {
      throw new ValidationErrorsException("name", "A rule with the same name already exists. Name must be unique.");
    }
  }

  @Override
  public void delete(final RoutingRule rule) {
    doDelete(rule.name());
    postEvent(rule);
  }

  @Transactional
  protected void doDelete(final String name) {
    dao().deleteByName(name);
  }

  private void postEvent(final RoutingRule rule) {
    // trigger invalidation of routing rule cache
    eventManager.post(new RoutingRuleInvalidatedEvent()
    {
      @Override
      public EntityId getRoutingRuleId() {
        return rule.id();
      }
    });
  }

  @VisibleForTesting
  static RoutingRule validate(final RoutingRule rule) { // NOSONAR
    ValidationErrorsException exception = new ValidationErrorsException();

    if (Strings2.isBlank(rule.name())) {
      exception.withError(NAME, "A non-empty value must be specified");
    }
    else if (!rule.name().matches(NamePatternConstants.REGEX)) {
      exception.withError(NAME,
          "Only letters, digits, underscores(_), hyphens(-), and dots(.) are allowed and may not start with underscore or dot.");
    }
    else if (rule.name().equalsIgnoreCase(NONE)) {
      exception.withError(NAME, "Rule must not be named None");
    }

    if (rule.description() == null) {
      exception.withError(DESCRIPTION, "A non-null value must be specified");
    }

    if (rule.mode() == null) {
      exception.withError(MODE, "A non-empty value must be specified");
    }

    if (rule.matchers() == null || rule.matchers().isEmpty()) {
      exception.withError(MATCHERS, "At least one rule must be specified");
    }
    else {
      int index = 0;
      for (String regex : rule.matchers()) {
        if (Strings2.isBlank(regex)) {
          exception.withError(MATCHERS + "[" + index + "]", "Empty matchers are not allowed");
        }
        else {
          try {
            Pattern.compile(regex);
          }
          catch (PatternSyntaxException e) { // NOSONAR
            exception.withError(MATCHERS + "[" + index + "]", "Invalid regex: " + e.getMessage());
          }
        }
        index++;
      }
    }

    if (!exception.getValidationErrors().isEmpty()) {
      throw exception;
    }

    return rule;
  }
}
