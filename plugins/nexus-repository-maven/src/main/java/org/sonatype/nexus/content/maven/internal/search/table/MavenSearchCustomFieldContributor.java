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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.search.sql.SearchCustomFieldContributor;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.search.sql.SearchRecord;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryContribution;

import static java.util.stream.Collectors.joining;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_ARTIFACT_ID;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_CLASSIFIER;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_EXTENSION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_GROUP_ID;

@Singleton
@Named(Maven2Format.NAME)
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
        .map(SqlSearchQueryContribution::preventTokenization)
        .ifPresent(searchTableData::addFormatFieldValue1);
    Optional.ofNullable(attributes.get(P_EXTENSION))
        .ifPresent(searchTableData::addFormatFieldValue2);
    Optional.ofNullable(attributes.get(P_CLASSIFIER))
        .ifPresent(searchTableData::addFormatFieldValue3);

    buildGavec(searchTableData, attributes);
  }

  private void buildGavec(final SearchRecord searchTableData, final Map<String, String> attributes) {
    searchTableData.addFormatFieldValue4(getMavenAttributes(attributes)
        .filter(Objects::nonNull)
        .map(SqlSearchQueryContribution::preventTokenization)
        .collect(joining(" "))
    );
  }

  private Stream<String> getMavenAttributes(final Map<String, String> attributes) {
    return Stream.of(attributes.get(P_GROUP_ID),
        attributes.get(P_ARTIFACT_ID),
        attributes.get(P_BASE_VERSION),
        attributes.get(P_EXTENSION),
        attributes.get(P_CLASSIFIER));
  }

  public static Set<String> toStoredBaseVersionFormat(final Set<String> baseVersions) {
    return baseVersions.stream()
        .map(SqlSearchQueryContribution::matchUntokenizedValue)
        .collect(Collectors.toSet());
  }
}
