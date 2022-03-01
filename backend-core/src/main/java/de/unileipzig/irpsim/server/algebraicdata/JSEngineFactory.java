package de.unileipzig.irpsim.server.algebraicdata;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.standingdata.DataLoader;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

public class JSEngineFactory extends BasePooledObjectFactory<Invocable> {
	static final Logger LOG = LogManager.getLogger(JSEngineFactory.class);

	@Override
	public Invocable create() throws Exception {
		ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine();
		LOG.debug("ScriptEngine created: {}", engine);

		try (Reader rdr = new InputStreamReader(DataLoader.class.getClassLoader().getResourceAsStream("algebraic.js"))) {
			final StopWatch sw = new StopWatch();
			sw.start();
			engine.eval(rdr);
			sw.stop();
			LOG.debug("Creating a new javascript engine for formulas in {} msec", sw.getTime());
		} catch (ScriptException | IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return (Invocable) engine;
	}

	@Override
	public PooledObject<Invocable> wrap(Invocable obj) {
		return new DefaultPooledObject<>(obj);
	}

}
