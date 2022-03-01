package de.unileipzig.irpsim.core.simulation.data.persistence;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.persistence.EntityManager;

import org.hibernate.Session;

import de.unileipzig.irpsim.core.data.timeseries.DatabaseConnectionHandler;

/**
 * Implementierung des {@link InvocationHandler}, nutzt {@link ClosableEntityManager} um {@link EntityManager} mit {@link AutoCloseable} zu vereinigen.
 * 
 * @author krauss
 */
public final class ClosableEntityManagerProxy implements InvocationHandler {

	private EntityManager manager;

	/**
	 * Getter.
	 * 
	 * @return {@link EntityManager}
	 */
	public EntityManager getManager() {
		return manager;
	}

	/**
	 * Privater Konstruktor.
	 */
	private ClosableEntityManagerProxy() {
		manager = DatabaseConnectionHandler.getInstance().getEntityManager();
	}

	/**
	 * Erstellt das eigentliche Proxy des {@link EntityManager}.
	 * 
	 * @return Proxy des {@link EntityManager}.
	 */
	public static ClosableEntityManager newInstance() {
		return (ClosableEntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(), new Class[] { ClosableEntityManager.class },
				new ClosableEntityManagerProxy());
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		if (method.getName().startsWith("close")) {
			Session session = manager.unwrap(Session.class);
			if (manager.isOpen() && session.isOpen()) {
				manager.close();
			}
			return true;
		} else {
			return method.invoke(manager, args);
		}
	}

}
