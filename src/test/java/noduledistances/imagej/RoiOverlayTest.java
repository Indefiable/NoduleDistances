package noduledistances.imagej;

import static org.junit.Assert.*;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.junit.*;
import org.junit.jupiter.api.Test;

import groovy.transform.Generated;
import ij.ImagePlus;
import ij.gui.ShapeRoi;

@Generated(value = "org.junit-tools-1.1.0")
public class RoiOverlayTest {

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

	private RoiOverlay createTestSubject() {
		return new RoiOverlay(new ImagePlus());
	}

	@MethodRef(name = "attachmentPoint", signature = "(QShapeRoi;QRootGraph;)[D")
	@Test
	public void testAttachmentPoint() throws Exception {
		RoiOverlay testSubject;
		ShapeRoi roi = null;
		RootGraph graph = null;
		double[] result;

		// default test
		testSubject = createTestSubject();
		result = testSubject.attachmentPoint(roi, graph);
	}

	@MethodRef(name = "getBoundaryPoints", signature = "(QShapeRoi;QArrayList<QShapeRoi;>;)[QPoint;")
	@Test
	public void testGetBoundaryPoints() throws Exception {
		RoiOverlay testSubject;
		ShapeRoi roi = null;
		ArrayList<ShapeRoi> intersections = null;
		Point[] result;

		// default test
		testSubject = createTestSubject();
		result = testSubject.getBoundaryPoints(roi, intersections);
	}

	@MethodRef(name = "onBoundary", signature = "(DDDDDD)Z")
	@Test
	public void testOnBoundary() throws Exception {
		double p1x = 0.0;
		double p1y = 0.0;
		double p2x = 0.0;
		double p2y = 0.0;
		double x = 0.0;
		double y = 0.0;
		boolean result;

		// default test
		result = RoiOverlay.onBoundary(p1x, p1y, p2x, p2y, x, y);
	}

	@MethodRef(name = "getHaltonSequence", signature = "(QShapeRoi;)QList<QClumpClusterPoint;>;")
	@Test
	public void testGetHaltonSequence() throws Exception {
		RoiOverlay testSubject;
		ShapeRoi roi = null;
		List<ClumpClusterPoint> result;

		// default test
		testSubject = createTestSubject();
		result = testSubject.getHaltonSequence(roi);
	}

	@MethodRef(name="intersectClumps", signature="(QArrayList<QShapeRoi;>;)V")
	@Test
	public void testIntersectClumps() throws Exception {
	RoiOverlay testSubject;ArrayList<ShapeRoi> rois = null;
	
	
	// default test
	testSubject=createTestSubject();Whitebox.invokeMethod(testSubject,"intersectClumps", new Object[]{ArrayList<ShapeRoi>.class});
	}
}