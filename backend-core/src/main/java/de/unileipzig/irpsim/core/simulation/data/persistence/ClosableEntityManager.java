package de.unileipzig.irpsim.core.simulation.data.persistence;

import javax.persistence.EntityManager;

/**
 * Interface zur Nutzung der try-with-resources Methode, vereint {@link EntityManager} mit {@link AutoCloseable}.
 * 
 * @author krauss
 */
public interface ClosableEntityManager extends EntityManager, AutoCloseable {

	@Override
	void close();
}
