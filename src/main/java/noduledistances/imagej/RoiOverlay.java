package noduledistances.imagej;


import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Overlay;

import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.TextRoi;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatPolygon;
import traceskeleton.TraceSkeleton;
import ij.gui.ShapeRoi;
import ij.gui.NewImage;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.random.HaltonSequenceGenerator;

import kn.uni.voronoitreemap.datastructure.OpenList;
import kn.uni.voronoitreemap.diagram.PowerDiagram;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import kn.uni.voronoitreemap.j2d.Site;


public class RoiOverlay {

	ShapeRoi[] rois;
	
	/**
	 * Extracts the Roi information from the loaded Tif image. 
	 * Returns early if passed image file does not have an Overlay
	 * 
	 * @param imp : image file with attached Overlay. Only TIF files can hold Overlays.
	 */
	public RoiOverlay(ImagePlus imp) {
		
		if(imp.getOverlay() == null) {
			IJ.log("Image does not have overlay. Did you load a Tif file?");
			return;
		}
		
		Overlay overlay = imp.getOverlay();
		
		ShapeRoi[] rois = new ShapeRoi[overlay.size()];
		
		for(int ii = 0; ii < overlay.size(); ii++) {
			rois[ii] = (ij.gui.ShapeRoi) overlay.get(ii);
			//System.out.println(rois[ii].getName());
		}
		
		this.rois = rois;
	}
	
	
	
	public double[] attachmentPoint(ShapeRoi roi, RootGraph graph) {
		
		double[] attachmentPoint = new double[2];
		
		double[] centroid = roi.getContourCentroid();
		
		Point2D.Double pt = new Point2D.Double(centroid[0], centroid[1]);
		
		//ArrayList<int[]> subgraph = graph.ballSubgraph(5, pt);
		ArrayList<ShapeRoi> lines = graph.ballSubgraphLines(7, pt);
		
		//System.out.println("size of lines: " + lines.size());
		//System.out.println("Size of subgraph: " + subgraph.size());
		
		ArrayList<ShapeRoi> intersections = new ArrayList<>();
		
		for(ShapeRoi line : lines) {
			line.and(roi);
			if(line.getContainedPoints().length == 0) {
				continue;
			}
			intersections.add(line);
		}
		
		Point[] bdPoints = getBoundaryPoints(roi, intersections);
		if(bdPoints.length == 0) {
			attachmentPoint[0] = centroid[0];
			attachmentPoint[1] = centroid[1];
		}
		else {
			Point center = averagePoint(bdPoints);
			attachmentPoint[0] = center.x;
			attachmentPoint[1] = center.y;
		}
		/** for looking at the ROI and the points found.
		ArrayList<ShapeRoi> testing = new ArrayList<>();
		
		testing.add(roi);
		
		for(int ii = 0; ii < intersections.size(); ii++) {
			testing.add(intersections.get(ii));
		}
		
		for(int ii = 0; ii < bdPoints.length; ii++) {
	        
	       OvalRoi ball = new OvalRoi( bdPoints[ii].x - 2,  bdPoints[ii].y - 2, 
				2 * 2, 2 * 2);
	       
			testing.add(new ShapeRoi(ball));
		}
		
		
		OvalRoi ball = new OvalRoi( center.x - 3,  center.y - 3, 
				2 * 3, 2 * 3);
	       
		testing.add(new ShapeRoi(ball));
		
		
		showRois(testing.toArray(new ShapeRoi[0]), bdPoints);
		*/
		
		
		return attachmentPoint;
	}
	

