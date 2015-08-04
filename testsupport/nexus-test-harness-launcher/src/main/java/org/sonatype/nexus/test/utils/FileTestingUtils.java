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
package org.sonatype.nexus.test.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple File testing utilities.
 */
public class FileTestingUtils
{

  private static final Logger LOG = LoggerFactory.getLogger(FileTestingUtils.class);

  private static final int BUFFER_SIZE = 0x1000;

  private static final String SHA1 = "SHA1";

  /**
   * Creates a SHA1 hash from a file.
   *
   * @param file The file to be digested.
   * @return An SHA1 hash based on the contents of the file.
   */
  public static String createSHA1FromFile(File file)
      throws IOException
  {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file);
      return createSHA1FromStream(fis);
    }
    finally {
      if (fis != null) {
        fis.close();
      }
    }
  }

  /**
   * Creates a SHA1 hash from a url.
   *
   * @param url The URL to opened and digested.
   * @return An SHA1 hash based on the contents of the URL.
   */
  public static String createSHA1FromURL(URL url)
      throws IOException
  {
    InputStream is = url.openStream();
    try {
      return createSHA1FromStream(is);
    }
    finally {
      if (is != null) {
        is.close();
      }
    }
  }

  /**
   * Creates a SHA1 hash from an InputStream.
   *
   * @param in Inputstream to be digested.
   * @returnn SHA1 hash based on the contents of the stream.
   */
  public static String createSHA1FromStream(InputStream in)
      throws IOException
  {

    byte[] bytes = new byte[BUFFER_SIZE];

    try {
      MessageDigest digest = MessageDigest.getInstance(SHA1);
      for (int n; (n = in.read(bytes)) >= 0; ) {
        if (n > 0) {
          digest.update(bytes, 0, n);
        }
      }

      bytes = digest.digest();
      StringBuilder sb = new StringBuilder(bytes.length * 2);
      for (int i = 0; i < bytes.length; i++) {
        int n = bytes[i] & 0xFF;
        if (n < 0x10) {
          sb.append('0');
        }
        sb.append(Integer.toHexString(n));
      }

      return sb.toString();
    }
    catch (NoSuchAlgorithmException noSuchAlgorithmException) {
      throw new IllegalStateException(noSuchAlgorithmException.getMessage(), noSuchAlgorithmException);
    }
  }

  public static boolean compareFileSHA1s(File file1, File file2)
      throws IOException
  {

    if (file1 != null && file1.exists() && file2 != null && file2.exists() && file1.length() == file2.length()) {
      String file1SHA1 = createSHA1FromFile(file1);
      String file2SHA1 = createSHA1FromFile(file2);

      return file1SHA1.equals(file2SHA1);
    }
    return false;
  }

  @SuppressWarnings("rawtypes")
  public static File getTestFile(Class clazz, String filename) {
    String resource = clazz.getName().replace('.', '/') + "Resources/" + filename;
    LOG.debug("Looking for resource: " + resource);
    URL classURL = Thread.currentThread().getContextClassLoader().getResource(resource);
    LOG.debug("found: " + classURL);
    return new File(classURL.getFile());
  }

  public static void main(String[] args) {
    String usage = "Usage: java " + FileTestingUtils.class + " <url>";

    if (args == null || args.length != 1) {
      LOG.info(usage);
      return;
    }

    try {
      URL url = new URL(args[0]);
      LOG.info(createSHA1FromURL(url));
    }
    catch (Exception e) {
      LOG.warn(usage, e);
    }
  }

  public static void interpolationFileCopy(File from, File dest, Map<String, String> variables)
      throws IOException
  {

    // we may also need to create any parent directories
    if (dest.getParentFile() != null && !dest.getParentFile().exists()) {
      dest.getParentFile().mkdirs();
    }

    FileReader fileReader = new FileReader(from);
    InterpolationFilterReader filterReader = new InterpolationFilterReader(fileReader, variables);

    FileWriter fos = new FileWriter(dest);

    int readChar = -1;
    while ((readChar = filterReader.read()) != -1) {
      fos.write(readChar);
    }

    // close everything
    fileReader.close();
    fos.close();
  }

  public static void interpolationDirectoryCopy(File from, File dest, Map<String, String> variables)
      throws IOException
  {
    dest.mkdirs();

    DirectoryScanner scan = new DirectoryScanner();
    scan.addDefaultExcludes();
    scan.setBasedir(from);
    scan.scan();

    String[] files = scan.getIncludedFiles();
    for (String fileName : files) {
      String extension = FilenameUtils.getExtension(fileName);
      File sourceFile = new File(from, fileName);
      File destFile = new File(dest, fileName);
      destFile.getParentFile().mkdirs();

      if (Arrays.asList("zip", "jar", "gz", "jpg", "png").contains(extension)) {
        // just copy know binaries
        FileUtils.copyFile(sourceFile, destFile);
      }
      else {
        try (FileReader reader = new FileReader(sourceFile);
             FileWriter writer = new FileWriter(destFile)) {
          InterpolationFilterReader filterReader = new InterpolationFilterReader(reader, variables);
          IOUtils.copy(filterReader, writer);
        }
      }
    }

  }

  public static File populate(File file, int sizeInMB)
      throws IOException
  {
    file.getParentFile().mkdirs();

    ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file));
    zip.putNextEntry(new ZipEntry("content.random"));
    for (int i = 0; i < sizeInMB * 1024; i++) {
      byte[] b = new byte[1024];
      SecureRandom r = new SecureRandom();
      r.nextBytes(b);

      zip.write(b);
    }
    zip.closeEntry();
    zip.close();

    return file;
  }

}
