/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehcache;

import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.CacheConfigurationBuilder;
import org.ehcache.config.ResourcePools;
import org.ehcache.config.ResourcePoolsBuilder;
import org.ehcache.config.ResourceType;
import org.ehcache.config.persistence.PersistenceConfiguration;
import org.ehcache.config.units.EntryUnit;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author rism
 */
public class RuntimeConfigurationTest {

  @Test
  public void testUpdateResources() {
    CacheConfiguration<Long, String> cacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder()
        .withResourcePools(ResourcePoolsBuilder.newResourcePoolsBuilder()
            .heap(10L, EntryUnit.ENTRIES).disk(50, EntryUnit.ENTRIES).build()).buildConfig(Long.class, String.class);

    final CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
        .with(new PersistenceConfiguration(new File(System.getProperty("java.io.tmpdir") + "/myData")))
        .withCache("cache", cacheConfiguration).build(true);

    Cache<Long, String> cache = cacheManager.getCache("cache", Long.class, String.class);

    ResourcePoolsBuilder poolsBuilder = ResourcePoolsBuilder.newResourcePoolsBuilder();
    poolsBuilder = poolsBuilder.heap(20L, EntryUnit.ENTRIES);
    ResourcePools pools = poolsBuilder.build();
    cache.getRuntimeConfiguration().updateResourcePools(pools);
    assertThat(cache.getRuntimeConfiguration().getResourcePools()
        .getPoolForResource(ResourceType.Core.HEAP).getSize(), is(20L));
    poolsBuilder = poolsBuilder.disk(100L, EntryUnit.ENTRIES);
    pools = poolsBuilder.build();
    cache.getRuntimeConfiguration().updateResourcePools(pools);
    assertThat(cache.getRuntimeConfiguration().getResourcePools()
        .getPoolForResource(ResourceType.Core.DISK).getSize(), is(100L));
    assertThat(cache.getRuntimeConfiguration().getResourcePools()
        .getPoolForResource(ResourceType.Core.HEAP).getSize(), is(20L));
    cacheManager.close();
  }

  @Test
  public void testUpdateFailureDoesNotUpdate() {
    CacheConfiguration<Long, String> cacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder()
        .withResourcePools(ResourcePoolsBuilder.newResourcePoolsBuilder()
            .heap(10L, EntryUnit.ENTRIES).build()).buildConfig(Long.class, String.class);

    final CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
        .withCache("cache", cacheConfiguration).build(true);

    Cache<Long, String> cache = cacheManager.getCache("cache", Long.class, String.class);

    ResourcePoolsBuilder poolsBuilder = ResourcePoolsBuilder.newResourcePoolsBuilder();
    poolsBuilder = poolsBuilder.heap(20L, EntryUnit.ENTRIES).disk(50, EntryUnit.ENTRIES);
    ResourcePools pools = poolsBuilder.build();
    try {
      cache.getRuntimeConfiguration().updateResourcePools(pools);
    } catch (IllegalArgumentException iae) {
//      expected
      assertThat(iae.getMessage(), is("Pools to be updated cannot contain previously undefined resources pools"));
    }
    assertThat(cache.getRuntimeConfiguration().getResourcePools()
        .getPoolForResource(ResourceType.Core.HEAP).getSize(), is(10L));
    cacheManager.close();
  }
}
