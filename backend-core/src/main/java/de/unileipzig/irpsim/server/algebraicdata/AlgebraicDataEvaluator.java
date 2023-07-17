package de.unileipzig.irpsim.server.algebraicdata;

import de.unileipzig.irpsim.core.data.timeseries.LoadElement;
import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.TimeInterval;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.DataLoader;
import de.unileipzig.irpsim.core.standingdata.StammdatenUtil;
import de.unileipzig.irpsim.core.standingdata.StaticDataUtil;
import de.unileipzig.irpsim.core.standingdata.data.*;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.hibernate.Session;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.script.Invocable;
import java.util.*;

/**
 * Each formula is applicable to each <code>Datensatz#szenario</code>. This class evaluates the formula of an instance of <code>AlgebraicData</code> using math.js. It transitively collects
 * dependencies (static or algebraic data), evaluates formulas the correct order and finally returns the evaluated data.
 * 
 * Rationale: Formulas may be recursive. For example, let A(n)=A(n-1)+B(n) and B(n)=A(n-1)+B(n-1). The number of predecessor series we would need to evaluate in a naive fashion would grow
 * exponentially!
 * 
 * @author sdienst
 *
 */
@SuppressWarnings("restriction")
public class AlgebraicDataEvaluator {
	static final Logger LOG = LogManager.getLogger(AlgebraicDataEvaluator.class);

	private static final GenericObjectPool<Invocable> enginePool;
	static {
		final GenericObjectPoolConfig<Invocable> cfg = new GenericObjectPoolConfig<Invocable>();
		cfg.setMinIdle(1);
		cfg.setMaxIdle(8);
		cfg.setBlockWhenExhausted(true);
		cfg.setLifo(true);
		enginePool = new GenericObjectPool<>(new JSEngineFactory(), cfg);
		// run pool initialization in a background thread, so startup of the server is not delayed
		final Thread initThread = new Thread(new Runnable() {
			public void run() {
				try {
					enginePool.preparePool();
				} catch (final Exception e) {
					LOG.error("Fehler beim Befüllen des JSEngine-Pools", e);
				}
			}
		});
		// don't keep the JVM from terminating just because we are still running
		initThread.setDaemon(true);
		initThread.start();

	}

	/**
	 * Value object, needed to memoize calls to {@link AlgebraicDataEvaluator#findDatensatz(Stammdatum, String, int)}
	 * 
	 * @author sdienst
	 *
	 */
	private static class StammdatumCacheObj {
		private final Stammdatum sd;
		private final int szenario;
		private final int jahr;

		public StammdatumCacheObj(final Stammdatum sd, final int szenario, final int jahr) {
			this.sd = sd;
			this.szenario = szenario;
			this.jahr = jahr;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + jahr;
			result = prime * result + ((sd == null) ? 0 : sd.hashCode());
			result = prime * result + ((szenario == 0) ? 0 : szenario);
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final StammdatumCacheObj other = (StammdatumCacheObj) obj;
			if (jahr != other.jahr)
				return false;
			if (sd == null) {
				if (other.sd != null)
					return false;
			} else if (!sd.equals(other.sd))
				return false;
			if (szenario != other.szenario) {
				return false;
			}
			return true;
		}
	}

	/**
	 * Cache to avoid re-querying the database for Datensatz instances
	 */
	private final Map<StammdatumCacheObj, Datensatz> cache;

	public AlgebraicDataEvaluator() {
		this.cache = new HashMap<>();
	}

	/**
	 * Try to load all dependencies needed to evaluate the formula. Returns a string describing the invalid state, if any.
	 * 
	 * @param algd
	 * @return
	 */
	public String validateDependencies(final AlgebraicData algd) {
		try {
			loadAndSortDependencies(algd);
			return null;
		} catch (final IllegalStateException e) {
			return e.getMessage();
		}
	}

	private void addDependencies(final DirectedGraph<Datensatz> graph, final Datensatz root) {
		if (root instanceof AlgebraicData) {
			final AlgebraicData ad = (AlgebraicData) root;
			for (final Variable var : ad.getVariablenZuordnung().values()) {
				final Datensatz dependency = findDependency(var, root.getSzenario(), root.getJahr());
				graph.addNode(dependency);
				graph.addEdge(root, dependency);
				addDependencies(graph, dependency);
			}
		}
	}

