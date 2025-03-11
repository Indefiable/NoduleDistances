package noduledistances.imagej;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.powermock.reflect.Whitebox;


@Generated(value = "org.junit-tools-1.1.0")
public class RootGraphTest {

	@Before
	public void setUp() throws Exception {

	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

	}

	@After
	public void tearDown() throws Exception {

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {

	}

	private RootGraph createTestSubject() {
		return new RootGraph(null, new GraphOverlay());
	}

	@MethodRef(name = "addEdge", signature = "([QNode;)V")
	@Test
	public void testAddEdge() throws Exception {
		RootGraph testSubject;
		Node[] edge = new Node[] { null };

		// default test
		testSubject = createTestSubject();
		testSubject.addEdge(edge);
	}

	@MethodRef(name = "addMissingEdges", signature = "()V")
	@Test
	public void testAddMissingEdges() throws Exception {
		RootGraph testSubject;

		// default test
		testSubject = createTestSubject();
		Whitebox.invokeMethod(testSubject, "addMissingEdges");
	}

	@MethodRef(name = "calculateClosestNode", signature = "(QNode;)QNode;")
	@Test
	public void testCalculateClosestNode() throws Exception {
		RootGraph testSubject;
		Node p = null;
		Node result;

		// default test
		testSubject = createTestSubject();
		result = Whitebox.invokeMethod(testSubject, "calculateClosestNode", new Object[] { Node.class });
	}

	@MethodRef(name = "containsEdge", signature = "(II)Z")
	@Test
	public void testContainsEdge() throws Exception {
		RootGraph testSubject;
		int a = 0;
		int b = 0;
		boolean result;

		// default test
		testSubject = createTestSubject();
		result = testSubject.containsEdge(a, b);
	}

	@MethodRef(name = "updatePointer", signature = "()V")
	@Test
	public void testUpdatePointer() throws Exception {
		RootGraph testSubject;

		// default test
		testSubject = createTestSubject();
		Whitebox.invokeMethod(testSubject, "updatePointer");
	}

	@MethodRef(name = "removeEdge", signature = "([QNode;)V")
	@Test
	public void testRemoveEdge() throws Exception {
		RootGraph testSubject;
		Node[] nodeEdge = new Node[] { null };

		// default test
		testSubject = createTestSubject();
		testSubject.removeEdge(nodeEdge);
	}
}