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
package org.sonatype.nexus.content.raw.internal.recipe;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.repository.view.Parameters;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonatype.nexus.repository.BadRequestException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sonatype.nexus.content.raw.internal.recipe.QueryParameterForwardingHelper.MAX_FORWARDED_PARAMS;

class QueryParameterForwardingHelperTest
{
  private QueryParameterForwardingHelper helper;

  @BeforeEach
  void setUp() {
    helper = new QueryParameterForwardingHelper();
  }

  @Test
  void buildQueryString_noParameters_returnsEmpty() {
    Parameters parameters = new Parameters();

    String result = helper.buildQueryString(parameters);

    assertThat(result, is(emptyString()));
  }

  @Test
  void buildQueryString_nullParameters_returnsEmpty() {
    String result = helper.buildQueryString(null);

    assertThat(result, is(emptyString()));
  }

  @Test
  void buildQueryString_singleParameter_returnsKeyValuePair() {
    helper.updateConfig(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    parameters.set("key", "value");

    String result = helper.buildQueryString(parameters);

    assertThat(result, is("key=value"));
  }

  @Test
  void buildQueryString_multipleParameters_returnsAmpersandSeparated() {
    helper.updateConfig(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    parameters.set("key1", "value1");
    parameters.set("key2", "value2");

    String result = helper.buildQueryString(parameters);

    assertThat(result, is("key1=value1&key2=value2"));
  }

  @Test
  void buildQueryString_multipleParameters_areSortedAlphabetically() {
    helper.updateConfig(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    parameters.set("zebra", "z");
    parameters.set("alpha", "a");
    parameters.set("middle", "m");

    String result = helper.buildQueryString(parameters);

    assertThat("Parameters should be sorted alphabetically", result, is("alpha=a&middle=m&zebra=z"));
  }

  @Test
  void buildQueryString_multiValuedParameter_repeatsKey() {
    helper.updateConfig(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    parameters.set("key", "val1", "val2");

    String result = helper.buildQueryString(parameters);

    assertThat(result, containsString("key=val1"));
    assertThat(result, containsString("key=val2"));
    assertThat(result, containsString("&"));
  }

  @Test
  void buildQueryStringMultiValuedParameterValuesAreSorted() {
    helper.updateConfig(true, Collections.emptyList());

    Parameters params1 = new Parameters();
    params1.set("tag", "beta", "alpha"); // unsorted order
    params1.set("platform", "darwin");

    Parameters params2 = new Parameters();
    params2.set("tag", "alpha", "beta"); // different order
    params2.set("platform", "darwin");

    String result1 = helper.buildQueryString(params1);
    String result2 = helper.buildQueryString(params2);

    // Both should produce the same normalized query string
    assertThat("Values should be sorted for cache deduplication", result1, is("platform=darwin&tag=alpha&tag=beta"));
    assertThat("Both requests should produce identical query strings", result1, is(result2));
  }

  @Test
  void buildQueryStringMultiValuedParameterWithNullValuesSortedCorrectly() {
    helper.updateConfig(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    parameters.set("tag", "zebra", null, "alpha"); // unsorted with null

    String result = helper.buildQueryString(parameters);

    // Null values should come first when sorted, then alphabetically
    assertThat(result, is("tag&tag=alpha&tag=zebra"));
  }

  @Test
  void buildQueryStringMultiValuedParameterReverseOrderSortedCorrectly() {
    helper.updateConfig(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    parameters.set("version", "v3.0", "v2.0", "v1.0"); // reverse order

    String result = helper.buildQueryString(parameters);

    // Should be sorted alphabetically
    assertThat(result, is("version=v1.0&version=v2.0&version=v3.0"));
  }

  @Test
  void buildQueryString_parameterWithoutValue_noEqualsSign() {
    helper.updateConfig(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    parameters.set("flag", (String) null);

    String result = helper.buildQueryString(parameters);

    assertThat(result, is("flag"));
  }

  @Test
  void buildQueryString_parameterWithEmptyValue_includesEqualsSign() {
    helper.updateConfig(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    parameters.set("key", "");

    String result = helper.buildQueryString(parameters);

    assertThat(result, is("key="));
  }

  @Test
  void buildQueryString_specialCharacters_areUrlEncoded() {
    helper.updateConfig(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    parameters.set("msg", "hello world");

    String result = helper.buildQueryString(parameters);

    assertThat(result, is("msg=hello+world"));
  }

  @Test
  void buildQueryString_ampersandAndEquals_areUrlEncoded() {
    helper.updateConfig(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    parameters.set("query", "a=b&c=d");

    String result = helper.buildQueryString(parameters);

    assertThat(result, is("query=a%3Db%26c%3Dd"));
  }

  @Test
  void buildQueryString_specialCharsInKey_areUrlEncoded() {
    helper.updateConfig(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    parameters.set("key with spaces", "value");

    String result = helper.buildQueryString(parameters);

    assertThat(result, is("key+with+spaces=value"));
  }

  @Test
  void buildQueryString_jetbrainsPluginId_spacesEncoded() {
    helper.updateConfig(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    parameters.set("pluginId", "Key Promoter X");
    parameters.set("version", "2024.2.2");

    String result = helper.buildQueryString(parameters);

    assertThat(result, containsString("pluginId=Key+Promoter+X"));
    assertThat(result, containsString("version=2024.2.2"));
    assertThat(result, containsString("&"));
  }

  @Test
  void buildQueryString_forwardingDisabled_returnsEmpty() {
    helper.updateConfig(false, Collections.emptyList());

    Parameters parameters = new Parameters();
    parameters.set("key", "value");

    String result = helper.buildQueryString(parameters);

    assertThat(result, is(emptyString()));
  }

  @Test
  void buildQueryString_multipleExclusions_filtersMultipleParameters() {
    helper.updateConfig(true, Arrays.asList("api_key", "access_token", "secret"));

    Parameters parameters = new Parameters();
    parameters.set("api_key", "secret1");
    parameters.set("access_token", "secret2");
    parameters.set("secret", "secret3");
    parameters.set("version", "1.0");
    parameters.set("format", "json");

    String result = helper.buildQueryString(parameters);

    assertThat(result, not(containsString("api_key")));
    assertThat(result, not(containsString("access_token")));
    assertThat(result, not(containsString("secret")));
    assertThat(result, containsString("version=1.0"));
    assertThat(result, containsString("format=json"));
  }

  @Test
  void buildQueryString_caseInsensitiveExclusion_matchesAllCases() {
    helper.updateConfig(true, Collections.singletonList("api_key"));

    Parameters parameters = new Parameters();
    parameters.set("API_KEY", "secret1");
    parameters.set("Api_Key", "secret2");
    parameters.set("api_key", "secret3");
    parameters.set("version", "1.0");

    String result = helper.buildQueryString(parameters);

    assertThat(result, not(containsString("API_KEY")));
    assertThat(result, not(containsString("Api_Key")));
    assertThat(result, not(containsString("api_key")));
    assertThat(result, containsString("version=1.0"));
  }

  @Test
  void buildQueryString_excludeAllParams_returnsEmpty() {
    helper.updateConfig(true, Arrays.asList("key1", "key2"));

    Parameters parameters = new Parameters();
    parameters.set("key1", "value1");
    parameters.set("key2", "value2");

    String result = helper.buildQueryString(parameters);

    assertThat(result, is(emptyString()));
  }

  @Test
  void buildQueryString_multiValuedExclusion_filtersAllValues() {
    helper.updateConfig(true, Collections.singletonList("tag"));

    Parameters parameters = new Parameters();
    parameters.set("tag", "v1", "v2", "v3");
    parameters.set("format", "json");

    String result = helper.buildQueryString(parameters);

    assertThat(result, not(containsString("tag")));
    assertThat(result, not(containsString("v1")));
    assertThat(result, not(containsString("v2")));
    assertThat(result, not(containsString("v3")));
    assertThat(result, containsString("format=json"));
  }

  @Test
  void updateConfig_switchFromDisabledToEnabled_forwardsParams() {
    // Start disabled (default)
    Parameters params = new Parameters();
    params.set("key", "value");
    assertThat(helper.buildQueryString(params), is(emptyString()));

    // Enable forwarding
    helper.updateConfig(true, Collections.emptyList());
    assertThat(helper.buildQueryString(params), is("key=value"));
  }

  @Test
  void updateConfig_switchFromEnabledToDisabled_stopsForwarding() {
    helper.updateConfig(true, Collections.emptyList());

    Parameters params = new Parameters();
    params.set("key", "value");
    assertThat(helper.buildQueryString(params), is("key=value"));

    // Disable forwarding
    helper.updateConfig(false, Collections.emptyList());
    assertThat(helper.buildQueryString(params), is(emptyString()));
  }

  @Test
  void updateConfig_changeExclusions_appliesNewExclusions() {
    helper.updateConfig(true, Collections.singletonList("old_param"));

    Parameters params = new Parameters();
    params.set("old_param", "v1");
    params.set("new_param", "v2");
    params.set("keep", "v3");

    // old_param excluded, new_param and keep forwarded
    String result1 = helper.buildQueryString(params);
    assertThat(result1, not(containsString("old_param")));
    assertThat(result1, containsString("new_param=v2"));
    assertThat(result1, containsString("keep=v3"));

    // Change exclusions: now exclude new_param instead
    helper.updateConfig(true, Collections.singletonList("new_param"));
    String result2 = helper.buildQueryString(params);
    assertThat(result2, containsString("old_param=v1"));
    assertThat(result2, not(containsString("new_param")));
    assertThat(result2, containsString("keep=v3"));
  }

  @Test
  void updateConfig_nullExcludedParams_treatedAsEmpty() {
    helper.updateConfig(true, null);

    Parameters params = new Parameters();
    params.set("key", "value");

    String result = helper.buildQueryString(params);
    assertThat(result, is("key=value"));
    assertThat(helper.getExcludedParamsLowercase(), is(ImmutableSet.of()));
  }

  @Test
  void updateConfig_excludedParamsWithNullsAndBlanks_filtered() {
    List<String> exclusions = Arrays.asList("valid", null, "", "  ", "also_valid");
    helper.updateConfig(true, exclusions);

    assertThat(helper.getExcludedParamsLowercase(), is(ImmutableSet.of("valid", "also_valid")));
  }

  @Test
  void updateConfig_disabledIgnoresExclusions() {
    helper.updateConfig(false, Arrays.asList("api_key", "token"));

    // Even though exclusions were provided, forwarding is disabled
    assertThat(helper.getExcludedParamsLowercase(), is(ImmutableSet.of()));
  }

  @Test
  void buildQueryString_atMaxLimit_succeeds() {
    helper.updateConfig(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    for (int i = 0; i < MAX_FORWARDED_PARAMS; i++) {
      parameters.set("param" + i, "value" + i);
    }

    String result = helper.buildQueryString(parameters);

    assertThat(result, is(not(emptyString())));
  }

  @Test
  void buildQueryString_exceedsMaxLimit_throwsBadRequest() {
    helper.updateConfig(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    for (int i = 0; i <= MAX_FORWARDED_PARAMS; i++) {
      parameters.set("param" + i, "value" + i);
    }

    BadRequestException ex = assertThrows(BadRequestException.class, () -> helper.buildQueryString(parameters));
    assertThat(ex.getMessage(), containsString(String.valueOf(MAX_FORWARDED_PARAMS + 1)));
    assertThat(ex.getMessage(), containsString(String.valueOf(MAX_FORWARDED_PARAMS)));
  }

  @Test
  void buildQueryString_paramCountIncludesExcludedInLimit() {
    helper.updateConfig(true, Collections.singletonList("excluded"));

    Parameters parameters = new Parameters();
    for (int i = 0; i < MAX_FORWARDED_PARAMS; i++) {
      parameters.set("param" + i, "value" + i);
    }
    parameters.set("excluded", "secret");

    // 50 forwardable + 1 excluded = 51 total — exceeds limit regardless of exclusion
    assertThrows(BadRequestException.class, () -> helper.buildQueryString(parameters));
  }

  @Test
  void buildQueryString_multiValuedParam_eachValueCounts() {
    helper.updateConfig(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    String[] values = new String[MAX_FORWARDED_PARAMS + 1];
    for (int i = 0; i <= MAX_FORWARDED_PARAMS; i++) {
      values[i] = "val" + i;
    }
    parameters.set("repeated", values);

    // 51 values on one key still exceeds the limit
    assertThrows(BadRequestException.class, () -> helper.buildQueryString(parameters));
  }
}
