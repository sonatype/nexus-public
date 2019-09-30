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
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.repository.routing.RoutingRule;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.routing.RoutingRulesConfiguration;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.validation.constraint.NamePatternConstants;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * @since 3.16
 */
@Named
@ManagedLifecycle(phase = SCHEMAS)
@Singleton
public class RoutingRuleStoreImpl extends StateGuardLifecycleSupport implements RoutingRuleStore
{
  private static final String NONE = "none";

  private final Provider<DatabaseInstance> databaseInstance;

  private final RoutingRuleEntityAdapter entityAdapter;

  private final boolean enabled;

  @Inject
  public RoutingRuleStoreImpl(@Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> databaseInstance,
                              final RoutingRuleEntityAdapter entityAdapter,
                              final RoutingRulesConfiguration configuration)
  {
    this.databaseInstance = databaseInstance;
    this.entityAdapter = entityAdapter;
    this.enabled = configuration.isEnabled();
  }

  @Override
  protected void doStart() throws Exception {
    if (!enabled) {
      return;
    }
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      entityAdapter.register(db);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public RoutingRule create(final RoutingRule rule) {
    checkNotNull(rule);
    validate(rule);

    persist(entityAdapter::addEntity, rule);

    return rule;
  }

  @Override
  @Guarded(by = STARTED)
  public void update(final RoutingRule rule) {
    checkNotNull(rule);
    validate(rule);

    persist(entityAdapter::editEntity, rule);
  }

  @Override
  @Guarded(by = STARTED)
  public List<RoutingRule> list() {
    return inTx(databaseInstance).call(db -> ImmutableList.copyOf(entityAdapter.browse(db)));
  }

  @Override
  public void delete(final RoutingRule rule) {
    checkNotNull(rule);

    inTx(databaseInstance).run(db -> entityAdapter.deleteEntity(db, rule));
  }

  @Override
  public RoutingRule getByName(final String name) {
    checkNotNull(name);
    return inTx(databaseInstance).call(db -> entityAdapter.read(db, name));
  }

  @Override
  public RoutingRule getById(final String id) {
    checkNotNull(id);

    return inTx(databaseInstance).call(db -> entityAdapter.read(db, new DetachedEntityId(id)));
  }

  @VisibleForTesting
  static void validate(final RoutingRule rule) { // NOSONAR
    ValidationErrorsException exception = new ValidationErrorsException();

    if (Strings2.isBlank(rule.name())) {
      exception.withError("name", "A non-empty value must be specified");
    }
    else if (!rule.name().matches(NamePatternConstants.REGEX)) {
      exception.withError("name",
          "Only letters, digits, underscores(_), hyphens(-), and dots(.) are allowed and may not start with underscore or dot.");
    }
    else if (rule.name().equalsIgnoreCase(NONE)) {
      exception.withError("name", "Rule must not be named None");
    }

    if (rule.description() == null) {
      exception.withError("description", "A non-null value must be specified");
    }

    if (rule.mode() == null) {
      exception.withError("mode", "A non-empty value must be specified");
    }

    if (rule.matchers() == null || rule.matchers().isEmpty()) {
      exception.withError("matchers", "At least one rule must be specified");
    }
    else {
      int index = 0;
      for (String regex : rule.matchers()) {
        if (Strings2.isBlank(regex)) {
          exception.withError("matchers[" + index + "]", "Empty matchers are not allowed");
        }
        else {
          try {
            Pattern.compile(regex);
          }
          catch (PatternSyntaxException e) { // NOSONAR
            exception.withError("matchers[" + index + "]", "Invalid regex: " + e.getMessage());
          }
        }
        index++;
      }
    }

    if (!exception.getValidationErrors().isEmpty()) {
      throw exception;
    }
  }

  private void persist(BiConsumer<ODatabaseDocumentTx, RoutingRule> entityFunction, RoutingRule rule) {
    try {
      inTxRetry(databaseInstance).run(db -> entityFunction.accept(db, rule));
    }
    catch (ORecordDuplicatedException e) {
      if (RoutingRuleEntityAdapter.I_NAME.equals(e.getIndexName())) {
        throw new ValidationErrorsException("name", "A rule with the same name already exists. Name must be unique.");
      }
      throw e;
    }
  }
}
