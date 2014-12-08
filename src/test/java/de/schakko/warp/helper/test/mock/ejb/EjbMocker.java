package de.schakko.warp.helper.test.mock.ejb;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.annotation.Annotation;

/**
 * For testing EJBs with Arquillian, {@link EjbMocker} creates a new class type
 * of an EJB with no-interface view. The behavior of the EJB can be completely
 * controlled by Mockito: every method of the deployed EJB is forwared to an
 * embedded shadow Mockito instance with the same method signature. The EJB acts
 * only as a facade for Mockito.
 * 
 * Define your @Deployment method like <blockquote>
 * 
 * <pre>
 * &#064;RunWith(Arquillian.class)
 * class JsfIntegrationTest {
 * 	public static WebArchive addControllableEjbFacade(WebArchive archive, String clazzName) throws Exception {
 * 		// Adds the facade/mock combination of given EJB class name as
 * 		// {@link ByteArrayAsset} to the web archive
 * 		archive.add(
 * 				new ByteArrayAsset(EjbMockerBuilder.create(clazzName).suppressExceptions(true)
 * 						.ignoreMethod(&quot;getRepository&quot;).stream()), &quot;WEB-INF/classes/&quot; + clazzName.replace('.', '/')
 * 						+ &quot;.class&quot;);
 * 		return archive;
 * 	}
 * 
 * 	&#064;Deployment
 * 	public static WebArchive createDeployment() throws Exception {
 * 		WebArchive r = ShrinkWrap
 * 				.create(WebArchive.class)
 * 				// Mockito is required inside the deployment
 * 				.addAsLibraries(
 * 						Maven.resolver().resolve(&quot;org.mockito:mockito-all:jar:1.8.5&quot;).withTransitivity().asFile())
 * 				.addClass(JsfControllerBeanWhichUsesEjbViewOnly.class)
 * 				.addAsManifestResource(EmptyAsset.INSTANCE, &quot;beans.xml&quot;);
 * 
 * 		addControllableEjbFacade(r, &quot;this.is.my.EjbViewOnly&quot;);
 * 
 * 		return r;
 * 	}
 * 
 * 	// EJB must be used with mappedName; Inject doesn't work
 * 	&#064;EJB(mappedName = &quot;java:module/EjbViewOnly&quot;)
 * 	EjbViewOnly ejbViewOnly;
 * 
 * 	&#064;Test
 * 	public void EJB_behavior_can_be_influend() throws Exception {
 * 		assertNotNull(ejbViewOnly);
 * 
 * 		// retrieve the embedded mock from EJB
 * 		EjbViewOnly embeddedMock = EjbMocker.getEmbeddedMock(ejbViewOnly, EjbViewOnly.class);
 * 		assertNotNull(embeddedMock);
 * 		// embedded mock has the same type as the facade
 * 		assertTrue(embeddedMock instanceof EjbViewOnly);
 * 		when(embeddedMock.someMethod(org.mockito.Matchers.anyLong(1L))).thenReturn(&quot;It works&quot;);
 * 		assertEquals(&quot;It works&quot;, ejbViewOnly.someMethod(1L));
 * 	}
 * }
 * </pre>
 * 
 * </blockquote>
 * 
 * @author Christopher Klein; christopher[dot]klein[at]neos-it[dot]de
 * 
 */
public class EjbMocker {
	private static final Logger log = Logger.getLogger(EjbMocker.class.getName());

	/**
	 * Exclude exceptions from source EJB methods
	 */
	private boolean suppressExceptions = false;

	/**
	 * Use javax.ejb.Singleton and javax.ejb.Startup instead of
	 * javax.ejb.Stateful
	 */
	private boolean useSingletonInsteadOfStateful = true;

	/**
	 * Methods with given name will not be copied from source EJB
	 */
	private List<String> ignoreMethods = new ArrayList<String>();

	/**
	 * Name of source EJB
	 */
	protected String sourceClazz;

	protected ClassPool cp = new ClassPool();

	/**
	 * Simple fluent interface for building new mocked EJBs
	 * 
	 * @author ckl
	 */
	public static class EjbMockerBuilder {
		private EjbMocker instance;

