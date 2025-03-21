package noduledistances.imagej;


import org.junit.jupiter.api.Test;


import ij.gui.Line;
import ij.gui.ShapeRoi;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
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

	
	@Test
	public void testBallSubgraphLines() throws Exception{
		RootGraph testSubject = createTestSubject();
		
		Point pt = new Point(15,0);
		
		ArrayList<ShapeRoi> out = testSubject.ballSubgraphLines(4, pt);
		
		assertTrue(out.size() == 2);
		ShapeRoi line1 = new ShapeRoi(new Line(0,0,10,0));
		Boolean contains = false;
		
		for(ShapeRoi shape : out) {
			if(Arrays.equals(shape.getContainedPoints(),line1.getContainedPoints())) {
				contains = true;
			}
		}
		assertTrue(contains);
		contains = false;
		ShapeRoi line2 = new ShapeRoi(new Line(19,0,29,0));
		
		for(ShapeRoi shape : out) {
			
			if(Arrays.equals(shape.getContainedPoints(),line2.getContainedPoints())) {
				contains = true;
			}
		}
		
		assertTrue(contains);
		
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


	//@MethodRef(name = "calculateClosestNode", signature = "(QNode;)QNode;")
	@Test
	public void testCalculateClosestNode() throws Exception {
		RootGraph testSubject = createTestSubject();
		Node p = testSubject.nodes.get(1);
		Node result = testSubject.nodes.get(2);
		
		Method method = RootGraph.class.getDeclaredMethod("calculateClosestNode", Node.class);
	    method.setAccessible(true); // Bypass access restriction
	    Node out = (Node) method.invoke(testSubject, p);
	    
	    assertTrue(out.equals(result));
	    
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
		RootGraph testSubject = createTestSubject();
		ArrayList<int[]> newfsRep = new ArrayList<>();
		newfsRep.add(new int[] {0,1,10});
		newfsRep.add(new int[] {0,3,5});
		newfsRep.add(new int[] {1,0,10});
		newfsRep.add(new int[] {2,3,10});
		newfsRep.add(new int[] {3,2,10});
		
		testSubject.fsRep.add(1, new int[] {0,3,5});
		
		Method method = RootGraph.class.getDeclaredMethod("updatePointer");
	    method.setAccessible(true); // Bypass access restriction
	    method.invoke(testSubject);
	    
	    assertTrue(Arrays.equals(testSubject.pointer, new int[] {0,2,3,4,5}));
	    
	}

	//@MethodRef(name = "removeEdge", signature = "([QNode;)V")
	@Test
	public void testRemoveEdge() throws Exception {
		RootGraph testSubject = createTestSubject();
		Node[] nodeEdge = new Node[] {testSubject.nodes.get(0), 
				testSubject.nodes.get(1)};
	
		testSubject.removeEdge(nodeEdge);
		assertTrue(!testSubject.containsEdge(0, 1));
		nodeEdge = new Node[] {testSubject.nodes.get(1), 
				testSubject.nodes.get(2)};
		testSubject.removeEdge(nodeEdge);
	}
}