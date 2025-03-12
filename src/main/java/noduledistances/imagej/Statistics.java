package noduledistances.imagej;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.StringJoiner;

import com.opencsv.exceptions.CsvValidationException;


import java.io.FileWriter;


/**
 * A class that generates all of the statistical data for analysis, and saves it as
 * one csv file.
 * 
 * 
 * Data we want for any given node:
 * ✅ distance to closest red nodule
 * ✅ distance to closest green nodule
 * ✅ mean distance to all red nodules
 * ✅ mean distance to all green nodules
 * ✅ whether closest nodule is red or green
 * ✅ # of red nodules within a given distance
 * ✅ # of green nodules within a given distance
 * 
 * 
 * As I code methods to compute this data, I can't help but feel there's a much more 
 * efficient way to go about this where I don't create a separate method iterating
 * through paths  for each data point desired. Rather than iterating through this paths list for
 *  each data desired, we itereate through the paths, and call methods that compute that specific data, 
 *  that way we only loop through ptahs once per nodule. Will have to refactor to do this. 
 * @author Farris
 */


public class Statistics {
	/**
	 * Represents the name of the image the program is currently doing analysis on. 
	 * Ripped from the user-inputted image file.
	 */
	String imageName;
	/**
	 * String red represents the user-defined attribute associated to red nodules of this image if one was given.
	 */
	String red;
	/**
	 * String red represents the user-defined attribute associated to green nodules of this image if one was given.
	 */
	String green;
	private static final int NUMNODSINBALL = 0;
	private static final int CLOSESTDISTANCE = 1;
	private static final int CLOSESTCOLORTYPE = 2;
	private static final int MEANDISTANCE = 3;
	
	private static final int CLOSESTDISTANCETORED = 4;
	private static final int CLOSESTDISTANCETOGREEN = 5;
	private static final int MEANDISTANCETORED = 6;
	private static final int MEANDISTANCETOGREEN = 7;
	private static final int NUMREDNODSINBALL = 8;
	private static final int NUMGREENNODSINBALL = 9;
	
	
	/**
	 * Initializer method for the Statistics class.
	 * 
	 * This method preps the program for computing the desired statistics.
	 * 
	 *
	 * @param imageName
	 * @param redAttribute: optional user input representing an attribute of the red nodules in the image.
	 * @param greenAttribute: optional user input representing an attribute of the green nodules in the image.
	 * @throws CsvValidationException
	 */
	public Statistics(String imageName, String redAttribute, String greenAttribute) throws CsvValidationException {
		this.imageName = imageName;	
		System.out.println("Image name: " + imageName);
		this.red = redAttribute;
		this.green = greenAttribute;
		
	}

	

