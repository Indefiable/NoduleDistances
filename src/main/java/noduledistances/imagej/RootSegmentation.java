package noduledistances.imagej;

import java.io.File;
import java.util.ArrayList;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import ij.IJ;
import ij.ImagePlus;

import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import trainableSegmentation.FeatureStackArray;
import trainableSegmentation.unsupervised.ColorClustering;
import trainableSegmentation.unsupervised.ColorClustering.Channel;

import traceskeleton.TraceSkeleton;




public class RootSegmentation {

	private ColorClustering cluster;
	private FeatureStackArray fsa;
	
	
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
		
		binarymap.show();
		IJ.log("pause");
	}

	
	public void Segment() {
		
	}


	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}//class