	public Point averagePoint(Point[] points) {
		if(points.length == 0) {
			System.out.println("Zero points.");
		}
		double avgX = 0;
		double avgY = 0;
		
		for(Point p : points) {
			avgX += p.x;
			avgY += p.y;
		}
		
		avgX = avgX / points.length;
		avgY = avgY / points.length;
		
		
		return new Point((int) avgX, (int) avgY);
		
	}
	
	
	public Point[] getBoundaryPoints(ShapeRoi roi, ArrayList<ShapeRoi> intersections) {
		
		HashSet<Point> bdPoints = new HashSet<>();
		
		for(ShapeRoi inter : intersections) {
			
			Point[] points = inter.getContainedPoints();
			Point st = points[0];
			Point end = points[points.length-1];
			double[] coords = new double[2];
	        double prevX = 0;
	        double prevY = 0;
	        double currX, currY;
	        
	        PathIterator iter = roi.getPolygon().getPathIterator(null);
	        
			while (!iter.isDone()) {
	            int segmentType = iter.currentSegment(coords);
	            currX = coords[0];
	            currY = coords[1];
	            
	            if(segmentType == PathIterator.SEG_MOVETO) {
	            	// Update previous point
		            prevX = currX;
		            prevY = currY;
		            iter.next();
	            	continue;
	            }
	            

	            if (onBoundary(prevX, prevY, currX, currY, st.x, st.y)) {
	                //System.out.println("Added " + st + " to boundary points.");
	                bdPoints.add(st);
	                break;
	            }
	            
	            if (onBoundary(prevX, prevY, currX, currY, end.x, end.y)) {
	               // System.out.println("Added " + end + " to boundary points.");
	                bdPoints.add(end);
	                break;
	            }
	            
	            // Update previous point
	            prevX = currX;
	            prevY = currY;
	            if(segmentType == PathIterator.SEG_CLOSE) {
	            	break;
	            }
	            // Move to the next segment
	            iter.next();
	        }
		}
		
		return bdPoints.toArray(new Point[0]);
		
	}
	
	
	public static boolean onBoundary(double p1x, double p1y, double p2x, double p2y, double x, double y) {
	    // Calculate the vector from p1 to x
	    double dx = x - p1x;
	    double dy = y - p1y;
	    
	    // Calculate the vector from p1 to p2
	    double vx = p2x - p1x;
	    double vy = p2y - p1y;
	    
	    // Calculate the dot product (vx * dx + vy * dy)
	    double dotProduct = vx * dx + vy * dy;
	    
	    // Calculate the squared magnitude of (p2 - p1)
	    double magnitudeSquared = vx * vx + vy * vy;
	    
	    // Calculate r
	    double r = dotProduct / magnitudeSquared;
	    
	    double dist;
	    
	    if (r < 0) {
	        // Closest point is p1
	        dist = distance(p1x, p1y, x, y);
	    } else if (r > 1) {
	        // Closest point is p2
	        dist = distance(p2x, p2y, x, y);
	    } else {
	        // Closest point is on the line segment
	        double px = p1x + r * vx;
	        double py = p1y + r * vy;
	        dist = distance(px, py, x, y);
	    }
	    
	    return dist < 2;
	}
	    
	 
	    // Method to calculate the distance between two points
	    private static double distance(double px, double py,double x, double y) {
	    	double dx = px - x;
	    	double dy = py - y;
	        return Math.sqrt(dx * dx + dy * dy);
	    }
	    
	    
	// Method to check if a point is on a line segment
    private static boolean isOnSegment(double x1, double y1, double x2, double y2, double x, double y) {
        // Check if the point (x, y) lies on the line segment defined by (x1, y1) and (x2, y2)
        // For simplicity, we'll use a distance-based approach
        double dx = x2 - x1;
        double dy = y2 - y1;
        double distance = Math.abs(dx * (y - y1) - dy * (x - x1)) / Math.sqrt(dx * dx + dy * dy);
        return distance < 1.0; // Adjust the threshold as needed
    }


	
	/**
	 * finds the centroid of all ROI's, and returns them in [color,x,y,area] format. 
	 * For nodules that were initially clumps, we return all of their information in one row. 
	 * color: red==1, green==2, mixed==3 
	 * @return [color,x,y] coordinates of the roi centroids. 
	 */
	public ArrayList<int[]> getRoiCentroids(RootGraph graph){
		
		ArrayList<int[]> centroids = new ArrayList<>();
		
		for(ShapeRoi roi : rois) {
			
			String name = roi.getName();
			
			int numNods = 0;
			
			try {
				numNods = Integer.parseInt(name.substring(2));
			}catch(Exception e) {
				System.out.println(name);
				System.out.println("Could not read ROI name.");
				numNods=1;
			}
			if(numNods>1){
				int[] clumpedRois = getClumpData(roi, numNods, graph);
				centroids.add(clumpedRois);
				continue;
			}
			
			addCentroid(centroids, roi, graph);
			
		}
		
		return centroids;
	}
	
	private void addCentroid(ArrayList<int[]> centroids, ShapeRoi roi,RootGraph graph) {
		int[] coords = new int[4];
		
		String name = roi.getName();
		if(name.substring(0, 1).equalsIgnoreCase("r")) {
			coords[0] =1;
		}
		else if(name.substring(0, 1).equalsIgnoreCase("g")) {
			coords[0] = 2;
		}
		else if(name.substring(0, 1).equalsIgnoreCase("m")) {
			coords[0] = 3;
		}
		
		double[] centroid = attachmentPoint(roi,graph);
		if(centroid[0] < 5 && centroid[1] < 5) {
			System.out.println("corner point.");
		}
		coords[1] = (int) centroid[0];
		coords[2] = (int) centroid[1];
		coords[3] = (int) roi.getContainedPoints().length;
		centroids.add(coords);
		
	}
	
