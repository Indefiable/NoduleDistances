package noduledistances.imagej;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.TextRoi;
import ij.process.ColorProcessor;



public class GraphOverlay {
	
	
	private final int NODERADIUS=6;
	
	
	public ImagePlus overlayedGraph;
	
	
	
	public GraphOverlay() {
		// TODO Auto-generated constructor stub
	}

	
	public void showGraph() {
		
		if(overlayedGraph != null) {
			overlayedGraph.show();
		}
		
	}
	
	
	/**
	 * created for testing purposes, this method returns a copy of the image
	 * overlay with the first set of nodes and edges highlighted in Color.ORANGE and the second
	 * set highlighted in Color.CYAN.
	 * 
	 * @param graph 
	 * @param nodeList : list of node indices from graph.nodes
	 * @param edgeList : list of edge indices from graph.fsRep
	 * @return a copy of overlayedGraph with highlighted nodes+edges.
	 */
	public ImagePlus highlightGraphSection(RootGraph graph, ArrayList<Integer> nodeList, 
		ArrayList<Integer> edgeList, ArrayList<Integer> nodeList1, 
		ArrayList<Integer> edgeList1) {
		
		ImagePlus highlightedGraph = new ImagePlus("highlighted", overlayedGraph.getProcessor());
		Overlay overlay = new Overlay();
		overlay.add(overlayedGraph.getOverlay());
		highlightedGraph.setOverlay(overlay);
		
		int highlightRadius = NODERADIUS;
		
		TextRoi.setFont("SansSerif",25 , Font.PLAIN);
		 Font font = new Font("SansSerif",Font.PLAIN,25);
		
		for(int nodeIndex : nodeList) {
			Node node = graph.nodes.get(nodeIndex);
			
			OvalRoi ball = new OvalRoi( node.x - highlightRadius,  node.y - highlightRadius, 
					2 * highlightRadius, 2 * highlightRadius);
			
			ball.setFillColor(Color.ORANGE);
			highlightedGraph.getOverlay().add(ball);
			
			TextRoi.setColor(Color.CYAN);
			TextRoi textROI = new TextRoi(node.x, node.y, Integer.toString(nodeIndex), font);
			textROI.setStrokeColor(Color.CYAN); 
	    	textROI.setStrokeWidth(1); 
	    	
			overlay.add(textROI);
			
		}
		
		for(int edgeIndex : edgeList) {
			int[] edge = graph.fsRep.get(edgeIndex);
			
			Node node1 = graph.nodes.get(edge[0]);
			Node node2 = graph.nodes.get(edge[1]);
			
			
			Line line = new Line(node1.x, node1.y, node2.x, node2.y);
		    line.setStrokeWidth(5);
		    line.setStrokeColor(Color.ORANGE);
		    overlay.add(line);
		}
		
		
		
		for(int nodeIndex : nodeList1) {
			Node node = graph.nodes.get(nodeIndex);
			
			OvalRoi ball = new OvalRoi( node.x - highlightRadius,  node.y - highlightRadius, 
					2 * highlightRadius, 2 * highlightRadius);
			
			ball.setFillColor(Color.CYAN);
			highlightedGraph.getOverlay().add(ball);
			
			TextRoi.setColor(Color.CYAN);
			TextRoi textROI = new TextRoi(node.x, node.y, Integer.toString(nodeIndex), font);
			textROI.setStrokeColor(Color.CYAN); 
	    	textROI.setStrokeWidth(1); 
	    	
			overlay.add(textROI);
			
		}
		
		for(int edgeIndex : edgeList1) {
			int[] edge = graph.fsRep.get(edgeIndex);
			
			Node node1 = graph.nodes.get(edge[0]);
			Node node2 = graph.nodes.get(edge[1]);
			
			
			Line line = new Line(node1.x, node1.y, node2.x, node2.y);
		    line.setStrokeWidth(5);
		    line.setStrokeColor(Color.CYAN);
		    overlay.add(line);
		}
		
		 
			
		return highlightedGraph;
	}
	
	
	public void overlayGraph(RootGraph graph, ColorProcessor cp) {
		ImagePlus skellyMap = new ImagePlus("skeleton", cp);
		TextRoi.setFont("SansSerif",30 , Font.BOLD);
    	Font font = new Font("SansSerif",Font.BOLD,30);
    	  
		Overlay overlay = new Overlay();
		skellyMap.setOverlay(overlay);
		int counter = 0;
		
		for (int[] edge : graph.fsRep) {
			counter++;
			Node node1 = graph.nodes.get(edge[0]);
			Node node2 = graph.nodes.get(edge[1]);
			
		 	Line line = new Line(node1.x, node1.y, node2.x, node2.y);
		    line.setStrokeWidth(5);
		    line.setStrokeColor(Color.DARK_GRAY);
		    overlay.add(line);
		}
		
		
		for(Node node : graph.nodes) {
			
			if(node.type < 1) {
			OvalRoi ball = new OvalRoi( node.x - NODERADIUS,  node.y - NODERADIUS, 2 * NODERADIUS, 2 * NODERADIUS);
			ball.setFillColor(Color.BLUE);
			skellyMap.getOverlay().add(ball);
			

			}
			
			else {
				
				String label = Double.toString(node.nodeNumber);

				String[] parts = label.split("\\.");
				String part2;
				
				if(parts.length > 1 && !parts[1].equals("0")) {
					parts[1] = Integer.toString(Integer.parseInt(parts[1]));
					part2 = "_" + parts[1];
				}
				else {
					part2 = "";
				}
				
				label = parts[0] + part2;
				
		    	TextRoi textLabel = new TextRoi(node.x,
			    			node.y, label,font);
		    	
		    	
		    	textLabel.setStrokeWidth(2); 
		    	
				int radius = NODERADIUS + 8;
				OvalRoi ball = new OvalRoi( node.x - radius,  node.y - radius, 2 * radius, 2 * radius);
				
				if(node.type ==1) {
					ball.setFillColor(Color.RED);
					textLabel.setStrokeColor(Color.RED); 
				}
				if(node.type == 2) {
					ball.setFillColor(Color.GREEN);
					textLabel.setStrokeColor(Color.GREEN); 
				}
				if(node.type == 3) {
					ball.setFillColor(Color.YELLOW);
					textLabel.setStrokeColor(Color.YELLOW); 
				}
				skellyMap.getOverlay().add(ball);
				skellyMap.getOverlay().add(textLabel);
		    	
			
			}
			
		}
		
		skellyMap.setTitle("Graph");
		
		this.overlayedGraph = skellyMap;
		
	}
	
	
	public void loadTif() {
		
		ImagePlus tifImp = null;
		
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Image to load or file to iterate through.");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        // Add a file filter for image files (you can customize this for specific image types)
        FileNameExtensionFilter imageFilter = new FileNameExtensionFilter("Tif", "tif");
        fileChooser.setFileFilter(imageFilter);
        
        int result = fileChooser.showOpenDialog(null);
        
        if( result == JFileChooser.APPROVE_OPTION) {
        	tifImp = new ImagePlus(fileChooser.getSelectedFile().getAbsolutePath());
        }
        
       if(tifImp == null) {
    	   System.out.println("failed to load Tif file. Did you choose a Tif file?");
    	   return;
       }
       
       tifImp.show();
       Overlay nodules = tifImp.getOverlay();
       
       if(nodules == null) {
    	   System.out.println("no overlay.");
       }
       
       
       
       System.out.println("loadTif breakpoint.");
       
       
	}

	
	
