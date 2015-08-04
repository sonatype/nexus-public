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
package org.sonatype.nexus.proxy.maven;

import org.sonatype.nexus.mime.DefaultMimeSupport;
import org.sonatype.nexus.mime.NexusMimeTypes;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.validator.AbstractFileTypeValidationUtilTest;
import org.sonatype.nexus.proxy.repository.validator.FileTypeValidator;
import org.sonatype.nexus.proxy.repository.validator.FileTypeValidatorHub;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import static org.mockito.Mockito.when;

/**
 * Tests for MavenFileTypeValidator specific file types.
 */
public class MavenFileTypeValidatorTest
    extends AbstractFileTypeValidationUtilTest
{

  private MavenFileTypeValidator underTest;

  @Spy
  private NexusMimeTypes mimeTypes = new NexusMimeTypes();

  @Mock
  private NexusMimeTypes.NexusMimeType mimeType;

  @Before
  public void setup()
      throws Exception
  {
    MockitoAnnotations.initMocks(this);
    underTest = new MavenFileTypeValidator(mimeTypes, new DefaultMimeSupport());
  }

  @Override
  protected FileTypeValidatorHub getValidationUtil()
      throws Exception
  {
    return new FileTypeValidatorHub()
    {
      @Override
      public boolean isExpectedFileType(final StorageItem item) {
        return !FileTypeValidator.FileTypeValidity.INVALID.equals(underTest.isExpectedFileType((StorageFileItem) item));
      }
    };
  }

  @Test
  public void testJar()
      throws Exception
  {
    doTest("something/else/myapp.jar", "test.jar", true);
    doTest("something/else/myapp.zip", "test.jar", true);
    doTest("something/else/myapp.war", "test.jar", true);
    doTest("something/else/myapp.ear", "test.jar", true);
    doTest("something/else/myapp.jar", "error.html", false);
  }

  @Test
  public void testPom()
      throws Exception
  {
    doTest("something/else/myapp.pom", "no-doctype-pom.xml", true);
    doTest("something/else/myapp.pom", "simple.xml", false);
    doTest("something/else/myapp.pom", "pom.xml", true);
  }

  @Test
  public void testXml()
      throws Exception
  {
    doTest("something/else/myapp.xml", "pom.xml", true);
    doTest("something/else/myapp.xml", "pom.xml", true, true);
    doTest("something/else/myapp.xml", "simple.xml", true);
    doTest("something/else/myapp.xml", "simple.xml", true, true);
    // will be INVALID with XML Lax validation OFF
    doTest("something/else/myapp.xml", "error.html", false, false);
    // will be VALID with XML Lax validation ON
    doTest("something/else/myapp.xml", "error.html", true, true);
    // will be INVALID with XML Lax validation OFF
    doTest("something/else/myapp.xml", "empty.xml", false, false);
    // will be VALID with XML Lax validation ON
    doTest("something/else/myapp.xml", "empty.xml", true, true);
  }

  @Test
  public void testNonHandled()
      throws Exception
  {
    doTest("something/else/image.jpg", "no-doctype-pom.xml", true);
    doTest("something/else/image.avi", "no-doctype-pom.xml", true);
  }

  @Test
  public void testFlexArtifacts()
      throws Exception
  {
    doTest("something/else/library.swc", "no-doctype-pom.xml", false);
    doTest("something/else/library.swc", "test.swc", true);
    doTest("something/else/app.swf", "no-doctype-pom.xml", false);
    doTest("something/else/app.swf", "test.swf", true);
  }

  @Test
  public void testTar()
      throws Exception
  {
    doTest("something/else/bundle.tar", "no-doctype-pom.xml", false);
    doTest("something/else/bundle.tar", "test.tar", true);
  }

  @Test
  public void testTarGz()
      throws Exception
  {
    doTest("something/else/bundle.tar.gz", "no-doctype-pom.xml", false);
    doTest("something/else/bundle.tar.gz", "test.tar.gz", true);
    doTest("something/else/bundle.tgz", "no-doctype-pom.xml", false);
    doTest("something/else/bundle.tgz", "test.tgz", true);
    doTest("something/else/bundle.gz", "no-doctype-pom.xml", false);
    doTest("something/else/bundle.gz", "test.gz", true);
  }

  @Test
  public void testTarBz2()
      throws Exception
  {
    doTest("something/else/bundle.tar.bz2", "no-doctype-pom.xml", false);
    doTest("something/else/bundle.tar.bz2", "test.tar.bz2", true);
    doTest("something/else/bundle.tbz", "no-doctype-pom.xml", false);
    doTest("something/else/bundle.tbz", "test.tbz", true);
    doTest("something/else/bundle.bz2", "no-doctype-pom.xml", false);
    doTest("something/else/bundle.bz2", "test.bz2", true);
  }

  @Test
  public void testChecksum()
      throws Exception
  {
    doTest("something/else/file.jar.sha1", "no-doctype-pom.xml", false);
    doTest("something/else/file.jar.sha1", "test.md5", false);
    doTest("something/else/file.jar.sha1", "test.sha1", true);
    doTest("something/else/file.jar.md5", "no-doctype-pom.xml", false);
    doTest("something/else/file.jar.md5", "test.sha1", false);
    doTest("something/else/file.jar.md5", "test.md5", true);
  }

  @Test
  public void testSiteXmlNEXUS6213()
      throws Exception
  {
    // neg tests
    doTest("something/else/file-site.xml", "nexus-6213-fubar-1.0.xml", false);
    doTest("something/else/file-site_hu.xml", "nexus-6213-fubar-1.0.xml", false);
    doTest("something/else/file-site_hu_HU.xml", "nexus-6213-fubar-1.0.xml", false);
    doTest("something/else/file-site_123.xml", "nexus-6213-fubar-1.0.xml", true); // is not site XML

    // positive tests
    doTest("something/else/file-site.xml", "nexus-6213-maven-3.0.5-site.xml", true);
    doTest("something/else/file-site_hu.xml", "nexus-6213-maven-3.0.5-site.xml", true);
    doTest("something/else/file-site_hu_HU.xml", "nexus-6213-maven-3.0.5-site.xml", true);
    doTest("something/else/file-site_123.xml", "nexus-6213-maven-3.0.5-site.xml", true);
  }

  @Test
  public void overrideRules()
      throws Exception
  {
    doTest("something/else/library.swc", "test.tar.bz2", false);

    // note: The real NexusMimeTypes handles file name and is invoked like this
    // so, to make MOCK react we use the same path as input will be
    when(mimeTypes.getMimeTypes("something/else/library.swc")).thenReturn(mimeType);
    when(mimeType.getMimetypes()).thenReturn(Lists.newArrayList("application/x-bzip2"));

    doTest("something/else/library.swc", "test.tar.bz2", true);
  }

}