	/**
	 * Computes the halton sequence for a given ROI to create a sample of uniformly 
	 * distributed points within the Roi polygon by intersecting the halton sequence with the polygon.
	 * We use the halton sequence to generate the points instead of random sampling becaues 
	 * the halton sequence produces a very uniform distribution of points.
	 * 
	 * @param roi : Rois that we are creating the points for.
	 * @return List<> of points to cluster.
	 */
	public List<ClumpClusterPoint> getHaltonSequence(ShapeRoi roi) {
		
		Rectangle rect = roi.getBounds();
		
		int sequenceSize = (int) ((rect.width * rect.height)*.15);
		
		List<ClumpClusterPoint> halton = getHaltonVectors(sequenceSize, rect);
		
		ArrayList<ClumpClusterPoint> returnHalton = new ArrayList<>();
		
		for(ClumpClusterPoint point : halton) {
			
			point = new ClumpClusterPoint(point.x + rect.x, point.y + rect.y);
			
			if(roi.getFloatPolygon().contains(point.x, point.y)) {
				returnHalton.add(point);
			
			}
		}
		
		return returnHalton;
	}
	
	
	/**
	 * performs k-means clustering on a given set of points to break nodule clumps into k parts. 
	 * 
	 * @param points point cloud representation of an ROI polygon
	 * @param k number of nodules to break the clump into.
	 * @return List<> of k centroids after performing k-means.
	 */
	public static List<CentroidCluster<ClumpClusterPoint>> kMeansClustering(List<ClumpClusterPoint> points, int k) {
        
		KMeansPlusPlusClusterer<ClumpClusterPoint> clusterer = new KMeansPlusPlusClusterer<>(k);
        
		List<CentroidCluster<ClumpClusterPoint>> clusters = clusterer.cluster(points);
        
		return clusters;
	}
    
	/**
	 * Computes generic halton vectors, and scales them to fill a bounding box. This is what intersects a
	 * polygon ROI to get the uniform point cloud representation of an ROI polygon.
	 * 
	 * @param numberOfPoints : number of halton vectors to generate.
	 * @param boundingBox : bounds of the halton vectors.
	 * @return List<> of halton vectors. Uses ClumpClusterPoint to implement Clusterable interface.
	 */
	public List<ClumpClusterPoint> getHaltonVectors(int numberOfPoints, Rectangle boundingBox) {
		
		int width = boundingBox.width;
		int height = boundingBox.height;
		HaltonSequenceGenerator haltonGenerator = new HaltonSequenceGenerator(2);
		List<ClumpClusterPoint> haltonPoints = new ArrayList<ClumpClusterPoint>();
	    
	        
	    for (int i = 0; i < numberOfPoints; i++) {
	    	double[] vector = haltonGenerator.nextVector();
	    	double x = (vector[0] * width);
	    	double y = (vector[1] * height);
	    	haltonPoints.add(new ClumpClusterPoint(x,y));
	    }
	        
	    return haltonPoints;
	}
	
