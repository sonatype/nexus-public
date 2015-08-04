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
package org.sonatype.nexus.proxy.repository.validator;

import java.io.File;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.FileContentLocator;
import org.sonatype.nexus.proxy.maven.MavenFileTypeValidator;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.Repository;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Assert;

/**
 * Base UT class for FileTypeValidatorHub tests. UTs extending this class should go and add asserts with some type
 * specific tests.
 */
public abstract class AbstractFileTypeValidationUtilTest
    extends NexusAppTestSupport
{
  /**
   * Override this method if your test files are located elsewhere.
   */
  protected File getTestFilesBasedir() {
    return getTestFile("target/test-classes/FileTypeValidationUtilTest");
  }

  protected FileTypeValidatorHub getValidationUtil()
      throws Exception
  {
    return lookup(FileTypeValidatorHub.class);
  }

  private Repository getDummyRepository()
      throws Exception
  {
    M2Repository repository = (M2Repository) lookup(Repository.class, "maven2");

    CRepository cRepo = new DefaultCRepository();
    cRepo.setId("test-repo");
    cRepo.setLocalStatus(LocalStatus.IN_SERVICE.toString());
    cRepo.setNotFoundCacheTTL(1);
    cRepo.setLocalStorage(new CLocalStorage());
    cRepo.getLocalStorage().setProvider("file");
    cRepo.setProviderRole(Repository.class.getName());
    cRepo.setProviderHint("maven2");

    Xpp3Dom ex = new Xpp3Dom("externalConfiguration");
    cRepo.setExternalConfiguration(ex);
    M2RepositoryConfiguration extConf = new M2RepositoryConfiguration(ex);
    extConf.setRepositoryPolicy(RepositoryPolicy.RELEASE);

    repository.configure(cRepo);

    return repository;
  }

  protected void doTest(String expectedFileName, String testFileName, boolean expectedResult)
      throws Exception
  {
    doTest(expectedFileName, testFileName, expectedResult, false);
  }

  protected void doTest(String expectedFileName, String testFileName, boolean expectedResult,
                        boolean laxXmlValidation)
      throws Exception
  {
    File testFile = new File(getTestFilesBasedir(), testFileName);

    DefaultStorageFileItem file =
        new DefaultStorageFileItem(getDummyRepository(), new ResourceStoreRequest(expectedFileName), true, true,
            new FileContentLocator(testFile, "this-is-neglected-in-this-test"));
    file.getItemContext().put(MavenFileTypeValidator.XML_DETECTION_LAX_KEY, Boolean.valueOf(laxXmlValidation));

    boolean result = getValidationUtil().isExpectedFileType(file);

    Assert.assertEquals("File name: " + expectedFileName + " and file: " + testFileName + " match result: "
        + result + " expected: " + expectedResult, expectedResult, result);
  }
}