	/**
	 * Populates the String[][] data with computed statistics relevant to distance data. 
	 * @param graph
	 */
	public void generateData(RootGraph graph, int[] radii, String saveFile) {
		ArrayList<Integer> options = new ArrayList<>();
		options.add(NUMNODSINBALL);
		options.add(CLOSESTDISTANCE);
		options.add(CLOSESTCOLORTYPE);
		options.add(MEANDISTANCE);
		options.add(NUMREDNODSINBALL);
		options.add(NUMGREENNODSINBALL);
		options.add(CLOSESTDISTANCETORED);
		options.add(CLOSESTDISTANCETOGREEN);
		options.add(MEANDISTANCETORED);
		options.add(MEANDISTANCETOGREEN);
	
		ArrayList<String> header = new ArrayList<>();
		
		header.add("Plant name");
        header.add("Roi");
        header.add("Area");
        header.add("Color");
        header.add("Nod.Strain");
        for(int radius : radii) {
        	header.add("numNods in r = " + radius);
            header.add("num Red " + red + " Nods in r = " + radius);
            header.add("num Green " + green + " Nods in r = " + radius);
        }
        header.add("distance to closest nodule");
        header.add("distance to closest Red " + red + " nodule");
        header.add("distance to closest Green " + green + " nodule");
        header.add("closest color type");
        header.add("mean distance");
        header.add("mean distance to Red " + red + " nodules");
        header.add("mean distance to Green " + green + " nodules");	
        
        
        
		String[][] mat = new String[graph.numNodules+1][header.size()];
		
	    mat[0] = header.toArray(new String[0]);
		HashMap<Integer,double[]> data = null;
		int matCounter = 0;
		if(graph.numNodules != graph.nodes.size()) {
			System.out.println(graph.nodes.size());
			System.out.println();
		}
		for (int ii = 0; ii < graph.nodes.size(); ii++) {
			Node nodule = graph.nodes.get(ii);
			
			
			//ignore skeleton nodes.
			if(nodule.type == Node.SKELETON) {
				continue;
			}
			
			data = computeStatistics(radii, nodule, graph, options);
			if(data == null) {
				System.out.println("Breakpoint.");
			}
			mat[matCounter+1][1] = Double.toString(nodule.nodeNumber);
			mat[matCounter+1][2] = Integer.toString(nodule.area);
			
			if(nodule.type == Node.RED) {
				mat[matCounter+1][3] = "Red";
			}
			else if(nodule.type == Node.GREEN) {
				mat[matCounter+1][3] = "Green";
			}
			else if(nodule.type == Node.MIXED) {
				mat[matCounter+1][3] = "Mixed";
			}
			else {
				System.out.println("Unknown node type.");
				System.out.println("breakpoint.");
			}
			
			if(nodule.type == Node.RED) {
				mat[matCounter+1][4] = "Red " + this.red;
		
			}
			else if(nodule.type == Node.GREEN) {
				mat[matCounter+1][4] = "Green " + this.green;
			}
			else if(nodule.type == Node.MIXED) {
				mat[matCounter+1][4] = "Mixed";
			}
			else {
				mat[matCounter+1][4] = "Unknown";
			}
			int kk = -1;
			int counter  =0;
			for(int jj = 5; jj < 3 *radii.length + 5; jj+=0 ) {
				mat[matCounter+1][jj++] = Double.toString(data.get(NUMNODSINBALL)[counter]);
				mat[matCounter+1][jj++] = Double.toString(data.get(NUMREDNODSINBALL)[counter]);
				mat[matCounter+1][jj++] = Double.toString(data.get(NUMGREENNODSINBALL)[counter++]);
				if(jj >= ((3 * radii.length) + 5)) {
					kk = jj;
				}
			}
			
			if(data.get(CLOSESTDISTANCE)[0] == Integer.MAX_VALUE) {
				mat[matCounter+1][kk++] = "No Path";
			}
			else {
				mat[matCounter+1][kk++] = Double.toString(data.get(CLOSESTDISTANCE)[0]);
			}
			
			if(data.get(CLOSESTDISTANCETORED)[0] == Integer.MAX_VALUE) {
				mat[matCounter+1][kk++] = "No Path";
			}
			else {
				mat[matCounter+1][kk++] = Double.toString(data.get(CLOSESTDISTANCETORED)[0]);
			}
			if(data.get(CLOSESTDISTANCETOGREEN)[0] == Integer.MAX_VALUE) {
				mat[matCounter+1][kk++] = "No Path";
			}
			else {
				mat[matCounter+1][kk++] = Double.toString(data.get(CLOSESTDISTANCETOGREEN)[0]);
			}
			
			int closestColor = (int) data.get(CLOSESTCOLORTYPE)[0];
					
			if(closestColor== Node.RED) {
				mat[matCounter+1][kk++] = "Red";
			}
			else if(closestColor == Node.GREEN) {
				mat[matCounter+1][kk++] = "Green";
			}
			else if(closestColor == Node.MIXED) {
				mat[matCounter+1][kk++] = "Mixed";
			}
			else {
				System.out.println("Unknown node type.");
				System.out.println("breakpoint.");
				mat[matCounter+1][kk++] = "unknown";
			}
			if(data.get(MEANDISTANCE)[0] == 0) {
				mat[matCounter+1][kk++] = "N/A";
			}
			else {
				mat[matCounter+1][kk++] = Double.toString(data.get(MEANDISTANCE)[0]);
			}
			
			if(data.get(MEANDISTANCETORED)[0] == 0) {
				mat[matCounter+1][kk++] = "N/A";
			}
			else {
				mat[matCounter+1][kk++] = Double.toString(data.get(MEANDISTANCETORED)[0]);
			}
			if(data.get(MEANDISTANCETOGREEN)[0] == 0) {
				mat[matCounter+1][kk++] = "N/A";
			}
			else {
				mat[matCounter+1][kk++] = Double.toString(data.get(MEANDISTANCETOGREEN)[0]);
			}
			
			matCounter++;
		}
		
		
		String save = saveFile + "\\" + this.imageName + "_data.csv";
	
		try(FileWriter writer = new FileWriter(save)){
	     		StringJoiner comma = new StringJoiner(",");
	     		for ( String[] row : mat) {
	     			comma.setEmptyValue("");
	     			comma.add(String.join(",", row));
	     			writer.write(comma.toString());
	     			writer.write(System.lineSeparator());
	     			comma = new StringJoiner(",");
	     		}
	     		
	     		writer.flush();
	     		writer.close();
	     		System.out.println("=================");
	     		System.out.println("CSV FILE SAVED.");
	     		System.out.println("=================");
	     		
	     	}catch(IOException e) {
	     		System.out.println("=============================");
	     		System.err.println("Error writing CSV file: " + e.getMessage());
	     		System.out.println("============================");
	     	}
		
		
	}
	
	

	
	
