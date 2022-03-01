package de.unileipzig.irpsim.server.optimisation.comparators;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.MapDifference;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.optimisation.comparators.modelDifferences.PerformanceModelComparator;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class PerformanceModelComparatorTest {

   @Test
   public void diff() throws FileNotFoundException {

      List<OptimisationJobPersistent> jobs = new LinkedList<>();
      OptimisationJobPersistent job1 = new OptimisationJobPersistent();
      job1.setCreation(new Date());
      job1.setEnd(new Date());
      final String parameters21 = DatabaseTestUtils.getParameterText(TestFiles.DAYS_21.make());
      final String parameters3 = DatabaseTestUtils.getParameterText(TestFiles.DAYS_3.make());

      job1.setJsonParameter(parameters3);
      job1.setId(1L);
      OptimisationJobPersistent job2 = new OptimisationJobPersistent();
      job2.setCreation(new Date());
      job2.setEnd(new Date());
      job2.setJsonParameter(parameters21);
      job2.setId(2L);
      jobs.add(job1);
      jobs.add(job2);
      PerformanceModelComparator pmc = new PerformanceModelComparator(jobs);
      Map<Pair<Long, Long>, MapDifference<String, Object>> differenceMap = null;
      try {
         differenceMap = pmc.diff();
      } catch (JsonProcessingException e) {
         e.printStackTrace();
         fail();
      }

      assertNotNull(differenceMap);
      assertFalse(differenceMap.isEmpty());

      Map.Entry<Pair<Long, Long>, MapDifference<String, Object>> entry = differenceMap.entrySet().iterator().next();
      Pair<Long, Long> expected = Pair.of(job1.getId(), job2.getId());
      assertEquals(expected,entry.getKey());
      MapDifference<String, Object> diffValues = entry.getValue();

      assertTrue(diffValues.entriesOnlyOnLeft().isEmpty());
      assertTrue(diffValues.entriesOnlyOnRight().isEmpty());
      assertFalse(diffValues.entriesDiffering().isEmpty());
      assertTrue(diffValues.entriesDiffering().containsKey("/models/0/years/0/config/simulationlength"));
   }
}