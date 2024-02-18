package noduledistances.imagej;


import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.TextRoi;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import traceskeleton.TraceSkeleton;
import ij.gui.ShapeRoi;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;

import org.apache.commons.math3.random.HaltonSequenceGenerator;



public class RoiOverlay {

	ShapeRoi[] rois;
	
	public RoiOverlay(ImagePlus imp) {
		
		if(imp.getOverlay() == null) {
			System.out.println("Image does not have overlay. Did you load a Tif file?");
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
	 * @return
	 */
	public int[][] getRoiCentroids(){
		int[][] centroids = new int[rois.length][3];
		int ij = 0;
		
		for(ShapeRoi roi : rois) {
			String name = roi.getName();
			
			if(name.substring(0, 1).equalsIgnoreCase("r")) {
				centroids[ij][0] = 1;
			}
			else if(name.substring(0, 1).equalsIgnoreCase("g")) {
				centroids[ij][0] = 2;
			}
			else if(name.substring(0, 1).equalsIgnoreCase("m")) {
				centroids[ij][0] = 3;
			}
			
			double[] centroid = roi.getContourCentroid();
			
			centroids[ij][1] = (int) centroid[0];
			centroids[ij++][2] = (int) centroid[1];
		}
		
		
		return centroids;
	}
	
	
	
	
	//ALL METHODS BELOW THIS LINE ARE FOR TESTING PURPOSES.
	
	
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
	
	
	/**
	 * Applies a polygonal subdivision algorithm for
	 * all ROI's that contain more than one nodule.
	 */
	public void haltonSequenceTesting() {
		int radius = 5;
		
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
			
			Overlay overlay = new Overlay();
			
			Rectangle rect = roi.getBounds();
			
			ImagePlus testing = testingSpace((int) (rect.width),(int) (rect.height));
			
			testing.setOverlay(overlay);
			
			double[][] halton = getHaltonVectors(3*numNods, rect);
			
			for(double[] point : halton) {
				
				OvalRoi ball = new OvalRoi( point[0] - radius,  point[1] - radius,
						2 * radius, 2 * radius);
				ball.setFillColor(Color.GREEN);
				
				testing.getOverlay().add(ball);
				
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
	}

	
	public double[][] getHaltonVectors(int numberOfPoints, Rectangle boundingBox) {
		int width = boundingBox.width;
		int height = boundingBox.height;
		HaltonSequenceGenerator haltonGenerator = new HaltonSequenceGenerator(2);
	    double[][] haltonPoints = new double[numberOfPoints][2];
	        
	    for (int i = 0; i < numberOfPoints; i++) {
	    	haltonPoints[i] = haltonGenerator.nextVector();
	    	haltonPoints[i][0] *= width;
	    	haltonPoints[i][1] *= height;
	    }
	        
	    return haltonPoints;
	}
	
	
	
	public ImagePlus testingSpace(int width, int height) {
		ColorProcessor cp = new ColorProcessor(width, height);
		cp.setColor(Color.WHITE);
		cp.fill();
		
		return new ImagePlus("testing Space", cp);
	}
}
