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
package org.sonatype.nexus.upgrade.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.sonatype.nexus.common.entity.Entity;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The persisted versions of models that are clustered.
 * 
 * @since 3.1
 */
public class ClusteredModelVersions
    extends Entity
    implements Iterable<Map.Entry<String, String>>
{
  private Map<String, String> modelVersions = new HashMap<>();

  public Map<String, String> getModelVersions() {
    return modelVersions;
  }

  public void setModelVersions(Map<String, String> modelVersions) {
    this.modelVersions = checkNotNull(modelVersions);
  }

  public String get(final String model) {
    checkNotNull(model);
    return modelVersions.get(model);
  }

  public void put(final String model, final String version) {
    checkNotNull(model);
    checkNotNull(version);
    modelVersions.put(model, version);
  }

  @Override
  public Iterator<Entry<String, String>> iterator() {
    return modelVersions.entrySet().iterator();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + modelVersions;
  }
}
