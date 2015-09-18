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
package org.sonatype.nexus.security.realm;

import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.Entity;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * Realm configuration.
 *
 * @since 3.0
 */
public class RealmConfiguration
  extends Entity
  implements Cloneable
{
  private List<String> realmNames;

  public List<String> getRealmNames() {
    if (realmNames == null) {
      realmNames = Lists.newArrayList();
    }
    return realmNames;
  }

  public void setRealmNames(final @Nullable List<String> realmNames) {
    this.realmNames = realmNames;
  }

  public RealmConfiguration copy() {
    try {
      RealmConfiguration copy = (RealmConfiguration) clone();
      if (realmNames != null) {
        copy.realmNames = Lists.newArrayList(realmNames);
      }
      return copy;
    }
    catch (CloneNotSupportedException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "realmNames=" + realmNames +
        '}';
  }
}
