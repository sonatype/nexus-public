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

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Iterables.isEmpty;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * Standard methods for replicating components/assets in Orientdb.
 *
 * @since 3.35
 */
@Named
@Singleton
public class OrientReplicationIngestionService
    extends ComponentSupport
{
  @TransactionalStoreBlob
  public Component replicateComponent(
      final Repository repository,
      final String name,
      final String version,
      @Nullable final String group,
      final NestedAttributesMap componentAttributes)
  {
    final StorageTx tx = UnitOfWork.currentTx();
    final Bucket bucket = tx.findBucket(repository);
    Component component = findComponent(repository, name, version, group);
    if (component == null) {
      component = tx.createComponent(bucket, repository.getFormat())
          .name(name)
          .version(version)
          .group(group)
          .attributes(componentAttributes);
      tx.saveComponent(component);
    }
    else {
      NestedAttributesMap currentAttributes = component.attributes();
      if (shouldSaveAttributes(currentAttributes, componentAttributes)) {
        component.attributes(componentAttributes);
        tx.saveComponent(component);
      }
    }
    return component;
  }

  @Nullable
  private static Component findComponent(
      final Repository repository,
      final String name,
      final String version,
      @Nullable String group)
  {
    final StorageTx tx = UnitOfWork.currentTx();

    Query.Builder query = Query.builder()
        .where(P_NAME).eq(name)
        .and(P_VERSION).eq(version);

    if (group != null) {
      query = query.and(P_GROUP).eq(group);
    }

    return getFirst(tx.findComponents(query.build(), singletonList(repository)), null);
  }

  private boolean shouldSaveAttributes(
      final NestedAttributesMap currentAttributes,
      final NestedAttributesMap componentAttributes)
  {
    // Since component attributes are rarely used, don't save if both current attributes and component attributes are empty
    return !currentAttributes.isEmpty() || !componentAttributes.isEmpty();
  }

  public Asset replicateAsset(
      final Repository repository,
      final String name,
      final NestedAttributesMap attributes,
      final AssetBlob blob)
  {
    return replicateAsset(repository, name, attributes, blob, null);
  }

  @TransactionalStoreBlob
  public Asset replicateAsset(
      final Repository repository,
      final String name,
      final NestedAttributesMap attributes,
      final AssetBlob blob,
      @Nullable Component component)
  {
    final StorageTx tx = UnitOfWork.currentTx();
    final Bucket bucket = tx.findBucket(repository);
    Asset asset = findAsset(tx, bucket, name);
    if (asset == null) {
      if (component != null) {
        asset = tx.createAsset(bucket, component).name(name);
      }
      else {
        asset = tx.createAsset(bucket, repository.getFormat()).name(name);
      }
    }

    blob.setReplicated(true);
    tx.attachBlob(asset, blob);
    asset.attributes(attributes);
    tx.saveAsset(asset);

    return asset;
  }

  @Nullable
  private Asset findAsset(final StorageTx tx, final Bucket bucket, final String name) {
    return tx.findAssetWithProperty(MetadataNodeEntityAdapter.P_NAME, name, bucket);
  }

  @TransactionalDeleteBlob
  public boolean replicateDeleteAsset(final Repository repository, final String name) {
    final StorageTx tx = UnitOfWork.currentTx();
    final Bucket bucket = tx.findBucket(repository);
    Asset asset = findAsset(tx, bucket, name);

    if (asset != null) {
      tx.deleteAsset(asset, true);
      if (asset.componentId() != null) {
        // delete component if there are no other assets
        Component component = tx.findComponent(asset.componentId());
        if (component != null && isEmpty(tx.browseAssets(component))) {
          tx.deleteComponent(component, true);
        }
      }
      return true;
    }

    return false;
  }
}
