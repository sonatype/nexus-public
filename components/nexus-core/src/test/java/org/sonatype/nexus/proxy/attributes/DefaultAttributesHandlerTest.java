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
package org.sonatype.nexus.proxy.attributes;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * AttributeStorage implementation driven by XStream.
 *
 * @author cstamas
 */
public class DefaultAttributesHandlerTest
    extends AbstractAttributesHandlerTest
{
  @Override
  protected AttributesHandler createAttributesHandler() throws Exception {
    return lookup(AttributesHandler.class);
  }

  @Test
  public void testRecreateAttrs()
      throws Exception
  {
    RepositoryItemUid uid =
        getRepositoryItemUidFactory().createUid(repository, "/activemq/activemq-core/1.2/activemq-core-1.2.jar");

    assertThat(attributesHandler.getAttributeStorage().getAttributes(uid), nullValue());

    repository.recreateAttributes(new ResourceStoreRequest(RepositoryItemUid.PATH_ROOT, true), null);

    assertThat(attributesHandler.getAttributeStorage().getAttributes(uid), notNullValue());
  }

  @Test
  public void testRecreateAttrsWithCustomAttrs()
      throws Exception
  {
    RepositoryItemUid uid =
        getRepositoryItemUidFactory().createUid(repository, "/activemq/activemq-core/1.2/activemq-core-1.2.jar");

    assertThat(attributesHandler.getAttributeStorage().getAttributes(uid), nullValue());

    Map<String, String> customAttrs = new HashMap<String, String>();
    customAttrs.put("one", "1");
    customAttrs.put("two", "2");

    repository.recreateAttributes(new ResourceStoreRequest(RepositoryItemUid.PATH_ROOT, true), customAttrs);

    assertThat(attributesHandler.getAttributeStorage().getAttributes(uid), notNullValue());

    Attributes item = attributesHandler.getAttributeStorage().getAttributes(uid);

    assertThat(item, notNullValue());

    assertThat(item.get("one"), equalTo("1"));
    assertThat(item.get("two"), equalTo("2"));
  }
}
