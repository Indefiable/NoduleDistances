package noduledistances.imagej;

import java.awt.Point;
import java.util.ArrayList;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import trainableSegmentation.FeatureStackArray;
import trainableSegmentation.unsupervised.ColorClustering;


/**
 * Class for segmenting the roots from the rest of the image using Weka's Color Clustering 
 * segmentation method.
 * 
 * @author Brandin Farris
 *
 */
public class RootSegmentation {


	private FeatureStackArray fsa;
	public ImagePlus binarymap;
	
	public ImagePlus skeletonMap = null;
	
	private final int NOISECUTOFF = 2000;
	
/**
 * performs color clustering segmentation to create a binary map of the root system.
 * 
 * @param cluster : clustering object that holds the model file used for segmentation.
 * 
 * It's one of the biggest bottlenecks of the program. I'm using Weka's color clustering
 * for this so I'm not sure what optimization I can do.
 */
	public RootSegmentation(ColorClustering cluster, ShapeRoi[] rois) {
		
		
		ImagePlus image = cluster.getImage();
		
		cluster.setNumSamples((int) (image.getWidth() * image.getHeight() * .02));
		this.fsa = new FeatureStackArray(image.getStackSize());
		
		fsa = cluster.createFSArray(image);
		
		ImagePlus binarymap = cluster.createProbabilityMaps(fsa); // intensive
		
		ImageStack mapStack = binarymap.getStack();
		
		binarymap = new ImagePlus("roots", mapStack.getProcessor(3));
		
		
		ByteProcessor pc = binarymap.getProcessor().convertToByteProcessor();
		this.binarymap = new ImagePlus(image.getShortTitle(), pc);
		
		
		clean();
		
	}

	/**
	 * initial segmentation of the root system includes a lot of noise. 
	 * We remove this by creating ROI's using an ImageJ plugin that outlines 
	 * all black objects, and remove them based on their size (in general, the 
	 * noise is small black shapes).
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
		
		 ImagePlus scaledImage = Skeletonize.scaleImage(binarymap, 2);
		 
		 ByteProcessor scaledbit = scaledImage.getProcessor().convertToByteProcessor();
		 
	        
	        int iterations = 1;
	        for (int i = 0; i < iterations; i++) {
	            Skeletonize.applyMedianBlur(scaledbit);
	        }
	        
	    scaledImage.setProcessor(scaledbit);    
	    
	    ImagePlus result = Skeletonize.scaleImage(scaledImage, 0.5);
	   
	    this.binarymap = result;
	    
	    manager.reset();
	    manager.close();
	}
		
	
	/**
	 * It replaces the given roi array with an roi array without the given indices and
	 * redraws the binary map by using the remaining rois.
	 * 
	 * @param indices indices to remove
	 * @param rois roi array to update
	 */
	private void delete(ArrayList<Integer> indices, ShapeRoi[] rois) {
		
		ImageProcessor newp = this.binarymap.getProcessor();
		
	
		for(int index : indices) {
			
			Point[] pt = rois[index].getContainedPoints();
			
			for(Point p : pt) {
				
				newp.putPixel(p.x, p.y, 255);
			}
			
		}	
	}


	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}//class
