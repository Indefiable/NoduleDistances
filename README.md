Nodule-Distances
================

An ImageJ Command plugin to calculate the pair-wise distances of nodules in 2-d nodule images. 
An extension of my other project, Nodule Segmentation.

For documentation of the source code, visit the documentation [website](https://indefiable.github.io/NoduleDistances/).

Limitations
===========
This plugin requires that you have the .tif file that is an output to my previous plugin,
 [Nodule Segmentation](https://github.com/Indefiable/Nodule-Segmentation). 

Additionally, it requires that you can segment the root system using the same image you segmented your nodules from
or that you have two images with different lighting that allows you to segment each one, but such that the images
overlap.(i.e. the plant isn't shifted over from image to image). This is because the plugin takes the tif file from my
previous plugin and overlays the positional location of the nodules onto the root system in this plugin, so the locations
need to align from image to image.


Installation
============
First install [fiji](https://imagej.net/software/fiji/). Then download the .jar file in the releases on this github page,
and drop the .jar file into the plugins folder wherever you installed fiji. 


Before you can run this plugin, you must generate a .model file for your dataset using Weka's color clustering plugin, and 
you must run my previous plugin, [Nodule Segmentation](https://github.com/Indefiable/Nodule-Segmentation) to get it's output. 
Note that the Nodule Segmentation github page also has a tutorial explaining how to create a .model file. You'll be following
the same procedure for this plugin, but instead attempting to segment the root system instead of the nodules.


Instructions
============

1. Create your model file for your data set(instructions above).

2. Install the plugin to your installation of Fiji(ImageJ) (instructions above).

3. Run the plugin at plugins -> Nodule Data.

4. You may be prompted to box the area of the image that the root system is in.

5. Browse your files and select the image with the nodules and the model file generated earlier.

6. Let the program run. 



Notes
=====
This Fiji(ImageJ) plugin uses two dependencies that are not found in the public maven repo.

The K-Shortest-Paths dependency is from Yan-Qi found [here](https://github.com/yan-qi/k-shortest-paths-java-version/), 
with a fork from github user [TomasJohansson](https://github.com/yan-qi/k-shortest-paths-java-version/issues/4)
 that allows the user to create graphs programmatically.
 
 The skeletonization dependency is from github user LingDong found [here](https://github.com/LingDong-/skeleton-tracing).