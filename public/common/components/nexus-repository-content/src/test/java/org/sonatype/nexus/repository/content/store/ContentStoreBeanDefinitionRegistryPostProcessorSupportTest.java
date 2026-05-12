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
package org.sonatype.nexus.repository.content.store;

import org.sonatype.nexus.repository.content.browse.store.BrowseNodeStore;
import org.sonatype.nexus.repository.content.browse.store.example.TestBrowseNodeDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetBlobDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetDAO;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;
import org.sonatype.nexus.repository.content.store.example.TestContentRepositoryDAO;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.ResolvableType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContentStoreBeanDefinitionRegistryPostProcessorSupportTest
{
  @Captor
  ArgumentCaptor<BeanDefinition> captor;

  @Mock
  BeanDefinitionRegistry registry;

  @Test
  void testPostProcessBeanDefinitionRegistry() {
    new TestContentStoreConfiguration().postProcessBeanDefinitionRegistry(registry);

    verifyStore(ComponentStore.class, TestComponentDAO.class);
    verifyDAO(TestComponentDAO.class, ComponentStore.class);
  }

  @Test
  void testPostProcessBeanDefinitionRegistry_formatStoreSupport() {
    new TestFormatStoreConfiguration().postProcessBeanDefinitionRegistry(registry);

    verifyStore(ContentRepositoryStore.class, TestContentRepositoryDAO.class);
    verifyDAO(TestContentRepositoryDAO.class, ContentRepositoryStore.class);

    verifyStore(ComponentStore.class, TestComponentDAO.class);
    verifyDAO(TestComponentDAO.class, ComponentStore.class);

    verifyStore(AssetStore.class, TestAssetDAO.class);
    verifyDAO(TestAssetDAO.class, AssetStore.class);

    verifyStore(AssetBlobStore.class, TestAssetBlobDAO.class);
    verifyDAO(TestAssetBlobDAO.class, AssetBlobStore.class);

    verifyStore(BrowseNodeStore.class, TestBrowseNodeDAO.class);
    verifyDAO(TestBrowseNodeDAO.class, BrowseNodeStore.class);

  }

  private void verifyDAO(final Class<?> daoClass, final Class<?> storeClass) {
    String name = "%s<%s>".formatted(storeClass.getName(), daoClass.getName());
    verify(registry).registerBeanDefinition(eq(name), captor.capture());
    assertThat(captor.getValue().getResolvableType(), is(ResolvableType.forClassWithGenerics(storeClass, daoClass)));

    verify(registry).registerBeanDefinition(eq(daoClass.getSimpleName()), captor.capture());
    assertThat(captor.getValue().getResolvableType(), is(ResolvableType.forClass(FormatStoreFactorySpringImpl.class)));
  }

  private void verifyStore(
      final Class<?> storeClass,
      final Class<?> daoClass)
  {
    verify(registry).registerBeanDefinition(eq(TypeUtils.parameterize(storeClass, daoClass).getTypeName()),
        captor.capture());
    assertThat(captor.getValue().getResolvableType(), is(ResolvableType.forClassWithGenerics(storeClass, daoClass)));
  }

  @Qualifier("test")
  public static class TestContentStoreConfiguration
      extends ContentStoreBeanDefinitionRegistryPostProcessorSupport<ComponentStore<TestComponentDAO>>
  {
    // nothing to add
  }

  @Qualifier("test")
  public static class TestFormatStoreConfiguration
      extends
      FormatStoreSupport<TestContentRepositoryDAO, TestComponentDAO, TestAssetDAO, TestAssetBlobDAO, TestBrowseNodeDAO>
  {
    // nothing to add
  }
}