	/**
	 * Uses k-means clustering to break up the given roi into several pieces. The given roi is assumed to be a
	 * clump of several nodules. 
	 * 
	 * @param roi : roi that we're breaking into parts.
	 * @param numNods : the number of parts we're breaking the roi into{'.
	 * @return centroid and area of each ROI in color,x,y,area format.;
	 */
	public int[] getClumpData(ShapeRoi roi, int numNods,RootGraph graph) {
		
		List<ClumpClusterPoint> halton = getHaltonSequence(roi);
		
		List<CentroidCluster<ClumpClusterPoint>> clusters = kMeansClustering(halton, numNods);
		
		ArrayList<Integer> nodn = new ArrayList<>();
		
		String name = roi.getName();
		
		ArrayList<ShapeRoi> brokenRois = breakupClump(roi, numNods,clusters);
		
		double[] centroid;
		
		for(ShapeRoi brokeRoi : brokenRois) {
			
			if(name.substring(0, 1).equalsIgnoreCase("r")) {
				nodn.add(1);
			}
			else if(name.substring(0, 1).equalsIgnoreCase("g")) {
				nodn.add(2);
			}
			else if(name.substring(0, 1).equalsIgnoreCase("m")) {
				nodn.add(3);
			}
			else {
				System.out.println("ERROR, no type known.");
				
			}
			centroid = attachmentPoint(brokeRoi, graph);
			
			nodn.add((int) centroid[0]);
			nodn.add((int) centroid[1]);
			nodn.add(brokeRoi.getContainedPoints().length);
		}
		
		return nodn.stream().mapToInt(Integer::intValue).toArray();
	}
	
	
	protected ArrayList<ShapeRoi> breakupClump(ShapeRoi roi, int numNods, List<CentroidCluster<ClumpClusterPoint>> clusters) {
		if(clusters.size() != numNods) {
			System.out.println("num clusters not equal to num nods");
			System.out.println("breakpoint.");
		}
		ArrayList<double[]> centers = new ArrayList<>();
		
		ArrayList<ShapeRoi> rois = new ArrayList<>();
		rois.add(roi);
		PolygonRoi converter;
		int[] intx;
		int[] inty;
		// power diagram object
		PowerDiagram diagram = new PowerDiagram();
		
		// custom list object holding the points used to compute power diagrams
		OpenList sites = new OpenList();

		/**
		// convert ShapeRoi into PolygonSimple\/\/
		Point[] points = roi.getContainedPoints();
		double[] x = new double[points.length];
		double[] y = new double[points.length];
		
		
		for( int ii = 0; ii < points.length; ii++) {
			x[ii] = (double) points[ii].x;
			y[ii] = (double) points[ii].y;
		}
		PolygonSimple roiPolygon = new PolygonSimple(x,y,x.length);
		// convert ShapeRoi into PolygonSimple/\/\
		**/

		PolygonSimple roiBoundingBox = new PolygonSimple();
		Rectangle box = roi.getBounds();
		int width = box.width;
		int height = box.height;
		roiBoundingBox.add(0, 0);
		roiBoundingBox.add(width, 0);
		roiBoundingBox.add(width, height);
		roiBoundingBox.add(0, height);
		
		//restrict voronoi diagrams to roiBoundingBox. package wanted the box to have a corner
		//at 0,0, so everything has been shifted.
		diagram.setClipPoly(roiBoundingBox);
		
		//add centroids as ball centers for power diagram
		for(CentroidCluster<ClumpClusterPoint> cluster : clusters) {
			double[] centroid = cluster.getCenter().getPoint();
			centers.add(centroid);
			Site site = new Site(centroid[0]-box.x, centroid[1]-box.y);
			//site.setWeight(30);
			sites.add(site);
		}
		
		
		diagram.setSites(sites);
		
		diagram.computeDiagram();
		
		if(sites.size != numNods) {
			System.out.println("number of sites not equal to number of nodules. Error.");
			System.out.println("Breakpoint.");
		}
		int numNullSites = 0;
		for(int ii = 0; ii < sites.size; ii++) {
			Site site = sites.array[ii];
			PolygonSimple polygon=site.getPolygon();
			if(polygon == null) {
				numNullSites++;
				System.out.println("Null site found");
				continue;
			}
			double[] x = polygon.getXPoints();
			double[] y = polygon.getYPoints();
			intx = doubleToInt(x);
			inty = doubleToInt(y);
			
			intx = trimArray(intx, polygon.length);
			inty = trimArray(inty, polygon.length);
			
			if(x.length != y.length) {
				System.out.println("The number of xpoints is different than the "
						+ "number of y points in this voronoi cell.");
				System.out.println("breakpoint.");
			}
			
			converter = new PolygonRoi(intx, inty, inty.length, Roi.POLYGON);
			
			ShapeRoi newRoi = new ShapeRoi(converter);
			newRoi.setName(roi.getName());
			rois.add(newRoi);
		}
		if(numNullSites !=0) {
			System.out.println("Number of null sites for this ROI: " + numNullSites);
		}
		intersectClumps(rois);
		//showRois(rois, width, height, centers);
		
		rois.remove(0);
		return rois;
	}
	
	
	private void intersectClumps(ArrayList<ShapeRoi> rois) {
		
		ShapeRoi original = rois.get(0);
		int dx = original.getBounds().x;
		int dy = original.getBounds().y;
		
		for(int ii =1 ; ii < rois.size(); ii ++){
			int cx = rois.get(ii).getBounds().x;
			int cy = rois.get(ii).getBounds().y;
			rois.get(ii).setLocation(cx+dx, cy+dy);
		}
		
		for(int ii =1 ; ii < rois.size(); ii ++){
			rois.get(ii).and(original);
			if(rois.get(ii).getContainedPoints().length == 0) {
				System.out.println("Error: Empty intersection.");
			}
		}
		
	}
	 
	
	private int[] doubleToInt(double[] x) {
		int[] y = new int[x.length];
		
		for (int ii = 0; ii < x.length; ii++) {
		y[ii] = (int) Math.round(x[ii]);
		}
	
		return y;
	}
		
	
	//ALL METHODS BELOW THIS LINE ARE FOR TESTING PURPOSES.
	
	private ArrayList<double[]> normalize(ArrayList<double[]> centers, int dx, int dy){
		ArrayList<double[]> nm = new ArrayList<>();
		
		for(double[] center : centers) {
			System.out.println("(" + center[0] + ", " + center[1] + ") - (" + dx + ", " + dy + ")");
			center[0] -= dx;
			center[1] -= dy;
			nm.add(center);
		}
		
		return nm;
	}
	
