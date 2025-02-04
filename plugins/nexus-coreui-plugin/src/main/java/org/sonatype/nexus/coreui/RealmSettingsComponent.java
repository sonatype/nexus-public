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
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.validation.Validate;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Key;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.realm.Realm;
import org.eclipse.sisu.inject.BeanLocator;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.StreamSupport.stream;

/**
 * Realm Security Settings {@link DirectComponentSupport}.
 */
@Named
@Singleton
@DirectAction(action = "coreui_RealmSettings")
public class RealmSettingsComponent
    extends DirectComponentSupport
{
  private final RealmManager realmManager;

  private final BeanLocator beanLocator;

  @Inject
  public RealmSettingsComponent(final RealmManager realmManager, final BeanLocator beanLocator) {
    this.realmManager = checkNotNull(realmManager);
    this.beanLocator = checkNotNull(beanLocator);
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
    return stream(beanLocator.locate(Key.get(Realm.class, Named.class)).spliterator(), false)
        .map(entry -> new ReferenceXO(((Named) entry.getKey()).value(), entry.getDescription()))
        .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
        .collect(Collectors.toList()); // NOSONAR
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
}
