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
package org.sonatype.nexus.yum.internal.capabilities;

import java.util.Map;

import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support class for Yum repository capability configurations.
 *
 * @since yum 3.0
 */
public abstract class MetadataCapabilityConfigurationSupport
{

  public static final String REPOSITORY_ID = "repository";

  private String repository;

  public MetadataCapabilityConfigurationSupport(final String repository) {
    this.repository = checkNotNull(repository);
  }

  public MetadataCapabilityConfigurationSupport(final Map<String, String> properties) {
    checkNotNull(properties);
    this.repository = properties.get(REPOSITORY_ID);
  }

  public String repository() {
    return repository;
  }

  public Map<String, String> asMap() {
    final Map<String, String> props = Maps.newHashMap();
    props.put(REPOSITORY_ID, repository);
    return props;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "repository=" + repository +
        '}';
  }

}
