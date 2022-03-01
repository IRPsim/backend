package de.unileipzig.irpsim.core.simulation.data.persistence;

import org.hibernate.dialect.MySQLMyISAMDialect;

/**
 * Lösung eines Bugs: https://hibernate.atlassian.net/browse/HHH-5988
 *
 * @author reichelt
 */
public class MySQLMyISAMDialectCorrected extends MySQLMyISAMDialect {
	@Override
	public final String getTableTypeString() {
		return " engine=MyISAM";
	}
}
