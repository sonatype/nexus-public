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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.formfields.FormField;

/**
 * Describes a blob store.
 * Note: Do not inject Map<String, BlobStoreDescriptors> and instead opt to use BlobStoreDescriptorProvider
 *
 * @since 3.6
 */
public interface BlobStoreDescriptor
{
  /**
   * A url-friendly identifier. This must match the identifier used for the blob store REST apis.
   */
  String getId();

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
   * Name of a custom form for configuring the blob store.
   *
   * @return custom form name
   */
  @Nullable
  default String customFormName() {
    return null;
  }

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
   * @since 3.17
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

  /**
   * @return true if the the blob store has a connection to be tested by the backend.
   *
   * @since 3.30
   */
  default boolean isConnectionTestable() {
    return false;
  }

  /**
   * @return a list of the names of the fields that should be encrypted in the configuration
   */
  default List<String> getSensitiveConfigurationFields() {
    return Collections.emptyList();
  }

  default Map<String, List<SelectOption>> getDropDownValues() {
    return null;
  }
}
