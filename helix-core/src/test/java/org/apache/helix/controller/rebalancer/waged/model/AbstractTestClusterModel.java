package org.apache.helix.controller.rebalancer.waged.model;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.helix.controller.dataproviders.ResourceControllerDataProvider;
import org.apache.helix.controller.rebalancer.constraint.MonitoredAbnormalResolver;
import org.apache.helix.model.BuiltInStateModelDefinitions;
import org.apache.helix.model.ClusterConfig;
import org.apache.helix.model.CurrentState;
import org.apache.helix.model.InstanceConfig;
import org.apache.helix.model.LiveInstance;
import org.apache.helix.model.ResourceConfig;
import org.mockito.Mockito;
import org.testng.annotations.BeforeClass;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public abstract class AbstractTestClusterModel {
  protected static String _sessionId = "testSessionId";
  protected String _testInstanceId;
  protected List<String> _resourceNames;
  protected List<String> _partitionNames;
  protected Map<String, Integer> _capacityDataMap;
  protected Map<String, List<String>> _disabledPartitionsMap;
  protected List<String> _testInstanceTags;
  protected String _testFaultZoneId;
  protected Set<String> _instances;

  @BeforeClass
  public void initialize() {
    _testInstanceId = "testInstanceId";
    _resourceNames = new ArrayList<>();
    _resourceNames.add("Resource1");
    _resourceNames.add("Resource2");
    _partitionNames = new ArrayList<>();
    _partitionNames.add("Partition1");
    _partitionNames.add("Partition2");
    _partitionNames.add("Partition3");
    _partitionNames.add("Partition4");
    _capacityDataMap = new HashMap<>();
    _capacityDataMap.put("item1", 20);
    _capacityDataMap.put("item2", 40);
    _capacityDataMap.put("item3", 30);
    List<String> disabledPartitions = new ArrayList<>();
    disabledPartitions.add("TestPartition");
    _disabledPartitionsMap = new HashMap<>();
    _disabledPartitionsMap.put("TestResource", disabledPartitions);
    _testInstanceTags = new ArrayList<>();
    _testInstanceTags.add("TestTag");
    _testFaultZoneId = "testZone";
    _instances = new HashSet<>();
    _instances.add(_testInstanceId);
  }

  protected InstanceConfig createMockInstanceConfig(String instanceId) {
    InstanceConfig testInstanceConfig = new InstanceConfig(instanceId);
    testInstanceConfig.setInstanceCapacityMap(_capacityDataMap);
    testInstanceConfig.addTag(_testInstanceTags.get(0));
    testInstanceConfig.setInstanceEnabled(true);
    testInstanceConfig.setZoneId(_testFaultZoneId);
    return testInstanceConfig;
  }

  protected LiveInstance createMockLiveInstance(String instanceId) {
    LiveInstance testLiveInstance = new LiveInstance(instanceId);
    testLiveInstance.setSessionId(_sessionId);
    return testLiveInstance;
  }

  protected ResourceControllerDataProvider setupClusterDataCache() throws IOException {
    ResourceControllerDataProvider testCache = Mockito.mock(ResourceControllerDataProvider.class);

    // 1. Set up the default instance information with capacity configuration.
    InstanceConfig testInstanceConfig = createMockInstanceConfig(_testInstanceId);
    testInstanceConfig.setInstanceEnabledForPartition("TestResource", "TestPartition", false);
    Map<String, InstanceConfig> instanceConfigMap = new HashMap<>();
    instanceConfigMap.put(_testInstanceId, testInstanceConfig);
    when(testCache.getInstanceConfigMap()).thenReturn(instanceConfigMap);

    // 2. Set up the basic cluster configuration.
    ClusterConfig testClusterConfig = new ClusterConfig("testClusterConfigId");
    testClusterConfig.setMaxPartitionsPerInstance(5);
    testClusterConfig.setDisabledInstances(Collections.emptyMap());
    testClusterConfig.setInstanceCapacityKeys(new ArrayList<>(_capacityDataMap.keySet()));
    testClusterConfig.setDefaultPartitionWeightMap(
        _capacityDataMap.keySet().stream().collect(Collectors.toMap(key -> key, key -> 0)));
    testClusterConfig.setTopologyAwareEnabled(true);
    when(testCache.getClusterConfig()).thenReturn(testClusterConfig);
    when(testCache.getAbnormalStateResolver(any()))
        .thenReturn(MonitoredAbnormalResolver.DUMMY_STATE_RESOLVER);

    // 3. Mock the live instance node for the default instance.
    LiveInstance testLiveInstance = createMockLiveInstance(_testInstanceId);
    Map<String, LiveInstance> liveInstanceMap = new HashMap<>();
    liveInstanceMap.put(_testInstanceId, testLiveInstance);
    when(testCache.getLiveInstances()).thenReturn(liveInstanceMap);

    // 4. Mock two resources, each with 2 partitions on the default instance.
    // The instance will have the following partitions assigned
    // Resource 1:
    // -------------- partition 1 - MASTER
    // -------------- partition 2 - SLAVE
    // Resource 2:
    // -------------- partition 3 - MASTER
    // -------------- partition 4 - SLAVE
    CurrentState testCurrentStateResource1 = Mockito.mock(CurrentState.class);
    Map<String, String> partitionStateMap1 = new HashMap<>();
    partitionStateMap1.put(_partitionNames.get(0), "MASTER");
    partitionStateMap1.put(_partitionNames.get(1), "SLAVE");
    when(testCurrentStateResource1.getResourceName()).thenReturn(_resourceNames.get(0));
    when(testCurrentStateResource1.getPartitionStateMap()).thenReturn(partitionStateMap1);
    when(testCurrentStateResource1.getStateModelDefRef()).thenReturn("MasterSlave");
    when(testCurrentStateResource1.getState(_partitionNames.get(0))).thenReturn("MASTER");
    when(testCurrentStateResource1.getState(_partitionNames.get(1))).thenReturn("SLAVE");
    when(testCurrentStateResource1.getSessionId()).thenReturn(_sessionId);

    CurrentState testCurrentStateResource2 = Mockito.mock(CurrentState.class);
    Map<String, String> partitionStateMap2 = new HashMap<>();
    partitionStateMap2.put(_partitionNames.get(2), "MASTER");
    partitionStateMap2.put(_partitionNames.get(3), "SLAVE");
    when(testCurrentStateResource2.getResourceName()).thenReturn(_resourceNames.get(1));
    when(testCurrentStateResource2.getPartitionStateMap()).thenReturn(partitionStateMap2);
    when(testCurrentStateResource2.getStateModelDefRef()).thenReturn("MasterSlave");
    when(testCurrentStateResource2.getState(_partitionNames.get(2))).thenReturn("MASTER");
    when(testCurrentStateResource2.getState(_partitionNames.get(3))).thenReturn("SLAVE");
    when(testCurrentStateResource2.getSessionId()).thenReturn(_sessionId);

    Map<String, CurrentState> currentStatemap = new HashMap<>();
    currentStatemap.put(_resourceNames.get(0), testCurrentStateResource1);
    currentStatemap.put(_resourceNames.get(1), testCurrentStateResource2);
    when(testCache.getCurrentState(_testInstanceId, _sessionId)).thenReturn(currentStatemap);
    when(testCache.getCurrentState(_testInstanceId, _sessionId, false)).thenReturn(currentStatemap);

    // 5. Set up the resource config for the two resources with the partition weight.
    Map<String, Integer> capacityDataMapResource1 = new HashMap<>();
    capacityDataMapResource1.put("item1", 3);
    capacityDataMapResource1.put("item2", 6);
    ResourceConfig testResourceConfigResource1 = new ResourceConfig("Resource1");
    testResourceConfigResource1.setPartitionCapacityMap(
        Collections.singletonMap(ResourceConfig.DEFAULT_PARTITION_KEY, capacityDataMapResource1));
    when(testCache.getResourceConfig("Resource1")).thenReturn(testResourceConfigResource1);

    Map<String, Integer> capacityDataMapResource2 = new HashMap<>();
    capacityDataMapResource2.put("item1", 5);
    capacityDataMapResource2.put("item2", 10);
    ResourceConfig testResourceConfigResource2 = new ResourceConfig("Resource2");
    testResourceConfigResource2.setPartitionCapacityMap(
        Collections.singletonMap(ResourceConfig.DEFAULT_PARTITION_KEY, capacityDataMapResource2));
    when(testCache.getResourceConfig("Resource2")).thenReturn(testResourceConfigResource2);
    Map<String, ResourceConfig> configMap = new HashMap<>();
    configMap.put("Resource1", testResourceConfigResource1);
    configMap.put("Resource2", testResourceConfigResource2);
    when(testCache.getResourceConfigMap()).thenReturn(configMap);

    // 6. Define mock state model
    for (BuiltInStateModelDefinitions bsmd : BuiltInStateModelDefinitions.values()) {
      when(testCache.getStateModelDef(bsmd.name())).thenReturn(bsmd.getStateModelDefinition());
    }

    return testCache;
  }

  // Add another resource. When compute, the two smaller resources' Master partition should be
  // assigned to one instance and the relatively larger one's Master partition should be assigned to
  // another.
  // The sorting algorithm in ConstraintBasedAlgorithm should garnette these 2 smaller resources
  // are placed after the larger one.
  // This is the only way to accommodate all 6 partitions.
  protected ResourceControllerDataProvider setupClusterDataCacheForNearFullUtil() throws IOException {
    _resourceNames.add("Resource3");
    _partitionNames.add("Partition5");
    _partitionNames.add("Partition6");
    ResourceControllerDataProvider testCache = setupClusterDataCache();

    CurrentState testCurrentStateResource3 = Mockito.mock(CurrentState.class);
    Map<String, String> partitionStateMap3 = new HashMap<>();
    partitionStateMap3.put(_partitionNames.get(4), "MASTER");
    partitionStateMap3.put(_partitionNames.get(5), "SLAVE");
    when(testCurrentStateResource3.getResourceName()).thenReturn(_resourceNames.get(2));
    when(testCurrentStateResource3.getPartitionStateMap()).thenReturn(partitionStateMap3);
    when(testCurrentStateResource3.getStateModelDefRef()).thenReturn("MasterSlave");
    when(testCurrentStateResource3.getState(_partitionNames.get(4))).thenReturn("MASTER");
    when(testCurrentStateResource3.getState(_partitionNames.get(5))).thenReturn("SLAVE");
    when(testCurrentStateResource3.getSessionId()).thenReturn(_sessionId);

    Map<String, CurrentState> currentStatemap = testCache.getCurrentState(_testInstanceId, _sessionId);
    currentStatemap.put(_resourceNames.get(2), testCurrentStateResource3);
    when(testCache.getCurrentState(_testInstanceId, _sessionId)).thenReturn(currentStatemap);
    when(testCache.getCurrentState(_testInstanceId, _sessionId, false)).thenReturn(currentStatemap);

    Map<String, Integer> capacityDataMapResource3 = new HashMap<>();
    capacityDataMapResource3.put("item1", 9);
    capacityDataMapResource3.put("item2", 17);
    ResourceConfig testResourceConfigResource3 = new ResourceConfig("Resource3");
    testResourceConfigResource3.setPartitionCapacityMap(
        Collections.singletonMap(ResourceConfig.DEFAULT_PARTITION_KEY, capacityDataMapResource3));
    when(testCache.getResourceConfig("Resource3")).thenReturn(testResourceConfigResource3);
    Map<String, ResourceConfig> configMap = testCache.getResourceConfigMap();
    configMap.put("Resource3", testResourceConfigResource3);
    when(testCache.getResourceConfigMap()).thenReturn(configMap);
    return testCache;
  }

  // Add another resource that has large capacity for negative test
  // TODO: this function has a lot similar line as previous one. We should have a more
  // generalized factory function instead.
  protected ResourceControllerDataProvider setupClusterDataCacheForNoFitUtil() throws IOException {
    _resourceNames.add("Resource4");
    _partitionNames.add("Partition7");
    _partitionNames.add("Partition8");
    ResourceControllerDataProvider testCache = setupClusterDataCache();

    CurrentState testCurrentStateResource4 = Mockito.mock(CurrentState.class);
    Map<String, String> partitionStateMap4 = new HashMap<>();
    partitionStateMap4.put(_partitionNames.get(4), "MASTER");
    partitionStateMap4.put(_partitionNames.get(5), "SLAVE");
    when(testCurrentStateResource4.getResourceName()).thenReturn(_resourceNames.get(2));
    when(testCurrentStateResource4.getPartitionStateMap()).thenReturn(partitionStateMap4);
    when(testCurrentStateResource4.getStateModelDefRef()).thenReturn("MasterSlave");
    when(testCurrentStateResource4.getState(_partitionNames.get(4))).thenReturn("MASTER");
    when(testCurrentStateResource4.getState(_partitionNames.get(5))).thenReturn("SLAVE");
    when(testCurrentStateResource4.getSessionId()).thenReturn(_sessionId);

    Map<String, CurrentState> currentStatemap =
        testCache.getCurrentState(_testInstanceId, _sessionId);
    currentStatemap.put(_resourceNames.get(2), testCurrentStateResource4);
    when(testCache.getCurrentState(_testInstanceId, _sessionId)).thenReturn(currentStatemap);
    when(testCache.getCurrentState(_testInstanceId, _sessionId, false)).thenReturn(currentStatemap);

    Map<String, Integer> capacityDataMapResource3 = new HashMap<>();
    capacityDataMapResource3.put("item1", 90);
    capacityDataMapResource3.put("item2", 9);
    ResourceConfig testResourceConfigResource3 = new ResourceConfig("Resource4");
    testResourceConfigResource3.setPartitionCapacityMap(
        Collections.singletonMap(ResourceConfig.DEFAULT_PARTITION_KEY, capacityDataMapResource3));
    when(testCache.getResourceConfig("Resource4")).thenReturn(testResourceConfigResource3);
    Map<String, ResourceConfig> configMap = testCache.getResourceConfigMap();
    configMap.put("Resource4", testResourceConfigResource3);
    when(testCache.getResourceConfigMap()).thenReturn(configMap);
    return testCache;
  }

  /**
   * Generate the replica objects according to the provider information.
   */
  protected Set<AssignableReplica> generateReplicas(ResourceControllerDataProvider dataProvider) {
    // Create assignable replica based on the current state.
    Map<String, CurrentState> currentStatemap =
        dataProvider.getCurrentState(_testInstanceId, _sessionId);
    Set<AssignableReplica> assignmentSet = new HashSet<>();
    for (CurrentState cs : currentStatemap.values()) {
      ResourceConfig resourceConfig = dataProvider.getResourceConfig(cs.getResourceName());
      // Construct one AssignableReplica for each partition in the current state.
      cs.getPartitionStateMap().entrySet().stream().forEach(entry -> assignmentSet
              .add(new AssignableReplica(dataProvider.getClusterConfig(), resourceConfig,
                  entry.getKey(), entry.getValue(), entry.getValue().equals("MASTER") ? 1 : 2)));
    }
    return assignmentSet;
  }

  protected Set<AssignableNode> generateNodes(ResourceControllerDataProvider testCache) {
    Set<AssignableNode> nodeSet = new HashSet<>();
    testCache.getInstanceConfigMap().values().forEach(config -> nodeSet
        .add(new AssignableNode(testCache.getClusterConfig(), config, config.getInstanceName())));
    return nodeSet;
  }
}
