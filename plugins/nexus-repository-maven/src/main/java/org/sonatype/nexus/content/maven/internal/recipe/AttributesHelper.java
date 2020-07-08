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
package org.sonatype.nexus.content.maven.internal.recipe;

import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;

import org.apache.maven.model.Model;

import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.repository.maven.internal.Attributes.AssetKind.ARTIFACT;
import static org.sonatype.nexus.repository.maven.internal.Attributes.AssetKind.ARTIFACT_SUBORDINATE;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_ARTIFACT_ID;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_CLASSIFIER;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_EXTENSION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_GROUP_ID;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_PACKAGING;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_POM_DESCRIPTION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_POM_NAME;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_VERSION;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * Helper class used by {@link MavenContentFacetImpl} for setting Asset and Component attributes
 *
 * @since 3.25.0
 */
final class AttributesHelper
{
  private static final String JAR = "jar";

  private AttributesHelper() {
    //no-op
  }

  static void setComponentAttributes(final FluentComponent component, final Coordinates coordinates) {
    component.withAttribute(P_GROUP_ID, coordinates.getGroupId());
    component.withAttribute(P_ARTIFACT_ID, coordinates.getArtifactId());
    component.withAttribute(P_VERSION, coordinates.getVersion());
    component.withAttribute(P_BASE_VERSION, coordinates.getBaseVersion());
  }

  static void setAssetAttributes(final FluentAsset asset, final MavenPath path) {
    Coordinates coordinates = path.getCoordinates();
    if (coordinates != null) {
      asset.withAttribute(P_GROUP_ID, coordinates.getGroupId());
      asset.withAttribute(P_ARTIFACT_ID, coordinates.getArtifactId());
      asset.withAttribute(P_VERSION, coordinates.getVersion());
      asset.withAttribute(P_BASE_VERSION, coordinates.getBaseVersion());
      ofNullable(coordinates.getClassifier()).ifPresent(value -> asset.withAttribute(P_CLASSIFIER, value));
      asset.withAttribute(P_EXTENSION, coordinates.getExtension());
      asset.withAttribute(P_ASSET_KIND, assetKind(path));
    }
  }

  static void setPomAttributes(final FluentComponent component, final Model model) {
    component.withAttribute(P_PACKAGING, getPackaging(model));
    ofNullable(model.getName()).ifPresent(name -> component.withAttribute(P_POM_NAME, name));
    ofNullable(model.getDescription()).ifPresent(desc -> component.withAttribute(P_POM_DESCRIPTION, desc));
  }

  static String getPackaging(Model model) {
    String packaging = model.getPackaging();
    return packaging == null ? JAR : packaging;
  }

  static String assetKind(final MavenPath path) {
    return path.isSubordinate() ? ARTIFACT_SUBORDINATE.name() : ARTIFACT.name();
  }
}
