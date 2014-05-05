package de.schakko.warp.presentation.jsf;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.warp.Activity;
import org.jboss.arquillian.warp.Inspection;
import org.jboss.arquillian.warp.Warp;
import org.jboss.arquillian.warp.WarpTest;
import org.jboss.arquillian.warp.jsf.AfterPhase;
import org.jboss.arquillian.warp.jsf.Phase;
import org.jboss.arquillian.warp.servlet.AfterServlet;
import org.jboss.arquillian.warp.servlet.BeforeServlet;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import de.schakko.warp.business.boundary.UserService;
import de.schakko.warp.business.entity.User;
import de.schakko.warp.helper.test.integration.WarpUtil;
import de.schakko.warp.helper.test.integration.WebArchiveUtil;
import de.schakko.warp.helper.test.mock.ejb.EjbMockerUtil;
import de.schakko.warp.presentation.controller.UserController;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * The {@link Category} annotation marks this tests only be executed if
 * maven-failsafe-plugin runs
 * 
 * @author ckl
 * 
 */
@RunWith(Arquillian.class)
@WarpTest
public class UserControllerIT {
	/**
	 * The method annotated with {@link Deployment} is executed first by
	 * Arquillian and sets up all dependencies which should be deployed to the
	 * target container. Arquillian uses src/test/resources/arquillian.xml to
	 * set up the target application server container.
	 * 
	 * @return
	 * @throws Exception
	 */
	@Deployment(testable = true)
	public static WebArchive createDeployment() throws Exception {
		WebArchive war = WarpUtil.createCustomerBaselineWAR();
		// our controller we want to test
		war.addClass(UserController.class);
		// create mocked EJB facades. We don't want to interact with the real
		// Oracle database which causes a lot of trouble setting up test data
		WebArchiveUtil.addControllableEjbFacade(war, "de.schakko.warp.business.boundary.UserService");
		// add XHTML files and other resources
		WebArchiveUtil.addStaticWebResources(war);

		return war;
	}

	/**
	 * @Drone ensures that Arquillian injects a web driver instance which is
	 *        HtmlUnit by default. I changed this behavior in arquillian.xml to
	 *        use Selenium because we need AJAX functioanlity.
	 */
	@Drone
	WebDriver driver;

	/**
	 * @ArquillianResource injects the URL of the current context
	 */
	@ArquillianResource
	private URL path;

	/**
	 * Every integration test with Arquillian, Graphene and Drone must be
	 * annotated with @RunAsClient.
	 * 
	 * @throws IOException
	 */
	@Test
	@RunAsClient
	public void validateSomething() throws IOException {
		// Arquillian requires running with JDK 7; getLoopbackAdress is not
		// available in JDK 6. I changed the SDK to JavaSE-1.7 for our project.
		Warp.initiate(new Activity() {
			/**
			 * The activity runs on the client side (*this* computer) and is
			 * used for triggering the browser.
			 */
			public void perform() {
				// force the web driver to navigate to our overview.xhtml page
				// we don't have to authenticate because the web.xml doesn't
				// include any filters
				driver.navigate().to(path.toString() + "/index.xhtml");

				// take a screenshot after navigating
				// WarpUtil.saveScreenshot(driver, new File("d:/temp/bla/bla.png"));
				assertNotNull(driver.findElement(By.className("output")).getText());
				assertEquals("A mocked user instance", driver.findElement(By.className("output")).getText());
			}
		}).inspect(new Inspection() {
			/**
			 * This class runs on the server side and has no access to the
			 * driver instance. You can only access EJBs or the JSF context.
			 * 
			 * There is no direct association between the instance in
			 * .initiate(new Activity() ... ) and the .inspect(new
			 * Inspection()...). Warp at first executes the deployment, starts
			 * the inspection with all annotated methods, executes the Activtiy
			 * instance and forces the @After... annotated methods.
			 */
			private static final long serialVersionUID = -6025852353577000667L;

			@EJB(mappedName = "java:module/UserService")
			UserService userService;

			/**
			 * @BeforeServlet will be executed on startup of the servlet. We
			 *                will set up the mocked data to be returned.
			 * @throws Exception
			 */
			@BeforeServlet
			public void Before() throws Exception {
				// does injection work?
				assertNotNull(userService);

				// we will just add a new row inside the overview with name
				// TEST-WARP. This should be seen in the screenshot
				List<User> result = new ArrayList<User>();
				result.add(new User(2, "A mocked user instance"));

				// now the magic. We get the embedded mock inside the
				// UserService. Technically, the UserService
				// is a EJB singleton and contains a mocked instance of itself.
				UserService embeddedServiceMock = EjbMockerUtil.getEmbeddedMock(userService, UserService.class);

				// ... and set up the mocking instance
				when(embeddedServiceMock.findUsers()).thenReturn(result);
			}

			@AfterServlet
			public void After() throws Exception {
			}

			/**
			 * This will be executed after JSF/PrimeFaces has renderered the
			 * response.
			 * 
			 * @throws Exception
			 */
			@AfterPhase(Phase.RENDER_RESPONSE)
			public void afterRender() throws Exception {
				// ensure any mocked value has been set
				assertNotNull(userService.findUsers());
				assertEquals(1, userService.findUsers().size());
				assertEquals(2, userService.findUsers().get(0).getId());
			}
		});
	}

	/**
	 * EJB must be used with mappedName inside the Unit test; @Inject annotation
	 * doesn't work as @EJB annotation without mappedNamed does not work
	 */
	@EJB(mappedName = "java:module/UserService")
	UserService userService;

	/**
	 * This test method would be executed on the current workstation and uses
	 * injected EJBs by Arquillian. I ignored the test because it just exists
	 * for documentation purposes.
	 */
	@Test
	@Ignore
	public void CostcenterViewService_is_mocked_and_returns_mocked_data() throws Exception {
		assertNotNull(userService);

		// retrieve the embedded mock from the EJB facade
		UserService embeddedMock = EjbMockerUtil.getEmbeddedMock(userService, UserService.class);
		assertNotNull(embeddedMock);

		List<User> expected = new ArrayList<User>();
		expected.add(new User(3, "EXPECTED_USER"));

		// embedded mock has the same type as the facade
		assertTrue(embeddedMock instanceof UserService);
		// don't use when(userService)...! You must always operate on
		// the embedded mock
		// when the embedded mock is called, it returns the expected string
		when(embeddedMock.findUsers()).thenReturn(expected);
		// success!
		assertEquals(1, userService.findUsers().size());
		assertEquals(3, userService.findUsers().get(0).getId());
	}
}