	/**
	 * Saves the pair-wise distance matrices as csv files. 
	 * the _i at the end of csv names is the i'th set of shortest paths 
	 * (i.e. the _1 is the absolute shortest paths)
	 */
	public void savePairwiseDistanceMatrices(RootGraph graph, String saveFile, int numIters) {
		String[][] distances;
		for (int ii = 0; ii < numIters; ii++) {
			distances = distances(graph, ii, numIters);
			String save = saveFile + "\\" + this.imageName + "_path_data_" + ii + ".csv";
			
			try(FileWriter writer = new FileWriter(save)){
	     		StringJoiner comma = new StringJoiner(",");
	     		for ( String[] row : distances) {
	     			comma.setEmptyValue("");
	     			comma.add(String.join(",", row));
	     			writer.write(comma.toString());
	     			writer.write(System.lineSeparator());
	     			comma = new StringJoiner(",");
	     		}
	     		
	     		writer.flush();
	     		writer.close();
	     		System.out.println("=================");
	     		System.out.println("CSV FILE SAVED.");
	     		System.out.println("=================");
	     		
	     	}catch(IOException e) {
	     		System.out.println("=============================");
	     		System.err.println("Error writing CSV file: " + e.getMessage());
	     		System.out.println("============================");
	     	}
		}
		
	}
	
	/**
	 * stores the pair-wise distances between all nodules at the (iter)th iteration of shortest paths.
	 * i.e. if iter = 2, will return all of the pair-wise 2nd shortest paths as a 2d array.
	 * @param graph : graph of the root system.
	 * @param iter : curren iteration (iter'th shortest paths)
	 * @param numIters : number of shortest paths we're computing.
	 * @return : a 2d array of pair-wise distances
	 */
	private String[][] distances(RootGraph graph, int iter, int numIters){
		String[][] distances = new String[graph.numNodules][graph.numNodules];
		
		for (int ii = 0; ii < graph.numNodules; ii++) {
			Node node = graph.getNodules()[ii];
			
			for (int jj = 0; jj < graph.numNodules; jj++) {
				if(ii == jj) {
					distances[ii][jj] = Integer.toString(0);
					continue;
				}
				Node node2 = graph.getNodules()[jj];
				ArrayList<int[]> paths = node.getPaths(graph.nodes.indexOf(node2));
				if (paths == null || paths.size() == 0) {
					System.out.println("No paths between " + node.nodeNumber + " and " + graph.getNodules()[jj].nodeNumber);
					continue;
				}
				else if(paths.size() < iter+1) {
					System.out.println("There are not " + numIters + " paths between " + node.nodeNumber + " and " + graph.getNodules()[jj].nodeNumber);
					continue;
				}
				distances[ii][jj] =Integer.toString( paths.get(iter)[1] );
			}
			
		}
		
		return distances;
	}
	
