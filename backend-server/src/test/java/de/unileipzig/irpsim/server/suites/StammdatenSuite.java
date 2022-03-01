package de.unileipzig.irpsim.server.suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.unileipzig.irpsim.server.data.stammdaten.TestStammdaten;
import de.unileipzig.irpsim.server.data.stammdaten.TestStammdatenParent;
import de.unileipzig.irpsim.server.data.stammdaten.TestStammdatumAlgebraicdataQuery;
import de.unileipzig.irpsim.server.data.stammdaten.TestStammdatumDelete;
import de.unileipzig.irpsim.server.data.stammdaten.TestStammdatumImport;
import de.unileipzig.irpsim.server.data.stammdaten.TestWrongJSON;
import de.unileipzig.irpsim.server.data.stammdaten.datensatz.AlgebraicDataTest;

@RunWith(Suite.class)
@SuiteClasses({
		TestWrongJSON.class,
		TestStammdatumImport.class,
		TestStammdaten.class,
		TestStammdatenParent.class,
		TestStammdatumAlgebraicdataQuery.class,
		TestStammdatumDelete.class,
		AlgebraicDataTest.class
})
public class StammdatenSuite {

}
