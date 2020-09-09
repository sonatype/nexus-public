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
package org.sonatype.nexus.repository.p2.internal.metadata

import javax.annotation.Nullable

import org.sonatype.nexus.repository.p2.internal.AssetKind

import groovy.transform.EqualsAndHashCode

import static java.util.Optional.ofNullable

/**
 * Shared attributes of P2 metadata files.
 */
@EqualsAndHashCode
class P2Attributes
{
  private String pluginName

  private String componentName

  private String componentVersion

  private String path

  private String fileName

  private String extension

  private AssetKind assetKind

  private P2Attributes(final Builder builder) {
    this.pluginName = builder.pluginName
    this.componentName = builder.componentName
    this.componentVersion = builder.componentVersion
    this.path = builder.path
    this.fileName = builder.fileName
    this.extension = builder.extension
    this.assetKind = builder.assetKind
  }

  static Builder builder() {
    return new Builder()
  }

  boolean isEmpty() {
    String[] mainP2Properties = [pluginName, componentName, componentVersion, path, fileName, extension, assetKind]
    for (String a : mainP2Properties) {
      if (a != null) {
        return false
      }
    }
    return true
  }

  static class Builder
  {
    private String pluginName

    private String componentName

    private String componentVersion

    private String path

    private String fileName

    private String extension

    private AssetKind assetKind

    private Builder() {
    }

    Builder pluginName(final String groupName) {
      this.pluginName = groupName
      return this
    }

    Builder componentName(final String componentName) {
      this.componentName = componentName
      return this
    }

    Builder componentVersion(final String componentVersion) {
      this.componentVersion = componentVersion
      return this
    }

    Builder path(final String path) {
      this.path = path
      return this
    }

    Builder fileName(final String fileName) {
      this.fileName = fileName
      return this
    }

    Builder extension(final String extension) {
      this.extension = extension
      return this
    }

    Builder assetKind(final AssetKind assetKind) {
      this.assetKind = assetKind
      return this
    }

    P2Attributes build() {
      return new P2Attributes(this)
    }

    Builder merge(final P2Attributes one, P2Attributes two) {
      componentVersion(ofNullable(two.getComponentVersion()).orElse(one.getComponentVersion()))
      componentName(ofNullable(two.getComponentName()).orElse(one.getComponentName()))
      pluginName(ofNullable(two.getPluginName()).orElse(one.getPluginName()))
      path(ofNullable(two.getPath()).orElse(one.getPath()))
      fileName(ofNullable(two.getFileName()).orElse(one.getFileName()))
      extension(ofNullable(two.getExtension()).orElse(one.getExtension()))
      assetKind(ofNullable(two.getAssetKind()).orElse(one.getAssetKind()))
      return this
    }
  }

  @Nullable
  String getPluginName() {
    return pluginName
  }

  @Nullable
  String getComponentName() {
    return componentName
  }

  @Nullable
  String getComponentVersion() {
    return componentVersion
  }

  @Nullable
  String getPath() {
    return path
  }

  @Nullable
  String getFileName() {
    return fileName
  }

  @Nullable
  String getExtension() {
    return extension
  }

  @Nullable
  AssetKind getAssetKind() {
    return assetKind
  }
}