		/**
		 * Creates a new builder instance
		 * 
		 * @param sourceClazz
		 *            name of EJB source class
		 * @return
		 */
		public static EjbMockerBuilder create(String sourceClazz) {
			return new EjbMockerBuilder(sourceClazz);
		}

		public EjbMockerBuilder(String sourceClazz) {
			this.instance = new EjbMocker(sourceClazz);
		}

		/**
		 * Suppress exceptions of source methods
		 * 
		 * @param suppress
		 * @return
		 */
		public EjbMockerBuilder suppressExceptions(boolean suppress) {
			instance.setSuppressExceptions(suppress);
			return this;
		}

		/**
		 * Switches between the use of javax.ejb.Stateful and
		 * javax.ejb.Singleton/javax.ejb.Startup
		 * 
		 * @param useStatefulAnnotation
		 * @return
		 */
		public EjbMockerBuilder useStatefulAnnotation(boolean useStatefulAnnotation) {
			instance.setUseSingletonInsteadOfStateful(!useStatefulAnnotation);
			return this;
		}

		/**
		 * Ignore method with given name; TODO: check method signature for
		 * overloaded messages
		 * 
		 * @param method
		 * @return
		 */
		public EjbMockerBuilder ignoreMethod(String method) {
			instance.getIgnoreMethods().add(method);
			return this;
		}

		/**
		 * Creates the EJB facade
		 * 
		 * @return
		 * @throws Exception
		 */
		public Class<?> create() throws Exception {
			return instance.create();
		}

		/**
		 * Creates the byte stream of the EJB facade
		 * 
		 * @return
		 * @throws Exception
		 */
		public byte[] stream() throws Exception {
			return instance.createCtClass().toBytecode();
		}
	}

	/**
	 * Creates a new mockable EJB facade. You must provide the sourceClazz by
	 * name or it will be loaded by the parent classloader. I didn't implement
	 * further classloader-foo for handling this.
	 * 
	 * @param sourceClazz
	 *            You are *not* allowed to load the given class before it is
	 *            mocked by this class.
	 */
	public EjbMocker(String sourceClazz) {
		this.sourceClazz = sourceClazz;
	}

	/**
	 * Creates a new {@link CtClass} instance.
	 * 
	 * @return
	 * @throws Exception
	 */
	public CtClass createCtClass() throws Exception {
		cp.appendSystemPath();

		log.info("Creating new facade for class " + this.sourceClazz);

		// append class name during creation or we will run into problems
		// (duplicate classes on classpath...)
		CtClass r = cp.makeClass(this.sourceClazz + "Intermediate");
		CtConstructor constructor = CtNewConstructor.defaultConstructor(r);
		r.addConstructor(constructor);

		// the order of building the class content is important. We can not
		// access fields which are not generated yet.
		addMockProviderField(r);
		addEjbAnnotation(r);
		createMethodSignatures(r);
		addEmbeddedMockAccessor(r);
		updateMethodBodiesForDelegatingToEmbeddedMock(r);

		r.setName(this.sourceClazz);

		return r;
	}

	/**
	 * Adds the javax.ejb.Stateful or javax.ejb.Singleton/javax.ejb.Startup
	 * annotations to the given class so we have only one EJB instance at the
	 * same time. Using the Singleton is the safer method as we can have
	 * multiple calls in different sessions.
	 * 
	 * @param clazz
	 * @throws Exception
	 */
	protected void addEjbAnnotation(CtClass clazz) throws Exception {
		log.fine("Adding javax.ejb.Stateful annotation on class level");

		ClassFile cf = clazz.getClassFile();

		if (isUseSingletonInsteadOfStateful()) {
			AnnotationsAttribute clazzAttributes = new AnnotationsAttribute(cf.getConstPool(),
					AnnotationsAttribute.visibleTag);
			
			Annotation singletonAnnotation = new Annotation(clazz.getClassFile().getConstPool(), ClassPool.getDefault()
					.get("javax.ejb.Singleton"));
			Annotation startupAnnotation = new Annotation(clazz.getClassFile().getConstPool(), ClassPool.getDefault()
					.get("javax.ejb.Startup"));

			clazzAttributes.addAnnotation(singletonAnnotation);
			clazzAttributes.addAnnotation(startupAnnotation);
			cf.addAttribute(clazzAttributes);
		} else {
			AnnotationsAttribute statefulAttribute = new AnnotationsAttribute(cf.getConstPool(),
					AnnotationsAttribute.visibleTag);
			Annotation statefulAnnotation = new Annotation(clazz.getClassFile().getConstPool(), ClassPool.getDefault()
					.get("javax.ejb.Stateful"));
			statefulAttribute.addAnnotation(statefulAnnotation);
			cf.addAttribute(statefulAttribute);
		}

		cf.setVersionToJava5();
	}

