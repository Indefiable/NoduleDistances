package noduledistances.imagej;


import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.TextRoi;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatPolygon;
import traceskeleton.TraceSkeleton;
import ij.gui.ShapeRoi;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.random.HaltonSequenceGenerator;


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
			System.out.println(rois[ii].getName());
		}
		
		this.rois = rois;
	}
	
	
	/**
	 * finds the centroid of all ROI's, and returns them in [color,x,y] format.
	 * color: red==1, green==2, mixed==3
	 * @return [color,x,y] coordinates of the roi centroids. 
	 */
	public int[][] getRoiCentroids(){
		
		ArrayList<ArrayList<Integer>> centroids = new ArrayList<>();
		
		ArrayList<Integer> coords;
		
		for(ShapeRoi roi : rois) {
			coords = new ArrayList<>();
			
			String name = roi.getName();
			
			int numNods = 0;
			
			try {
				numNods = Integer.parseInt(name.substring(2));
			}catch(Exception e) {
				System.out.println(name);
				System.out.println("Could not convert to an integer.");
				numNods=1;
			}
			if(numNods>1){
				// if is clump, use k-means to separate into individual points, add those
				// individual points, and go to next roi.
				ArrayList<ArrayList<Integer>> clump = breakupClumps(roi, numNods);
				for(ArrayList<Integer> cords : clump) {
					centroids.add(cords);
				}
				continue;
			}
			
			if(name.substring(0, 1).equalsIgnoreCase("r")) {
				coords.add(1);
			}
			else if(name.substring(0, 1).equalsIgnoreCase("g")) {
				coords.add(2);
			}
			else if(name.substring(0, 1).equalsIgnoreCase("m")) {
				coords.add(3);
			}
			
			double[] centroid = roi.getContourCentroid();
			coords.add((int) centroid[0]);
			coords.add((int) centroid[1]);
			
			centroids.add(coords);
		}
		
		//convert List<List<>> to int[][] and return
		return centroids.stream().map(row -> row.stream().mapToInt(Integer::intValue).toArray()).toArray(int[][]::new);
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
	 * @param numNods : the number of parts we're breaking the roi into.
	 * @return x,y coordinates of the contour centroids of the broken up roi.
	 */
	public ArrayList<ArrayList<Integer>> breakupClumps(ShapeRoi roi, int numNods) {
		
		List<ClumpClusterPoint> halton = getHaltonSequence(roi);
		
		List<CentroidCluster<ClumpClusterPoint>> clusters = kMeansClustering(halton, numNods);
		
		ArrayList<ArrayList<Integer>> nods = new ArrayList<>();
		ArrayList<Integer> nodn;
		String name = roi.getName();
		
		
		for(CentroidCluster<ClumpClusterPoint> cluster : clusters) {
			nodn = new ArrayList<>();
			
			if(name.substring(0, 1).equalsIgnoreCase("r")) {
				nodn.add(1);
			}
			else if(name.substring(0, 1).equalsIgnoreCase("g")) {
				nodn.add(2);
			}
			else if(name.substring(0, 1).equalsIgnoreCase("m")) {
				nodn.add(3);
			}
			double[] coords = cluster.getCenter().getPoint();
			
			nodn.add((int) coords[0]);
			nodn.add((int) coords[1]);
			
			nods.add(nodn);
		}
		
		return nods;
		
	}
	
	
	
	//ALL METHODS BELOW THIS LINE ARE FOR TESTING PURPOSES.
	
	
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
