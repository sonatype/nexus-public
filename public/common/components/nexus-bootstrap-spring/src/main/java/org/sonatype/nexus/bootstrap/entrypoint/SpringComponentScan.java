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
package org.sonatype.nexus.bootstrap.entrypoint;

import java.util.regex.Pattern;

import org.sonatype.nexus.bootstrap.application.JavaxProviderDefaultListableBeanFactory;

import jakarta.inject.Named;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Application listener on {@link ContextRefreshedEvent} to perform a component scan of the Nexus packages. it performs
 * a scan and refreshes a children context including the scanned components.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class SpringComponentScan
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String CHILDREN_CONTEXT_ID = "nexus-spring-component-scan";

  private static final Pattern ENTRYPOINT_PACKAGES_PATTERN =
      Pattern.compile("(org|com)\\.sonatype\\.nexus\\.bootstrap\\.entrypoint\\..*");

  private static final String[] JAVA_PACKAGES_FOR_NEXUS_SCANNING =
      {"org.sonatype.nexus", "com.sonatype.nexus"};

  private final ConfigurableApplicationContext parentContext;

  public SpringComponentScan(
      final ConfigurableApplicationContext applicationContext)
  {
    this.parentContext = checkNotNull(applicationContext);
  }

  /**
   * Upon the completion of this method, the component scan done by the @SpringBootApplication and any @ComponentScan
   * annotations has completed. Meaning the app has started, selected the proper edition of nexus, and now needs to scan
   * the (org|com)/sonatype/nexus packages for any classes from modules that need to be checked for loading. Each of the
   * modules may potentially do @ComponentScan of their own module for injection if custom scanning is required.
   */
  public ApplicationContext finishBootstrapComponentScanning() {
    log.info("Scanning for components in packages: {}", (Object) JAVA_PACKAGES_FOR_NEXUS_SCANNING);

    AnnotationConfigApplicationContext childContext = getChildContext();
    // create a new scanner for the child context
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(childContext);
    // recognize JSR-330 @Component
    scanner.addIncludeFilter(new AnnotationTypeFilter(Named.class));
    // exclude bootstrap.entrypoint packages already scanned on application init
    scanner.addExcludeFilter(new RegexPatternTypeFilter(ENTRYPOINT_PACKAGES_PATTERN));
    scanner.addExcludeFilter(new RegexPatternTypeFilter(
        Pattern.compile("com\\.sonatype\\.nexus\\.licensing\\.ext\\.(builder|bootstrap\\.internal).*")));
    scanner.setBeanNameGenerator(new FullyQualifiedAnnotationBeanNameGenerator());

    scanner.getBeanDefinitionDefaults().setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);

    scanner.scan(JAVA_PACKAGES_FOR_NEXUS_SCANNING);

    log.debug("triggering context refresh on child context");
    childContext.refresh();

    log.debug("component scan complete");

    return childContext;
  }

  /**
   * Create a child context to perform component scan. With this approach we isolate the context refresh from the parent
   * context
   */
  private AnnotationConfigApplicationContext getChildContext() {
    JavaxProviderDefaultListableBeanFactory beanFactory =
        new JavaxProviderDefaultListableBeanFactory(parentContext.getBeanFactory());

    AnnotationConfigApplicationContext childContext = new AnnotationConfigApplicationContext(beanFactory);
    childContext.setParent(parentContext);
    childContext.setId(CHILDREN_CONTEXT_ID);

    PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
    configurer.setEnvironment(parentContext.getEnvironment()); // use parent context environment
    childContext.addBeanFactoryPostProcessor(configurer);

    return childContext;
  }
}
