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
package org.sonatype.nexus.repository.content.store;

import java.util.Optional;
import java.util.OptionalInt;

import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.RepositoryContent;
import org.sonatype.nexus.repository.content.facet.ContentFacet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Integer.parseUnsignedInt;
import static java.lang.Integer.toHexString;
import static java.util.Optional.ofNullable;
import static java.util.OptionalInt.empty;

/**
 * Helper methods to access internal identifiers - USE WITH CAUTION!
 *
 * Where possible prefer the external identifiers such as component coordinates and asset paths.
 *
 * @since 3.24
 */
public class InternalIds
{
  private InternalIds() {
    // static utility class
  }

  // use Knuth's (reversible) multiplicative hash to scatter internal ids
  private static final long TO_EXTERNAL = 1327217885L; // 2 ^ 31 / golden ratio

  private static final long TO_INTERNAL = 828308341L; // multiplicative inverse

  public static EntityId toExternalId(final int internalId) {
    checkArgument(internalId > 0);
    return EntityHelper.id(toHexString((int) (internalId * TO_EXTERNAL))); // NOSONAR: we want truncation
  }

  public static int toInternalId(final EntityId externalId) {
    try {
      return (int) (parseUnsignedInt(externalId.getValue(), 16) * TO_INTERNAL); // NOSONAR: we want truncation
    } catch (NumberFormatException e) {
      // likely a big internal id that was passed as an external id, attempt to parse it directly
      return Integer.parseInt(externalId.getValue());
    }
  }

  public static Optional<Integer> contentRepositoryId(final Repository repository) {
    return repository.optionalFacet(ContentFacet.class).map(ContentFacet::contentRepositoryId);
  }

  public static int contentRepositoryId(final RepositoryContent content) {
    return checkInternalId(((AbstractRepositoryContent) unwrap(content)).repositoryId);
  }

  public static int contentRepositoryId(final ContentStoreEvent event) {
    return event.contentRepositoryId;
  }

  public static int internalComponentId(final Component component) {
    return checkInternalId(((ComponentData) unwrap(component)).componentId);
  }

  public static int internalAssetBlobId(final AssetBlob assetBlob) {
    return checkInternalId(((AssetBlobData) assetBlob).assetBlobId);
  }

  public static int internalAssetId(final Asset asset) {
    return checkInternalId(((AssetData) unwrap(asset)).assetId);
  }

  public static OptionalInt internalComponentId(final Asset asset) {
    AssetData assetData = (AssetData) unwrap(asset);
    return ofNullable(assetData.componentId).map(OptionalInt::of).orElseGet(() ->
        assetData.component()
            .map(InternalIds::internalComponentId)
            .map(OptionalInt::of)
            .orElse(empty()));
  }

  public static OptionalInt internalAssetBlobId(final Asset asset) {
    return ofNullable(((AssetData) unwrap(asset)).assetBlobId).map(OptionalInt::of).orElse(empty());
  }

  private static int checkInternalId(final Integer internalId) {
    checkState(internalId != null, "Entity does not have an internal id; is it detached?");
    return internalId;
  }

  private static RepositoryContent unwrap(final RepositoryContent content) {
    return content instanceof WrappedContent<?> ? ((WrappedContent<?>) content).unwrap() : content;
  }
}
