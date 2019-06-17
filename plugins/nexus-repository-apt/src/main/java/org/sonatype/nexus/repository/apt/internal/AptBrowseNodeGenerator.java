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
package org.sonatype.nexus.repository.apt.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.browse.BrowseNodeGenerator;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import com.google.common.base.Splitter;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.17
 */
@Singleton
@Named(AptFormat.NAME)
public class AptBrowseNodeGenerator
    implements
    BrowseNodeGenerator
{

  @Override
  public List<String> computeAssetPath(final Asset asset, final Component component) {
    List<String> path;
    if (component != null) {
      path = computeComponentPath(asset, component);
      path.add(asset.name());
      return path;
    } else {
      path = new ArrayList<>();
      String name = asset.name();
      if (name.endsWith(".deb") || name.endsWith(".udeb")) {
        path.add("packages");
      } else if (!name.startsWith("snapshots")) {
        path.add("metadata");
      }
      path.addAll(newArrayList(Splitter.on('/').omitEmptyStrings().split(name)));
    }
    return path;
  }

  @Override
  public List<String> computeComponentPath(final Asset asset, final Component component) {
    List<String> path = new ArrayList<>();
    path.add("packages");
    path.add(component.name().substring(0, 1).toLowerCase());
    path.add(component.name());
    path.add(component.version());
    path.add(component.group());
    path.add(component.name());
    return path;
  }
}
