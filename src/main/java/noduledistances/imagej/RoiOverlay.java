package noduledistances.imagej;


import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;

import ij.gui.ShapeRoi;



import java.awt.Point;
import java.awt.Rectangle;

import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import java.util.HashSet;
import java.util.List;



import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.random.HaltonSequenceGenerator;

import kn.uni.voronoitreemap.datastructure.OpenList;
import kn.uni.voronoitreemap.diagram.PowerDiagram;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import kn.uni.voronoitreemap.j2d.Site;


/**
 * This class object is used to extract the nodule information from
 * the Nodule Segmentation plugin and incoporate that data
 * with the rest of this plugin. <br>
 * 
 * It extracts the Roi data from the Tif image which stores the pixel location, area,
 * and name of each nodule. It uses this data to add the nodules as nodes to the RootGraph.
 * 
 * @author Brandin Farris
 *
 */
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
			
		}
		
		this.rois = rois;
	}
	
	
	/**
	 * Computes what we will use as the node for the given nodule. 
	 * Does this in two different ways:
	 * First it will try the following procedure to find an ideal attachmnet point:<br>
	 * 1. Find all edges intersecting the nodule<br>
	 * 2. find the list of points that is those lines intersecting with the boundary 
	 * of the roi <br>
	 * 3. compute the average of those points.<br>
	 * 
	 * If no such points can be found, the method defaults to computing
	 * the contour centroid of the roi.<br><br>
	 * 
	 *All versions of the plugin prior to 2/4/25 contain an error causing the first
	 *method to always return empty, so only the contour centroid was used.
	 * 
	 * @param roi :nodule to find the node location(attachment point) of.
	 * @param graph graph object.
	 * @return : [x,y] location of where the nodule node will go.
	 */
	public double[] attachmentPoint(ShapeRoi roi, RootGraph graph) {
		
		double[] attachmentPoint = new double[2];
		
		double[] centroid = roi.getContourCentroid();
		
		Point2D.Double pt = new Point2D.Double(centroid[0], centroid[1]);
		
	
		ArrayList<ShapeRoi> lines = graph.ballSubgraphLines(7, pt);
		
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
		
		
		return attachmentPoint;
	}
	
	
/**
 * Computes the average(or centroid) of the given array of points. i.e. it computes
 * the mean of the X values and the mean of the Y values, and returns those as a point.
 * 
 * @param points : points to compute the centroid of
 * 
 * @return : a Point consisting of [meanX, meanY]
 */
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
	
	/**
	 *
	 * For each line in intersections, it records the points of where that line
	 * intersects the given roi, if any. Returns all such points in a Point[] object.
	 * 
	 * @param roi : Roi whose boundary is intersected with all lines in intersections.
	 * @param intersections : ArrayList of lines stored as ShapeRoi's to intersect 
	 * with the given roi.
	 * 
	 * @return : Returns an array of points that are where the boundary of the roi 
	 * meet with any of the lines in intersections.
	 */
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
	                bdPoints.add(st);
	                break;
	            }
	            
	            if (onBoundary(prevX, prevY, currX, currY, end.x, end.y)) {
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
	
	 /**
	 * ChatGPT was used in making this method.<br>
	 * 
     * Determines whether a given point (x, y) is within a small threshold distance 
     * from a line segment defined by the points (p1x, p1y) and (p2x, p2y).
     *
     * The method calculates the perpendicular distance from the point to the line 
     * segment and checks if it is less than 2 units.
     *
     * @param p1x  The x-coordinate of the first endpoint of the line segment.
     * @param p1y  The y-coordinate of the first endpoint of the line segment.
     * @param p2x  The x-coordinate of the second endpoint of the line segment.
     * @param p2y  The y-coordinate of the second endpoint of the line segment.
     * @param x    The x-coordinate of the point to check.
     * @param y    The y-coordinate of the point to check.
     * @return     {@code true} if the point is within 2 units of the line segment, 
     *             {@code false} otherwise.
     */
	public static boolean onBoundary(double p1x, double p1y, double p2x, double p2y, double x, double y) {
	   
	    double dx = x - p1x;
	    double dy = y - p1y;
	    
	    double vx = p2x - p1x;
	    double vy = p2y - p1y;
	    
	   
	    double dotProduct = vx * dx + vy * dy;
	    
	    double magnitudeSquared = vx * vx + vy * vy;
	    
	  
	    double r = dotProduct / magnitudeSquared;
	    
	    double dist;
	    
	    if (r < 0) {
	       
	        dist = distance(p1x, p1y, x, y);
	    } else if (r > 1) {
	       
	        dist = distance(p2x, p2y, x, y);
	    } else {
	       
	        double px = p1x + r * vx;
	        double py = p1y + r * vy;
	        dist = distance(px, py, x, y);
	    }
	    
	    return dist < 2;
	}
	    
	 
    /**
     * 
     * @return : the euclidean distance between two points
     */
    private static double distance(double px, double py,double x, double y) {
	    	double dx = px - x;
	    	double dy = py - y;
	        return Math.sqrt(dx * dx + dy * dy);
	    }
	    
	
	/**
	 * finds the centroid of all ROI's, and returns them in [color,x,y,area] format. 
	 * For nodules that were initially clumps, we return all of their information in one row. 
	 * color: red==1, green==2, mixed==3 
	 * 
	 * @return [color,x,y,area] coordinates of the roi centroids. 
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
	
	
	
	/**
	 * For the given nodule roi, it computes the x,y coordinate that should be used
	 * to represent the roi as a node in the RootGraph. 
	 * 
	 * @param centroids : list of data where each entry in the ArrayList is nodule 
	 * data of the form [x,y,area].
	 * 
	 * @param roi : roi to find the node x,y coordinates for.
	 * @param graph : graph of the root system.
	 */
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
	
	/**
	 * Uses the computed clusters to breakup the given roi into an arrayList of individual Rois.
	 * Returns the separated Roi objects.
	 * 
	 * 
	 * @param roi : nodule Roi that is found to be a nodule clump and is being separated.
	 * @param numNods : number of nodules within the given nodule Roi.
	 * @param clusters : computed clusters within the given nodule Roi.
	 * @return : numNods shapeRois that togehter make up the original roi.
	 */
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
		
		PowerDiagram diagram = new PowerDiagram();
		
		// custom list object holding the points used to compute power diagrams
		OpenList sites = new OpenList();

	

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
		
		
		rois.remove(0);
		return rois;
	}
	
	 /**
     * Computes the intersections of a list of ShapeRoi objects with 
     * the first ShapeRoi in the list.<br>
     *
     * The method first adjusts the positions of all ShapeRoi objects relative to the first one.
     * Then, it performs an intersection operation (`and`) between each subsequent ShapeRoi 
     * and the first ShapeRoi. If any intersection results in an empty shape, an error message is printed.
     *
     * @param rois An ArrayList of ShapeRoi objects. The first ShapeRoi serves as the reference 
     *             for alignment and intersection.
     */
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
	 
/** Converts the list of doubles to a list of integers
 * 
 * @param x : array of doubles.
 * */	
	private int[] doubleToInt(double[] x) {
		int[] y = new int[x.length];
		
		for (int ii = 0; ii < x.length; ii++) {
		y[ii] = (int) Math.round(x[ii]);
		}
	
		return y;
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
	

}//class
