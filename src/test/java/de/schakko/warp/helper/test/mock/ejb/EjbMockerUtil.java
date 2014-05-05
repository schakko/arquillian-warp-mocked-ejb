package de.schakko.warp.helper.test.mock.ejb;

import java.lang.reflect.Method;

public class EjbMockerUtil {
	/**
	 * Name of field in the enriched EJB which contains the embedded mocked
	 * instance
	 */
	public final static String TARGET_FIELD_MOCK = "__mock__";

	/**
	 * Name of method to access the mocked interface. Every access to
	 * {@value #MOCK_ACCESSOR} ensures that the mocked instance is initialized.
	 */
	public final static String MOCK_ACCESSOR = "__getMock__";

	/**
	 * Returns the embedded Mockito instance from the facade. This mehod is
	 * needed because we can not work with interface methods.
	 * 
	 * @param anyMockedEjb
	 *            the EJB which has been enriched
	 * @param clazz
	 *            class type
	 * @return
	 * @throws Exception
	 *             should only occur if anyMockedEjb has not been enriched by us
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getEmbeddedMock(Object anyMockedEjb, Class<T> clazz) throws Exception {
		assert anyMockedEjb != null;

		Object embeddedMock;

		try {
			Method getMock = anyMockedEjb.getClass().getMethod(MOCK_ACCESSOR);
			embeddedMock = getMock.invoke(anyMockedEjb);
		} catch (Exception e) {
			throw new Exception("Unable to invoke " + MOCK_ACCESSOR + "() on " + anyMockedEjb
					+ ". Has the object been enriched?", e);
		}

		return (T) embeddedMock;
	}
}
