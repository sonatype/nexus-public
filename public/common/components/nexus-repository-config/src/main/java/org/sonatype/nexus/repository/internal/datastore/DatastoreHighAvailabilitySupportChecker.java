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
package org.sonatype.nexus.repository.internal.datastore;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.HighAvailabilitySupportChecker;

import org.springframework.stereotype.Component;

@Component
public class DatastoreHighAvailabilitySupportChecker
    extends HighAvailabilitySupportChecker
{
  @Autowired
  public DatastoreHighAvailabilitySupportChecker(final NodeAccess nodeAccess) {
    super(nodeAccess);
  }

  @Override
  protected List<String> getEnabledFormats() {
    return List.of("docker", "oci", "maven2", "npm", "r", "raw", "pypi", "conan", "conda", "gitlfs",
        "cocoapods", "go", "p2", "nuget", "rubygems", "helm", "apt", "yum", "cargo", "composer",
        "huggingface", "terraform", "swift", "pub", "ansiblegalaxy", "alpine", "openvsx");
  }
}
