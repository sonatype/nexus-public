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

import org.springframework.beans.factory.annotation.Autowired;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.validation.Validate;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.realm.Realm;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.QualifierUtil.description;
import org.springframework.stereotype.Component;

/**
 * Realm Security Settings {@link DirectComponentSupport}.
 */
@Component
@DirectAction(action = "coreui_RealmSettings")
public class RealmSettingsComponent
    extends DirectComponentSupport
    implements ApplicationContextAware
{
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
   * @return updated security realm settings
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:update")
  @Validate
  public RealmSettingsXO update(@NotNull @Valid final RealmSettingsXO realmSettingsXO) {
    realmManager.setConfiguredRealmIds(realmSettingsXO.getRealms());
    return read();
  }

  @Override
  public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}
