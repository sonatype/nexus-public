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
package org.sonatype.nexus.repository.npm.internal.audit.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

/**
 * Parser for package-lock.json, shrinkwrap.json is also parsable by this parser as it has same format
 *
 * @since 3.24
 */
public class PackageLockParser
{
  public static final String ROOT = "root";

  private static final Gson gson = new Gson();

  private PackageLockParser() {
  }

  public static PackageLock parse(final String jsonString) {
    RootPackageLockNode rootNode = gson.fromJson(jsonString, RootPackageLockNode.class);
    return new PackageLock(getAllDependencies(ROOT, rootNode, null, new HashMap<>()));
  }

  private static Map<String, List<PackageLockNode>> getAllDependencies(
      final String key,
      final PackageLockNode packageLockNode,
      final String parentNode,
      final Map<String, List<PackageLockNode>> npmDependencies)
  {
    packageLockNode.setParentNodeName(parentNode);
    npmDependencies.computeIfAbsent(key.toLowerCase(), k -> new ArrayList<>()).add(packageLockNode);
    if (packageLockNode.getDependencies() != null) {
      packageLockNode.getDependencies().forEach((dependencyKey, dependencyValue) -> {
            String parentKey = !ROOT.equals(key) ? key : null;
            getAllDependencies(dependencyKey, dependencyValue, parentKey, npmDependencies);
          }
      );
    }

    return npmDependencies;
  }
}
