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
package org.sonatype.nexus.ruby;


public class MetadataSnapshotBuilder
    extends AbstractMetadataBuilder
{
  protected final StringBuilder xml;

  public MetadataSnapshotBuilder(String name, String version, long modified) {
    super(modified);
    String dotted = timestamp.substring(0, 8) + "." + timestamp.substring(8);
    String value = version + "-" + dotted + "-1";
    xml = new StringBuilder();
    xml.append("<metadata>\n");
    xml.append("  <groupId>rubygems</groupId>\n");
    xml.append("  <artifactId>").append(name).append("</artifactId>\n");
    xml.append("  <versioning>\n");
    xml.append("    <versions>\n");
    xml.append("      <snapshot>\n");
    xml.append("        <timestamp>").append(dotted).append("</timestamp>\n");
    xml.append("        <buildNumber>1</buildNumber>\n");
    xml.append("      </snapshot>\n");
    xml.append("      <lastUpdated>").append(timestamp).append("</lastUpdated>\n");
    xml.append("      <snapshotVersions>\n");
    xml.append("        <snapshotVersion>\n");
    xml.append("          <extension>gem</extension>\n");
    xml.append("          <value>").append(value).append("</value>\n");
    xml.append("          <updated>").append(timestamp).append("</updated>\n");
    xml.append("        </snapshotVersion>\n");
    xml.append("        <snapshotVersion>\n");
    xml.append("          <extension>pom</extension>\n");
    xml.append("          <value>").append(value).append("</value>\n");
    xml.append("          <updated>").append(timestamp).append("</updated>\n");
    xml.append("        </snapshotVersion>\n");
    xml.append("      </snapshotVersions>\n");
    xml.append("    </versions>\n");
    xml.append("  </versioning>\n");
    xml.append("</metadata>\n");
  }

  public String toString() {
    return xml.toString();
  }
}
