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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.repository.npm.internal.audit.report.Resolve;
import org.sonatype.nexus.repository.vulnerability.AuditComponent;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.sonatype.nexus.repository.npm.internal.NpmFormat.NAME;
import static org.sonatype.nexus.repository.npm.internal.audit.parser.PackageLockParser.ROOT;

/**
 * Product of parsing package-lock.json by PackageLockParser Used for fetching all components witch contains in
 * package-lock.json and creation Resolve instance (entity from npm report for each component)
 *
 * @since 3.24
 */
public class PackageLock
{
  private final Map<String, List<PackageLockNode>> nodes;

  public PackageLock(Map<String, List<PackageLockNode>> nodes) {
    this.nodes = nodes;
  }

  public Set<AuditComponent> getComponents() {
    String applicationId = getRoot().getApplicationId();
    return nodes.entrySet().stream()
        .filter(entry -> !ROOT.equals(entry.getKey()))
        .flatMap(entry -> entry.getValue().stream()
            .map(dependency -> new AuditComponent(applicationId, NAME, entry.getKey(), dependency.getVersion())))
        .collect(toSet());
  }

  public List<PackageLockNode> getNodes(final String name, final String version) {
    return this.nodes.get(name).stream()
        .filter(packageNode -> version.equals(packageNode.getVersion()))
        .collect(toList());
  }

  public RootPackageLockNode getRoot() {
    return (RootPackageLockNode) nodes.get(ROOT).iterator().next();
  }

  public List<Resolve> createResolve(final int id, final String name, final String version) {
    List<PackageLockNode> packageLockNodes = this.nodes.get(name).stream()
        .filter(packageNode -> version.equals(packageNode.getVersion()))
        .collect(toList());

    return packageLockNodes.stream()
        .map(node -> createResolve(id, name, node))
        .collect(toList());
  }

  private Resolve createResolve(final int id, final String name, final PackageLockNode node) {
    LinkedList<String> pathList = new LinkedList<>();
    pathList.add(name);
    getPathToNode(node, pathList);
    String path = String.join(">", pathList);
    return new Resolve(id, pathList, path, node.isDev(), node.isOptional(), false);
  }

  private void getPathToNode(final PackageLockNode node, final LinkedList<String> path) {
    String parentNodeName = node.getParentNodeName();
    if (parentNodeName != null) {
      path.addFirst(parentNodeName);
      PackageLockNode parentNode = nodes.get(parentNodeName).stream()
          .filter(packageLockNode -> packageLockNode.getDependencies() != null)
          .filter(packageLockNode -> packageLockNode.getDependencies().containsValue(node))
          .findFirst()
          .orElseThrow(
              () -> new IllegalArgumentException(parentNodeName + " wasn't found in parsed package-lock.json"));

      getPathToNode(parentNode, path);
    }
  }
}
