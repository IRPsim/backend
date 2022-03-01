package de.unileipzig.irpsim.gams;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


/**
 * Testsuite für die schnelle Testausführung zur grundlegenden Sicherstellung der Funktionalität. Wird für das Testen nach einem Commit benutzt.
 *
 * @author reichelt
 */
@RunWith(Suite.class)
@SuiteClasses({
		TimeseriesTooShortTest.class,
		GAMSOutputConsistencyTest.class,
		IRPoptDirectTest.class
})
public class FastTestSuite {

}
