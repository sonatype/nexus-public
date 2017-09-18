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
package org.sonatype.nexus.repository.browse.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.browse.AbstractPathBrowseNodeGenerator;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import com.google.inject.Singleton;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default handler of events that require management of folder data. This implementation will create a folder structure
 * based on the group, name, and version of the component and put the assets underneath the component node.
 *
 * @since 3.6
 */
@Singleton
@Named
public class DefaultBrowseNodeGenerator
    extends AbstractPathBrowseNodeGenerator
{
  /**
   * @return componentPath/assetName if the component was not null, otherwise assetName
   */
  @Override
  public List<String> computeAssetPath(final Asset asset, @Nullable final Component component) {
    checkNotNull(asset);

    if (component != null) {
      List<String> path = new ArrayList<>();

      if (!Strings2.isBlank(component.group())) {
        path.add(component.group());
      }

      path.add(component.name());

      if (!Strings2.isBlank(component.version())) {
        path.add(component.version());
      }

      String name = asset.name();
      int lastSlash = name.lastIndexOf('/');
      if (lastSlash != -1 && lastSlash != name.length() - 1) {
        name = name.substring(lastSlash + 1);
      }
      path.add(name);

      return path;
    }
    else {
      return super.computeAssetPath(asset, null);
    }
  }

  /**
   * @return [componentGroup]/componentName/[componentVersion] if component is not null, otherwise an emptyList
   */
  @Override
  public List<String> computeComponentPath(final Asset asset, final Component component) {
    if (component == null) {
      return Collections.emptyList();
    }
    List<String> path = computeAssetPath(asset, component);
    return path.subList(0, path.size() - 1);
  }
}
