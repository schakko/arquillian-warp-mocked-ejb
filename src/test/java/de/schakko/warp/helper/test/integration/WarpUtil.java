package de.schakko.warp.helper.test.integration;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.warp.impl.server.execution.WarpFilter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import de.schakko.warp.business.entity.User;
import de.schakko.warp.helper.test.mock.ejb.EjbMockerUtil;

/**
 * Utility class for integration testing with Arquillian, Warp, Drone, Graphene
 * and EjbMocker.
 * 
 * @author ckl
 * 
 */
public class WarpUtil {
	/**
	 * Creates the baseline WAR which consists of Mockito, the
	 * {@link EjbMockerUtil}. enabled CDI and JSF and a web.xml with JSF
	 * enabled.
	 * 
	 * @return
	 */
	public static WebArchive createBaselineWar() {
		WebArchive r = ShrinkWrap.create(WebArchive.class)
		// Generic dependencies
		// Utils for getting Mocks and Warp to work
				.addClass(EjbMockerUtil.class)
				// WarpFilter is needed by Warp
				.addClass(WarpFilter.class)
				// Enable CDI
				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
				// Enable JSF
				.addAsWebInfResource(new File("src/main/webapp/WEB-INF/faces-config.xml"))
				// cusomtized web.xml; we don't need tooglz or other influences
				.addAsWebInfResource(new File("src/test/resources/integration/web.xml"))
				// Mockito is needed for mocked EJBs
				.addAsLibraries(
						Maven.resolver().resolve("org.mockito:mockito-all:jar:1.8.5").withTransitivity().asFile());

		return r;
	}

	/**
	 * Creates the baseline WAR which consists of PrimeFaces, Mockito, Joda
	 * Time, all views for the customer application, IDs, Stub for active user,
	 * the {@link EjbMockerUtil}.
	 * 
	 * The baselines enables CDI and JSF and uses a downsized web.xml without
	 * any Togglz dependencies
	 * 
	 * @return
	 */
	public static WebArchive createCustomerBaselineWAR() {
		return createBaselineWar()
				// project specific dependencies
				.addAsLibraries(
						Maven.resolver().resolve("org.primefaces:primefaces:jar:3.5").withTransitivity().asFile())
				// good old JodaTime
				.addAsLibraries(Maven.resolver().resolve("joda-time:joda-time:jar:2.2").withTransitivity().asFile())
				// enable access to the active client user
				.addClass(User.class);
	}

	/**
	 * Saves a screenshot of the current {@link WebDriver} instance
	 * 
	 * @param driver
	 * @param targetFile
	 */
	public static void saveScreenshot(WebDriver driver, File targetFile) {
		File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

		try {
			FileUtils.copyFile(srcFile, targetFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
