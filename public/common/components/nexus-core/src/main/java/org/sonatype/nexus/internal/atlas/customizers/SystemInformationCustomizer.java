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
package org.sonatype.nexus.internal.atlas.customizers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.nexus.common.atlas.SystemInformationGenerator;
import org.sonatype.nexus.supportzip.GeneratedContentSourceSupport;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundleCustomizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.REQUIRED;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.SYSINFO;
import org.springframework.stereotype.Component;

/**
 * Adds system information report to support bundle.
 *
 * @since 2.7
 */
@Component
@Singleton
public class SystemInformationCustomizer
    implements SupportBundleCustomizer
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final SystemInformationGenerator systemInformationGenerator;

  private final ObjectMapper objectMapper;

  @Inject
  public SystemInformationCustomizer(final SystemInformationGenerator systemInformationGenerator) {
    this.systemInformationGenerator = checkNotNull(systemInformationGenerator);
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public void customize(final SupportBundle supportBundle) {
    supportBundle.add(new GeneratedContentSourceSupport(SYSINFO, "info/sysinfo.json", REQUIRED)
    {
      @Override
      protected void generate(final File file) {
        Map<String, Object> report = systemInformationGenerator.report();
        try (FileOutputStream fos = new FileOutputStream(file)) {
          objectMapper.writerWithDefaultPrettyPrinter().writeValue(fos, report);
        }
        catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    });
  }
}
