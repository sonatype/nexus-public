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
package org.sonatype.nexus.cleanup.internal.preview;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.rest.api.ComponentXO;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

@Named
@Singleton
public class CsvCleanupPreviewContentWriter
    extends ComponentSupport
{
  public void write(final Repository repository, final Stream<ComponentXO> components, final OutputStream outputStream)
      throws IOException
  {
    log.debug("Creating CSV content for the repository {}.", repository.getName());

    CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
        .setHeader("namespace", "name", "version", "path", "blob store name", "size")
        .build();

    AtomicInteger flushCount = new AtomicInteger();
    AtomicInteger totalCount = new AtomicInteger();
    try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
         CSVPrinter printer = new CSVPrinter(outputStreamWriter, csvFormat)) {

      printer.flush();
      components
          .forEach(componentXO -> {
            try {
              for (AssetXO asset : componentXO.getAssets()) {
                printer.printRecord(componentXO.getGroup(), componentXO.getName(), componentXO.getVersion(),
                    asset.getPath(), asset.getBlobStoreName(), asset.getFileSize());
                totalCount.incrementAndGet();
              }

              if (flushCount.get() == Continuations.BROWSE_LIMIT) {
                printer.flush();
                flushCount.set(0);
              }
              else {
                flushCount.incrementAndGet();
              }
            }
            catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });
    }
    catch (UncheckedIOException e) {
      log.error("Unable to finish writing CSV content for the repository {}. {}.",
          repository.getName(), e.getMessage(), log.isDebugEnabled() ? e : null
      );
      throw new IOException(e);
    }

    log.debug("Finished CSV content for the repository {}. Total lines {}.", repository.getName(), totalCount.get());
  }
}