	 /**
	  * Computes the number of nodules within a given radius of a given color. Note that 
	  * our ball here is not a literal ball, as we use the distances along the root systems
	  * to compute whether a node falls within the ball, i.e. distance along root system &lt; radius. 
	 *
	 * @param radii : array of radii we'll search around.
	 * @param node : node we're searching around (center)
	 * @param graph : graph object
	 * @param options : list telling us what statistics to compute
	 * @return
	 */
	protected static HashMap<Integer,double[]> computeStatistics(int[] radii, Node node, RootGraph graph, ArrayList<Integer> options) {
		
		HashMap<Integer, double[]> map = new HashMap<>();
		
		int closestDistance = Integer.MAX_VALUE; 
		int closestColorDistance = Integer.MAX_VALUE;
		int closestDistanceToRed = Integer.MAX_VALUE;
		int closestDistanceToGreen = Integer.MAX_VALUE;
		
		
		int closestColorType = -1;
		double[] numNodsInBall = new double[radii.length];
		double[] numRedNodsInBall = new double[radii.length];
		double[] numGreenNodsInBall = new double[radii.length];
		Arrays.fill(numNodsInBall, 0);
	    Arrays.fill(numRedNodsInBall, 0);
	    Arrays.fill(numGreenNodsInBall, 0);
		
		double meanDistance = 0;
		int meanDistanceCounter = 0;
		double meanDistanceToRed = 0;
		int redMeanDistanceCounter = 0;
		double meanDistanceToGreen = 0;
		int greenMeanDistanceCounter = 0;
		
		ArrayList<ArrayList<int[]>> paths = node.paths;
		
		for(int ii = 0; ii < paths.size(); ii++) {
			ArrayList<int[]> SPToNodeii = paths.get(ii);
			if(SPToNodeii == null){
				continue;
			}
			else if(SPToNodeii.size() == 0) {
				continue;
			}
			// iterating through every computed path to node ii. First path is shortest path.
			for(int jj = 0; jj < SPToNodeii.size(); jj++) {
			    int[] path = SPToNodeii.get(jj);
			    
			   if(options.contains(NUMNODSINBALL)) {
				    // if distance is smaller and node is correct color.
				   for(int kk = 0; kk < radii.length; kk++) {
					   if(path[1] < radii[kk]) {
						   numNodsInBall[kk]++;
					   }
				   }
			   }
			   if(options.contains(NUMREDNODSINBALL)) {
				   
				   for(int kk = 0; kk < radii.length; kk++) {
					   if(path[1] < radii[kk] && graph.nodes.get(path[0]).type == Node.RED) {
						   numRedNodsInBall[kk]++;
					   }
				   }
			   }
			   if(options.contains(NUMGREENNODSINBALL)) {
				   
				   for(int kk = 0; kk < radii.length; kk++) {
					   if(path[1] < radii[kk]&& graph.nodes.get(path[0]).type == Node.GREEN) {
						   numGreenNodsInBall[kk]++;
					   }
				   }
				  
			   }
			   if(options.contains(CLOSESTDISTANCE)) {
				// if distance is smaller and node is correct color.
				    if(path[1] < closestDistance) {
				    	closestDistance = path[1];
				    }
			   }
			   if(options.contains(CLOSESTDISTANCETORED)) {
				   if(path[1] < closestDistanceToRed && graph.nodes.get(path[0]).type == Node.RED) {
				    	closestDistanceToRed = path[1];
				    }
			   }
			   if(options.contains(CLOSESTDISTANCETOGREEN)) {
				   if(path[1] < closestDistanceToGreen && graph.nodes.get(path[0]).type == Node.GREEN) {
				    	closestDistanceToGreen = path[1];
				    }
			   }
			   if(options.contains(CLOSESTCOLORTYPE)) {
				   int type = graph.nodes.get(path[0]).type;
				   if(path[1] < closestColorDistance && type != 0) {
				    	closestColorDistance = path[1];
				    	closestColorType = graph.nodes.get(path[0]).type;
				    }
			   }
			   
			   if(options.contains(MEANDISTANCE)) {
			    	meanDistance += path[1];
			    	meanDistanceCounter++;
			   }
			   if(options.contains(MEANDISTANCETORED)) {
				   if(graph.nodes.get(path[0]).type == Node.RED) {
				    	meanDistanceToRed += path[1];
				    	redMeanDistanceCounter++;
				    }
			   }
			   if(options.contains(MEANDISTANCETOGREEN)) {
				   if(graph.nodes.get(path[0]).type == Node.GREEN) {
				    	meanDistanceToGreen += path[1];
				    	greenMeanDistanceCounter++;
				    }
			   }
			}	
		}
		
		map.put(NUMNODSINBALL, numNodsInBall);
		
		map.put(NUMREDNODSINBALL, numRedNodsInBall);
		
		map.put(NUMGREENNODSINBALL, numGreenNodsInBall);
		
		map.put(CLOSESTDISTANCE, new double[] { closestDistance});
		
		map.put(CLOSESTCOLORTYPE, new double[] { closestColorType});
		
		map.put(CLOSESTDISTANCETORED, new double[] { closestDistanceToRed});
		
		map.put(CLOSESTDISTANCETOGREEN, new double[] {closestDistanceToGreen});
		
		if(options.contains(MEANDISTANCE)) {
			meanDistance = meanDistance / meanDistanceCounter;
			meanDistance = Math.round(meanDistance * 1000.0) / 1000.0;
			
			map.put(MEANDISTANCE, new double[] { meanDistance});
		}
		else {
		
			map.put(MEANDISTANCE, new double[] { -1});
		}
		if(options.contains(MEANDISTANCETORED)) {
			meanDistanceToRed = meanDistanceToRed / redMeanDistanceCounter;
			meanDistanceToRed = Math.round(meanDistanceToRed * 1000.0) / 1000.0;
		
			map.put(MEANDISTANCETORED,new double[] { meanDistanceToRed});
		}
		else {
		
			map.put(MEANDISTANCETORED,  new double[] {-1});
		}
		if(options.contains(MEANDISTANCETOGREEN)) {
			meanDistanceToGreen = meanDistanceToGreen / greenMeanDistanceCounter;
			meanDistanceToGreen = Math.round(meanDistanceToGreen * 1000.0) / 1000.0;
			
			map.put(MEANDISTANCETOGREEN, new double[] { meanDistanceToGreen});
		}
		else {
		
			map.put(MEANDISTANCETOGREEN, new double[] {-1});
		}
		
		
		return map;
	}
	
	
/**
 * Finds the number of nodules in the ball of given radius, centered at given node.	
 * @param color : color of the given nodule node.
 * @param radius : radius of the ball.
 * @param node : center of the ball.
 * @param graph : graph object.
 * @return : number of nodules in radius distance away from node.
 */
	protected int numNodulesinBall(int color, int radius, Node node, RootGraph graph) {
		int numNods = 0;
		
		ArrayList<ArrayList<int[]>> paths = node.paths;
		
		for(int ii = 0; ii < paths.size(); ii++) {
			ArrayList<int[]> SPToNodeii = paths.get(ii);
			
			for(int jj = 0; jj < SPToNodeii.size(); jj++) {
			    int[] path = SPToNodeii.get(jj);
			   
			    // if distance is smaller and node is correct color.
			    if(path[1] < radius && graph.nodes.get(path[0]).type == color) {
			    	numNods++;
			    }
			    
			}	
		}
		
		return numNods;
	}
	
	
	/**
	 * @param color  == 1 -> red nodule
	 *  			 == 2 -> green nodule
	 * 				 == 3 -> mixed nodule
	 * 
	 * @return : the closest distance from a given node to another node of the given color
	 */
	protected int closestDistance(int color, Node node, RootGraph graph) {
		int distance = Integer.MAX_VALUE;
		int[] index = new int[2];
		
		ArrayList<ArrayList<int[]>> paths = node.paths;
		
		for(int ii = 0; ii < paths.size(); ii++) {
			ArrayList<int[]> SPToNodeii = paths.get(ii);
			
			for(int jj = 0; jj < SPToNodeii.size(); jj++) {
			    int[] path = SPToNodeii.get(jj);
			   
			    // if distance is smaller and node is correct color.
			    if(path[1] < distance && graph.nodes.get(path[0]).type == color) {
			    	distance = path[1];
			    	index[0] = ii;
			    	index[1] = jj;
			    }
			    
			    
			    
			}	
		}
		
		System.out.println("closest " + color + " colored node to " + node.nodeNumber + 
				" is " + paths.get(index[0]).get(index[1])[0]);
		
		return distance;
	}
	
	
	/**
	 * computes what color the closest nodule is. If there is a tie, it... 
	 * @param node
	 * @param graph
	 * @return
	 */
	protected int closestColorType(Node node, RootGraph graph) {
		int type = -1;
		int distance = Integer.MAX_VALUE;
		
		ArrayList<ArrayList<int[]>> paths = node.paths;
		
		
		for(int ii = 0; ii < paths.size(); ii++) {
			ArrayList<int[]> SPToNodeii = paths.get(ii);
			
			for(int jj = 0; jj < SPToNodeii.size(); jj++) {
			    int[] path = SPToNodeii.get(jj);
			   
			    if(path[1] < distance) {
			    	distance = path[1];
			    	type = graph.nodes.get(path[0]).type;
			    }
			    
			}	
		}
		
		return type;
	}
	
	
	/**
	 * Computes the mean distance from the given nodule to all other nodules of a given type.
	 * @param color : color to restrict the mean distances to 
	 * @param node : node we're computing the mean distances from
	 * @param graph : the graph object.
	 * @return : mean distance from the given nodule to all other nodules of a given type.
	 */
	protected static double meanDistance(int color, Node node, RootGraph graph) {
		double mean = 0;
		int counter = 0;
		
		ArrayList<ArrayList<int[]>> paths = node.paths;
		
		
		for(int ii = 0; ii < paths.size(); ii++) {
			ArrayList<int[]> SPToNodeii = paths.get(ii);
			
			for(int jj = 0; jj < SPToNodeii.size(); jj++) {
			    int[] path = SPToNodeii.get(jj);
			   
			    
			    if(graph.nodes.get(path[0]).type == color) {
			    	mean += path[1];
			    	counter++;
			    }
			    
			}	
		}
		mean = mean / counter;
		mean = Math.round(mean * 1000.0) / 1000.0;
		
		System.out.println("The mean distance from " + node.nodeNumber + " to all " + color 
				+ " colored nodes is " + mean);
		
		return mean;
		
	}
	
	
	
}
