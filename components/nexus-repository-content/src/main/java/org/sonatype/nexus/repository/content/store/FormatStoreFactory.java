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

/**
 * Assisted-Inject generic template that defines the API to create content stores for a particular format.
 *
 * @since 3.24
 */
interface FormatStoreFactory
    <
    // first capture the format's store types (order must match BespokeFormatStoreModule)
    CONTENT_REPOSITORY_STORE extends ContentRepositoryStore<CONTENT_REPOSITORY_DAO>,
    COMPONENT_STORE          extends ComponentStore<COMPONENT_DAO>,
    ASSET_STORE              extends AssetStore<ASSET_DAO>,
    ASSET_BLOB_STORE         extends AssetBlobStore<ASSET_BLOB_DAO>,

    // now capture the format's DAO classes (must be in the same order!)
    CONTENT_REPOSITORY_DAO   extends ContentRepositoryDAO,
    COMPONENT_DAO            extends ComponentDAO,
    ASSET_DAO                extends AssetDAO,
    ASSET_BLOB_DAO           extends AssetBlobDAO
    >
{
  CONTENT_REPOSITORY_STORE contentRepositoryStore(String contentStoreName, Class<CONTENT_REPOSITORY_DAO> daoClass);

  COMPONENT_STORE componentStore(String contentStoreName, Class<COMPONENT_DAO> daoClass);

  ASSET_STORE assetStore(String contentStoreName, Class<ASSET_DAO> daoClass);

  ASSET_BLOB_STORE assetBlobStore(String contentStoreName, Class<ASSET_BLOB_DAO> daoClass);
}
