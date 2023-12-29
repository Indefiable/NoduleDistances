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
import ij.gui.Overlay;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import trainableSegmentation.FeatureStackArray;
import trainableSegmentation.unsupervised.ColorClustering;
import trainableSegmentation.unsupervised.ColorClustering.Channel;


import traceskeleton.TraceSkeleton;




public class RootSegmentation {

	private ColorClustering cluster;
	private FeatureStackArray fsa;
	private ImagePlus binarymap;
	
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
		
		binarymap.getProcessor().setAutoThreshold("Default"); //intensive
		Roi roi = ThresholdToSelection.run(binarymap);        // outline all nodules as one ROI.
		
		binarymap.show();
		
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
	
	public void segment() {
		
		
		ArrayList<ArrayList<int[]>> skeleton; 
		
		int width = this.binarymap.getWidth();
		int height = this.binarymap.getHeight();
		
		boolean[] im = convertToBooleanArray(this.binarymap.getProcessor().convertToByteProcessor());
	
		TraceSkeleton.thinningZS(im, width,height);
		
		skeleton = TraceSkeleton.traceSkeleton(im, width, height, width * height);
		
		ColorProcessor cp = this.binarymap.getProcessor().convertToColorProcessor();
		ImagePlus test = new ImagePlus("polylined", cp);
		
		 Overlay overlay = new Overlay();
		test.setOverlay(overlay);
		 
		for(ArrayList<int[]> polyline : skeleton) {
			
			int[] x = polyline.get(0);
			int[] y = polyline.get(1);
			System.out.println("( " + x[0] + ", " + y[0] + ") "
				       	  + "-> ( " + x[1]  + ", " + y[1] + ")");
			
			Line line = new Line(x[0], y[0], x[1], y[1]);
			line.setStrokeWidth(2); // Set the line width
	        line.setStrokeColor(Color.black); // Set the line color (red in RGB format)
	        overlay.add(line);
		}
		
		
		test.show();
		
		IJ.log("breakpoint");
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
