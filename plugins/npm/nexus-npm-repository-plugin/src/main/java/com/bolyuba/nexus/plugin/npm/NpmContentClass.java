/*
 * Copyright (c) 2007-2014 Sonatype, Inc. and Georgy Bolyuba. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.registry.AbstractIdContentClass;

/**
 * This class drives:
 * - read-only value for field called "Format" on New repo pages
 * Details:
 * = REST: service/local/components/repo_types?repoType=proxy
 * = Class RepositoryTypesComponentListPlexusResource
 *
 * - item in Repository Targets list. A default "All (npm) .*" target will be created on startup if missing
 * Details:
 * = REST: service/local/repo_targets
 * = Event listener class DefaultTargetRegistryEventInspector adds repo target into nexus config on startup
 *
 * @author Georgy Bolyuba (georgy@bolyuba.com)
 */
@Named(NpmContentClass.ID)
@Singleton
public class NpmContentClass
    extends AbstractIdContentClass
{

  public static final String ID = "npm";

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return ID;
  }
}