	public void testPolygonRoi() {
		int[][] vertices = new int[2][4];
		
		
		vertices[0] = new int[] {0,0,100,100};
		vertices[1] = new int[] {0,100,100,0};
		
		PolygonRoi roi = new PolygonRoi(vertices[0], vertices[1], 4, Roi.POLYGON);
		
		ShapeRoi[] rois = new ShapeRoi[1];
		rois[0] = new ShapeRoi(roi);
		showRois(rois, 300,300);
		
		
	}
	/**
	 * This is an edited version of the sample code provided on the github page of the Power Diagram 
	 * package begin utilized. Author : ArlindNocaj
	 */
	public void powerDiagramTestCode(ArrayList<double[]> pointCloud) {
		
		/**
		double[][] pointCloud = new double[3][2];
		pointCloud[0] = new double[] {100,100};
		pointCloud[1] = new double[] {300,100};
		pointCloud[2] = new double[] {200,200};
		*/
		
		ImagePlus imp = NewImage.createImage("Points Image", 500, 500, 1, 8, NewImage.FILL_WHITE);
		Overlay overlay = new Overlay();
		imp.setOverlay(overlay);
		PowerDiagram diagram = new PowerDiagram();

		// normal list based on an array
		OpenList sites = new OpenList();

		Random rand = new Random(100);
		// create a root polygon which limits the voronoi diagram.
		// here it is just a rectangle.

		PolygonSimple rootPolygon = new PolygonSimple();
		int width = 1000;
		int height = 1000;
		rootPolygon.add(0, 0);
		rootPolygon.add(width, 0);
		rootPolygon.add(width, height);
		rootPolygon.add(0, height);
		
		
		// create 100 points (sites) and set random positions in the rectangle defined above.
	/**
		for (int i = 0; i < 100; i++) {
			Site site = new Site(rand.nextInt(width), rand.nextInt(width));
			
			// we could also set a different weighting to some sites
			site.setWeight(30);
			sites.add(site);
		}*/
		
		for(double[] pt : pointCloud) {
			System.out.println("pt to be added.");
			System.out.println(pt[0] + ", " + pt[1]);
			Site site = new Site(pt[0], pt[1]);
			
			site.setWeight(30);
			sites.add(site);
			
			PointRoi pointRoi = new PointRoi(pt[0], pt[1]);
            pointRoi.setFillColor(Color.GREEN);
            pointRoi.setStrokeColor(Color.GREEN);
            imp.getOverlay().add(pointRoi);
            
		}
		
		// set the list of points (sites), necessary for the power diagram
		diagram.setSites(sites);
		// set the clipping polygon, sets boundaries the power voronoi diagram
		diagram.setClipPoly(rootPolygon);

		// do the computation
		diagram.computeDiagram();

		// for each site we can no get the resulting polygon of its cell. 
		// note that the cell can also be empty, in this case there is no polygon for the corresponding site.
		PointRoi.setColor(Color.RED);
		Line.setColor(Color.BLACK);
		int[] intx;
		int[] inty;
		for (int i=0;i<sites.size;i++){
			Site site=sites.array[i];
			PolygonSimple polygon=site.getPolygon();
			System.out.println("polygon length: " + polygon.length);
			
			double[] x = polygon.getXPoints();
			//System.out.println("len of this polygon: "+  x.length);
			double[] y = polygon.getYPoints();
			
			intx = doubleToInt(x);
			inty = doubleToInt(y);
			System.out.println("length of intx:" + intx.length);
			intx = trimArray(intx, polygon.length);
			inty = trimArray(inty,polygon.length);
			System.out.println("length of trimmed intx: " + intx.length);
			System.out.println("Showing the raw x and y values of the vertices of the polygon:");
			System.out.println(Arrays.toString(x));
			System.out.println(Arrays.toString(y));
			
			  for (int ii = 0; ii < intx.length-1; ii++) {
		            PointRoi pointRoi = new PointRoi(intx[ii], inty[ii]);
		            pointRoi.setFillColor(Color.RED);
		            pointRoi.setStrokeColor(Color.RED);
		           
		            System.out.println("creating line: " + intx[ii] + ", " + inty[ii] + " -> " + x[ii+1] + ", " + y[ii+1]);
		            Line line = new Line(intx[ii], inty[ii], intx[ii+1], inty[ii+1]);
		            line.setStrokeColor(Color.BLACK);
		            
		            imp.getOverlay().add(line);
		            imp.getOverlay().add(pointRoi);
		            
		        }
			  
	            Line line = new Line(intx[0], inty[0], intx[intx.length-1], inty[inty.length-1]);
	            line.setStrokeColor(Color.BLACK);
	            imp.getOverlay().add(line);
	            
			 
			 //System.out.println("GOING TO NEW POLYGON.");
		}
		
		
	}
	
	public void testPowerDiagram() {
		
		double[][] pointCloud = new double[3][2];
		pointCloud[0] = new double[] {100,100};
		pointCloud[1] = new double[] {300,100};
		pointCloud[2] = new double[] {200,200};
		
		ArrayList<ShapeRoi> rois = new ArrayList<>();
		
		PolygonRoi converter;
		
		int[] intx;
		int[] inty;
		
		
		PowerDiagram diagram = new PowerDiagram();
		
		// custom list object holding the points used to compute power diagrams
		OpenList sites = new OpenList();
		
		PolygonSimple roiPolygon = new PolygonSimple();
		int width = 1000;
		int height =1000;
		
		roiPolygon.add(0, 0);
		roiPolygon.add(width,0);
		roiPolygon.add(width, height);
		roiPolygon.add(0, height);
		
		for(double[] pt : pointCloud) {
			System.out.println("pt to be added.");
			System.out.println(pt[0] + ", " + pt[1]);
			Site site = new Site(pt[0], pt[1]);
			site.setWeight(30);
			sites.add(site);
		}
		
		diagram.setSites(sites);
		diagram.setClipPoly(roiPolygon);
		
		diagram.computeDiagram();
		
		
		int numNullSites = 0;
		
		for(int ii = 0; ii < sites.size; ii++) {
			Site site = sites.array[ii];
			PolygonSimple polygon=site.getPolygon();
			
			if(polygon == null) {
				numNullSites++;
				continue;
			}
			
			double[] x = polygon.getXPoints();
			double[] y = polygon.getYPoints();
			System.out.println("Showing the raw x and y values of the vertices of the polygon:");
			System.out.println(Arrays.toString(x));
			System.out.println(Arrays.toString(y));
			
			intx = doubleToInt(x);
			inty = doubleToInt(y);
			
			intx = trimArray(intx, polygon.length);
			inty = trimArray(inty, polygon.length);
			
			if(intx.length != inty.length) {
				System.out.println("Error, not the same number of zeros. ");
				System.out.println("breakpoint.");
			}
			
			if(intx.length == 0) {
				System.out.println("Empty polygon. Skipping.");
				continue;
			}
			System.out.println("showing the x and y values of the vertices of the polygon.");
			System.out.println(Arrays.toString(intx));
			System.out.println(Arrays.toString(inty));
			System.out.println("==============================");
			converter = new PolygonRoi(intx, inty, inty.length, Roi.POLYGON);
			converter.getXCoordinates();
			System.out.println("showing the PolygonRoi x and y coordinates.");
			System.out.println(Arrays.toString(converter.getPolygon().xpoints));
			System.out.println(Arrays.toString(converter.getPolygon().ypoints));
			System.out.println("==============================");
			rois.add(new ShapeRoi(converter));
			
		}
		
		System.out.println("num null sites in testing:" + numNullSites);
		
		showRois(rois, width+100,height+100, null);
		
		
	}
	
