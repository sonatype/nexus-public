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
package org.sonatype.security.realms.tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.model.CPrivilege;
import org.sonatype.security.model.CProperty;
import org.sonatype.security.model.Configuration;
import org.sonatype.security.rest.users.AbstractSecurityRestTest;

import junit.framework.Assert;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;

public class AllPermissionsAreDefinedTest
    extends AbstractSecurityRestTest
{

  private static String SECURITY_FILE = "./target/security.xml";

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    // FileUtils.copyFile( new File(""), new File( SECURITY_FILE ) );
  }

  @Override
  protected void customizeContext(Context context) {
    super.customizeContext(context);
    context.put("security-xml-file", SECURITY_FILE);
  }

  /**
   * While in security-rest-api, this method was returning security related resource only, and assertion
   * is done also against it. Now that the security-rest-api is moved into restlet1x plugin, this method
   * would return way more than needed, hence, we are filtering for resources who's implementation is
   * security related, is in package org.sonatype.security only.
   */
  public List<PlexusResource> getPlexusResources()
      throws ComponentLookupException
  {
    final ArrayList<PlexusResource> result = new ArrayList<PlexusResource>();
    final List<ComponentDescriptor<PlexusResource>> cds =
        getContainer().getComponentDescriptorList(PlexusResource.class, null);
    for (ComponentDescriptor<PlexusResource> cd : cds) {
      if (cd.getImplementation().startsWith("org.sonatype.security")) {
        result.add(getContainer().lookup(PlexusResource.class, cd.getRoleHint()));
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public void testEnsurePermissions()
      throws Exception
  {
    Set<String> restPerms = new HashSet<String>();
    Set<String> staticPerms = new HashSet<String>();

    for (PlexusResource plexusResource : this.getPlexusResources()) {
      PathProtectionDescriptor ppd = plexusResource.getResourceProtection();

      String expression = ppd.getFilterExpression();
      if (expression.contains("[")) {
        String permission =
            ppd.getFilterExpression().substring(expression.indexOf('[') + 1, expression.indexOf(']'));
        restPerms.add(permission);
      }
    }

    // now we have a list of permissions, we need to make sure all of these are in the static security xml.

    StaticSecurityResource restResource =
        this.lookup(StaticSecurityResource.class, "SecurityRestStaticSecurityResource");
    Configuration staticConfig = restResource.getConfiguration();

    List<CPrivilege> privs = staticConfig.getPrivileges();
    for (CPrivilege privilege : privs) {
      staticPerms.add(this.getPermssionFromPrivilege(privilege));
    }

    // make sure everything in the restPerms is in the staticPerms
    for (String perm : restPerms) {

      // TODO: need to find a way of dealing with test resources
      if (!perm.startsWith("sample")) {
        Assert.assertTrue("Permission: " + perm + " is missing from SecurityRestStaticSecurityResource",
            staticPerms.contains(perm));
      }
    }

  }

  private String getPermssionFromPrivilege(CPrivilege privilege) {
    for (Iterator<CProperty> iter = privilege.getProperties().iterator(); iter.hasNext(); ) {
      CProperty prop = iter.next();
      if (prop.getKey().equals("permission")) {
        return prop.getValue();
      }
    }
    return null;
  }
}
