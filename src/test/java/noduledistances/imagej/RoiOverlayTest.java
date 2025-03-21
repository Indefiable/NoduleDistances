package noduledistances.imagej;


import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.ShapeRoi;
import java.awt.Rectangle;
import java.lang.reflect.Method;


//@Generated(value = "org.junit-tools-1.1.0")
public class RoiOverlayTest {
	RootGraph graph;
	
	@BeforeEach
	public void setUp() throws Exception {
		ArrayList<ArrayList<int[]>> skeleton = new ArrayList<>();
		ArrayList<int[]> chunk = new ArrayList<>();
		chunk.add(new int[]{0,0});
		chunk.add(new int[] {10,0});
		chunk.add(new int[] {20,0});
		chunk.add(new int[] {29,0});
		skeleton.add(chunk);
		this.graph = new RootGraph(skeleton, null);
	}

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
	
		
	}

	@AfterEach
	public void tearDown() throws Exception {

	}

	@AfterAll
	public static void tearDownAfterClass() throws Exception {

	}

	private RoiOverlay createTestSubject() {
	
		return new RoiOverlay(new ImagePlus());
	}

	
	
	//@MethodRef(name = "attachmentPoint", signature = "(QShapeRoi;QRootGraph;)[D")
	@Test
	public void testAttachmentPoint() throws Exception {
		RoiOverlay testSubject = createTestSubject();	
		
		double[] result;
		Rectangle rect = new Rectangle(9, 0, 12, 2);  // x, y, width, height
		ShapeRoi roi = new ShapeRoi(rect);
		double[] expected = new double[] {14,0};
		result = testSubject.attachmentPoint(roi, graph);
		//currently off by one. ROI objects are shifted left, i.e
		// line from (0,0) of size ten does not contain (10,0).
		assertTrue(Arrays.equals(expected, result));
	}

	//@MethodRef(name = "getBoundaryPoints", signature = "(QShapeRoi;QArrayList<QShapeRoi;>;)[QPoint;")
	@Test
	public void testGetBoundaryPoints() throws Exception {
		RoiOverlay testSubject = createTestSubject();
		
		Rectangle rect = new Rectangle(9, 0, 12, 2);  // x, y, width, height
		ShapeRoi roi = new ShapeRoi(rect);	
		
		ArrayList<ShapeRoi> lines = new ArrayList<>();
		//Line line = new Line(nodes.get(n1).x,nodes.get(n1).y, nodes.get(n2).x,nodes.get(n2).y);
		lines.add(new ShapeRoi(new Line(0,0,10,0)));
		lines.add(new ShapeRoi(new Line(20,0,29,0)));
		
		Point[] expected = new Point[2];
		expected[0] = new Point(9,0);
		expected[1] = new Point(20,0);

		// default test
		testSubject = createTestSubject();
		Point[] result = testSubject.getBoundaryPoints(roi, lines);
		
		assertTrue(Arrays.equals(result, expected));
	}

	//@MethodRef(name = "onBoundary", signature = "(DDDDDD)Z")
	@Test
	public void testOnBoundary() throws Exception {
		double p1x = 0.0;
		double p1y = 0.0;
		double p2x = 1;
		double p2y = 1;
		double x = 1;
		double y = 0.0;
		

		
		assertTrue(RoiOverlay.onBoundary(p1x, p1y, p2x, p2y, x, y));
		
		
	}


	//@MethodRef(name="intersectClumps", signature="(QArrayList<QShapeRoi;>;)V")
	@Test
	public void testIntersectClumps() throws Exception {
	RoiOverlay testSubject = createTestSubject();
	
	ArrayList<ShapeRoi> rois = new ArrayList<>();
	Method method = RoiOverlay.class.getDeclaredMethod("intersectClumps", ArrayList.class);
	method.setAccessible(true);
	
	Rectangle rect1 = new Rectangle(0, 0, 2, 2);  // x, y, width, height
	ShapeRoi roi1 = new ShapeRoi(rect1);
	Rectangle rect2 = new Rectangle(1, 0, 2, 2);  // x, y, width, height
	ShapeRoi roi2 = new ShapeRoi(rect2);
	
	rois.add(roi1);
	rois.add(roi2);
	
	Rectangle expectedRect = new Rectangle(1,0,1,2);
	ShapeRoi expectedRoi = new ShapeRoi(expectedRect);
	Point[] expectedRoiPoints = expectedRoi.getContainedPoints();
	
	method.invoke(testSubject, rois);
	
	Point[] result = rois.get(1).getContainedPoints();
	assertTrue(Arrays.equals(expectedRoiPoints, result));
	
	
	}
}