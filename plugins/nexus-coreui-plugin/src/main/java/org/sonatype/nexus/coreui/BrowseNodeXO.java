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
package org.sonatype.nexus.coreui;

import java.util.Objects;
import javax.validation.constraints.NotBlank;

/**
 * Component exchange object.
 *
 * @since 3.6
 */
public class BrowseNodeXO
{
  @NotBlank
  private String id;

  @NotBlank
  private String text;

  @NotBlank
  private String type;

  private boolean leaf;

  private String componentId;

  private String assetId;

  private String packageUrl;

  public String getId() {
    return id;
  }

  public BrowseNodeXO withId(final String id) {
    this.id = id;
    return this;
  }

  public String getText() {
    return text;
  }

  public BrowseNodeXO withText(final String text) {
    this.text = text;
    return this;
  }

  public String getType() {
    return type;
  }

  public BrowseNodeXO withType(final String type) {
    this.type = type;
    return this;
  }

  public boolean isLeaf() {
    return leaf;
  }

  public BrowseNodeXO withLeaf(final boolean leaf) {
    this.leaf = leaf;
    return this;
  }

  public String getComponentId() {
    return componentId;
  }

  public BrowseNodeXO withComponentId(final String componentId) {
    this.componentId = componentId;
    return this;
  }

  public String getAssetId() {
    return assetId;
  }

  public BrowseNodeXO withAssetId(final String assetId) {
    this.assetId = assetId;
    return this;
  }

  public String getPackageUrl() {
    return packageUrl;
  }

  public BrowseNodeXO withPackageUrl(final String packageUrl) {
    this.packageUrl = packageUrl;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BrowseNodeXO other = (BrowseNodeXO) o;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "BrowseNodeXO{" +
        "id='" + id + '\'' +
        ", text='" + text + '\'' +
        ", type='" + type + '\'' +
        ", leaf=" + leaf +
        ", componentId='" + componentId + '\'' +
        ", assetId='" + assetId + '\'' +
        ", packageUrl='" + packageUrl + '\'' +
        '}';
  }
}
