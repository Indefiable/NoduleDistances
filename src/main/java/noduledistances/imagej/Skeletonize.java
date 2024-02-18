package noduledistances.imagej;

import java.util.ArrayList;

import ij.ImagePlus;
import ij.plugin.filter.RankFilters;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import traceskeleton.TraceSkeleton;

public class Skeletonize {
	ImagePlus binarymap;
	ImagePlus skeletonMap;
	
	public Skeletonize() {
		
		System.out.println("I dunno, somethin.");
		
	}
	
	/**
	 * Converts a boolean[] image to a byte image
	 * @param im
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
	

	public static ArrayList<ArrayList<int[]>> skeletonize(ImagePlus imp) {
		
		imp.show();
		
		ArrayList<ArrayList<int[]>> skeleton; 
		
		int width = imp.getWidth();
		int height = imp.getHeight();
		
		boolean[] im = convertToBooleanArray(imp.getProcessor().convertToByteProcessor());
	
		for(int ii = 0; ii < im.length; ii++) {
			im[ii] = !im[ii];
		}
		
		TraceSkeleton.thinningZS(im, width,height);
		
		ByteProcessor byt = new ByteProcessor(width, height, booleanToByte(im));
		
		skeleton = TraceSkeleton.traceSkeleton(im, width, height, 100);
		
		
		return skeleton;
	}

	
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
    

	static void applyMedianBlur(ByteProcessor processor) {
        RankFilters rankFilters = new RankFilters();
        rankFilters.rank(processor, 7, RankFilters.MEDIAN);
    }
    
}//class
