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
package org.sonatype.nexus.security.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.security.realm.RealmConfiguration;
import org.sonatype.nexus.security.realm.RealmConfigurationStore;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Initial {@link RealmConfiguration} provider.
 *
 * @since 3.0
 */
@Component
@Qualifier("initial")
public class InitialRealmConfigurationProvider
    implements FactoryBean<RealmConfiguration>
{
  private final RealmConfigurationStore store;

  @Autowired
  public InitialRealmConfigurationProvider(final RealmConfigurationStore store) {
    this.store = checkNotNull(store);
  }

  @Override
  public RealmConfiguration getObject() {
    RealmConfiguration configuration = store.newEntity();
    configuration.setRealmNames(Lists.newArrayList(
        AuthenticatingRealmImpl.NAME,
        AuthorizingRealmImpl.NAME));
    return configuration;
  }

  @Override
  public Class<?> getObjectType() {
    return RealmConfiguration.class;
  }
}