	/**
	 * removes trailing zeros that result from the Power Diagram output.
	 * @param array
	 * @return
	 */
	protected int[] trimArray(int[] array, int length) {
		ArrayList<Integer> newArray = new ArrayList<>();
		
		for(int ii = 0; ii < length; ii++) {
			newArray.add(array[ii]);
		}
		
		return newArray.stream().mapToInt(Integer::intValue).toArray();
		
		
	}
	
	public void showRois(ShapeRoi[] rois, int width, int height) {
		Overlay overlay = new Overlay();
		ShapeRoi original = rois[0];
		int offsetX = original.getBounds().x;
		int offsetY = original.getBounds().y;
		ImagePlus testing = testingSpace(width, height);
		
		testing.setOverlay(overlay);
		
		for(ShapeRoi roi : rois) {
			if(roi == null) {
				continue;
			}
			int cx = roi.getBounds().x;
			int cy = roi.getBounds().y;
			
		    roi.update(true, false);
		    roi.setLocation(cx-offsetX, cy-offsetY);
		    
		    // Add outlines to the ROI
		    roi.setStrokeColor(Color.BLACK);
		    roi.setStrokeWidth(2);
		   
		    testing.getOverlay().add(roi);
		}
		
		testing.show();
		System.out.println("breakpoint.");
		
	}
	
	public void showRois(ArrayList<ShapeRoi> rois, int width, int height, ArrayList<double[]> centers) {
		
		Overlay overlay = new Overlay();
		ShapeRoi original = rois.get(0);
		System.out.println("num ROIS: " + rois.size());
		//int offsetX = original.getBounds().x - (int) (.2 * original.getBounds().width);
		//int offsetY = original.getBounds().y - (int) (.2 * original.getBounds().height);
		
		ImagePlus testing = testingSpace(width, height);
		
		testing.setOverlay(overlay);
		Color[] colors = generateUniqueColors(rois.size());
		
		for(int ii = 0; ii < rois.size(); ii++) {
			ShapeRoi roi = rois.get(ii);
			if(roi == null) {
				System.out.println("Found null ROI.");
				continue;
			}
			int cx = roi.getBounds().x;
			int cy = roi.getBounds().y;
			
		  //  roi.update(true, false);
		  //  roi.setLocation(cx-offsetX, cy-offsetY);
			
			if(roi.equals(original)) {
				roi.setLocation(0, 0);
			}
		    
		    // Add outlines to the ROI
			
		    roi.setStrokeColor(colors[ii]);
		    roi.setStrokeWidth(ii+2);
		   
		    testing.getOverlay().add(roi);
		}
		if(centers  == null) {
			testing.show();
			System.out.println("breakpoint.");
			return;
		}
		
		for(double[] center : centers) {
			//OvalRoi ball = new OvalRoi( (int) center[0] - 2 - offsetX,  (int) center[1] - 2 - offsetY, 
			//		4, 4);
			OvalRoi ball = new OvalRoi(center[0]-2, center[1]-2, 4,4);
			ball.setFillColor(Color.cyan);
			testing.getOverlay().add(ball);
		}
		
		testing.show();
		System.out.println("breakpoint.");
		
	}
	
	
	public int maximum(int[] x) {
		int min = Integer.MIN_VALUE;
		
		for (int y : x) {
			if (y > min) {
				min = y;
			}
		}
		
		return min;
	}
	