	/**
	 * Creates a new {@link Class} instance for Arquillian deployment
	 * 
	 * @return
	 * @throws Exception
	 */
	public Class<?> create() throws Exception {
		CtClass newClazz = createCtClass();
		newClazz.defrost();
		//
		// CtClass clazz = cp.get(sourceClazz.getName());
		// clazz.defrost();
		// clazz.detach();

		return newClazz.toClass();
	}

	/**
	 * Add field for "real" mocked instance.
	 * 
	 * @param clazz
	 * @throws Exception
	 */
	protected void addMockProviderField(CtClass clazz) throws Exception {
		log.fine("Adding field " + EjbMockerUtil.TARGET_FIELD_MOCK + " to facade");
		CtField field = new CtField(cp.get(clazz.getName()), EjbMockerUtil.TARGET_FIELD_MOCK, clazz);
		field.setModifiers(Modifier.PUBLIC);
		clazz.addField(field);
	}

	/**
	 * Adds the mocking provider method {@link MockObjectProvider#getMock()} to
	 * the generated implementation.
	 * 
	 * @param clazz
	 * @throws Exception
	 */
	protected void addEmbeddedMockAccessor(CtClass clazz) throws Exception {
		// in the first place I tried to add an interface to the class to easily
		// access the embedded mock.
		// Unfortunately this means the EJB can only be injected by the
		// interface type and not the real EJB type.
		// clazz.addInterface(cp.get(MockObjectProvider.class.getName()));

		log.fine("Adding " + EjbMockerUtil.MOCK_ACCESSOR + "() to facade");

		// must use FQDN for static methods;
		CtMethod mockitoMethod = CtNewMethod.make("public Object " + EjbMockerUtil.MOCK_ACCESSOR + "() { if (this."
				+ EjbMockerUtil.TARGET_FIELD_MOCK + " == null) { this." + EjbMockerUtil.TARGET_FIELD_MOCK + " = ("
				+ clazz.getName() + ")org.mockito.Mockito.mock(" + clazz.getName() + ".class); } return this."
				+ EjbMockerUtil.TARGET_FIELD_MOCK + "; }", clazz);

		// @PostConstruct *should* be working but:
		// https://community.jboss.org/thread/231014?tstart=0 and
		// http://lists.jboss.org/pipermail/jbossas-pull-requests/2013-February/013871.html
		// :-/
		// AnnotationsAttribute attribute = new
		// AnnotationsAttribute(clazz.getClassFile().getConstPool(),
		// AnnotationsAttribute.visibleTag);
		// add PostConstruct so mock will be initiated on EJB startup
		// Annotation postConstructAnnotation = new
		// Annotation(clazz.getClassFile().getConstPool(),
		// ClassPool.getDefault()
		// .get("javax.annotation.PostConstruct"));
		// attribute.addAnnotation(postConstructAnnotation);
		// mockitoMethod.getMethodInfo().addAttribute(attribute);

		clazz.addMethod(mockitoMethod);
	}

