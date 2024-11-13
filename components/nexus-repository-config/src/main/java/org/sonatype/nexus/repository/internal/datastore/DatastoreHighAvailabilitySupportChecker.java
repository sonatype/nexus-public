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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.HighAvailabilitySupportChecker;

import com.google.common.collect.ImmutableList;

import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;

@FeatureFlag(name = DATASTORE_ENABLED)
@Named
@Singleton
public class DatastoreHighAvailabilitySupportChecker
    extends HighAvailabilitySupportChecker
{
  @Inject
  public DatastoreHighAvailabilitySupportChecker(final NodeAccess nodeAccess) {
    super(nodeAccess);
  }

  @Override
  protected List<String> getEnabledFormats() {
    return ImmutableList.<String>builder()
        .add("docker")
        .add("maven2")
        .add("npm")
        .add("r")
        .add("raw")
        .add("pypi")
        .add("conan")
        .add("conda")
        .add("gitlfs")
        .add("cocoapods")
        .add("go")
        .add("p2")
        .add("nuget")
        .add("rubygems")
        .add("helm")
        .add("apt")
        .add("yum")
        .add("cargo")
        .add("composer")
        .build();
  }
}