	/**
	 * First, we need to identify the correct evaluation order to prevent excessive reevaluations or formulas, especially in recursive definitions. To find the correct order, we define a graph of
	 * dependencies (formula->input) and sort the vertices of this graph topologically.
	 * 
	 * @param algd
	 * @return list of {@link Datensatz} objects in necessary evaluation order to satisfy dependencies of argument
	 */
	private List<Datensatz> loadAndSortDependencies(final AlgebraicData algd) {
		final DirectedGraph<Datensatz> graph = new DirectedGraph<>();
		graph.addNode(algd);
		addDependencies(graph, algd);
		List<Datensatz> order = null;
		try {
			order = TopologicalSort.sort(graph);
			Collections.reverse(order);
			return order;
		} catch (final IllegalArgumentException e) {
			throw new RuntimeException("Die Formel " + algd.getId() + " hat zyklische Abhängigkeiten und kann nicht ausgewertet werden!");
		}
	}

	/**
	 * Evaluate a formula. This method will recursivly evalutate all needed inputs (either {@link StaticData} or {@link AlgebraicData}).
	 * 
	 * @return array of values of length determined by {@link Timeseries#loadTimeseries(boolean)} and {@link GAMSModelParametrisationUtil#rolloutTimeseriesTo35040(TimeInterval, List)}.
	 */
	public double[] evaluateFormula(final AlgebraicData algd) {
		final Map<Datensatz, double[]> dataCache = new HashMap<>();
		final List<Datensatz> order = loadAndSortDependencies(algd);
		try {
			for (final Datensatz ds : order) {
				if (ds instanceof StaticData) {
					dataCache.put(ds, fetchValue((StaticData) ds));
				} else if (ds instanceof AlgebraicData) {
					final double[] intermediaryResult = evaluateFormula((AlgebraicData) ds, dataCache);
					// TODO persist materialized results
					dataCache.put(ds, intermediaryResult);
				}
			}
			// TODO persist results
			return dataCache.get(algd);
		} catch (final Exception e) {
			throw new RuntimeException("Formel konnte nicht ausgewertet werden.", e);
		}
	}

	/**
	 * Evaluate a math.js formula.
	 * 
	 * @param ds
	 *            contains formula
	 * @param szenario
	 *            target scenario
	 * @param zielJahr
	 *            target year
	 * @param dataCache
	 *            materialized data of formula dependencies
	 * @return materialized numerical results
	 * @throws Exception
	 */
	private double[] evaluateFormula(final AlgebraicData ds, final Map<Datensatz, double[]> dataCache) throws Exception {
		final Map<String, double[]> params = new HashMap<>();
		for (final String key : ds.getVariablenZuordnung().keySet()) {
			final Variable var = ds.getVariablenZuordnung().get(key);

			final Datensatz dependency = findDependency(var, ds.getSzenario(), ds.getJahr());
			final double[] value = dataCache.get(dependency);
			params.put(key, value);
		}
		final String json = new JSONObject(params).toString();
		final Invocable engine = enginePool.borrowObject();
		final ScriptObjectMirror result = (ScriptObjectMirror) engine.invokeFunction("evaluateFormula", ds.getFormel(), json);
		enginePool.returnObject(engine);
		final Collection<Object> results = result.values();
		final double[] res = new double[results.size()];
		int idx = 0;
		for (final Object o : results) {
			res[idx++] = ((Number) o).doubleValue();
		}
		LOG.trace(ds.getStammdatum() + ",  formel=" + ds.getFormel() + ", Jahr=" + ds.getJahr() + ", res=" + Arrays.toString(Arrays.copyOf(res, 10)));
		return res;
	}

	/**
	 * Fetch values from database.
	 * 
	 * @param ds
	 * @return
	 */
	private double[] fetchValue(final StaticData ds) {
		final List<Double> rawValues = Timeseries.build(ds.getId()).loadTimeseries(false).getValues();
		final TimeInterval timeInterval = TimeInterval.getInterval(rawValues.size());
		final List<Double> values = StammdatenUtil.rolloutTimeseriesTo35040(timeInterval, rawValues);
		final int length = values.size();
		final double[] res = new double[length];
		int idx = 0;
		for (final Number num : values) {
			if (idx >= length) {
				break;
			}
			res[idx++] = num.doubleValue();
		}
		return res;
	}

	/**
	 * @param var
	 * @param szenario
	 * @param zielJahr
	 * @return
	 */
	private Datensatz findDependency(final Variable var, final int szenario, final int zielJahr) {
		int jahr;
		switch (var.getJahr()) {
		case 0:
			jahr = zielJahr;
			break;
		case -1:
			jahr = zielJahr - 1;
			break;
		default:
			jahr = var.getStammdatum().getBezugsjahr();
		}
		return findDatensatz(var.getStammdatum(), szenario, jahr);
	}

