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
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.search.table.SearchCustomFieldContributor;
import org.sonatype.nexus.repository.content.search.table.SearchTableData;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;

import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_CLASSIFIER;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_EXTENSION;

@Singleton
@Named(Maven2Format.NAME)
public class MavenSearchCustomFieldContributor
    implements SearchCustomFieldContributor
{
  @Override
  public void populateSearchCustomFields(final SearchTableData searchTableData, final Asset asset)
  {
    Object formatAttributes = asset.attributes().get(Maven2Format.NAME);

    @SuppressWarnings("unchecked")
    Map<String, String> attributes =
        formatAttributes instanceof Map ? (Map<String, String>) formatAttributes : Collections.emptyMap();

    searchTableData.setFormatField1(attributes.get(P_BASE_VERSION));
    searchTableData.setFormatField2(attributes.get(P_EXTENSION));
    searchTableData.setFormatField3(attributes.get(P_CLASSIFIER));
  }
}