	public void showRois(ShapeRoi[] rois) {
		
		Overlay overlay = new Overlay();
		int[] widths = new int[rois.length];
		int[] heights = new int[rois.length];
		ShapeRoi original = rois[0];
		int offsetX = original.getBounds().x;
		int offsetY = original.getBounds().y;
		Color[] colors = generateUniqueColors(rois.length);
		
		for(int ii = 0; ii < rois.length; ii++) {
			ShapeRoi roi = rois[ii];
			if(roi == null) {
				continue;
			}
			Rectangle rect = roi.getBounds();
			widths[ii] = rect.width;
			heights[ii] = rect.height;
		}
		ImagePlus testing = testingSpace(maximum(widths), maximum(heights));
		
		testing.setOverlay(overlay);
		int ii = 0;
		
		for(ShapeRoi roi : rois) {
			if(roi == null) {
				System.out.println("null Roi.");
				continue;
			}
			
			int cx = roi.getBounds().x;
			int cy = roi.getBounds().y;
		    roi.update(true, false);

		    	
		    // Add outlines to the ROI
		    roi.setStrokeColor(Color.BLACK);
		    roi.setStrokeWidth(2);

		    int newlocx = cx-offsetX;
		    int newlocy = cy-offsetY;
		    System.out.println("Moving to (" + newlocx + ", " + newlocy + ")");
		    roi.setLocation(newlocx, newlocy);
		    roi.setStrokeWidth(2);
		    
		    // Add outlines to the ROI
		    roi.setStrokeColor(colors[ii++]);
		    
		   
		    testing.getOverlay().add(roi);
		}
		
		testing.show();
		System.out.println("breakpoint.");
		
	}

	
	
	
	public void showRois(ShapeRoi[] rois, Point[] bdPoints) {
		
		Overlay overlay = new Overlay();
		int[] widths = new int[rois.length];
		int[] heights = new int[rois.length];
		ShapeRoi original = rois[0];
		int offsetX = original.getBounds().x;
		int offsetY = original.getBounds().y;
		
		Color[] colors = generateUniqueColors(rois.length);
		System.out.println("colors size : " + colors.length);
		for(int ii = 0; ii < rois.length; ii++) {
			ShapeRoi roi = rois[ii];
			if(roi == null) {
				continue;
			}
			Rectangle rect = roi.getBounds();
			widths[ii] = rect.width;
			heights[ii] = rect.height;
		}
		
		ImagePlus testing = testingSpace(maximum(widths), maximum(heights));
		
		testing.setOverlay(overlay);
		int ii = 0;
		
		for(ShapeRoi roi : rois) {
			if(roi == null) {
				System.out.println("null Roi.");
				continue;
			}
			
			int cx = roi.getBounds().x;
			int cy = roi.getBounds().y;
		    roi.update(true, false);
		    int newlocx = cx-offsetX;
		    int newlocy = cy-offsetY;
		    System.out.println("Moving to (" + newlocx + ", " + newlocy + ")");
		    roi.setLocation(newlocx, newlocy);
		    roi.setStrokeWidth(2);
		    
		    // Add outlines to the ROI
		    roi.setStrokeColor(colors[ii++]);
		    
		   
		    testing.getOverlay().add(roi);
		}
		
		
		for(Point p : bdPoints) {
			
		}
		
		testing.show();
		System.out.println("breakpoint.");
		
	}
	
	
	
	
	public void showGraphBreakup(List<CentroidCluster<ClumpClusterPoint>> clusters, ShapeRoi roi, int radius, int numNods, ShapeRoi[] rois) {

		Overlay overlay = new Overlay();
		
		Rectangle rect = roi.getBounds();
		ImagePlus testing = testingSpace((int) (rect.width),(int) (rect.height));
		
		testing.setOverlay(overlay);
		
		Color[] colors = generateUniqueColors(numNods);
		
		
		int counter = 0;
		for(CentroidCluster<ClumpClusterPoint> cluster : clusters) {
			Color color = colors[counter++];
			
			for(ClumpClusterPoint point : cluster.getPoints()) {
				OvalRoi ball = new OvalRoi( point.x -rect.x - radius,  point.y - rect.y - radius, 
						2 * radius, 2 * radius);
				
				ball.setFillColor(color);
				testing.getOverlay().add(ball);
			}
		}
		
		int colorCounter = 0;
		for(ShapeRoi brokeRoi : rois) {
			if(brokeRoi == null) {
				colorCounter++;
				continue;
			}
			brokeRoi.setPosition(0);
			brokeRoi.setLocation(0, 0);
		    brokeRoi.update(true, false);
		    	
		    // Add outlines to the ROI
		    brokeRoi.setStrokeColor(colors[colorCounter++]);
		    brokeRoi.setStrokeWidth(2);
		   
		    testing.getOverlay().add(roi);
		}
		
		
		roi.setPosition(0);
		roi.setLocation(0, 0);
	    roi.update(true, false);
	    	
	    // Add outlines to the ROI
	    roi.setStrokeColor(Color.BLACK);
	    roi.setStrokeWidth(2);
	   
	    testing.getOverlay().add(roi);
		
		testing.show();
		System.out.println("breakpoint");
	}
	
