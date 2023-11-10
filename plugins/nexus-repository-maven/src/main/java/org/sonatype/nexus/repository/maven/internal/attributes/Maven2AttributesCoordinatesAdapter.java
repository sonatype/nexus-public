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
package org.sonatype.nexus.repository.maven.internal.attributes;

import java.util.Map;
import java.util.TreeMap;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.application.scan.AttributesCoordinatesAdapter;
import org.sonatype.nexus.repository.maven.internal.Attributes;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;

@Named(Maven2Format.NAME)
@Singleton
public class Maven2AttributesCoordinatesAdapter
    implements AttributesCoordinatesAdapter
{
  private static final String WILDCARD = "*";

  @Override
  public Map<String, String> toCoordinates(final NestedAttributesMap attributes) {
    Map<String, String> coordinates = new TreeMap<>();
    coordinates.put(Attributes.P_GROUP_ID, attributes.get(Attributes.P_GROUP_ID, String.class));
    coordinates.put(Attributes.P_ARTIFACT_ID, attributes.get(Attributes.P_ARTIFACT_ID, String.class));
    coordinates.put(Attributes.P_VERSION, attributes.get(Attributes.P_VERSION, String.class));
    coordinates.put(Attributes.P_CLASSIFIER, attributes.get(Attributes.P_CLASSIFIER, String.class));
    coordinates.put(Attributes.P_EXTENSION, attributes.get(Attributes.P_EXTENSION, String.class));
    return coordinates;
  }

  @Override
  public Map<String, String> toArtifactCoordinates(final Map<String, String> coordinates) {
    Map<String, String> artifactCoordinates = new TreeMap<>(coordinates);
    artifactCoordinates.put(Attributes.P_CLASSIFIER, WILDCARD);
    artifactCoordinates.put(Attributes.P_EXTENSION, WILDCARD);
    return artifactCoordinates;
  }
}
