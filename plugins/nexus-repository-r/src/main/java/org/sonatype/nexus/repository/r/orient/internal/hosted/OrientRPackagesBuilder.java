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
package org.sonatype.nexus.repository.r.orient.internal.hosted;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.mail.internet.InternetHeaders;

import org.sonatype.nexus.repository.r.internal.hosted.RPackageVersion;
import org.sonatype.nexus.repository.storage.Asset;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import static com.google.common.base.Charsets.UTF_8;
import static java.util.Collections.unmodifiableMap;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.GZIP;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_DEPENDS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_IMPORTS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_LICENSE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_LINKINGTO;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_NEEDS_COMPILATION;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_PACKAGE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_SUGGESTS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_VERSION;

/**
 * Builds the contents of a PACKAGES.gz file based on the provided assets, taking into account the greatest version of a
 * particular package that is available in a (hosted) repository.
 *
 * Note that this maintains all pertinent information for the "latest" version of each package in memory, though the
 * actual amount of information for each package is rather small.
 *
 * TODO: Add support for other metadata types (PACKAGES, PACKAGES.rds etc.)
 *
 * @since 3.28
 */
public class OrientRPackagesBuilder
{
  /**
   * The greatest version of each package currently encountered during this run.
   */
  private final Map<String, RPackageVersion> packageVersions = new HashMap<>();

  /**
   * The package information as of the last time we processed the greatest version of this package. A {@code TreeMap} is
   * used to maintain ordering by package name.
   */
  private final Map<String, Map<String, String>> packageInformation = new TreeMap<>();

  /**
   * Returns an unmodifiable map containing the package information to write to a PACKAGES file. The iteration order of
   * the map's keys is such that the package names will be returned in sorted order.
   *
   * @return The map of package information, keyed by package name.
   */
  public Map<String, Map<String, String>> getPackageInformation() {
    return unmodifiableMap(packageInformation);
  }

  /**
   * Processes an asset, updating the greatest version and details for the package if appropriate.
   *
   * @param asset The asset to process.
   */
  public void append(final Asset asset) {
    // is this a newer version of this asset's package than the one we currently have (if we have one)?
    String packageName = asset.formatAttributes().get(P_PACKAGE, String.class);
    RPackageVersion oldVersion = packageVersions.get(packageName);
    RPackageVersion newVersion = new RPackageVersion(asset.formatAttributes().get(P_VERSION, String.class));
    if (oldVersion == null || newVersion.compareTo(oldVersion) > 0) {

      // if so, use the most recent information instead and update the greatest version encountered
      Map<String, String> newInformation = new HashMap<>();
      newInformation.put(P_PACKAGE, asset.formatAttributes().get(P_PACKAGE, String.class));
      newInformation.put(P_VERSION, asset.formatAttributes().get(P_VERSION, String.class));
      newInformation.put(P_DEPENDS, asset.formatAttributes().get(P_DEPENDS, String.class));
      newInformation.put(P_IMPORTS, asset.formatAttributes().get(P_IMPORTS, String.class));
      newInformation.put(P_SUGGESTS, asset.formatAttributes().get(P_SUGGESTS, String.class));
      newInformation.put(P_LINKINGTO, asset.formatAttributes().get(P_LINKINGTO, String.class));
      newInformation.put(P_LICENSE, asset.formatAttributes().get(P_LICENSE, String.class));
      newInformation.put(P_NEEDS_COMPILATION, asset.formatAttributes().get(P_NEEDS_COMPILATION, String.class));

      packageVersions.put(packageName, newVersion);
      packageInformation.put(packageName, newInformation);
    }
  }

  /**
   * Using collected package details builds PACKAGES.gz file and returns it as byte array.
   * <p>
   * Call this method ONLY after all information about packages is appended to packageInformation map.
   *
   * @return PACKAGES.gz as byte array.
   */
  public byte[] buildPackagesGz() throws IOException {
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      CompressorStreamFactory compressorStreamFactory = new CompressorStreamFactory();
      try (CompressorOutputStream cos = compressorStreamFactory.createCompressorOutputStream(GZIP, os);
           OutputStreamWriter writer = new OutputStreamWriter(cos, UTF_8)) {
        for (Entry<String, Map<String, String>> eachPackage : packageInformation.entrySet()) {
          writePackageInfo(writer, eachPackage.getValue());
        }
      }
      return os.toByteArray();
    }
    catch (CompressorException e) {
      throw new IOException("Error compressing metadata", e);
    }
  }

  private void writePackageInfo(final OutputStreamWriter writer, final Map<String, String> packageInfo)
      throws IOException
  {
    InternetHeaders headers = new InternetHeaders();
    headers.addHeader(P_PACKAGE, packageInfo.get(P_PACKAGE));
    headers.addHeader(P_VERSION, packageInfo.get(P_VERSION));
    headers.addHeader(P_DEPENDS, packageInfo.get(P_DEPENDS));
    headers.addHeader(P_IMPORTS, packageInfo.get(P_IMPORTS));
    headers.addHeader(P_SUGGESTS, packageInfo.get(P_SUGGESTS));
    headers.addHeader(P_LINKINGTO, packageInfo.get(P_LINKINGTO));
    headers.addHeader(P_LICENSE, packageInfo.get(P_LICENSE));
    headers.addHeader(P_NEEDS_COMPILATION, packageInfo.get(P_NEEDS_COMPILATION));
    Enumeration<String> headerLines = headers.getAllHeaderLines();
    while (headerLines.hasMoreElements()) {
      String line = headerLines.nextElement();
      writer.write(line, 0, line.length());
      writer.write('\n');
    }
    writer.write('\n');
  }
}
