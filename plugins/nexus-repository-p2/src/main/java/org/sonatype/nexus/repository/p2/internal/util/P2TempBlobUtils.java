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
package org.sonatype.nexus.repository.p2.internal.util;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.p2.internal.exception.AttributeParsingException;
import org.sonatype.nexus.repository.p2.internal.metadata.P2Attributes;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Shared code between P2 facets.
 *
 * @since 3.28
 */
@Named
public class P2TempBlobUtils
    extends ComponentSupport
{
  private final AttributesParserFeatureXml featureXmlParser;

  private final AttributesParserManifest manifestParser;

  @Inject
  public P2TempBlobUtils(final AttributesParserFeatureXml featureXmlParser,
                         final AttributesParserManifest manifestParser)
  {
    this.featureXmlParser = checkNotNull(featureXmlParser);
    this.manifestParser = checkNotNull(manifestParser);
  }

  public P2Attributes mergeAttributesFromTempBlob(final TempBlob tempBlob, final P2Attributes sourceP2Attributes)
      throws IOException
  {
    checkNotNull(sourceP2Attributes.getExtension());
    P2Attributes p2Attributes = null;
    try {
      // first try Features XML
      p2Attributes = featureXmlParser.getAttributesFromBlob(tempBlob, sourceP2Attributes.getExtension());

      // second try Manifest
      if (p2Attributes.isEmpty()) {
        p2Attributes = manifestParser.getAttributesFromBlob(tempBlob, sourceP2Attributes.getExtension());
      }
    }
    catch (AttributeParsingException ex) {
      log.warn("Could not get attributes from feature.xml due to following exception: {}", ex.getMessage());
    }

    return Optional.ofNullable(p2Attributes)
        .filter(jarP2Attributes -> !jarP2Attributes.isEmpty())
        .map(jarP2Attributes -> P2Attributes.builder().merge(sourceP2Attributes, jarP2Attributes).build())
        .orElse(sourceP2Attributes);
  }
}