	/**
	 * Creates the delegate methods inside the facade. Every EJB/facade method
	 * will be forwarded to the embedded Mockito instance. These methods only
	 * create the method and contain empty method bodies. This is necessary
	 * for preventing method-dependency issues.
	 * 
	 * @param clazz
	 * @throws Exception
	 */
	protected void createMethodSignatures(CtClass clazz) throws Exception {
		CtClass jaSourceClazz = cp.get(this.sourceClazz);

		// only declared methods and no java.lang.Object methods or other
		// inherited methods (no-interface view)
		for (CtMethod sourceMethod : jaSourceClazz.getDeclaredMethods()) {
			log.info("Copying method " + sourceMethod.getName() + sourceMethod.getSignature() + " to facade");
			// final String signature = sourceMethod.getName() +
			// sourceMethod.getSignature();

			if (getIgnoreMethods().contains(sourceMethod.getName())) {
				log.info("Method " + sourceMethod.getName()
						+ " will be ignored and not copied to facade or embedded mock");
				continue;
			}

			StringBuilder sb = new StringBuilder();
			sb.append("{");

			if (sourceMethod.getReturnType() != CtClass.voidType) {
				// build up dummy return values for a valid method body
				sb.append("return ");

				if (sourceMethod.getReturnType().isPrimitive()) {
					if (sourceMethod.getReturnType() == CtClass.booleanType) {
						sb.append("false");
					} else if (sourceMethod.getReturnType() == CtClass.charType) {
						sb.append("'a'");
					} else {
						sb.append("0");
					}
				} else {
					sb.append("null");
				}

				sb.append(";");
			}
			sb.append("}");

			// clone original method from real EJB and set a new method body
			CtMethod newMethod = CtNewMethod.copy(sourceMethod, clazz, null);

			if (isSuppressExceptions()) {
				log.info("removing throws-clause from method " + newMethod.getName());
				newMethod.setExceptionTypes(null);
			}

			newMethod.setBody(sb.toString());
			// don't forget to add the method to our class
			clazz.addMethod(newMethod);
		}
	}

	/**
	 * Updates every facade method to forward the incoming method calls to the
	 * embedded Mockito instance
	 * 
	 * @param clazz
	 * @throws Exception
	 */
	protected void updateMethodBodiesForDelegatingToEmbeddedMock(CtClass clazz) throws Exception {
		for (CtMethod method : clazz.getDeclaredMethods()) {
			// the accesor method must be ignored
			if (method.getName().equals(EjbMockerUtil.MOCK_ACCESSOR)) {
				continue;
			}

			log.fine("Uptdating method body for " + method.getLongName());

			StringBuilder sb = new StringBuilder();

			if (method.getReturnType() != CtClass.voidType) {
				sb.append("return ");
			}

			// the class cast is required, otherwise we don't fulfil the interface
			// specification.
			sb.append("((" + clazz.getName() + ")this." + EjbMockerUtil.MOCK_ACCESSOR + "()).");
			sb.append(method.getName());
			sb.append("(");
			// $$ resolves to: "every method parameter"
			sb.append("$$");
			sb.append(");");

			String methodBody = sb.toString();

			log.finest("Generated method body: " + methodBody);

			// replace empty method body with forwarding body
			method.setBody(methodBody);
		}
	}

	/**
	 * @return the suppressExceptions
	 */
	public boolean isSuppressExceptions() {
		return suppressExceptions;
	}

	/**
	 * Suppresses all exceptions from the methods
	 * 
	 * @param suppressExceptions
	 *            the suppressExceptions to set
	 */
	public void setSuppressExceptions(boolean suppressExceptions) {
		this.suppressExceptions = suppressExceptions;
	}

	public List<String> getIgnoreMethods() {
		return ignoreMethods;
	}

	/**
	 * Given method name will be ignored from source EJB
	 * 
	 * @param ignoreMethods
	 */
	public void setIgnoreMethods(List<String> ignoreMethods) {
		this.ignoreMethods = ignoreMethods;
	}

	/**
	 * @return the useSingletonInsteadOfStateful
	 */
	public boolean isUseSingletonInsteadOfStateful() {
		return useSingletonInsteadOfStateful;
	}

	/**
	 * The facade will be annotated with javax.ejb.Singleton and
	 * javax.ejb.Startup instead of javax.ejb.Stateful; this is the default.
	 * 
	 * @param useSingletonInsteadOfStateful
	 *            the useSingletonInsteadOfStateful to set
	 */
	public void setUseSingletonInsteadOfStateful(boolean useSingletonInsteadOfStateful) {
		this.useSingletonInsteadOfStateful = useSingletonInsteadOfStateful;
	}
}
