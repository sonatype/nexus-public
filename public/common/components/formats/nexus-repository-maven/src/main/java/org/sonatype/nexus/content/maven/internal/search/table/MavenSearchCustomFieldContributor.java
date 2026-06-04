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
package org.sonatype.nexus.content.maven.internal.search.table;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.search.sql.SearchCustomFieldContributor;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.search.sql.SearchRecord;

import static org.sonatype.nexus.repository.maven.internal.Attributes.P_ARTIFACT_ID;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_CLASSIFIER;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_EXTENSION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_GROUP_ID;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

@Component
@Qualifier(Maven2Format.NAME)
public class MavenSearchCustomFieldContributor
    implements SearchCustomFieldContributor
{
  @Override
  public void populateSearchCustomFields(final SearchRecord searchTableData, final Asset asset) {

    Object formatAttributes = asset.attributes().get(Maven2Format.NAME);

    @SuppressWarnings("unchecked")
    Map<String, String> attributes =
        formatAttributes instanceof Map ? (Map<String, String>) formatAttributes : Collections.emptyMap();

    Optional.ofNullable(attributes.get(P_BASE_VERSION))
        .ifPresent(token -> searchTableData.addFormatFieldValue1(token, true));
    Optional.ofNullable(attributes.get(P_EXTENSION))
        .ifPresent(searchTableData::addFormatFieldValue2);
    Optional.ofNullable(attributes.get(P_CLASSIFIER))
        .ifPresent(searchTableData::addFormatFieldValue3);

    buildGavec(searchTableData, attributes);
  }

  private static void buildGavec(final SearchRecord searchTableData, final Map<String, String> attributes) {
    Stream.of(attributes.get(P_GROUP_ID),
        attributes.get(P_ARTIFACT_ID),
        attributes.get(P_BASE_VERSION),
        attributes.get(P_EXTENSION),
        attributes.get(P_CLASSIFIER))
        .filter(Objects::nonNull)
        .forEach(field -> searchTableData.addFormatFieldValue4(field, true));
  }
}
