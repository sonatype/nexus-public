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
package org.sonatype.nexus.repository.r.internal.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.mail.internet.InternetHeaders;

import org.sonatype.nexus.repository.r.internal.RException;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import static com.google.common.base.Charsets.UTF_8;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.GZIP;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_DEPENDS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_IMPORTS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_LICENSE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_LINKINGTO;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_NEEDS_COMPILATION;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_PACKAGE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_SUGGESTS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_VERSION;
import static org.sonatype.nexus.repository.r.internal.util.RMetadataUtils.parseDescriptionFile;

/**
 * Utility methods for working with R Packages.
 *
 * @since 3.28
 */
public final class RPackagesUtils
{
  public static List<Map<String, String>> parseMetadata(final InputStream in) {
    List<Map<String, String>> entries = new ArrayList<>();
    try (InputStreamReader inr = new InputStreamReader(in, UTF_8)) {
      try (BufferedReader br = new BufferedReader(inr)) {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
          if (line.trim().isEmpty()) {
            String content = sb.toString();
            if (!content.trim().isEmpty()) {
              entries.add(parseDescriptionFile(new ByteArrayInputStream(content.getBytes(UTF_8))));
            }
            sb = new StringBuilder();
          }
          else {
            sb.append(line);
            sb.append('\n');
          }
        }
        String content = sb.toString();
        if (!content.trim().isEmpty()) {
          entries.add(parseDescriptionFile(new ByteArrayInputStream(content.getBytes(UTF_8))));
        }
      }
      return entries;
    }
    catch (IOException e) {
      throw new RException(null, e);
    }
  }

  public static Content buildPackages(final Collection<Map<String, String>> entries) throws IOException {
    CompressorStreamFactory compressorStreamFactory = new CompressorStreamFactory();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try (CompressorOutputStream cos = compressorStreamFactory.createCompressorOutputStream(GZIP, os)) {
      try (OutputStreamWriter writer = new OutputStreamWriter(cos, UTF_8)) {
        for (Map<String, String> entry : entries) {
          InternetHeaders headers = new InternetHeaders();
          headers.addHeader(P_PACKAGE, entry.get(P_PACKAGE));
          headers.addHeader(P_VERSION, entry.get(P_VERSION));
          headers.addHeader(P_DEPENDS, entry.get(P_DEPENDS));
          headers.addHeader(P_IMPORTS, entry.get(P_IMPORTS));
          headers.addHeader(P_SUGGESTS, entry.get(P_SUGGESTS));
          headers.addHeader(P_LINKINGTO, entry.get(P_LINKINGTO));
          headers.addHeader(P_LICENSE, entry.get(P_LICENSE));
          headers.addHeader(P_NEEDS_COMPILATION, entry.get(P_NEEDS_COMPILATION));
          Enumeration<String> headerLines = headers.getAllHeaderLines();
          while (headerLines.hasMoreElements()) {
            String line = headerLines.nextElement();
            writer.write(line, 0, line.length());
            writer.write('\n');
          }
          writer.write('\n');
        }
      }
    }
    catch ( CompressorException e ) {
      throw new RException(null, e);
    }
    return new Content(new BytesPayload(os.toByteArray(), "application/x-gzip"));
  }

  public static List<Map<String, String>> merge(List<List<Map<String, String>>> parts) {
    final LinkedHashMap<String, Map<String, String>> merged = new LinkedHashMap<>();
    for (List<Map<String, String>> part : parts) {
      for (Map<String, String> thisEntry : part) {
        merged.putIfAbsent(thisEntry.get(P_PACKAGE), thisEntry);
      }
    }
    return new ArrayList<>(merged.values());
  }

  private RPackagesUtils() {
    // empty
  }
}
