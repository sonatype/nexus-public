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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.security.realm.SecurityRealm;
import org.sonatype.nexus.validation.Validate;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.realm.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.QualifierUtil.description;

/**
 * Realm Security Settings {@link DirectComponentSupport}.
 */
@Component
@DirectAction(action = "coreui_RealmSettings")
public class RealmSettingsComponent
    extends DirectComponentSupport
    implements ApplicationContextAware
{
  private static final Logger log = LoggerFactory.getLogger(RealmSettingsComponent.class);

  private final RealmManager realmManager;

  private ApplicationContext applicationContext;

  @Autowired
  public RealmSettingsComponent(final RealmManager realmManager) {
    this.realmManager = checkNotNull(realmManager);
  }

  /**
   * Retrieves security realm settings.
   *
   * @return security realm settings
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:settings:read")
  public RealmSettingsXO read() {
    RealmSettingsXO settingsXO = new RealmSettingsXO();
    settingsXO.setRealms(realmManager.getConfiguredRealmIds());
    return settingsXO;
  }

  /**
   * Retrieves realm types.
   *
   * @return a list of realm types
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:settings:read")
  public List<ReferenceXO> readRealmTypes() {
    return applicationContext.getBeansOfType(Realm.class)
        .entrySet()
        .stream()
        .map(entry -> new ReferenceXO(entry.getKey(), description(entry.getValue())))
        .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
        .toList();
  }

  /**
   * Updates security realm settings.
   *
   * Validates that all submitted realm IDs correspond to available realms before persisting.
   * Throws {@link ValidationErrorsException} if any unknown realm IDs are submitted.
   *
   * @param realmSettingsXO the realm settings to update
   * @return updated security realm settings
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:update")
  @Validate
  public RealmSettingsXO update(@NotNull @Valid final RealmSettingsXO realmSettingsXO) {
    // Validate that all realm IDs exist in the available realm set
    Set<String> knownRealmIds = realmManager.getAvailableRealms()
        .stream()
        .map(SecurityRealm::getId)
        .collect(Collectors.toSet());

    List<String> unknownRealmIds = realmSettingsXO.getRealms()
        .stream()
        .filter(id -> !knownRealmIds.contains(id))
        .toList();

    if (!unknownRealmIds.isEmpty()) {
      log.debug("Request to set realms with unknown IDs: {}", unknownRealmIds);
      throw new ValidationErrorsException("Unknown realmIds: " + unknownRealmIds);
    }

    realmManager.setConfiguredRealmIds(realmSettingsXO.getRealms());
    return read();
  }

  @Override
  public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}
