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

import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.MavenPathParser;

import org.apache.maven.model.Model;

import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.repository.maven.internal.Attributes.AssetKind.ARTIFACT;
import static org.sonatype.nexus.repository.maven.internal.Attributes.AssetKind.ARTIFACT_SUBORDINATE;
import static org.sonatype.nexus.repository.maven.internal.Attributes.AssetKind.OTHER;
import static org.sonatype.nexus.repository.maven.internal.Attributes.AssetKind.REPOSITORY_INDEX;
import static org.sonatype.nexus.repository.maven.internal.Attributes.AssetKind.REPOSITORY_METADATA;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_ARTIFACT_ID;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_CLASSIFIER;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_EXTENSION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_GROUP_ID;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_PACKAGING;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_POM_DESCRIPTION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_POM_NAME;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Maven2Format.NAME;

/**
 * Helper class used by {@link MavenContentFacetImpl} for setting Asset and Component attributes
 *
 * @since 3.26
 */
final class MavenAttributesHelper
{
  private static final String JAR = "jar";

  private MavenAttributesHelper() {
    //no-op
  }

  static void setMavenAttributes(final FluentComponent component, final Coordinates coordinates) {
    Map<String, String> mavenAttributes = new HashMap<>();
    mavenAttributes.put(P_GROUP_ID, coordinates.getGroupId());
    mavenAttributes.put(P_ARTIFACT_ID, coordinates.getArtifactId());
    mavenAttributes.put(P_VERSION, coordinates.getVersion());
    mavenAttributes.put(P_BASE_VERSION, coordinates.getBaseVersion());
    component.withAttribute(NAME, mavenAttributes);
  }

  static void setMavenAttributes(final FluentAsset asset, final MavenPath mavenPath) {
    Map<String, String> mavenAttributes = new HashMap<>();
    Coordinates coordinates = mavenPath.getCoordinates();
    if (coordinates != null) {
      mavenAttributes.put(P_GROUP_ID, coordinates.getGroupId());
      mavenAttributes.put(P_ARTIFACT_ID, coordinates.getArtifactId());
      mavenAttributes.put(P_VERSION, coordinates.getVersion());
      mavenAttributes.put(P_BASE_VERSION, coordinates.getBaseVersion());
      ofNullable(coordinates.getClassifier()).ifPresent(value -> mavenAttributes.put(P_CLASSIFIER, value));
      mavenAttributes.put(P_EXTENSION, coordinates.getExtension());
    }
    asset.withAttribute(NAME, mavenAttributes);
  }

  static void setPomAttributes(final FluentComponent component, final Model model) {
    Map<String, Object> pomAttributes = new HashMap<>(component.attributes(NAME).backing());
    pomAttributes.put(P_PACKAGING, getPackaging(model));
    ofNullable(model.getName()).ifPresent(name -> pomAttributes.put(P_POM_NAME, name));
    ofNullable(model.getDescription()).ifPresent(desc -> pomAttributes.put(P_POM_DESCRIPTION, desc));
    component.withAttribute(NAME, pomAttributes);
  }

  static String getPackaging(Model model) {
    String packaging = model.getPackaging();
    return packaging == null ? JAR : packaging;
  }

  static String assetKind(final MavenPath mavenPath, final MavenPathParser mavenPathParser) {
    if (mavenPath.getCoordinates() != null) {
      return artifactRelatedAssetKind(mavenPath);
    }
    else {
      return fileAssetKindFor(mavenPath, mavenPathParser);
    }
  }

  private static String artifactRelatedAssetKind(final MavenPath mavenPath) {
    return mavenPath.isSubordinate() ? ARTIFACT_SUBORDINATE.name() : ARTIFACT.name();
  }

  private static String fileAssetKindFor(final MavenPath mavenPath, final MavenPathParser mavenPathParser) {
    if (mavenPathParser.isRepositoryMetadata(mavenPath)) {
      return REPOSITORY_METADATA.name();
    }
    else if (mavenPathParser.isRepositoryIndex(mavenPath)) {
      return REPOSITORY_INDEX.name();
    }
    else {
      return OTHER.name();
    }
  }
}
