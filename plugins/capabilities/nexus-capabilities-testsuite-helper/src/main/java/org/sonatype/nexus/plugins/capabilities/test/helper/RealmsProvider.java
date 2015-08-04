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
package org.sonatype.nexus.plugins.capabilities.test.helper;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.capabilities.model.SelectableEntryXO;
import org.sonatype.nexus.capability.spi.SelectableEntryProvider;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.shiro.realm.Realm;

/**
 * A test/demo {@link Realm} {@link SelectableEntryProvider}.
 *
 * @since 2.7
 */
@Named("realms")
@Singleton
public class RealmsProvider
    implements SelectableEntryProvider
{

  private final Map<String, Realm> realms;

  @Inject
  public RealmsProvider(final Map<String, Realm> realms) {this.realms = Preconditions.checkNotNull(realms);}

  @Override
  public List<SelectableEntryXO> get(final Parameters params) {
    String prepend = params.getFirst("prepend");
    if (prepend == null) {
      prepend = "";
    }
    List<SelectableEntryXO> entries = Lists.newArrayList();
    for (Entry<String, Realm> entry : realms.entrySet()) {
      entries.add(new SelectableEntryXO().withId(entry.getKey()).withName(prepend + entry.getValue().getName()));
    }
    return entries;
  }

}
