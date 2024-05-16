package noduledistances.imagej;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.StringJoiner;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;

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
	
	String[][] masterCSV = new String[2][18];
	String imageName;
	/**
	 * Strings stating whether an image's red nodules are Fix+ or Fix-, and same with green.
	 * i.e. red = Fix+ , green = Fix- or vice versa. 
	 */
	String red;
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
	
	public Statistics(String imageName) throws CsvValidationException {
		this.imageName = imageName;
		String[][] dummy = new String[2][20];
		System.out.println("Image name: " + imageName);
		try  {
			CSVReader reader = new CSVReader(new FileReader("assets\\master.csv"));
            String[] nextLine = reader.readNext();
            
            while(nextLine == null) {
            	nextLine = reader.readNext();
            }
            dummy[0] = nextLine;
            
            while ((nextLine = reader.readNext()) != null) {
                // Process each row (nextLine) as needed	
            	if (nextLine.length > 0 && nextLine[0].equalsIgnoreCase(imageName)) {
            		int size = nextLine.length;
			        dummy[1] = nextLine;
			        break;
			    }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
		if(masterCSV[1] == null) {
			System.out.println("No data has been found for that the following image: " + imageName);
		}
		String color = dummy[1][1];
		String strain = dummy[1][19];
		
		if(color.equalsIgnoreCase("Red") && strain.equalsIgnoreCase("Fix-")) {
			this.red = "Fix+";
			this.green = "Fix-";
		}
		else if(color.equalsIgnoreCase("Red") && strain.equalsIgnoreCase("Fix+")) {
			this.red = "Fix-";
			this.green = "Fix+";
		}
		else if(color.equalsIgnoreCase("Green") && strain.equalsIgnoreCase("Fix+")) {
			this.green = "Fix+";
			this.red = "Fix-";
		}
		else if(color.equalsIgnoreCase("Green") && strain.equalsIgnoreCase("Fix-")) {
			this.green = "Fix-";
			this.red = "Fix+";
		}
		else {
			System.out.println("ERROR WITH DETERMINING THE STRAIN FOR THE IMAGE.");
		}
		
		for(int ii =0; ii < dummy[0].length; ii++) {
			
			if(ii < 1) {
				masterCSV[0][ii] = dummy[0][ii];
				masterCSV[1][ii] = dummy[1][ii];
			}
			if (ii > 1 && ii < 19) {
				masterCSV[0][ii-1] = dummy[0][ii];
				masterCSV[1][ii-1] = dummy[1][ii];
			}
			if(ii > 19) {
				masterCSV[0][ii-2] = dummy[0][ii];
				masterCSV[1][ii-2] = dummy[1][ii];
			}
		}
		
		System.out.println("COLOR: " + color);
		System.out.println("STRAIN: " + strain);
		System.out.println(Arrays.toString(masterCSV[0]));
		System.out.println(Arrays.toString(masterCSV[1]));
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
	
		ArrayList<String> distanceHeader = new ArrayList<>();
		
		distanceHeader.add(masterCSV[0][0]);
        distanceHeader.add("Roi");
        distanceHeader.add("Area");
        distanceHeader.add("Color");
        distanceHeader.add("Nod.Strain");
        for(int radius : radii) {
        	distanceHeader.add("numNods in r = " + radius);
            distanceHeader.add("num " + red + " Nods in r = " + radius);
            distanceHeader.add("num " + green + " Nods in r = " + radius);
        }
        distanceHeader.add("distance to closest nodule");
        distanceHeader.add("distance to closest " + red + " nodule");
        distanceHeader.add("distance to closest " + green + " nodule");
        distanceHeader.add("closest color type");
        distanceHeader.add("mean distance");
        distanceHeader.add("mean distance to " + red + " nodules");
        distanceHeader.add("mean distance to " + green + " nodules");
	    
		String[] header = new String[distanceHeader.size() + masterCSV[0].length-1];
		
		System.arraycopy(distanceHeader.toArray(), 0, header, 0, distanceHeader.size());
		System.arraycopy(masterCSV[0], 1, header, distanceHeader.size(), masterCSV[0].length-1);
		
		String[][] mat = new String[graph.numNodules+1][header.length];
		
	    mat[0] = header;
		HashMap<Integer,double[]> data = null;
		int matCounter = 0;
		
		for (int ii = 0; ii < graph.nodes.size(); ii++) {
			Node nodule = graph.nodes.get(ii);
			if(nodule.nodeNumber == 4) {
				System.out.println("breakpoint.");
			}
			//ignore skeleton nodes.
			if(nodule.type == Node.SKELETON) {
				continue;
			}
			
			data = computeStatistics(radii, nodule, graph, options);
			
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
				mat[matCounter+1][4] = this.red;
			}
			else if(nodule.type == Node.GREEN) {
				mat[matCounter+1][4] = this.green;
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
			
			if(kk == -1) {
				System.out.println("Breakpoint.");
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
			
			mat[matCounter+1][kk++] = Double.toString(data.get(MEANDISTANCE)[0]);
			mat[matCounter+1][kk++] = Double.toString(data.get(MEANDISTANCETORED)[0]);
			mat[matCounter+1][kk++] = Double.toString(data.get(MEANDISTANCETOGREEN)[0]);
			
			matCounter++;
		}
		
		mergeMasterData(mat, distanceHeader.size());
		
		String save = saveFile + "\\" + this.imageName + "\\" + this.imageName + "_data.csv";
	
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
	 * Merges data from master csv file to the image specific distance data.
	 * @param data
	 */
	private void mergeMasterData(String[][] data, int size) {
		
		System.out.println("=====================");
		System.out.println("inputCSVData");
		System.out.println("data[0].length: " + data[0].length);
		System.out.println("data.length: " + data.length);
		
		for(int ii = 1; ii < data.length; ii++) {
			data[ii][0] = masterCSV[1][0];
			
			for(int jj = size; jj  < data[0].length; jj++) {
				data[ii][jj] = masterCSV[1][jj-size+1];
			}
			
		}
		System.out.println("=====================");
	}
	
	
	
	 /**
	  * Computes the number of nodules within a given radius of a given color. Note that 
	  * our ball here is not a literal ball, as we use the distances along the root systems
	  * to compute whether a node falls within the ball, i.e. distance along root system < radius. 
	  *
	  * @param color : color of nodules we're counting
	  * @param radius : radius cutoff
	  * @param node : node we're searching around (center)
	  * @param graph : graph object
	  * @return : the number of nodules of the given color with distance < radius.
	  */
	
	
	/**
	 * 
	 * @param color : color to restrict statistics to.
	 * @param radius : radius used for computing number of nodules within a radius
	 * @param node : node we're computing the statistics of	
	 * @param graph : graph object
	 * @param options : list telling us what statistics to compute
	 * @return
	 */
	protected static HashMap<Integer,double[]> computeStatistics(int[] radii, Node node, RootGraph graph, ArrayList<Integer> options) {
		
		//ArrayList<double[]> data = new ArrayList<>();
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
		
		//data.add(NUMNODSINBALL,numNodsInBall);
		map.put(NUMNODSINBALL, numNodsInBall);
		
		//data.add(NUMREDNODSINBALL, numRedNodsInBall);
		map.put(NUMREDNODSINBALL, numRedNodsInBall);
		
		//data.add(NUMGREENNODSINBALL, numGreenNodsInBall);
		map.put(NUMGREENNODSINBALL, numGreenNodsInBall);
		
		//data.add(CLOSESTDISTANCE, new double[] { closestDistance});
		map.put(CLOSESTDISTANCE, new double[] { closestDistance});
		
		//data.add(CLOSESTCOLORTYPE, new double[] { closestColorType});
		map.put(CLOSESTCOLORTYPE, new double[] { closestColorType});
		
		//data.add(CLOSESTDISTANCETORED, new double[] { closestDistanceToRed});
		map.put(CLOSESTDISTANCETORED, new double[] { closestDistanceToRed});
		
		//data.add(CLOSESTDISTANCETOGREEN, new double[] {closestDistanceToGreen});
		map.put(CLOSESTDISTANCETOGREEN, new double[] {closestDistanceToGreen});
		
		if(options.contains(MEANDISTANCE)) {
			meanDistance = meanDistance / meanDistanceCounter;
			meanDistance = Math.round(meanDistance * 1000.0) / 1000.0;
			//data.add(MEANDISTANCE, new double[] { meanDistance});
			map.put(MEANDISTANCE, new double[] { meanDistance});
		}
		else {
			//data.add(MEANDISTANCE, new double[] { -1});
			map.put(MEANDISTANCE, new double[] { -1});
		}
		if(options.contains(MEANDISTANCETORED)) {
			meanDistanceToRed = meanDistanceToRed / redMeanDistanceCounter;
			meanDistanceToRed = Math.round(meanDistanceToRed * 1000.0) / 1000.0;
			//data.add(MEANDISTANCETORED,new double[] { meanDistanceToRed});
			map.put(MEANDISTANCETORED,new double[] { meanDistanceToRed});
		}
		else {
			//data.add(MEANDISTANCETORED,  new double[] {-1});
			map.put(MEANDISTANCETORED,  new double[] {-1});
		}
		if(options.contains(MEANDISTANCETOGREEN)) {
			meanDistanceToGreen = meanDistanceToGreen / greenMeanDistanceCounter;
			meanDistanceToGreen = Math.round(meanDistanceToGreen * 1000.0) / 1000.0;
			//data.add(MEANDISTANCETORED, new double[] { meanDistanceToRed});
			map.put(MEANDISTANCETOGREEN, new double[] { meanDistanceToGreen});
		}
		else {
		//	data.add(MEANDISTANCETOGREEN, new double[] {-1});
			map.put(MEANDISTANCETOGREEN, new double[] {-1});
		}
		
		
		
		return map;
	}
	
	
	
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
