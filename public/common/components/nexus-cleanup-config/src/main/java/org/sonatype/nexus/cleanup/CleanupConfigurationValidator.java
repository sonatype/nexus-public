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
package org.sonatype.nexus.cleanup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.ConstraintViolation;

import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.ConfigurationValidator;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import org.springframework.context.annotation.Lazy;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.CLEANUP_ATTRIBUTES_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.CLEANUP_NAME_KEY;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicy.ALL_CLEANUP_POLICY_FORMAT;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Ensures that cleanup configuration is valid
 *
 * @since 3.19
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CleanupConfigurationValidator
    implements ConfigurationValidator
{
  private final ConstraintViolationFactory constraintViolationFactory;

  private final RepositoryManager repositoryManager;

  private final CleanupPolicyStorage cleanupPolicyStorage;

  @Autowired
  public CleanupConfigurationValidator(
      final ConstraintViolationFactory constraintViolationFactory,
      @Lazy final RepositoryManager repositoryManager,
      final CleanupPolicyStorage cleanupPolicyStorage)
  {
    this.constraintViolationFactory = checkNotNull(constraintViolationFactory);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.cleanupPolicyStorage = checkNotNull(cleanupPolicyStorage);
  }

  @Nullable
  @Override
  public ConstraintViolation<?> validate(final Configuration configuration) {
    ConstraintViolation<?> cleanupPolicyViolation = validateCleanupPoliciesExistence(configuration);

    if (cleanupPolicyViolation != null) {
      return cleanupPolicyViolation;
    }
    List<CleanupPolicy> cleanupPolicies = getCleanupPolicy(configuration);

    if (!cleanupPolicies.isEmpty()) {
      Optional<Format> format = getConfigurationFormat(configuration);
      if (format.isPresent()) {
        // report on the first one oly allowing
        return validCleanupFormat(configuration.getRepositoryName(), format.get().getValue(), cleanupPolicies.get(0));
      }
    }
    return null;
  }

  private Optional<Format> getConfigurationFormat(final Configuration configuration) {
    return repositoryManager.getAllSupportedRecipes()
        .stream()
        .filter(recipe -> configuration.getRecipeName().equals(recipe.getFormat().getValue() + "-" + recipe.getType()))
        .map(Recipe::getFormat)
        .findFirst();
  }

  private List<CleanupPolicy> getCleanupPolicy(final Configuration configuration) {
    List<CleanupPolicy> cleanupPolicies = new ArrayList<>();

    Map<String, Map<String, Object>> attributes = configuration.getAttributes();
    if (attributes != null && attributes.containsKey(CLEANUP_ATTRIBUTES_KEY)) {
      addToCleanupPoliciesFromCleanupAttributes(cleanupPolicies, attributes.get(CLEANUP_ATTRIBUTES_KEY));
    }

    return cleanupPolicies;
  }

  private void addToCleanupPoliciesFromCleanupAttributes(
      final List<CleanupPolicy> cleanupPolicies,
      final Map<String, Object> cleanupAttributes)
  {
    if (cleanupAttributes.containsKey(CLEANUP_NAME_KEY)) {
      @SuppressWarnings("unchecked")
      Collection<String> policyNames = (Collection<String>) cleanupAttributes.get(CLEANUP_NAME_KEY);

      policyNames.forEach(policyName -> {
        CleanupPolicy cleanupPolicy = cleanupPolicyStorage.get(policyName);

        if (nonNull(cleanupPolicy)) {
          cleanupPolicies.add(cleanupPolicy);
        }
      });
    }
  }

  private ConstraintViolation<?> validCleanupFormat(
      final String repoName,
      final String repoFormat,
      final CleanupPolicy cleanupPolicy)
  {
    if (!repoFormat.equals(cleanupPolicy.getFormat()) && !ALL_CLEANUP_POLICY_FORMAT.equals(cleanupPolicy.getFormat())) {
      return constraintViolationFactory.createViolation(CLEANUP_ATTRIBUTES_KEY,
          String.format("Repository %s is of format type %s, unable to assign cleanup policy %s of format type %s",
              repoName, repoFormat, cleanupPolicy.getName(), cleanupPolicy.getFormat()));
    }
    return null;
  }

  private ConstraintViolation<?> validateCleanupPoliciesExistence(final Configuration configuration) {
    Map<String, Object> cleanupAttributes = Optional.ofNullable(configuration.getAttributes())
        .map(attributes -> attributes.get(CLEANUP_ATTRIBUTES_KEY))
        .orElse(null);

    if (cleanupAttributes != null && cleanupAttributes.containsKey(CLEANUP_NAME_KEY)) {
      Object policyNamesObj = cleanupAttributes.get(CLEANUP_NAME_KEY);

      if (!(policyNamesObj instanceof Collection)) {
        return constraintViolationFactory.createViolation(CLEANUP_ATTRIBUTES_KEY,
            "Expected a collection for cleanup policy names.");
      }

      Collection<?> policyNames = (Collection<?>) policyNamesObj;
      for (Object policyNameObj : policyNames) {
        if (!(policyNameObj instanceof String)) {
          return constraintViolationFactory.createViolation(CLEANUP_ATTRIBUTES_KEY,
              "Policy name should be a string.");
        }

        String policyName = (String) policyNameObj;
        CleanupPolicy cleanupPolicy = cleanupPolicyStorage.get(policyName);

        if (cleanupPolicy == null) {
          String errorMessage = String.format("Cleanup Policy '%s' does not exist.", policyName);
          return constraintViolationFactory.createViolation(CLEANUP_ATTRIBUTES_KEY, errorMessage);
        }
      }
    }
    return null;
  }
}