	// methods beyond this line are deprecated or used for testing purposes.
	//==========================================================================
	/**
	 * Overlays the skeleton of the root system onto the image. Also overlays 
	 * @param nodes: list of nodes.
	 */
	public void overlaySkeleton(RootGraph graph, ColorProcessor cp) {

		ImagePlus skellyMap = new ImagePlus("skeleton", cp);

		Overlay overlay = new Overlay();
		skellyMap.setOverlay(overlay);

		for (ArrayList<int[]> chunk : graph.skeleton) {
			
			for(int ii = 0; ii < chunk.size(); ii+=2) {
				
				 	int[] edgeStart = chunk.get(ii);
				    int[] edgeEnd = chunk.get(ii+1);
				    
				    int startX = edgeStart[0];
				    int startY = edgeStart[1];
				    int endX = edgeEnd[0];
				    int endY = edgeEnd[1];

				    Line line = new Line(startX, startY, endX, endY);
				    line.setStrokeWidth(2);
				    line.setStrokeColor(Color.MAGENTA);
				    overlay.add(line);
			}
		}
		
		
		for(Point point : graph.nodes) {
			OvalRoi ball = new OvalRoi( point.x - NODERADIUS,  point.y - NODERADIUS, 2 * NODERADIUS, 2 * NODERADIUS);
			ball.setFillColor(Color.BLUE);
			skellyMap.getOverlay().add(ball);
		}
		
		//check if nodules have been added to graph yet.
		int[] types = graph.fsRep.stream().mapToInt(row -> row[2]).toArray();
		boolean containsNodules = false;
		
		for(int type : types) {
			if(type >0) {
				containsNodules=true;
			}
		}
		
		
		if(containsNodules) {
			overlayNodules(graph, skellyMap);
		}
		
		
		skellyMap.setTitle("skeleton");
		
		this.overlayedGraph = skellyMap;
		
	}
	
	public void overlayNodules(RootGraph graph, ImagePlus skellyMap){
		
		ArrayList<int[]> nodules = graph.noduleFSRep();
		
		
		for(int[] edge : nodules) {
			System.out.println("(" + edge[0] + ", " + edge[1] + ")");
			Node node1 = graph.nodes.get(edge[0]);
			Node node2 = graph.nodes.get(edge[1]);
			Node nodule = null;
			
			if(node1.type > 0) {
				nodule = node1;
			}
			else if(node2.type > 0){
				nodule = node2;
			}
			else {
				System.out.println("I'm confused, you're not supposed to see this.");
			}
			
			Line line = new Line(node2.x, node2.y, node1.x, node1.y);
			line.setStrokeWidth(4);
			
		    line.setStrokeColor(Color.ORANGE);
		    
			skellyMap.getOverlay().add(line);
		}
		
		 TextRoi.setFont("SansSerif",100 , Font.BOLD);
		 Font font = new Font("SansSerif",Font.BOLD,50);
   	  
		int counter = 1;
		for(Node nodule : graph.getNodules()) {
			 int noduleRadius = NODERADIUS+10;
			    
				OvalRoi ball = new OvalRoi( nodule.x - noduleRadius,  nodule.y - noduleRadius,
						2 * noduleRadius, 2 * noduleRadius);
				
				
				if(nodule.type == 1) {
					ball.setFillColor(Color.RED);
				}
				else if(nodule.type == 2) {
					ball.setFillColor(Color.GREEN);
				}
				else if(nodule.type == 3) {
					ball.setFillColor(Color.YELLOW);
				}
				
				TextRoi.setColor(Color.CYAN);
				TextRoi textROI = new TextRoi(nodule.x, nodule.y, Integer.toString(counter++), font);
				textROI.setStrokeColor(Color.CYAN); 
 		    	textROI.setStrokeWidth(2); 
 		    	
 		    	
				skellyMap.getOverlay().add(textROI);
				skellyMap.getOverlay().add(ball);
			
		}
		
	}
	
	
}
