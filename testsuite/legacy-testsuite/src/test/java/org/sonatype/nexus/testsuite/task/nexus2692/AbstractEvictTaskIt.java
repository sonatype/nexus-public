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
package org.sonatype.nexus.testsuite.task.nexus2692;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.proxy.attributes.Attributes;
import org.sonatype.nexus.proxy.attributes.JacksonJSONMarshaller;
import org.sonatype.nexus.proxy.attributes.Marshaller;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.EvictUnusedItemsTaskDescriptor;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.junit.Assert;
import org.junit.Before;

public class AbstractEvictTaskIt
    extends AbstractNexusIntegrationTest
{

  /**
   * Path, Days map. This map holds all the modified attribute paths and the int represents the "offset" (might be negative)
   * or how many days it was shifted.
   */
  private Map<String, Integer> pathMap = Maps.newHashMap();

  private List<String> neverDeleteFiles = Lists.newArrayList();

  private File storageWorkDir;

  @Before
  public void setupStorageAndAttributes()
      throws Exception
  {
    final File workDir = new File(AbstractNexusIntegrationTest.nexusWorkDir);
    storageWorkDir = new File(workDir, "storage");

    FileUtils.copyDirectory(getTestResourceAsFile("storage/"), storageWorkDir);

    // now setup all the attributes
    File attributesInfo = getTestResourceAsFile("attributes.info");

    final Marshaller marshaller = new JacksonJSONMarshaller();

    final long now = System.currentTimeMillis();

    try (BufferedReader reader = new BufferedReader(new FileReader(attributesInfo))) {

      String line = reader.readLine();
      while (line != null) {
        final String[] parts = line.split(" ");
        final String repoFilePart = parts[0];
        final String repoId = repoFilePart.substring(0, repoFilePart.indexOf('/'));
        final String filePart = repoFilePart.substring(repoFilePart.indexOf('/'), repoFilePart.length());
        long offset = TimeUnit.DAYS.toMillis(Long.parseLong(parts[1]));

        // get the file
        File itemFile = new File(storageWorkDir, repoFilePart);
        if (itemFile.isFile()) {
          this.pathMap.put(repoFilePart, Integer.parseInt(parts[1]));

                    /*
                     * groups are not checked, so the hashes are left behind, see: NEXUS-3026
                     */
          if (repoFilePart.startsWith("releases/") || repoFilePart.startsWith("releases-m1/")
              || repoFilePart.startsWith("public/") || repoFilePart.startsWith("snapshots/")
              || repoFilePart.startsWith("thirdparty/") || repoFilePart.contains(".meta")
              || repoFilePart.contains(".index")) {
            neverDeleteFiles.add(repoFilePart);
          }

          // modify the file corresponding attribute
          final File attributeFile = new File(new File(new File(storageWorkDir, repoId), ".nexus/attributes"), filePart);
          Attributes attributes;
          try (FileInputStream in = new FileInputStream(attributeFile)) {
            attributes = marshaller.unmarshal(in);
          }

          // set new value
          attributes.setLastRequested(now + offset);

          // write it out
          try (FileOutputStream out = new FileOutputStream(attributeFile)) {
            marshaller.marshal(attributes, out);
          }
        }

        line = reader.readLine();
      }
    }
  }

  protected void runTask(int days, String repoId)
      throws Exception
  {
    TaskScheduleUtil.waitForAllTasksToStop(); // be sure nothing else is locking tasks

    ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
    prop.setKey("repositoryId");
    prop.setValue(repoId);

    ScheduledServicePropertyResource age = new ScheduledServicePropertyResource();
    age.setKey("evictOlderCacheItemsThen");
    age.setValue(String.valueOf(days));

    TaskScheduleUtil.runTask(EvictUnusedItemsTaskDescriptor.ID, EvictUnusedItemsTaskDescriptor.ID, 300, true,
        prop, age);

    getEventInspectorsUtil().waitForCalmPeriod();
  }

  protected SortedSet<String> buildListOfExpectedFilesForAllRepos(int days) {
    SortedSet<String> expectedFiles = new TreeSet<String>();
    expectedFiles.addAll(getNeverDeleteFiles());
    for (Entry<String, Integer> entry : pathMap.entrySet()) {
      if (entry.getValue() > (-days)) {
        expectedFiles.add(entry.getKey());
      }
    }

    List<String> expectedShadows = new ArrayList<String>();

    // loop once more to look for the shadows (NOTE: the shadow id must be in the format of targetId-*
    for (String expectedFile : expectedFiles) {
      String prefix = expectedFile.substring(0, expectedFile.indexOf("/")) + "-";
      String fileName = new File(expectedFile).getName();

      for (String originalFile : pathMap.keySet()) {
        if (originalFile.startsWith(prefix) && originalFile.endsWith(fileName)) {
          expectedShadows.add(originalFile);
          break;
        }
      }
    }

    expectedFiles.addAll(expectedShadows);

    return expectedFiles;
  }

  protected SortedSet<String> buildListOfExpectedFiles(int days, List<String> otherNotChangedRepoids) {
    SortedSet<String> expectedFiles = this.buildListOfExpectedFilesForAllRepos(days);

    for (String path : pathMap.keySet()) {
      String repoId = path.substring(0, path.indexOf("/"));
      if (otherNotChangedRepoids.contains(repoId)) {
        log.debug("found it:" + path);
        expectedFiles.add(path);
      }
    }
    return expectedFiles;
  }

  protected void checkForEmptyDirectories()
      throws IOException
  {
    // make sure we don't have any empty directories
    Set<String> emptyDirectories = new HashSet<String>();

    SortedSet<String> resultDirectories = this.getDirectoryPaths(this.getStorageWorkDir());
    for (String itemPath : resultDirectories) {
      if (itemPath.split(Pattern.quote(File.separator)).length != 1) {
        // introduced with NEXUS-5400: maybe ignore all paths starting with ".nexus"?
        if (!itemPath.contains(".nexus")) {
          File directory = new File(this.getStorageWorkDir(), itemPath);
          if (directory.list().length == 0) {
            emptyDirectories.add(itemPath);
          }
        }
      }
    }

    Assert.assertTrue("Found empty directories: " + emptyDirectories, emptyDirectories.size() == 0);
  }

  protected String prettyList(Set<String> list) {
    StringBuilder buffer = new StringBuilder();
    for (String string : list) {
      buffer.append(string).append("\n");
    }

    return buffer.toString();
  }

  protected SortedSet<String> getItemFilePaths()
      throws IOException
  {
    SortedSet<String> result = new TreeSet<String>();

    SortedSet<String> paths = getFilePaths(getStorageWorkDir());

    for (String path : paths) {
      if (!path.contains("/.nexus")) {
        result.add(path);
      }
    }

    return result;
  }

  @SuppressWarnings("unchecked")
  protected SortedSet<String> getFilePaths(File basedir)
      throws IOException
  {
    SortedSet<String> result = new TreeSet<String>();
    Collection<File> files = FileUtils.listFiles(basedir, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
    for (File file : files) {
      if (!file.equals(basedir)) {
        String path = file.getPath();
        if (path.startsWith(basedir.getAbsolutePath())) {
          path = path.substring(basedir.getAbsolutePath().length() + 1);
        }
        result.add(path.replaceAll(Pattern.quote("\\"), "/"));
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  protected SortedSet<String> getDirectoryPaths(File basedir)
      throws IOException
  {
    SortedSet<String> result = new TreeSet<String>();
    Collection<File> files = FileUtils.listFilesAndDirs(basedir, FalseFileFilter.FALSE, TrueFileFilter.TRUE);
    for (File file : files) {
      if (!file.equals(basedir)) {
        String path = file.getPath();
        if (path.startsWith(basedir.getAbsolutePath())) {
          path = path.substring(basedir.getAbsolutePath().length() + 1);
        }
        result.add(path.replaceAll(Pattern.quote("\\"), "/"));
      }
    }
    return result;
  }

  public File getStorageWorkDir() {
    return storageWorkDir;
  }

  public Map<String, Integer> getPathMap() {
    return pathMap;
  }

  public Collection<String> getNeverDeleteFiles() {
    DirectoryScanner scan = new DirectoryScanner();
    scan.setBasedir(new File(nexusWorkDir, "storage"));
    scan.setIncludes(new String[]{"**/.index/", "**/.meta/", "*/archetype-catalog.xml", "public/**", "public-snapshots/**"});
    scan.setExcludes(new String[]{"**/.nexus/", "**/.svn", "**/.svn/**"});

    scan.scan();

    Collection<String> files = new LinkedHashSet<String>();
    files.addAll(neverDeleteFiles);

    String[] includes = scan.getIncludedFiles();
    for (String file : includes) {
      files.add(file.replace('\\', '/'));
    }

    return files;
  }

}
