package de.unileipzig.irpsim.server.optimisation.comparators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import de.unileipzig.irpsim.server.optimisation.comparators.modelconnectors.ConnectionType;
import de.unileipzig.irpsim.server.optimisation.comparators.modelconnectors.DependencyType;
import de.unileipzig.irpsim.server.optimisation.comparators.modelconnectors.SyncableParameter;
import de.unileipzig.irpsim.server.optimisation.comparators.modelconnectors.SyncableParameterHandler;
import org.apache.commons.math3.util.Pair;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

public class SyncableParameterHandlerTest {

   @Test
   public void fetchSyncableParameter() {
      SyncableParameterHandler handler = new SyncableParameterHandler();
      Map<ConnectionType, SyncableParameter> sp = handler.fetchSyncableParameter(new Pair<>(1, 3));

      assertFalse(sp.isEmpty());
      assertTrue(sp.containsKey(ConnectionType.INPUT));
      assertTrue(sp.containsKey(ConnectionType.OUTPUT));
      assertTrue(sp.containsKey(ConnectionType.OUTPUT_TO_INPUT));

      SyncableParameter syncableParameter = sp.get(ConnectionType.OUTPUT_TO_INPUT);
      assertNotNull(syncableParameter);
      MatcherAssert.assertThat(syncableParameter.getParameterMap().get(DependencyType.SET_TIMESERIES), Matchers.containsInAnyOrder("par_IuO_ESector_CustSide"));

      Map<DependencyType, List<String>> map = syncableParameter.getParameterMap();
      assertTrue(map.containsKey(DependencyType.SET_TIMESERIES));
      List<String> attributes = map.get(DependencyType.SET_TIMESERIES);
      assertFalse(attributes.isEmpty());
   }

   @Test
   public void testFetchSyncableParameterGenerics() {
      ConnectionType type = ConnectionType.OUTPUT_TO_INPUT;

      SyncableParameterHandler handler = new SyncableParameterHandler();
      SyncableParameter sp = handler.fetchSyncableParameter(new Pair<>(1, 3), type);

      assertNotNull(sp);

      Pair<Integer, Integer> modelDefinitionCombination = sp.getModelDefinitionCombination();
      assertNotNull(modelDefinitionCombination);
      assertEquals(1, modelDefinitionCombination.getKey().intValue());
      assertEquals(3, modelDefinitionCombination.getValue().intValue());

      assertEquals(type, sp.getType());

      Map<DependencyType, List<String>> parameterMap = sp.getParameterMap();
      assertNotNull(parameterMap);
      assertFalse(parameterMap.isEmpty());
   }
}
