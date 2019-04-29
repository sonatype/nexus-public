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
package org.sonatype.nexus.blobstore;

import java.util.List;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.formfields.FormField;

/**
 * Describes a blob store.
 *
 * @since 3.6
 */
public interface BlobStoreDescriptor
{
  /**
   * A user friendly name of the blob store type to be presented in UI.
   *
   * @return blob store name
   */
  String getName();

  /**
   * Form fields to configure the blob store.
   *
   * @return blob store configuration form fields
   */
  List<FormField> getFormFields();

  /**
   * @return true if the blob store can be modified after creating.
   *
   * @since 3.14
   */
  default boolean isModifiable() {
    return true;
  }

  /**
   * Validate configuration.
   *
   * @since 3.14
   */
  default void validateConfig(BlobStoreConfiguration config) {
  }

  /**
   * Modifies the config to ensure the input is valid
   *
   * @since 3.next
   */
  default void sanitizeConfig(BlobStoreConfiguration config) {
  }

  /**
   * @return true if the blob store type is enabled.
   *
   * @since 3.14
   */
  default boolean isEnabled() {
    return true;
  }

  /**
   * @return true if the configuration has a dependency on another blobstore with the given name
   *
   * @since 3.14
   */
  default boolean configHasDependencyOn(BlobStoreConfiguration config, String blobStoreName) {
    return false;
  }
}
