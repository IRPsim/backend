package de.unileipzig.irpsim.gams;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.core.utils.PersistenceFolderUtil;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.modelstart.GAMSModelStarter;
import de.unileipzig.irpsim.server.modelstart.ModelStarter;

public class GAMSParameterizerIT {

	@Before
	public void init() throws IOException, SQLException {
		FileUtils.copyFileToDirectory(new File("src/main/scripts/backend-gams-0.2-SNAPSHOT.jar"), new File("target"));
		DatabaseTestUtils.setupDbConnectionHandler();

		TestFiles.TEST_PERSISTENCE_FOLDER.mkdirs();
		PersistenceFolderUtil.setPersistenceFolder(TestFiles.TEST_PERSISTENCE_FOLDER);
	}

	@Test
	public void testParametrisation() throws IOException, InterruptedException {
		final OptimisationJobPersistent job = new OptimisationJobPersistent();
		final String jsonParameter = DatabaseTestUtils.getParameterText(TestFiles.DAYS_3.make());
		job.setJsonParameter(jsonParameter);

		try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			em.getTransaction().begin();
			em.persist(job);
			em.getTransaction().commit();
		}
		// job.set

		final File tempDir = PersistenceFolderUtil.getRunningJobFolder();
		final ModelStarter caller = new GAMSModelStarter(job.getId(), 0, 1, 1);
		caller.parameterize();

		final File gdxResultFile = new File(tempDir, "job_" + job.getId() + File.separator + "gamsirpsim.gdx");
		// Assert.assertThat(gdxResultFile,
		System.out.println(gdxResultFile.getAbsolutePath());
		Assert.assertTrue(gdxResultFile.exists());
	}
}
