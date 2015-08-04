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
package org.sonatype.security.ldap.realms.persist;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.sonatype.nexus.test.PlexusTestCaseSupport;
import org.sonatype.security.ldap.realms.persist.model.Configuration;
import org.sonatype.security.ldap.realms.persist.model.io.xpp3.LdapConfigurationXpp3Reader;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;

import junit.framework.Assert;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Test;

public class DefaultLdapConfigurationValidatorTest
    extends PlexusTestCaseSupport
{
  @Override
  protected void customizeContainerConfiguration(final ContainerConfiguration containerConfiguration) {
    super.customizeContainerConfiguration(containerConfiguration);
    containerConfiguration.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
  }

  @Override
  protected void customizeContext(Context context) {
    super.customizeContext(context);

    String packageName = this.getClass().getPackage().getName();
    context.put("test-path", getBasedir() + "/target/test-classes/" + packageName.replace('.', '/')
        + "/validation");
  }

  @Test
  public void testConf()
      throws Exception
  {

    List<LdapConfigrationValidatorTestBean> beans =
        this.getContainer().lookupList(LdapConfigrationValidatorTestBean.class);
    ConfigurationValidator validator = lookup(ConfigurationValidator.class);

    for (LdapConfigrationValidatorTestBean testBean : beans) {
      ValidationResponse response =
          validator.validateModel(new ValidationRequest(this.getLdapConfiguration(testBean.getConfigFile())));

      Assert.assertEquals(
          "Config File: " + testBean.getConfigFile() + " errors:\n" + this.getDebugStringFromResponse(response),
          testBean.getNumberOfErrors(), response.getValidationErrors().size());
      Assert.assertEquals(
          "Config File: " + testBean.getConfigFile() + " warnings:" + this.getDebugStringFromResponse(response),
          testBean.getNumberOfWarnings(), response.getValidationWarnings().size());
    }
  }

  private String getDebugStringFromResponse(ValidationResponse response) {
    StringBuilder buffer = new StringBuilder();
    if (!response.getValidationErrors().isEmpty()) {
      buffer.append("Errors:");
      for (ValidationMessage message : response.getValidationErrors()) {
        buffer.append("\t").append(message.getKey()).append(" - ").append(message.getMessage()).append(
            "\n");
      }
      buffer.append("\n");
    }
    if (!response.getValidationWarnings().isEmpty()) {
      buffer.append("Warnings:");
      for (ValidationMessage message : response.getValidationWarnings()) {
        buffer.append("\t").append(message.getKey()).append(" - ").append(message.getMessage()).append(
            "\n");
      }
    }
    return buffer.toString();
  }

  private Configuration getLdapConfiguration(File configFile)
      throws IOException, XmlPullParserException
  {

    Configuration defaultConfig = null;

    Reader fr = null;
    InputStream is = null;
    try {
      is = new FileInputStream(configFile);
      LdapConfigurationXpp3Reader reader = new LdapConfigurationXpp3Reader();
      fr = new InputStreamReader(is);
      defaultConfig = reader.read(fr);
    }
    finally {
      if (fr != null) {
        try {
          fr.close();
        }
        catch (IOException e) {
          // just closing if open
        }
      }

      if (is != null) {
        try {
          is.close();
        }
        catch (IOException e) {
          // just closing if open
        }
      }
    }
    return defaultConfig;
  }

}
