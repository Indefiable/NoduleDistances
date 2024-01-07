package noduledistances.imagej;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;


import ij.gui.Line;

/**
 * The purpose of the class Graph is to hold all information related to the graph derived from 
 * the initial image, including the data and methods to translate between the abstract graph and
 * the point and edges on the skeleton of the image. 
 * The class also holds all computational methods relating to the abstract graph.
 * 
 * 
 * 
 * one pixel is (2601/ 4064256) mm^2
 */
public class Graph {

	ArrayList<int[]> graph;
	public ArrayList<ArrayList<int[]>> skeleton;
	
	/**
	 * Node is an extension of java.awt.point that
	 * includes an identifier for the node, i.e.
	 * Node.type == 0 -> skeleton node
	 * type == 1 -> red nodule
	 * type == 2 -> green nodule
	 * type == 3 -> mixed nodule
	 */
	public ArrayList<Node> nodes = new ArrayList<>();
	
	ArrayList<int[]> fsRep = new ArrayList<>();
	
	/**
	 * initializer. Constructs the graph object from the skeleton.
	 * @param skeleton
	 */
	public Graph(ArrayList<ArrayList<int[]>> skeleton) {
		
		this.skeleton = skeleton;
		
		// enumerate all nodes and creates the forward start rep.
		for( ArrayList<int[]> chunk : skeleton) {
			
			for(int ii = 0; ii < chunk.size(); ii+=2) {
				int[] start = chunk.get(ii);
			    int[] end = chunk.get(ii+1);
			    
			    Node startPt = new Node(start[0], start[1], 0);
			    Node endPt = new Node(end[0], end[1], 0);
			    
			    if(!nodes.contains(startPt)) {
			    	nodes.add(startPt);
			    }
			    
			    if(!nodes.contains(endPt)) {
			    	nodes.add(endPt);
			    }
			    
			    Line line = new Line(startPt.x, startPt.y, endPt.x, endPt.y);
			    int length = line.size();
			    int node1 = nodes.indexOf(startPt);
			    int node2 = nodes.indexOf(endPt);
			    
			    fsRep.add(new int[] {node1, node2, length});
			    fsRep.add(new int[] {node2, node1, length});

			}
		}
		
		
		System.out.println("number of nodes: " + nodes.size());
		System.out.println("number of edges: " + (fsRep.size() / 2));
		}//enumerate nodes
	
	
	/**
	 * Finds all instances within the graph that contain the input node as an out-node.
	 * @param node
	 * @return
	 */
	public ArrayList<Point[]> getInstances(Point node){
		ArrayList<Point[]> instances = new ArrayList<>();
		
		for(ArrayList<int[]> chunk : skeleton) {
			
			for(int ii = 0; ii < chunk.size(); ii+=2 ) {
				
				int[] start = chunk.get(ii);
			    int[] end = chunk.get(ii+1);
			    
			    if(start[0] == node.x && start[1] == node.y) {
			    	instances.add(new Point[] {node, new Point(end[0], end[1])});
			    	}
			    }
			}
		
		return instances;
	}
	
	/**
	 * Read's in the nodule data from NoduleData
	 * @param filePath
	 * @return
	 * @throws IOException
	 */
    private static String[][] readCSV(String filePath) throws IOException {
        List<String[]> records = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Split the CSV line by comma
                String[] values = line.split(",");
                records.add(values);
            }
        }

        // Convert List<String[]> to String[][]
        String[][] data = new String[records.size()][];
        for (int i = 0; i < records.size(); i++) {
            data[i] = records.get(i);
        }

        return data;
    }
	
    
    public void addNodules(String nodules) {

		String[][] nodLocations = null;
		
		try {
			nodLocations = readCSV(nodules);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(nodLocations == null) {
			System.out.println("Error reading nodule locations. Unable to add them.");
			return;
			}
		
		
    for(String[] nodule : nodLocations) {
    	if(nodule[0].equals("null")) {
    		continue;
    	}
    	if(nodule[1].equals("null")) {
    		continue;
    	}
    	if(nodule[2].equals("null")) {
    		continue;
    	}
    	
    	int noduleX = 0;
    	int noduleY = 0;
    	try {
    	noduleX = Integer.parseInt(nodule[0]);
    	noduleY = Integer.parseInt(nodule[1]);
    	}
    	catch(Exception e) {
    		System.out.println("error");
    		e.printStackTrace();
    	}
    	Node nod = null;
    	
    	if(nodule[2].equalsIgnoreCase("red")) {
    		nod = new Node(noduleX, noduleY, 1);
    	}
    	else if(nodule[2].equalsIgnoreCase("green")) {
    		nod = new Node(noduleX, noduleY, 2);
    	}
    	else if(nodule[2].equalsIgnoreCase("mixed")) {
    		nod = new Node(noduleX, noduleY, 3);
    	}
    	else {
    		System.out.println(nodule[2]);
    		continue;
    	}
    	
    	nodes.add(nod);
    	int nodeIndex = nodes.indexOf(nod);
    	
    	Node closestNode = calculateClosestNode(nod);
    	
    	int closestIndex = nodes.indexOf(closestNode);
    	
    	int length = (int) Node.distance(nod.x, nod.y, closestNode.x, closestNode.y);
    	
    	fsRep.add(new int[] {nodeIndex,closestIndex , length});
    	fsRep.add(new int[] {closestIndex,nodeIndex , length});
    	
    }
    }// addNodes()
    
    
    public ArrayList<int[]> noduleFSRep(){
    	ArrayList<int[]> nodFSRep = null;
    	
    	ArrayList<Node> nodules = new ArrayList<>();
    	
    	for(Node nod : nodes) {
    		if(nod.type > 0) {
    			nodules.add(nod);
    		}
    	}
    	//TODO: finish method.
    	
    	
    	return nodFSRep;
    	
    }
    
    
    private Node calculateClosestNode(Node p) {
    	
    	Node closest = null;
    	double dist = Integer.MAX_VALUE;
    	double compDist;
    	
    	for(Node node : nodes) {
    		compDist = Point.distance(node.x, node.y, p.x, p.y);
    		
    		if(compDist < dist) {
    			closest = node;
    			dist = compDist;
    		}
    	}
    	
    	return closest;
    }
    
    
    
    
}