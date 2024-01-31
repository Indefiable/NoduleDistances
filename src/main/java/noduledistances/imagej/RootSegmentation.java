package noduledistances.imagej;

import java.awt.Point;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.filter.RankFilters;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import traceskeleton.TraceSkeleton;
import trainableSegmentation.FeatureStackArray;
import trainableSegmentation.unsupervised.ColorClustering;




public class RootSegmentation {

	private ColorClustering cluster;
	private FeatureStackArray fsa;
	public ImagePlus binarymap;
	
	public ImagePlus skeletonMap = null;
	
	private final int NOISECUTOFF = 2000;
	
/**
 * 
 */
	public RootSegmentation(ColorClustering cluster) {
		this.cluster = cluster;
		ImagePlus image = cluster.getImage();
	
		cluster.setNumSamples((int) (image.getWidth() * image.getHeight() * .04));
		this.fsa = new FeatureStackArray(image.getStackSize());
		
		fsa = cluster.createFSArray(image);
		
		ImagePlus binarymap = cluster.createProbabilityMaps(fsa); // intensive
		
		ImageStack mapStack = binarymap.getStack();
		
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
		
		 ImagePlus scaledImage = scaleImage(binarymap, 2);
		 
		 ByteProcessor scaledbit = scaledImage.getProcessor().convertToByteProcessor();
		 
	        // Apply median blur multiple times
	        int iterations = 1;
	        for (int i = 0; i < iterations; i++) {
	            applyMedianBlur(scaledbit);
	        }
	        
	    scaledImage.setProcessor(scaledbit);    
	    
	    ImagePlus result = scaleImage(scaledImage, 0.5);
	    
	    this.binarymap = result;
	    
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
		
		TraceSkeleton.thinningZS(im, width,height);
		
		ByteProcessor byt = new ByteProcessor(width, height, booleanToByte(im));
		 
		this.skeletonMap = new ImagePlus("Skeleton", byt);
		
		skeleton = TraceSkeleton.traceSkeleton(im, width, height, 100);
		
		
		return skeleton;
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

	private static void applyMedianBlur(ByteProcessor processor) {
        RankFilters rankFilters = new RankFilters();
        rankFilters.rank(processor, 7, RankFilters.MEDIAN);
    }
	
	/**
    private static ImagePlus scaleImage(ImagePlus imp, double scaleFactor) {
        
    	int width = (int) (imp.getWidth() * scaleFactor);
        int height = (int) (imp.getHeight() * scaleFactor);

        ImagePlus scaledImage = new ImagePlus("scaled Image", imp.getProcessor().convertToByteProcessor());
        IJ.run(scaledImage, "Scale...", "x=" + scaleFactor + " y=" + scaleFactor + " interpolation=Bilinear");

        return scaledImage;
    }*/
	
    public static ImagePlus scaleImage(ImagePlus originalImage, double scaleFactor) {
        // Get the ImageProcessor from the original image
        ImageProcessor originalProcessor = originalImage.getProcessor();

        // Get the dimensions of the original image
        int originalWidth = originalProcessor.getWidth();
        int originalHeight = originalProcessor.getHeight();

        // Calculate the new dimensions after scaling
        int newWidth = (int) (originalWidth * scaleFactor);
        int newHeight = (int) (originalHeight * scaleFactor);

        // Create a new ImageProcessor for the scaled image
        ImageProcessor scaledProcessor = originalProcessor.createProcessor(newWidth, newHeight);

        // Scale the image using bilinear interpolation
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                // Calculate the corresponding position in the original image
                double originalX = x / scaleFactor;
                double originalY = y / scaleFactor;

                // Use bilinear interpolation to get the pixel value
                int value = bilinearInterpolation(originalProcessor, originalX, originalY);
                
                // Set the pixel value in the scaled image
                scaledProcessor.putPixel(x, y, value);
            }
        }

        // Create a new ImagePlus object for the scaled image
        ImagePlus scaledImage = new ImagePlus("Scaled Image", scaledProcessor);

        // Optionally, convert the image to 8-bit if needed
        ImageConverter converter = new ImageConverter(scaledImage);
        converter.convertToGray8();

        return scaledImage;
    }
	
    private static int bilinearInterpolation(ImageProcessor processor, double x, double y) {
        int x1 = (int) x;
        int y1 = (int) y;
        int x2 = x1 + 1;
        int y2 = y1 + 1;

        // Get pixel values of the four surrounding pixels
        int q11 = processor.getPixel(x1, y1);
        int q21 = processor.getPixel(x2, y1);
        int q12 = processor.getPixel(x1, y2);
        int q22 = processor.getPixel(x2, y2);

        // Bilinear interpolation formula
        double value = (1 - (x - x1)) * (1 - (y - y1)) * q11 +
                       (x - x1) * (1 - (y - y1)) * q21 +
                       (1 - (x - x1)) * (y - y1) * q12 +
                       (x - x1) * (y - y1) * q22;

        return (int) value;
    }
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}//class
