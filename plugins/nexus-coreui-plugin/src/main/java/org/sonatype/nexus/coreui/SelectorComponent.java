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
package org.sonatype.nexus.coreui;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import org.sonatype.nexus.extdirect.DirectComponent;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.repository.security.RepositoryContentSelectorPrivilegeDescriptor;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorConfigurationStore;
import org.sonatype.nexus.selector.SelectorFactory;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.validation.ConstraintViolationFactory;
import org.sonatype.nexus.validation.Validate;
import org.sonatype.nexus.validation.group.Create;
import org.sonatype.nexus.validation.group.Update;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;

import javax.validation.constraints.NotEmpty;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Selector {@link DirectComponent}.
 */
@Named
@Singleton
@DirectAction(action = "coreui_Selector")
public class SelectorComponent
    extends DirectComponentSupport
{
  private static final String EXPRESSION_KEY = "expression";

  private final SelectorManager selectorManager;

  private final ConstraintViolationFactory constraintViolationFactory;

  private final SelectorFactory selectorFactory;

  private final SecuritySystem securitySystem;

  private final SelectorConfigurationStore store;

  @Inject
  public SelectorComponent(
      final SelectorManager selectorManager,
      final ConstraintViolationFactory constraintViolationFactory,
      final SelectorFactory selectorFactory,
      final SecuritySystem securitySystem,
      final SelectorConfigurationStore store)
  {
    this.selectorManager = checkNotNull(selectorManager);
    this.constraintViolationFactory = checkNotNull(constraintViolationFactory);
    this.selectorFactory = checkNotNull(selectorFactory);
    this.securitySystem = checkNotNull(securitySystem);
    this.store = checkNotNull(store);
  }

  /**
   * @return a list of selectors
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:selectors:read")
  public List<SelectorXO> read() {
    Set<Privilege> privileges = securitySystem.listPrivileges();
    return store.browse()
        .stream()
        .map(config -> asSelector(config, privileges))
        .collect(Collectors.toList()); // NOSONAR
  }

  /**
   * Creates a selector.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:selectors:create")
  @Validate(groups = {Create.class, Default.class})
  public SelectorXO create(@NotNull @Valid final SelectorXO selectorXO) {
    selectorFactory.validateSelector(selectorXO.getType(), selectorXO.getExpression());

    SelectorConfiguration configuration = selectorManager.newSelectorConfiguration(
        selectorXO.getName(), selectorXO.getType(), selectorXO.getDescription(),
        Collections.singletonMap(EXPRESSION_KEY, selectorXO.getExpression()));
    selectorManager.create(configuration);
    return asSelector(configuration, securitySystem.listPrivileges());
  }

  /**
   * Updates a selector.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:selectors:update")
  @Validate(groups = {Update.class, Default.class})
  public SelectorXO update(@NotNull @Valid final SelectorXO selectorXO) {
    selectorFactory.validateSelector(selectorXO.getType(), selectorXO.getExpression());
    SelectorConfiguration config = selectorManager.readByName(selectorXO.getName());
    config.setDescription(selectorXO.getDescription());
    config.setAttributes(Collections.singletonMap(EXPRESSION_KEY, selectorXO.getExpression()));
    selectorManager.update(config);
    return selectorXO;
  }

  /**
   * Deletes a selector.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:selectors:delete")
  @Validate
  public void remove(@NotEmpty final String name) {
    try {
      selectorManager.delete(selectorManager.readByName(name));
    }
    catch (IllegalStateException e) {
      throw new ConstraintViolationException(e.getMessage(),
          Collections.singleton(constraintViolationFactory.createViolation("*", e.getMessage())));
    }
  }

  /**
   * Retrieve a list of available selector references.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:selectors:read")
  public List<ReferenceXO> readReferences() {
    return selectorManager.browse()
        .stream()
        .map(config -> new ReferenceXO(config.getName(), config.getName()))
        .collect(Collectors.toList()); // NOSONAR
  }

  private SelectorXO asSelector(final SelectorConfiguration configuration, final Set<Privilege> privilegeSet) {
    List<String> privileges = getPrivilegesUsingSelector(configuration, privilegeSet);

    SelectorXO selectorXO = new SelectorXO();
    selectorXO.setId(configuration.getName());
    selectorXO.setName(configuration.getName());
    selectorXO.setType(configuration.getType());
    selectorXO.setDescription(configuration.getDescription());
    selectorXO.setExpression(configuration.getAttributes().get(EXPRESSION_KEY));
    selectorXO.setUsedBy(canReadPrivileges() ? privileges : Collections.emptyList());
    selectorXO.setUsedByCount(privileges.size());

    return selectorXO;
  }

  private List<String> getPrivilegesUsingSelector(
      final SelectorConfiguration selectorConfiguration,
      final Set<Privilege> privileges)
  {
    return privileges.stream()
        .filter(privilege -> RepositoryContentSelectorPrivilegeDescriptor.TYPE.equals(privilege.getType()))
        .filter(privilege -> selectorConfiguration.getName()
            .equals(privilege.getProperties().get(RepositoryContentSelectorPrivilegeDescriptor.P_CONTENT_SELECTOR)))
        .map(Privilege::getName)
        .collect(Collectors.toList()); // NOSONAR
  }

  private static boolean canReadPrivileges() {
    return SecurityUtils.getSubject().isPermitted("nexus:privileges:read");
  }
}
