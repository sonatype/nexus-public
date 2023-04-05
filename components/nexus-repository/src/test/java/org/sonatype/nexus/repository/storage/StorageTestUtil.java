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
package org.sonatype.nexus.repository.storage;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.DetachedEntityMetadata;
import org.sonatype.nexus.common.entity.DetachedEntityVersion;

import org.joda.time.DateTime;

import static org.sonatype.nexus.common.entity.EntityHelper.id;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;

/**
 * Test utils for storage classes
 *
 * @since 3.6.1
 */
public class StorageTestUtil
{
  private static AtomicInteger VERSION_INCREMENTER = new AtomicInteger(1);

  public static Bucket createBucket(final String repositoryName)
  {
    Bucket bucket = new Bucket()
        .attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
    bucket.setEntityMetadata(
        new DetachedEntityMetadata(new DetachedEntityId(repositoryName), new DetachedEntityVersion(getNextVersion())));
    bucket.setRepositoryName(repositoryName);
    return bucket;
  }

  public static Component createComponent(final Bucket bucket,
                                          final String group,
                                          final String name,
                                          final String version)
  {
    return new DefaultComponent()
        .bucketId(id(bucket))
        .format("format-id")
        .group(group)
        .name(name)
        .version(version)
        .attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
  }

  public static Component createDetachedComponent(final Bucket bucket,
                                                  final String group,
                                                  final String name,
                                                  final String version)
  {
    Component component = createComponent(bucket, group, name, version);
    component.setEntityMetadata(
        new DetachedEntityMetadata(new DetachedEntityId(name), new DetachedEntityVersion(getNextVersion())));
    return component;
  }

  public static Asset createAsset(final Bucket bucket, final String name, final Component component) {
    return new Asset()
        .bucketId(id(bucket))
        .format("format-id")
        .name(name)
        .componentId(id(component))
        .createdBy("anonymous")
        .createdByIp("127.0.0.1")
        .size(Long.MAX_VALUE)
        .lastDownloaded(new DateTime(2022, 10, 10, 5, 30, 0))
        .attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
  }

  public static Asset createDetachedAsset(final Bucket bucket, final String name, final Component component) {
    Asset asset = createAsset(bucket, name, component);
    asset.setEntityMetadata(
        new DetachedEntityMetadata(new DetachedEntityId(name), new DetachedEntityVersion(getNextVersion())));
    return asset;
  }

  private static String getNextVersion() {
    return Integer.toString(VERSION_INCREMENTER.addAndGet(1));
  }
}
