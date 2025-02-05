package noduledistances.imagej;

import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.RankFilters;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import traceskeleton.TraceSkeleton;



/**
 * Class using Ling Dong's skeletonization algorithm. Used for finding a graph representation
 * of the root system. 
 * 
 * @author Brandin Farris
 *
 */
public class Skeletonize {
	ImagePlus binarymap;
	ImagePlus skeletonMap;
	
	public Skeletonize() {
		// Don't need to actually construct an object of this type.
		
	}
	
	/**
	 * Converts a boolean[] image to a byte[] image
	 * 
	 * @param im : image to convert.
	 */
	static byte[] booleanToByte(boolean[] im) {
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
	
	/**
	 * Uses Ling Dong's skeletonization algorithm to find the skeleton of the root system.
	 * @param imp : binary image with the root system segmented.
	 * @return the skeleton of the root system.
	 */
	public static ArrayList<ArrayList<int[]>> skeletonize(ImagePlus imp) {
		
		//imp.show();
		
		ArrayList<ArrayList<int[]>> skeleton; 
		
		int width = imp.getWidth();
		int height = imp.getHeight();
		
		boolean[] im = convertToBooleanArray(imp.getProcessor().convertToByteProcessor());
	
		for(int ii = 0; ii < im.length; ii++) {
			im[ii] = !im[ii];
		}
		TraceSkeleton.thinningZS(im, width,height);
		
		//ByteProcessor byt = new ByteProcessor(width, height, booleanToByte(im));
		
		skeleton = TraceSkeleton.traceSkeleton(im, width, height, 100);
		
		
		return skeleton;
	}

	/**
	 * converts the ByteProcessor image to a boolean[] image using billinear interpolation.
	 * 
	 * @param byteProcessor : image data to convert.
	 * @return boolean[] representation of a binary image.
	 */
	static boolean[] convertToBooleanArray(ByteProcessor byteProcessor) {
        int width = byteProcessor.getWidth();
        int height = byteProcessor.getHeight();
        byte[] pixels = (byte[]) byteProcessor.getPixels();
        boolean[] result = new boolean[width * height];

        for (int i = 0; i < pixels.length; i++) {
            result[i] = pixels[i] != 0; // Assuming black pixels are represented by 0
        }

        return result;
    }
	
	/**
	 * Scales the image according to the scale factor. 
	 * 
	 * @param originalImage : image to be scaled.
	 * @param scaleFactor : factor to scale the image by.
	 * 
	 * @return a scaled version of the originla image.
	 */
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
	
    /**
     * Performs bilinear interpolation to estimate the pixel value at non-integer 
     * coordinates (x, y) in an image.
     *
     * @param processor The {@code ImageProcessor} used to access pixel values.
     * @param x The x-coordinate (can be a fractional value).
     * @param y The y-coordinate (can be a fractional value).
     * @return The interpolated pixel value as an integer.
     * @throws ArrayIndexOutOfBoundsException if the coordinates are out of bounds.
     */ 
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
    
       
    /**
     * blurs the image. Used to make the edges of the image smoother to improve the output of the 
     * skeletonization algorithm.
     * 
     * @param processor : image data to blur. Overrides the image data of the input with the blurred version.
     */
	static void applyMedianBlur(ByteProcessor processor) {
        RankFilters rankFilters = new RankFilters();
        rankFilters.rank(processor, 7, RankFilters.MEDIAN);
    }
    
}//class