	/**
	 * Look up Datensatz. Uses cache to avoid excessive DB queries (especially in recursive formulas).
	 * 
	 * @param sd
	 * @param szenario
	 * @param zielJahr
	 * @return
	 */
	private Datensatz findDatensatz(final Stammdatum sd, final int szenario, final int zielJahr) {
		final StammdatumCacheObj cobj = new StammdatumCacheObj(sd, szenario, zielJahr);
		Datensatz ds = cache.get(cobj);
		if (ds != null) {
			return ds;
		} else {
			try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
				// algebraicdata can be used for all scenarios, so we need distinct queries for static and algebraic data
				LOG.info("Suche: Zieljahr:{}, Stammdatum-ID:{}, Szenario:{}", zielJahr, sd.getId(), szenario);
				final Session s = (Session) em.getDelegate();
				CriteriaBuilder builder = s.getCriteriaBuilder();
				CriteriaQuery<Datensatz> criteria = builder.createQuery(Datensatz.class);
				Root<Datensatz> queryRoot = criteria.from(Datensatz.class);

				Predicate likeRestrictions = builder.and(
						builder.equal(queryRoot.get("jahr"), zielJahr),
						builder.equal(queryRoot.get("stammdatum"), sd),
						builder.equal(queryRoot.get("szenario"), szenario)
				);

				criteria.select(queryRoot).where(likeRestrictions);

				ds = s.createQuery(criteria).uniqueResult();

				if (ds != null) {
					StaticDataUtil.fillStammdatum(ds.getStammdatum());
					s.detach(ds);
				}
				if (ds == null) {
					throw new IllegalStateException("Could not find a dataset for Stammdatum " + sd.getId() + ", szenario " + szenario + " and jahr=" + zielJahr);
				} else {
					cache.put(cobj, ds);
					return ds;
				}
			}
		}
	}

	//////////////// interaktive Experimente, nicht Bestandteil des offiziellen Codes //////////////////////////////////////////////////

	private static double[] arrayOf(final double value) {
		final double[] arr = new double[35040];
		Arrays.fill(arr, value);
		return arr;
	}

	public static void main(final String[] args) throws Exception {
		enableTraceLog();
//		initDBConnection();

		// evalTest();
		// visualizationDataTest();
		// staticJSEvalTest();
		testRolledUp();
	}

	public static void testRolledUp() {
		final DataLoader loader = new DataLoader(Arrays.asList(4078), new DateTime(2001, 1, 1, 0, 0), new DateTime(2001, 12, 31, 23, 59), 1);
		LOG.info(loader.getResultData());
	}

	public static void evalTest() throws InterruptedException {
		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final Session session = (Session) em.getDelegate();
			final AlgebraicData datensatz = session.get(AlgebraicData.class, 4078);
			StaticDataUtil.fillStammdatum(datensatz.getStammdatum());
			final StopWatch sw = new StopWatch();
			sw.start();
			final double[] data = new AlgebraicDataEvaluator().evaluateFormula(datensatz);
			sw.stop();
			LOG.info("Elemente insgesamt: {}, 10 Werte: {}", data.length, Arrays.toString(Arrays.copyOf(data, 10)));
			LOG.info("evaluation took " + sw.getTime() + "ms");
		}
	}

	private static void enableTraceLog() {
		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		final Configuration config = ctx.getConfiguration();
		final LoggerConfig loggerConfig = config.getLoggerConfig(LOG.getName());
		loggerConfig.setLevel(Level.TRACE);
		ctx.updateLoggers();

	}

	private static void visualizationDataTest() {
		final int id = 1279;
		final List<LoadElement> res = (List<LoadElement>) new DataLoader(Arrays.asList(id), new DateTime(2017, 1, 1, 0, 0), new DateTime(2017, 12, 31, 23, 45), 35040).getResultData().get(id);
		LOG.info("Elemente insgesamt: {}, 10 Werte: {}", res.size(), res.subList(0, 10));
	}

	private static void staticJSEvalTest() throws Exception {
		final StopWatch sw = new StopWatch();
		sw.start();
		final Map<String, double[]> params = new HashMap<>();
		params.put("a", arrayOf(1));
		params.put("b", arrayOf(2));
		params.put("c", arrayOf(3));
		params.put("d", arrayOf(4));
		params.put("e", arrayOf(5));
		final String json = new JSONObject(params).toString();
		sw.split();
		LOG.info("serialization time:" + sw.getSplitTime());
		final Invocable engine = enginePool.borrowObject();
		sw.split();
		LOG.info("borrow time:" + sw.getSplitTime());
		final ScriptObjectMirror result = (ScriptObjectMirror) engine.invokeFunction("evaluateFormula", "result=(a+b+c+d).*e", json);
		sw.split();
		LOG.info("evaluation time:" + sw.getSplitTime());
		enginePool.returnObject(engine);
		sw.stop();
		LOG.info("return time:" + sw.getTime());
		LOG.info(result.values().size() + " elements, first value=" + result.values().iterator().next());
	}
}
