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
package org.sonatype.nexus.proxy.maven.maven2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataBuilder;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataException;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataOperand;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataOperation;
import org.sonatype.nexus.proxy.maven.metadata.operations.NexusMergeOperation;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.junit.Test;

// This is an IT just because it runs longer then 15 seconds
public class NexusMetadataMergeIT
{
  private static final boolean DUMP = false;

  public Metadata doMergeAsM2GroupRepositoryDoes(final List<Metadata> existingMetadatas)
      throws MetadataException
  {
    Metadata result = existingMetadatas.get(0);

    List<MetadataOperation> ops = new ArrayList<MetadataOperation>();

    for (int i = 1; i < existingMetadatas.size(); i++) {
      ops.add(new NexusMergeOperation(new MetadataOperand(existingMetadatas.get(i))));
    }

    MetadataBuilder.changeMetadataIgnoringFailures(result, ops);

    return result;
  }

  public Metadata parseMetadata(InputStream inputStream)
      throws IOException
  {
    try (InputStream in = inputStream) {
      return MetadataBuilder.read(in);
    }
  }

  public void dumpMetadata(final Metadata metadata, PrintStream pw)
      throws IOException
  {
    if (DUMP) {
      final ByteArrayOutputStream sos = new ByteArrayOutputStream();

      MetadataBuilder.write(metadata, sos);

      pw.println(sos.toString());
    }
  }

  public List<Metadata> prepareListOfClonedMetadata(final String resourceName, final int count)
      throws IOException
  {
    final Metadata metadata = parseMetadata(getClass().getClassLoader().getResourceAsStream(resourceName));

    ArrayList<Metadata> metadatas = new ArrayList<Metadata>(count);

    for (int i = 0; i < count; i++) {
      metadatas.add(metadata.clone());
    }

    return metadatas;
  }

  public int performGMetadataMerge(final String resourceName, final int cloneCount, final boolean muck)
      throws Exception
  {
    List<Metadata> metadatas = prepareListOfClonedMetadata(resourceName, cloneCount);

    if (muck) {
      int cnt = 0;

      for (Metadata metadata : metadatas) {
        cnt++;

        for (Plugin plugin : metadata.getPlugins()) {
          plugin.setPrefix(plugin.getPrefix() + String.valueOf(cnt));
        }
      }
    }

    final Metadata result = doMergeAsM2GroupRepositoryDoes(metadatas);

    dumpMetadata(result, System.out);

    return result.getPlugins().size();
  }

  public int performGAMetadataMerge(final String resourceName, final int cloneCount, final boolean muck)
      throws Exception
  {
    List<Metadata> metadatas = prepareListOfClonedMetadata(resourceName, cloneCount);

    if (muck) {
      int cnt = 0;

      for (Metadata metadata : metadatas) {
        cnt++;

        Versioning versioning = metadata.getVersioning();
        versioning.setLatest(versioning.getLatest() + String.valueOf(cnt));
        versioning.setRelease(versioning.getRelease() + String.valueOf(cnt));

        ArrayList<String> mockedVersions = new ArrayList<String>(versioning.getVersions().size());
        for (String version : versioning.getVersions()) {
          mockedVersions.add(version + String.valueOf(cnt));
        }
        versioning.setVersions(mockedVersions);
      }
    }

    final Metadata result = doMergeAsM2GroupRepositoryDoes(metadatas);

    dumpMetadata(result, System.out);

    return result.getVersioning().getVersions().size();
  }

  public int performGAVMetadataMerge(final String resourceName, final int cloneCount, final boolean muck)
      throws Exception
  {
    List<Metadata> metadatas = prepareListOfClonedMetadata(resourceName, cloneCount);

    if (muck) {
      int cnt = 0;

      for (Metadata metadata : metadatas) {
        cnt++;

        Versioning versioning = metadata.getVersioning();
        versioning.setLatest(versioning.getLatest() + String.valueOf(cnt));
        versioning.setRelease(versioning.getRelease() + String.valueOf(cnt));

        for (SnapshotVersion version : versioning.getSnapshotVersions()) {
          version.setClassifier(version.getClassifier() + String.valueOf(cnt));
          version.setVersion(version.getVersion() + String.valueOf(cnt));
        }
      }
    }

    final Metadata result = doMergeAsM2GroupRepositoryDoes(metadatas);

    dumpMetadata(result, System.out);

    return result.getVersioning().getSnapshotVersions().size();
  }

