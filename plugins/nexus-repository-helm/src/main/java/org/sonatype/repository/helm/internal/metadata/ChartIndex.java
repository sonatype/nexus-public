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
package org.sonatype.repository.helm.internal.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.repository.helm.internal.util.JodaDateTimeDeserializer;
import org.sonatype.repository.helm.internal.util.JodaDateTimeSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.joda.time.DateTime;

/**
 * Object for storing attributes in a Helm index.yaml file
 *
 * @since 3.28
 */
public final class ChartIndex
{
  private String apiVersion;
  private Map<String, List<ChartEntry>> entries;
  @JsonSerialize(using = JodaDateTimeSerializer.class)
  @JsonDeserialize(using = JodaDateTimeDeserializer.class)
  private DateTime generated;

  public ChartIndex() {
    this.entries = new HashMap<>();
  }

  public String getApiVersion() {
    return this.apiVersion;
  }

  public void setApiVersion(final String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public Map<String, List<ChartEntry>> getEntries() {
    return this.entries;
  }

  public void addEntry(final ChartEntry chartEntry) {
    this.entries.computeIfAbsent(chartEntry.getName(), k -> new ArrayList<>()).add(chartEntry);
  }

  public void setEntries(final Map<String, List<ChartEntry>> entries) {
    this.entries = entries;
  }

  public DateTime getGenerated() { return this.generated; }

  public void setGenerated(final DateTime generated) {
    this.generated = generated;
  }
}
