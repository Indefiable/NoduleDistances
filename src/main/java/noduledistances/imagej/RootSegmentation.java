package noduledistances.imagej;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import trainableSegmentation.FeatureStackArray;
import trainableSegmentation.unsupervised.ColorClustering;
import trainableSegmentation.unsupervised.ColorClustering.Channel;
import ij.plugin.filter.GaussianBlur;
import ij.gui.EllipseRoi;


import traceskeleton.TraceSkeleton;




public class RootSegmentation {

	private ColorClustering cluster;
	private FeatureStackArray fsa;
	private ImagePlus binarymap;
	public ImagePlus skeletonMap = null;
	
	
	private final int NOISECUTOFF = 1000;
	
/**
 * 
 */
	public RootSegmentation(ColorClustering cluster) {
		this.cluster = cluster;
		ImagePlus image = cluster.getImage();
	
		
		cluster.setNumSamples(image.getWidth() * image.getHeight());
		this.fsa = new FeatureStackArray(image.getStackSize());
		
		fsa = cluster.createFSArray(image);
		
		ImagePlus binarymap = cluster.createProbabilityMaps(fsa); // intensive
		
		ImageStack mapStack = binarymap.getStack();
		
		mapStack.deleteSlice(1);
		mapStack.deleteSlice(2);
		binarymap = new ImagePlus("roots", mapStack.getProcessor(1));
		ByteProcessor pc = binarymap.getProcessor().convertToByteProcessor();
		this.binarymap = new ImagePlus(image.getShortTitle(), pc);
		
		
		clean();
	
		
	}

	/**
	 * initial segmentation of the root system includes a lot of noise. 
	 * We remove this by creating ROI's using an ImageJ plugin that outlines 
	 * all black objects, and remove them based on their size.
	 */
	private void clean() {
		RoiManager manager = new RoiManager();
		manager.setEnabled(true);
		manager.reset();
		
		if (this.binarymap.getProcessor() == null) {
			System.out.println("Error: no map found. Generate a map first.");
			return;
		}
		binarymap.show();
		
		binarymap.getProcessor().setAutoThreshold("Default"); //intensive
		Roi roi = ThresholdToSelection.run(binarymap);        // outline all nodules as one ROI.
		
		manager.add(binarymap,roi,0);  
		
		
		if (manager.getRoisAsArray() == null) {
			System.out.println("No ROI's found");
			return;
		}
		if( manager.getRoi(0) == null) {
			System.out.println("No ROI's found");
			return;
		}
		
		manager.select(0);	 // select all nodules as one ROI
		
		
		if(manager.getRoi(0).getType()== Roi.COMPOSITE) {
			manager.runCommand("split"); 
			int[] temp = {0};			 // selecting roi[0], which is all nodules as one roi.
			manager.setSelectedIndexes(temp);
			manager.runCommand("delete");// deleting that ^^^^
		}
		
		Roi[] tempRois = manager.getRoisAsArray();     // getting all roi's in array
		ShapeRoi[] rois = new ShapeRoi[tempRois.length];
		int ij = 0;
		
		ArrayList<Integer> indices = new ArrayList<>();
		
		for(Roi troi : tempRois) {
			rois[ij] = new ShapeRoi(troi);
			if(rois[ij].getContainedPoints().length < NOISECUTOFF) {
				indices.add(ij);
			}
			ij++;
		}
		
		delete(indices, rois);
		ByteProcessor bit = binarymap.getProcessor().convertToByteProcessor();
		
		binarymap.close();
		
		binarymap.setProcessor(bit);
	}
	
	private void delete(ArrayList<Integer> indices, ShapeRoi[] rois) {
		
		
		ImageProcessor newp = this.binarymap.getProcessor();
		
	//	ByteProcessor newp = new ByteProcessor(this.binarymap.getWidth(),
	//			this.binarymap.getHeight());
		
		
	
		
		for(int index : indices) {
			
			Point[] pt = rois[index].getContainedPoints();
			
			for(Point p : pt) {
				//System.out.println(newp.getValue(p.x, p.y));
				newp.putPixel(p.x, p.y, 255);
			}
			
		}
		
		
		ImagePlus test3 = new ImagePlus("cleaned", newp.convertToByteProcessor());
		
	}
	
	/**
	 * Converts a boolean[] image to a byte image
	 * @param im
	 */
	private byte[] booleanToByte(boolean[] im) {
		byte[] imp = new byte[im.length];
		
		for(int ii = 0; ii < im.length; ii++) {
			
			if(im[ii] == true) {
				imp[ii] = (byte) 255;
			}
			else {
				imp[ii] = (byte) 0;
			}
			
		}
		
		return imp;
	}
	
	
	public ArrayList<ArrayList<int[]>> skeletonize() {
		
		ArrayList<ArrayList<int[]>> skeleton; 
		
		int width = this.binarymap.getWidth();
		int height = this.binarymap.getHeight();
		
		boolean[] im = convertToBooleanArray(this.binarymap.getProcessor().convertToByteProcessor());
	
		for(int ii = 0; ii < im.length; ii++) {
			im[ii] = !im[ii];
		}
		
		
		ByteProcessor byt = new ByteProcessor(width, height, booleanToByte(im));
		
		
		ImagePlus testImp = new ImagePlus("pre-thinned", byt);
		
		testImp.show();
		
		//byt.smooth();
		
		//im = convertToBooleanArray(byt);
		
		
		//testImp = new ImagePlus("blurred", byt);
		
		//testImp.show();
		
		TraceSkeleton.thinningZS(im, width,height);
		
		 byt = new ByteProcessor(width, height, booleanToByte(im));
		
		testImp = new ImagePlus("thinned", byt);
		
		testImp.show();
		
		skeleton = TraceSkeleton.traceSkeleton(im, width, height, 100);
		
		
		return skeleton;
	}

	/**
	 * creates small dots where nodes are in the image. Overrides skeletonMap.
	 * @param nodes: list of nodes.
	 */
	public void overlayGraph(ArrayList<Point> nodes, ArrayList<ArrayList<int[]>> skeleton) {

		ColorProcessor cp = this.binarymap.getProcessor().convertToColorProcessor();
		ImagePlus skellyMap = new ImagePlus("skeleton", cp);

		Overlay overlay = new Overlay();
		skellyMap.setOverlay(overlay);

		for (ArrayList<int[]> chunk : skeleton) {
			
			for(int ii = 0; ii < chunk.size(); ii+=2) {
				
				 	int[] start = chunk.get(ii);
				    int[] end = chunk.get(ii+1);
				    
				    int startX = start[0];
				    int startY = start[1];
				    int endX = end[0];
				    int endY = end[1];

				    Line line = new Line(startX, startY, endX, endY);
				    line.setStrokeWidth(2);
				    line.setStrokeColor(Color.pink);
				    overlay.add(line);
			}
			
		   
		}
		
		this.skeletonMap = skellyMap;
		
		for(Point point : nodes) {
			double radius = 3;
			OvalRoi ball = new OvalRoi( point.x - radius,  point.y - radius, 2 * radius, 2 * radius);
			ball.setFillColor(Color.BLUE);
			skeletonMap.getOverlay().add(ball);
		}
	}
	
	private static boolean[] convertToBooleanArray(ByteProcessor byteProcessor) {
        int width = byteProcessor.getWidth();
        int height = byteProcessor.getHeight();
        byte[] pixels = (byte[]) byteProcessor.getPixels();
        boolean[] result = new boolean[width * height];

        for (int i = 0; i < pixels.length; i++) {
            result[i] = pixels[i] != 0; // Assuming black pixels are represented by 0
        }

        return result;
    }

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}//class
