package de.schakko.warp.helper.test.integration;

import java.io.File;

import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import de.schakko.warp.helper.test.mock.ejb.EjbMocker.EjbMockerBuilder;

public class WebArchiveUtil {
	public final static String WEBAPP_SRC = "src/main/webapp";

	/**
	 * Adds the facade/mock combination of given EJB class name as
	 * {@link ByteArrayAsset} to the web archive
	 * 
	 * @param archive
	 * @param clazzName
	 *            *don't* use YourClass.class.getName(); use the complete FQDN
	 *            instead
	 * @return
	 * @throws Exception
	 */
	public static WebArchive addControllableEjbFacade(WebArchive archive, String clazzName) throws Exception {
		archive.add(
				new ByteArrayAsset(EjbMockerBuilder.create(clazzName).suppressExceptions(true)
						.ignoreMethod("getRepository").stream()), "WEB-INF/classes/" + clazzName.replace('.', '/')
						+ ".class");
		return archive;
	}

	/**
	 * Adds all web resources which are *not* inside META-INF or WEB-INF
	 * 
	 * @param archive
	 * @throws Exception
	 */
	public static void addStaticWebResources(WebArchive archive) throws Exception {
		archive.as(ExplodedImporter.class).importDirectory(WEBAPP_SRC, Filters.exclude(".*\\-INF"));
	}

	/**
	 * Saves the content of given WAR to local directory
	 * 
	 * @param war
	 * @param targetDirectory
	 */
	public static void saveWarContent(WebArchive war, File targetDirectory) {
		war.as(ExplodedExporter.class).exportExploded(targetDirectory);
	}

}
