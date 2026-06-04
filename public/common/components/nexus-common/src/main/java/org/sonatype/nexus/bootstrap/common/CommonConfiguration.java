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
package org.sonatype.nexus.bootstrap.common;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

import jakarta.inject.Provider;

import org.sonatype.nexus.common.io.ByteSize;
import org.sonatype.nexus.common.time.Time;
import org.sonatype.nexus.common.conversion.BooleanPropertyEditor;
import org.sonatype.nexus.common.conversion.ByteSizePropertyEditor;
import org.sonatype.nexus.common.conversion.DurationPropertyEditor;
import org.sonatype.nexus.common.conversion.FilePropertyEditor;
import org.sonatype.nexus.common.conversion.ProviderPropertyEditor;
import org.sonatype.nexus.common.conversion.TimePropertyEditor;
import org.sonatype.nexus.common.conversion.URIPropertyEditor;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.sonatype.nexus.common.cluster.ClusterCoordinationService;
import org.sonatype.nexus.common.cluster.LocalClusterCoordinationService;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.CustomEditorConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

@Configuration
public class CommonConfiguration
{
  @Bean
  public static CustomEditorConfigurer commonCustomEditorConfigurer() {
    CustomEditorConfigurer configurer = new CustomEditorConfigurer();
    configurer.setCustomEditors(Map.of(Time.class, TimePropertyEditor.class,
        ByteSize.class, ByteSizePropertyEditor.class,
        Duration.class, DurationPropertyEditor.class,
        boolean.class, BooleanPropertyEditor.class,
        URI.class, URIPropertyEditor.class,
        File.class, FilePropertyEditor.class,
        Provider.class, ProviderPropertyEditor.class));
    return configurer;
  }

  /**
   * Provider for the default configuration of {@link JsonMapper}. Marked with a sub-zero priority to prioritize
   * existing {@link ObjectMapper} providers.<br/>
   */
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  @Primary
  @Qualifier("default")
  @Bean
  public JsonMapper jsonMapper() {
    return JsonMapper.builder()
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .addModule(new JavaTimeModule())
        .addModule(new Jdk8Module())
        .addModule(new ParameterNamesModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();
  }

  @Bean
  @ConditionalOnMissingBean(ClusterCoordinationService.class)
  public ClusterCoordinationService localClusterCoordinationService() {
    return new LocalClusterCoordinationService();
  }
}