  public void doTestGMetadataMerge(final String mdResourceName, final int sources, final boolean muck)
      throws Exception
  {
    execTimed(new Callable<String>()
    {
      @Override
      public String call()
          throws Exception
      {
        return "G merged, " + sources + " sources, resulting unique entry count "
            + performGMetadataMerge(mdResourceName, sources, muck);
      }
    });
  }

  public void doTestGAMetadataMerge(final String mdResourceName, final int sources, final boolean muck)
      throws Exception
  {
    execTimed(new Callable<String>()
    {
      @Override
      public String call()
          throws Exception
      {
        return "GA merged, " + sources + " sources, resulting unique entry count "
            + performGAMetadataMerge(mdResourceName, sources, muck);
      }
    });
  }

  public void doTestGAVMetadataMerge(final String mdResourceName, final int sources, final boolean muck)
      throws Exception
  {
    execTimed(new Callable<String>()
    {
      @Override
      public String call()
          throws Exception
      {
        return "GAV merged, " + sources + " sources, resulting unique entry count "
            + performGAVMetadataMerge(mdResourceName, sources, muck);
      }

    });
  }

  public static void execTimed(final Callable<String> callable)
      throws Exception
  {
    final long started = System.currentTimeMillis();

    final String msg = callable.call();

    System.out.println(String.format("Finished in %s seconds: %s ",
        (System.currentTimeMillis() - started) / 1000, msg));
  }

  @Test
  public void testGMetadataMerge()
      throws Exception
  {
    final String mdResourceName = "nexus-4464/g-metadata.xml";

    System.out.println("===");
    System.out.println(mdResourceName);
    System.out.println("===");

    doTestGMetadataMerge(mdResourceName, 100, false);
    doTestGMetadataMerge(mdResourceName, 100, true);
    doTestGMetadataMerge(mdResourceName, 200, false);
    doTestGMetadataMerge(mdResourceName, 200, true);
    doTestGMetadataMerge(mdResourceName, 300, false);
    doTestGMetadataMerge(mdResourceName, 300, true);

    System.out.println("===");
  }

  @Test
  public void testGAMetadataMerge()
      throws Exception
  {
    final String mdResourceName = "nexus-4464/ga-metadata.xml";

    System.out.println("===");
    System.out.println(mdResourceName);
    System.out.println("===");

    doTestGAMetadataMerge(mdResourceName, 2, false);
    doTestGAMetadataMerge(mdResourceName, 2, true);
    doTestGAMetadataMerge(mdResourceName, 5, false);
    doTestGAMetadataMerge(mdResourceName, 5, true);
    doTestGAMetadataMerge(mdResourceName, 10, false);
    doTestGAMetadataMerge(mdResourceName, 10, true);

    System.out.println("===");
  }

  @Test
  public void testGAVMetadataMerge()
      throws Exception
  {
    final String mdResourceName = "nexus-4464/gav-metadata.xml";

    System.out.println("===");
    System.out.println(mdResourceName);
    System.out.println("===");

    doTestGAVMetadataMerge(mdResourceName, 100, false);
    doTestGAVMetadataMerge(mdResourceName, 100, true);
    doTestGAVMetadataMerge(mdResourceName, 200, false);
    doTestGAVMetadataMerge(mdResourceName, 200, true);
    doTestGAVMetadataMerge(mdResourceName, 300, false);
    doTestGAVMetadataMerge(mdResourceName, 300, true);

    System.out.println("===");
  }

  // ==
  // Unusual cases

  @Test
  public void testUnusualGAMetadataMerge()
      throws Exception
  {
    final String mdResourceName = "nexus-4464/releases-all-maven-metadata.xml";

    System.out.println("===");
    System.out.println(mdResourceName);
    System.out.println("===");

    doTestGAMetadataMerge(mdResourceName, 2, false);
    doTestGAMetadataMerge(mdResourceName, 2, true);
    doTestGAMetadataMerge(mdResourceName, 5, false);
    doTestGAMetadataMerge(mdResourceName, 5, true);
    doTestGAMetadataMerge(mdResourceName, 10, false);
    doTestGAMetadataMerge(mdResourceName, 10, true);

    System.out.println("===");
  }

}
