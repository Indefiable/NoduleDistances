package noduledistances.imagej;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;


//@Generated(value = "org.junit-tools-1.1.0")
public class RootGraphTest {
	
	/**
	 *  assertEquals(expected, actual);  // Checks if expected == actual
    	assertTrue(actual > 40);         // Checks if condition is true
    	assertFalse(actual < 40);        // Checks if condition is false
    	assertNotNull(actual);           // Checks if actual is not null
    	assertNull(nullValue);           // Checks if value is null
    	assertSame(expectedObj, actualObj); // Checks if both references point to the same object
    	assertNotSame(obj1, obj2);       // Checks if both references are different
	 *
	 */
	
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
		ArrayList<ArrayList<int[]>> skeleton = new ArrayList<>();
		ArrayList<int[]> chunk = new ArrayList<>();
		chunk.add(new int[]{0,0});
		chunk.add(new int[] {10,0});
		chunk.add(new int[] {19,0});
		chunk.add(new int[] {29,0});
		skeleton.add(chunk);
	return	new RootGraph(skeleton, new GraphOverlay());
	/**
		 * Can test the following methods with this graph:
		 * addEdge()
		 * addMissingEdges()
		 * calculateClosestNode()
		 * containsEdge()
		 * testRemoveEdge()
		 * 
		 * NOT
		 * testUpdatePointer()
		 * 
		 */
	}

	//@MethodRef(name = "addEdge", signature = "([QNode;)V")
	@Test
	public void testAddEdge() throws Exception {
		RootGraph testSubject;
		
		// default test
		testSubject = createTestSubject();
		Node n1 = testSubject.nodes.get(1);
		Node n2 = testSubject.nodes.get(2);
		Node[] edge = new Node[] {n1, n2};	
		testSubject.addEdge(edge);
		int[] fsRepEdge = new int[] {1,2,9};
		boolean contains = false;
		
		for(int[] edg : testSubject.fsRep) {
			if(Arrays.equals(edg, fsRepEdge)) {
					contains = true;
			}
		}
		
		assertTrue(contains);
		
	}

	
	//@MethodRef(name = "addMissingEdges", signature = "()V")
	@Test
	public void testAddMissingEdges() throws Exception {
		RootGraph testSubject = createTestSubject();
		Method method = RootGraph.class.getDeclaredMethod("addMissingEdges");
	    method.setAccessible(true); // Bypass access restriction
	    method.invoke(testSubject);
		assertTrue(testSubject.containsEdge(1, 2));
		
	}
	

	//@MethodRef(name = "calculateClosestNode", signature = "(QNode;)QNode;")
	@Test
	public void testCalculateClosestNode() throws Exception {
		RootGraph testSubject;
		Node p = null;
		Node result;

		// default test
		testSubject = createTestSubject();
		result = Whitebox.invokeMethod(testSubject, "calculateClosestNode", new Object[] { Node.class });
	}

	//@MethodRef(name = "containsEdge", signature = "(II)Z")
	@Test
	public void testContainsEdge() throws Exception {
		RootGraph testSubject = createTestSubject();	
		assertTrue(testSubject.containsEdge(0, 1));
		assertTrue(!testSubject.containsEdge(1, 2));
	}

	//@MethodRef(name = "updatePointer", signature = "()V")
	@Test
	public void testUpdatePointer() throws Exception {
		RootGraph testSubject;

		// default test
		testSubject = createTestSubject();
		Whitebox.invokeMethod(testSubject, "updatePointer");
	}

	//@MethodRef(name = "removeEdge", signature = "([QNode;)V")
	@Test
	public void testRemoveEdge() throws Exception {
		RootGraph testSubject;
		Node[] nodeEdge = new Node[] { null };

		// default test
		testSubject = createTestSubject();
		testSubject.removeEdge(nodeEdge);
	}
}