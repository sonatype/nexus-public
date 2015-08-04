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
package org.sonatype.nexus.yum.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.io.File.pathSeparator;
import static java.io.File.separator;
import static java.lang.String.format;
import static org.apache.commons.io.IOUtils.readLines;
import static org.apache.commons.io.IOUtils.write;
import static org.apache.commons.io.IOUtils.writeLines;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonatype.nexus.yum.internal.RpmScanner.getRelativePath;

/**
 * @since yum 3.0
 */
public class RpmListWriter
{

  private static final int POSITION_AFTER_SLASH = 1;

  private static final Logger LOG = LoggerFactory.getLogger(RpmListWriter.class);

  private final File rpmListFile;

  private final String version;

  private final String addedFiles;

  private final File baseRpmDir;

  private final boolean singleRpmPerDirectory;

  private final boolean forceFullScan;

  private final ListFileFactory fileFactory;

  private final RpmScanner scanner;

  public RpmListWriter(final File baseRpmDir,
                       final String addedFiles,
                       final String version,
                       final boolean singleRpmPerDirectory,
                       final boolean forceFullScan,
                       final ListFileFactory fileFactory,
                       final RpmScanner scanner)
  {
    this.baseRpmDir = baseRpmDir;
    this.addedFiles = addedFiles;
    this.version = version;
    this.singleRpmPerDirectory = singleRpmPerDirectory;
    this.forceFullScan = forceFullScan;
    this.fileFactory = fileFactory;
    this.scanner = scanner;
    this.rpmListFile = fileFactory.getRpmListFile();
  }

  public File writeList()
      throws IOException
  {
    if (rpmListFile.exists() && !forceFullScan) {
      LOG.debug("Reuse existing rpm list file : {}", rpmListFile);
      List<String> rpmFileList = pruneToExistingRpms();

      if (isNotBlank(version)) {
        return extractVersionOfListFile(rpmFileList);
      }

      if (isNotBlank(addedFiles)) {
        addNewlyAddedRpmFileToList(rpmFileList);
      }

      writeRpmFileList(rpmFileList);
    }
    else {
      rewriteList();
    }

    return rpmListFile;
  }

  private File extractVersionOfListFile(List<String> files)
      throws IOException
  {
    List<String> filesWithRequiredVersion = new ArrayList<String>();
    for (String file : files) {
      if (hasRequiredVersion(file)) {
        filesWithRequiredVersion.add(file);
      }
    }

    File rpmVersionedListFile = fileFactory.getRpmListFile(version);
    writeRpmFileList(filesWithRequiredVersion, rpmVersionedListFile);
    return rpmVersionedListFile;
  }

  private boolean hasRequiredVersion(String file) {
    String[] segments = file.split("\\/");
    return (segments.length >= 2) && version.equals(segments[segments.length - 2]);
  }

  private void addNewlyAddedRpmFileToList(List<String> fileList)
      throws IOException
  {
    final String[] filenames = addedFiles.split(pathSeparator);
    for (String filename : filenames) {
      filename = addFileToList(fileList, filename);
    }
  }

  private String addFileToList(List<String> fileList, String filename) {
    final int startPosition = filename.startsWith("/") ? POSITION_AFTER_SLASH : 0;
    filename = filename.substring(startPosition);

    if (!fileList.contains(filename)) {
      fileList.add(filename);
      LOG.debug("Added rpm {} to file list.", filename);
    }
    else {
      LOG.debug("Rpm {} already exists in file list.", filename);
    }
    return filename;
  }

  private List<String> pruneToExistingRpms()
      throws IOException
  {
    List<String> files = readRpmFileList();
    for (int i = 0; i < files.size(); i++) {
      if (!new File(baseRpmDir, files.get(i)).exists()) {
        LOG.debug("Removed {} from rpm list.", files.get(i));
        files.remove(i);
        i--;

      }
    }
    return files;
  }

  private void writeRpmFileList(Collection<String> files)
      throws IOException
  {
    writeRpmFileList(files, rpmListFile);
  }

  private void writeRpmFileList(Collection<String> files, File rpmListOutputFile)
      throws IOException
  {
    FileOutputStream outputStream = new FileOutputStream(rpmListOutputFile);
    try {
      writeLines(files, "\n", outputStream);
      if (files.isEmpty()) {
        LOG.debug(
            "Write non existing package to rpm list file {} to avoid an empty packge list that would cause createrepo to scan the whole directory",
            rpmListOutputFile);
        write(
            ".foo/.bar.rpm/to-avoid-an-empty-rpm-list-file/that-would-cause-createrepo-to-scan-the-whole-repo.rpm",
            outputStream);
      }
      else {
        LOG.debug("Wrote {} rpm packages to rpm list file {} .", files.size(), rpmListOutputFile);
      }
    }
    finally {
      outputStream.close();
    }
  }

  @SuppressWarnings("unchecked")
  private List<String> readRpmFileList()
      throws IOException
  {
    FileInputStream inputStream = new FileInputStream(rpmListFile);
    try {
      return readLines(inputStream);
    }
    finally {
      inputStream.close();
    }
  }

  private void rewriteList()
      throws IOException
  {
    if (singleRpmPerDirectory) {
      rewriteFileList(getSortedFilteredFileList());
    }
    else {
      writeRpmFileList(getRelativeFilenames(scanner.scan(baseRpmDir)), rpmListFile);
    }
  }

  private List<String> getRelativeFilenames(Collection<File> rpmFileList) {
    List<String> result = new ArrayList<String>(rpmFileList.size());
    for (File rpmFile : rpmFileList) {
      result.add(getRelativePath(baseRpmDir, rpmFile));
    }
    return result;
  }

  private void rewriteFileList(Map<String, String> fileMap)
      throws IOException
  {
    Writer writer = new FileWriter(rpmListFile);
    try {
      for (Entry<String, String> entry : fileMap.entrySet()) {
        writer.append(format("%s%s\n", entry.getKey(), entry.getValue()));
      }
    }
    finally {
      writer.close();
    }
    LOG.debug("Wrote temporary package list to {}", rpmListFile.getAbsoluteFile());
  }

  private Map<String, String> getSortedFilteredFileList() {
    String absoluteBasePath = baseRpmDir + separator;

    Map<String, String> fileMap = new TreeMap<String, String>();

    for (File file : scanner.scan(baseRpmDir)) {
      File parentFile = file.getParentFile();
      if (matchesRequestedVersion(parentFile)) {
        String parentDir = getRelativePath(baseRpmDir, parentFile);
        putLatestArtifactInMap(parentDir, file.getName(), fileMap);
      }
    }
    return fileMap;
  }

  private void putLatestArtifactInMap(String parentDir, String filename, Map<String, String> fileMap) {
    if (!fileMap.containsKey(parentDir) || (filename.compareTo(fileMap.get(parentDir)) > 0)) {
      fileMap.put(parentDir, filename);
    }
  }

  private boolean matchesRequestedVersion(File parentFile) {
    return (version == null) || parentFile.getName().equals(version);
  }

}