	public void showGraphBreakup(List<CentroidCluster<ClumpClusterPoint>> clusters, ShapeRoi roi, int radius, int numNods) {

		Overlay overlay = new Overlay();
		
		Rectangle rect = roi.getBounds();
		ImagePlus testing = testingSpace((int) (rect.width),(int) (rect.height));
		
		testing.setOverlay(overlay);
		
		Color[] colors = generateUniqueColors(numNods);
		
		
		int counter = 0;
		for(CentroidCluster<ClumpClusterPoint> cluster : clusters) {
			Color color = colors[counter++];
			
			for(ClumpClusterPoint point : cluster.getPoints()) {
				OvalRoi ball = new OvalRoi( point.x -rect.x - radius,  point.y - rect.y - radius, 
						2 * radius, 2 * radius);
				
				ball.setFillColor(color);
				testing.getOverlay().add(ball);
			}
		}
		
		roi.setPosition(0);
		roi.setLocation(0, 0);
	    roi.update(true, false);
	    	
	    // Add outlines to the ROI
	    roi.setStrokeColor(Color.BLACK);
	    roi.setStrokeWidth(2);
	   
	    testing.getOverlay().add(roi);
		
		testing.show();
		System.out.println("breakpoint");
	}
	
	public static Color[] generateUniqueColors(int a) {
        if (a <= 0) {
            throw new IllegalArgumentException("Input integer must be greater than 0");
        }

        Set<Color> colorSet = new HashSet<>();
        Random random = new Random();

        while (colorSet.size() < a) {
            // Generate random RGB values
            int red = random.nextInt(256);
            int green = random.nextInt(256);
            int blue = random.nextInt(256);
            
            // Create a Color object with the generated RGB values
            Color color = new Color(red, green, blue);
            
            // Add the Color object to the set (ensures uniqueness)
            colorSet.add(color);
        }

        // Convert the set to an array
        return colorSet.toArray(new Color[0]);
    }
	
	
	
	public static List<Point2D.Double> generateRandomPoints(List<PolygonRoi> polygons) {
        List<Point2D.Double> points = new ArrayList<>();
        Random random = new Random();

        for (PolygonRoi polygon : polygons) {
            FloatPolygon floatPolygon = polygon.getFloatPolygon();
            
            for (int i = 0; i < floatPolygon.npoints; i++) {
                points.add(new Point2D.Double(floatPolygon.xpoints[i], floatPolygon.ypoints[i]));
            }
            
        }

        return points;
    }
	
	
	
	public void skeletonTesting() {
		
		for(ShapeRoi roi : rois) {
			String name = roi.getName();
			name = name.substring(2);
			int numNods = 0;
			
			try {
				numNods = Integer.parseInt(name);
			}catch(Exception e) {
				System.out.println(name);
				System.out.println("Could not convert to an integer.");
			}
			
			if(numNods < 2) {
				continue;
			}
			
			ImagePlus imp = binaryNoduleOutline(roi);
			ImagePlus imp2 = new ImagePlus("testing.", imp.getProcessor());
			Overlay overlay = new Overlay();
			imp2.setOverlay(overlay);
			
			ImagePlus scaledImage = Skeletonize.scaleImage(imp, 2);
			 
			 ByteProcessor scaledbit = scaledImage.getProcessor().convertToByteProcessor();
			 
		        // Apply median blur multiple times
		        int iterations = 1;
		        for (int i = 0; i < iterations; i++) {
		            Skeletonize.applyMedianBlur(scaledbit);
		        }
		        
		    scaledImage.setProcessor(scaledbit);    
		    
		    imp = Skeletonize.scaleImage(scaledImage, 0.5);
		    
		    boolean[] im = Skeletonize.convertToBooleanArray(imp.getProcessor().convertToByteProcessor());
			
			for(int ii = 0; ii < im.length; ii++) {
				im[ii] = !im[ii];
			}
			
			TraceSkeleton.thinningZS(im, imp.getWidth(),imp.getHeight());
			
			ArrayList<ArrayList<int[]>> skeleton = TraceSkeleton.traceSkeleton(im, imp.getWidth(), imp.getHeight(), 10);
			
			for( ArrayList<int[]> chunk : skeleton) {
				
				for(int ii = 0; ii < chunk.size(); ii+=2) {
					int[] start = chunk.get(ii);
				    int[] end = chunk.get(ii+1);
				    
				    Line line = new Line(start[0], start[1], end[0], end[1]);
					line.setStrokeWidth(4);
					
				    line.setStrokeColor(Color.ORANGE);
				    
					imp2.getOverlay().add(line);
				}
				
			}
			imp2.show();
			
			System.out.println("breakpoint");
			
		}
	}

	
	public ImagePlus binaryNoduleOutline(ShapeRoi roi) {
		
		Rectangle rect = roi.getBounds();
		int dw = (int) (.3 * rect.width);
		int dh = (int) (.3 * rect.height);
		
		int width = (int) (rect.width + dw);
		int height = (int) (rect.height + dh);
		
		ByteProcessor bp = new ByteProcessor(width, height);
		
		bp.setColor(Color.WHITE);
		bp.fill();
		
		roi.setLocation((int) dw/2, (int) dh/2);
		bp.setColor(Color.BLACK);
		bp.fill(roi);
		
		return new ImagePlus("skeleton Testing", bp);
	}
	
	
	
	public ImagePlus testingSpace(int width, int height) {
		ColorProcessor cp = new ColorProcessor(width, height);
		cp.setColor(Color.WHITE);
		cp.fill();
		
		return new ImagePlus("testing Space", cp);
	}
}
