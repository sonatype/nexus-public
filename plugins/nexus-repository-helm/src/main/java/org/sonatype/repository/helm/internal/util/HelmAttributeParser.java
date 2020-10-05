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
package org.sonatype.repository.helm.internal.util;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.repository.helm.HelmAttributes;
import org.sonatype.repository.helm.internal.AssetKind;

import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.28
 */
@Named
@Singleton
public class HelmAttributeParser
{
  private TgzParser tgzParser;
  private YamlParser yamlParser;
  private ProvenanceParser provenanceParser;

  @Inject
  public HelmAttributeParser(final TgzParser tgzParser,
                             final YamlParser yamlParser,
                             final ProvenanceParser provenanceParser) {
    this.tgzParser = checkNotNull(tgzParser);
    this.yamlParser = checkNotNull(yamlParser);
    this.provenanceParser = checkNotNull(provenanceParser);
  }

  public HelmAttributes getAttributes(final AssetKind assetKind, final InputStream inputStream) throws IOException {
    switch (assetKind) {
      case HELM_PACKAGE:
        return getAttributesFromInputStream(inputStream);
      case HELM_PROVENANCE:
        return getAttributesProvenanceFromInputStream(inputStream);
      default:
        return new HelmAttributes();
    }
  }

  private HelmAttributes getAttributesProvenanceFromInputStream(final InputStream inputStream) throws IOException {
    return provenanceParser.parse(inputStream);
  }

  private HelmAttributes getAttributesFromInputStream(final InputStream inputStream) throws IOException {
    try (InputStream is = tgzParser.getChartFromInputStream(inputStream)) {
      return new HelmAttributes(yamlParser.load(is));
    }
  }

  public static HelmAttributes validateAttributes(final HelmAttributes attributes) {
    if (StringUtils.isBlank(attributes.getName())) {
      throw new ValidationErrorsException("Metadata is missing the name attribute");
    }

    if (StringUtils.isBlank(attributes.getVersion())) {
      throw new ValidationErrorsException("Metadata is missing the version attribute");
    }

    return attributes;
  }
}
