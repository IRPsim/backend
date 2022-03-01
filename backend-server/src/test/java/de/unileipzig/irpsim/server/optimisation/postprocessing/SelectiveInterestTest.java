package de.unileipzig.irpsim.server.optimisation.postprocessing;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import de.unileipzig.irpsim.core.simulation.data.AggregatedResult;
import de.unileipzig.irpsim.core.simulation.data.PostProcessing;
import de.unileipzig.irpsim.core.testutils.UseAlternateDependencies;
import de.unileipzig.irpsim.core.utils.TestFiles;

/**
 * @author krauss
 */
public final class SelectiveInterestTest {

	private static final String INTEREST_SUM = "par_out_I_DES_EB_flexibility";
	private static final String CONSTANT_SUM = "par_out_C_MS_E";

	@Rule
	public UseAlternateDependencies alt = new UseAlternateDependencies(TestFiles.ALTERNATIVE_DEPENDENCY);

	/**
	 * Testet, ob nur parameter mit I, O und IuO verzinst werden.
	 */
	@Test
	public void testCorrectInterestHandling() {
		final MultipleYearPostprocessorHandler myph = new MultipleYearPostprocessorHandler(1.1, UseAlternateDependencies.ALTERNATIVE_DEPENDENCIES_ID);
		final PostProcessing postprocessing = new PostProcessing();
		final Map<String, AggregatedResult> scalars = new HashMap<>();
		postprocessing.setScalars(scalars);
		final AggregatedResult constant = new AggregatedResult();
		scalars.put(CONSTANT_SUM, constant);
		final AggregatedResult interest = new AggregatedResult();
		scalars.put(INTEREST_SUM, interest);
		constant.setSum(11d);
		interest.setSum(11d);
		myph.addResult(postprocessing, 1);
		final PostProcessing result = myph.fetchPostprocessingResults();
		Assert.assertEquals(11d, result.getScalars().get(CONSTANT_SUM).getSum(), 0.1);
		Assert.assertEquals(10d, result.getScalars().get(INTEREST_SUM).getSum(), 0.1);
	}
}
