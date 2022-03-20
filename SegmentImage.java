import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;
import java.util.*;
import java.util.stream.IntStream;
// Main class
public class SegmentImage extends Frame implements ActionListener {
	BufferedImage input;
	int width, height;
	int numK=4;
	int cc = 0;
	CanvasImage source, target;
	CheckboxGroup metrics = new CheckboxGroup();
	// Constructor
	public SegmentImage(String name) {
		super("Image Segmentation");
		// load image
		try {
			input = ImageIO.read(new File(name));
		}
		catch ( Exception ex ) {
			ex.printStackTrace();
		}
		width = input.getWidth();
		height = input.getHeight();
		// prepare the panel for image canvas.
		Panel main = new Panel();
		source = new CanvasImage(input);
		target = new CanvasImage(width, height);
		main.setLayout(new GridLayout(1, 2, 10, 10));
		main.add(source);
		main.add(target);
		// prepare the panel for buttons.
		Panel controls = new Panel();
		controls.add(new Checkbox("Intensity", metrics, true));
		controls.add(new Checkbox("Gradient", metrics, false));
		controls.add(new Checkbox("Distance", metrics, false));
		Button button = new Button("Watershed");
		button.addActionListener(this);
		controls.add(button);

		JLabel label = new JLabel("cluster num=" + numK);
		label.setPreferredSize(new Dimension(90, 20));
		controls.add(label);
		JSlider slider = new JSlider(2, 16, numK);
		slider.setPreferredSize(new Dimension(96, 20));
		controls.add(slider);
		slider.addChangeListener(changeEvent -> {
			numK = slider.getValue();
			label.setText("cluster num=" + numK);
		});

		button = new Button("K-mean clustering");
		button.addActionListener(this);
		controls.add(button);
		// add two panels
		add("Center", main);
		add("South", controls);
		addWindowListener(new ExitListener());
		pack();
		setVisible(true);
	}
	class ExitListener extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			System.exit(0);
		}
	}
	// Action listener for button click events
	public void actionPerformed(ActionEvent e) {
		// generate Moravec corner detection result
		source.resetImage(input);
		BufferedImage src_image = source.image;
		int pixel_size = src_image.getColorModel().getPixelSize();

		if ( ((Button)e.getSource()).getLabel().equals("Watershed") ) {
			if ( metrics.getSelectedCheckbox().getLabel().equals("Intensity") ) {
				if (pixel_size == 24){
					src_image = grayscale(src_image);
				}

				target.resetImage(intensity_watershed(src_image));
				source.resetImage(src_image);

			}
			if ( metrics.getSelectedCheckbox().getLabel().equals("Gradient") ) {
				BufferedImage blurredImage = approximationFilter(src_image);
				BufferedImage gradient_magnitude = grad_mag(derivatives_x(blurredImage), derivatives_y(blurredImage));
				// target.resetImage(gradient_magnitude);
				source.resetImage(gradient_magnitude);
				target.resetImage(intensity_watershed(gradient_magnitude));
			}
			if ( metrics.getSelectedCheckbox().getLabel().equals("Distance") ) {
				if (pixel_size == 24){
					src_image = grayscale(src_image);
				}
				source.resetImage(distance_transform(thresholding(src_image)));
				target.resetImage(intensity_watershed(distance_transform(thresholding(src_image))));

			}
		}
		if ( ((Button)e.getSource()).getLabel().equals("K-mean clustering") ) {
			target.resetImage(k_means(src_image, numK));
		}
	}
	
	/*Function to apply the intensity watershed segmentation algorithm to a grayscale image
		Input (BufferedImage): Original Grayscale Image
		Output (BufferedImage): Intensity watershed segmented image
	*/
	public BufferedImage intensity_watershed(BufferedImage img){
			BufferedImage output_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			int left, middle, right, up, down, up_left, up_right, down_left, down_right, intensity;
			Color black = new Color(0,0,0);
			Color white = new Color(255,255,255);
			Color output_image_color = new Color(0,0,0);
			int max_intensity = max_intensity(img);
			int min_intensity = min_intensity(img);
			for (intensity = min_intensity; intensity < max_intensity; intensity++){
				for ( int q=0 ; q<height ; q++ ) {
					for ( int p=0 ; p<width ; p++ ) {
						int current_pixel_intensity = img.getRaster().getSample(p, q, 0);
						if (current_pixel_intensity == intensity){
						// if (current_pixel_intensity > intensity - numK -1 && current_pixel_intensity < intensity + numK - 1){
							left = p==0 ? p : p-1;
							middle = p;
							right = p==width-1 ? p : p+1;
							up = q==0 ? q : q-1;
							down = q==height-1 ? q : q+1;
							
							//Finding intensity of 8 surrounding pixels to the current pixel 
							int left_pixel = output_image.getRGB(left, q);
							int right_pixel = output_image.getRGB(right, q);
							int up_pixel = output_image.getRGB(p, up);
							int down_pixel = output_image.getRGB(p, down);
							int up_left_pixel = output_image.getRGB(left, up);
							int up_right_pixel = output_image.getRGB(right, up);
							int down_left_pixel = output_image.getRGB(left, down);
							int down_right_pixel = output_image.getRGB(right, down);
							ArrayList<Integer> surrounding_pixels = new ArrayList<>();

							surrounding_pixels.add(left_pixel);
							surrounding_pixels.add(right_pixel);
							surrounding_pixels.add(up_pixel);
							surrounding_pixels.add(down_pixel);
							surrounding_pixels.add(up_left_pixel);
							surrounding_pixels.add(up_right_pixel);
							surrounding_pixels.add(down_left_pixel);
							surrounding_pixels.add(down_right_pixel);

							ArrayList<Integer> adjacent_pixels = new ArrayList<>();
							//Iterate through all surrounding pixels
							for (int i = 0; i < surrounding_pixels.size(); i++){
								//If any of the surrounding pixels have a color, there is a region already defined adjacent to the current pixel.
								if (surrounding_pixels.get(i) != black.getRGB()){
									if (surrounding_pixels.get(i) != white.getRGB()){
										//If any of the surrounding pixels is not black, add it to the adjacent_pixels array.
										adjacent_pixels.add(surrounding_pixels.get(i));
									}
								}
							}
							if (adjacent_pixels.size() == 0){
								//If current pixel is not adjacent to any existing region, assign new random color
								output_image_color = new Color((int) (Math.random()*255d),(int) (Math.random()*255d),(int) (Math.random()*255d));
								int hsv = Color.HSBtoRGB(intensity/255f , 1f, 1f);
							}else if(adjacent_pixels.size() == 1){

								//If current pixel is adjacent to a single existing region, assign color of existing region
								output_image_color = new Color(adjacent_pixels.get(0), true);
							}else if(adjacent_pixels.size() > 1){
								//If current pixel is adjacent to 2 or more existing regions
								HashSet<Integer> hset = new HashSet<Integer>(adjacent_pixels);
								//If both existing regions have the same color, set to color
								if (hset.size() == 1){
									output_image_color = new Color(adjacent_pixels.get(0), true);
								}else{
								//If two different regions, set to white
									output_image_color = new Color(255,255,255);
								}
							}
							output_image.setRGB(p, q, output_image_color.getRGB());
						}
					}
				}
			}
			return output_image;
	}
	
	/*Function to find the distance transform of a Binary image
		Input (BufferedImage): Original Binary Image
		Output (BufferedImage): Distance Transform Image
		Distance Transform Algorithm (Dynamic Programming Implementation):
			- Iterate through each pixel in the grayscale image
			- If pixel intensity is white, set output to infinity (Large number)
			- If pixel intensity is black, set output to 0
			- Forward Pass:
				- Iterate through each pixel
				- Find distance to the closest feature on left
					- Check top and left in the forward pass
			- Backward Pass:
				- Iterate through each pixel
				- Find distance to the closest feature on the right
					- Check bottom and right in the backward pass
			- Normalizing:
				- Distance transform image may be very dim
					- Normalize the image by linearly scaling each pixel between a range of 0 and 255
	*/
	public BufferedImage distance_transform(BufferedImage img){
		BufferedImage output_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int[][] D = new int[width][height];
		
		//Check if binary image pixel is foreground or background
		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				if (img.getRGB(p, q) == Color.white.getRGB()){
					D[p][q] = 255;
				}else{
					D[p][q] = 0;
				}
			}
		}

		//Forward Pass
		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				int l = p==0 ? p : p-1;
				int r = p==width-1 ? p : p+1;
				int u = q==0 ? q : q-1;
				int d = q==height-1 ? q : q+1;
				D[p][q] = Math.min(Math.min(D[p][q], D[l][q]+1), D[p][u]+1);
			}
		}

		//Backward Pass
		for ( int q=height-1 ; q>=0 ; q-- ) {
			for ( int p=width-1 ; p>=0 ; p-- ) {
				int l = p==0 ? p : p-1;
				int r = p==width-1 ? p : p+1;
				int u = q==0 ? q : q-1;
				int d = q==height-1 ? q : q+1;
				D[p][q] = Math.min(Math.min(D[p][q], D[r][q]+1), D[p][d]+1);
			}
		}
	
		//Convert Matrix to BufferedImage
		for ( int q=0 ; q<height ; q++) {
			for ( int p=0 ; p<width ; p++ ) {
				int d_color = D[p][q];
				d_color = Math.min(d_color, 255);
				output_image.setRGB(p, q, new Color(d_color,d_color,d_color).getRGB());
			}
		}

		//Inverse Grayscale Image
		output_image = inverse(output_image);
		int min_int = 255;
		int max_int = 0;

		//Find minimum and maximum intensity of image
		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				Color clr = new Color(output_image.getRGB(p,q));
				int intensity = (clr.getRed() + clr.getGreen() + clr.getBlue())/3;
				if (intensity > max_int){
					max_int = intensity;
				}
				if (intensity < min_int){
					min_int = intensity;
				}
			}
		}

		//Linearly scale pixels to a range between 0 and 255
		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				Color clr = new Color(output_image.getRGB(p,q));
				int intensity = (clr.getRed() + clr.getGreen() + clr.getBlue())/3;
				double slope = (255)/(max_int - min_int);
				int output_intensity = ((int) slope*intensity) - (int) slope*min_int;
				output_intensity = Math.min(output_intensity, 255);

				output_image.setRGB(p, q, new Color(output_intensity, output_intensity, output_intensity).getRGB());

			}
		}
		return output_image;
	}

	/*Function to invert grayscale image
		Input (BufferedImage): Original Grayscale Image
		Output (BufferedImage): Inverted Image
		Inverting Algorithm:
			- Iterate through each pixel in the grayscale image
			- Subtract pixel intensity by 255
	*/
	public BufferedImage inverse(BufferedImage image){
		int l, r, dr, dg, db;
		Color clr1, clr2;
		BufferedImage t = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				clr1 = new Color(image.getRGB(p,q));
				int intensity = (clr1.getRed() + clr1.getGreen() + clr1.getBlue())/3;
				intensity = 255 - intensity;
				t.setRGB(p, q, new Color(intensity, intensity, intensity).getRGB());
				
			}
		}
		return t;
	}

	/*Function to threshold grayscale image
		Input (BufferedImage): Original Grayscale Image
		Output (BufferedImage): Binary Image
		Thresholding Algorithm:
			- Iterate through each pixel in the grayscale image
			- If pixel intensity is greater than the threshold value, set to white
			- If pixel intensity is less than the threshold value, set to black
	*/
	public BufferedImage thresholding(BufferedImage image) {
		int l, r, dr, dg, db;
		Color clr1, clr2;
		BufferedImage t = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				clr1 = new Color(image.getRGB(p,q));
				int intensity = (clr1.getRed() + clr1.getGreen() + clr1.getBlue())/3;
				if (intensity < 128){
					// t.setRGB(p, q, new Color(255, 255, 255).getRGB());
					t.setRGB(p, q, new Color(0, 0, 0).getRGB());

				}else{
					// t.setRGB(p, q, new Color(0, 0, 0).getRGB());
					t.setRGB(p, q, new Color(255, 255, 255).getRGB());

				}
			}
		}
		

		return t;
	}

	/*Function to get the intensity of the image in the x direction
		Input (BufferedImage): Original Image
		Output (BufferedImage): Intensity of input in the x direction. RGB values are offset by 128
	*/
	public BufferedImage derivatives_x(BufferedImage image) {
		int l, r, dr, dg, db;
		Color clr1, clr2;
		BufferedImage t = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				l = p==0 ? p : p-1;
				r = p==width-1 ? p : p+1;
				clr1 = new Color(image.getRGB(l,q));
				clr2 = new Color(image.getRGB(r,q));
				dr = clr2.getRed() - clr1.getRed();
				dg = clr2.getGreen() - clr1.getGreen();
				db = clr2.getBlue() - clr1.getBlue();
				dr = Math.max(0, Math.min(dr+128, 255));
				dg = Math.max(0, Math.min(dg+128, 255));
				db = Math.max(0, Math.min(db+128, 255));
				t.setRGB(p, q, new Color(dr, dg, db).getRGB());
			}
		}
		return t;
	}
	
	/*Function to get the intensity of the image in the y direction
		Input (BufferedImage): Original Image
		Output (BufferedImage): Intensity of input in the y direction. RGB values are offset by 128
	*/
	public BufferedImage derivatives_y(BufferedImage image) {
		int l, r, dr, dg, db;
		Color clr1, clr2;
		BufferedImage t = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		for ( int p=0 ; p<width ; p++ ) {
			for ( int q=0 ; q<height ; q++ ) {
				l = q==0 ? q : q-1;
				r = q==height-1 ? q : q+1;
				clr1 = new Color(image.getRGB(p,l));
				clr2 = new Color(image.getRGB(p,r));
				dr = clr2.getRed() - clr1.getRed();
				dg = clr2.getGreen() - clr1.getGreen();
				db = clr2.getBlue() - clr1.getBlue();
				dr = Math.max(0, Math.min(dr+128, 255));
				dg = Math.max(0, Math.min(dg+128, 255));
				db = Math.max(0, Math.min(db+128, 255));
				t.setRGB(p, q, new Color(dr, dg, db).getRGB());
			}
		}
		return t;
	}

	/*Function to get the gradient magnitude using the intensities in the x and y direction
		Input (BufferedImage, BufferedImage): Image intensity in the x direction, Image intensity in the y direction
		Output (BufferedImage): Gradient Magnitude of the image
		Gradient Magnitude Function: |G| = sqrt((G_x)^2 + (G_y)^2)
	*/
	public BufferedImage grad_mag(BufferedImage GoD_x, BufferedImage GoD_y){
		int l, r, dr, dg, db;
		Color GoD_x_color, GoD_y_color;
		BufferedImage t = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				l = p==0 ? p : p-1;
				r = p==width-1 ? p : p+1;
				GoD_x_color = new Color(GoD_x.getRGB(p,q));
				GoD_y_color = new Color(GoD_y.getRGB(p,q));
				double magnitude_r = Math.sqrt(Math.pow(GoD_x_color.getRed()-128, 2) + Math.pow(GoD_y_color.getRed()-128, 2));
				double magnitude_g = Math.sqrt(Math.pow(GoD_x_color.getGreen()-128, 2) + Math.pow(GoD_y_color.getGreen()-128, 2));
				double magnitude_b = Math.sqrt(Math.pow(GoD_x_color.getBlue()-128, 2) + Math.pow(GoD_y_color.getBlue()-128, 2));
				int rr = (int) Math.min(Math.max(0, magnitude_r), 255);
				int gg = (int) Math.min(Math.max(0, magnitude_g), 255);
				int bb = (int) Math.min(Math.max(0, magnitude_b), 255);
				int max_intensity_val = Math.max(Math.max(rr, gg), bb);
				t.setRGB(p, q, new Color(max_intensity_val, max_intensity_val, max_intensity_val).getRGB());

			}
		}
		return t;
	}

	/*Function to apply K-Means image segmentation
		Input (BufferedImage, int): Original RGB image, number of K clusters desired
		Output (BufferedImage): Image segmented with K randomly generated colors
		K-Means Function:
			- Randomly assign K colors to K cluster centers
			- Calculate distance from each pixel color in original image to cluster centers
			- Pick closest cluster to pixel color
			- Assign pixel to closest cluster
			- Once all pixels are assigned to it's respective cluster, computer the new cluster center
				- Average all colors in the cluster by adding all colors in the cluster and dividing by total number of pixels in the cluster
			- Repeat algorithm with new cluster centers
			- Stop algorithm until there is no further change in assignment of pixel points to clusters.
	*/
	public BufferedImage k_means(BufferedImage img, int k){
		BufferedImage output_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Color[] cluster_center = new Color[k];
		int[][] cluster_pixels = new int[k][3];
		int[] cluster_pixel_count = new int[k];
		double closest_distance_overall = 1000000d;
		
		//Picking K cluster centers randomly
		for (int i = 0; i < k; i++){
			int random_red = (int) (Math.random()*255d);
			int random_green = (int) (Math.random()*255d);
			int random_blue = (int) (Math.random()*255d);
			Color center_color = new Color(random_red,random_green,random_blue);
			cluster_center[i] = center_color;
			cluster_pixel_count[i] = 0;
			cluster_pixels[i][0] = 0;
			cluster_pixels[i][1] = 0;
			cluster_pixels[i][2] = 0;
		}

		double[] final_distances = new double[5];
		final_distances[0] = 1000000d;
		final_distances[1] = 100000d;
		final_distances[2] = 10000d;
		final_distances[3] = 1000d;
		final_distances[4] = 100d;
		double dis = 10000;
		int cc = 0;
		boolean stop = false;
		//Run algorithm till distance becomes minimal
		while (stop == false){
			for ( int q=0 ; q<height ; q++ ) {
				for ( int p=0 ; p<width ; p++ ) {
					int closest_k = 100;
					Color closest_color = new Color(0,0,0);
					double closest_distance = 10000d;
					//Calculate distance from pixel to each cluster
					for (int i = 0; i < k; i++) {
						Color img_color = new Color(img.getRGB(p,q));
						int img_color_r = img_color.getRed();
						int img_color_g = img_color.getGreen();
						int img_color_b = img_color.getBlue();

						int cls_r = cluster_center[i].getRed();
						int cls_g = cluster_center[i].getBlue();
						int cls_b = cluster_center[i].getGreen();

						double distance = Math.sqrt(Math.pow(img_color_r - cls_r, 2) + Math.pow(img_color_g -cls_g, 2) + Math.pow(img_color_b - cls_b, 2));
						// System.out.println("Dis" + distance);
						//Storing cluster with closest distance to pixel
						if (distance < closest_distance){
							closest_distance = distance;
							closest_k = i;
							closest_color = cluster_center[closest_k];
						}
					}

					if (closest_distance < closest_distance_overall){
						closest_distance_overall = closest_distance;
					}
					
					dis = closest_distance;
					cluster_pixel_count[closest_k]++;

					output_image.setRGB(p, q, closest_color.getRGB());

					//Adding pixel color values in each cluster
					cluster_pixels[closest_k][0] = cluster_pixels[closest_k][0] + closest_color.getRed();
					cluster_pixels[closest_k][1] = cluster_pixels[closest_k][1] + closest_color.getGreen();
					cluster_pixels[closest_k][2] = cluster_pixels[closest_k][2] + closest_color.getBlue();
				}
				
			}
			
			//Dividing total pixel color value with total pixels in each cluster to find new cluster center
			for (int i = 0; i < k; i++) {
				if (cluster_pixel_count[i] != 0){
					double red = cluster_pixels[i][0] / cluster_pixel_count[i];
					double green = cluster_pixels[i][1] / cluster_pixel_count[i];
					double blue = cluster_pixels[i][2] / cluster_pixel_count[i];

					cluster_center[i] = new Color((int) red, (int) green, (int) blue);
				}
			}

			//Clear out pixel count and cluster colors
			for (int i = 0; i < k; i++) {
				cluster_pixels[i][0] = 0;
				cluster_pixels[i][1] = 0;
				cluster_pixels[i][2] = 0;
				cluster_pixel_count[i] = 0;
			}
			final_distances[0] = final_distances[1];
			final_distances[1] = final_distances[2];
			final_distances[2] = final_distances[3];
			final_distances[3] = final_distances[4];
			final_distances[4] = dis;

			if (final_distances[4] == final_distances[3]){
				if (final_distances[3] == final_distances[2]){
					if (final_distances[2] == final_distances[1]){
						if (final_distances[1] == final_distances[0]){
							stop = true;
						}
					}
				}
			}
						
			// System.out.println(final_distances[0] + " " + final_distances[1] + " " + final_distances[2] + " " +final_distances[3] + " " + final_distances[4]);

			cc++;
			// System.out.println(cc);
		}
		
		return output_image;
	}

	/*Grayscale conversion Function
		Input (BufferedImage): Image to convert to grayscale
		Output (BufferedImage): Grayscale Image
	*/
	public BufferedImage grayscale(BufferedImage img){
		BufferedImage grayscaleImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		for (int i = 0; i < grayscaleImage.getHeight(); i++) {
			for (int j = 0; j < grayscaleImage.getWidth(); j++) {
				Color c = new Color(img.getRGB(j, i));
				int red = (int) c.getRed();
				int green = (int) c.getGreen();
				int blue = (int) c.getBlue();
				Color newColor = new Color(
						(red + green + blue)/3,
						(red + green + blue)/3,
						(red + green + blue)/3);
				grayscaleImage.setRGB(j, i, newColor.getRGB());
			}
		}
		return grayscaleImage;
	}
	
	/*Approximation Filter Function
		Input (BufferedImage): Image to apply filter to
		Output (BufferedImage): Approximated Image
	*/
	public BufferedImage approximationFilter(BufferedImage img){
		int[] filter = {1, 2, 1, 2, 4, 2, 1, 2, 1};
		int filterWidth = 3;
		
		BufferedImage approximated_img = blur(img, filter, filterWidth);

		return approximated_img;
	}

	/*Gaussian Blur Filter Function
		Input (BufferedImage, int list, int): Image to apply gaussian filter, filter kernel, filter width
		Output (BufferedImage): Gaussian blurred image
		Reference: https://stackoverflow.com/a/39686530
	*/
	public static BufferedImage blur(BufferedImage image, int[] filter, int filterWidth) {
		if (filter.length % filterWidth != 0) {
			throw new IllegalArgumentException("filter contains a incomplete row");
		}
	
		final int width = image.getWidth();
		final int height = image.getHeight();
		final int sum = IntStream.of(filter).sum();
	
		int[] input = image.getRGB(0, 0, width, height, null, 0, width);
	
		int[] output = new int[input.length];
	
		final int pixelIndexOffset = width - filterWidth;
		final int centerOffsetX = filterWidth / 2;
		final int centerOffsetY = filter.length / filterWidth / 2;
	
		// apply filter
		for (int h = height - filter.length / filterWidth + 1, w = width - filterWidth + 1, y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int r = 0;
				int g = 0;
				int b = 0;
				for (int filterIndex = 0, pixelIndex = y * width + x;
						filterIndex < filter.length;
						pixelIndex += pixelIndexOffset) {
					for (int fx = 0; fx < filterWidth; fx++, pixelIndex++, filterIndex++) {
						int col = input[pixelIndex];
						int factor = filter[filterIndex];
	
						// sum up color channels seperately
						r += ((col >>> 16) & 0xFF) * factor;
						g += ((col >>> 8) & 0xFF) * factor;
						b += (col & 0xFF) * factor;
					}
				}
				r /= sum;
				g /= sum;
				b /= sum;
				// combine channels with full opacity
				output[x + centerOffsetX + (y + centerOffsetY) * width] = (r << 16) | (g << 8) | b | 0xFF000000;
			}
		}
	
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		result.setRGB(0, 0, width, height, output, 0, width);
		return result;
	}

	/*Function to find the Maximum intensity of a grayscale image
		Input (BufferedImage): Original Grayscale Image
		Output (int): Maximum Intensity of input image
	*/
	public int max_intensity(BufferedImage img){
		int max = 0;
		
		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				int val = img.getRaster().getSample(p, q, 0);

				if (val > max){
					max = val;
				}
			}
		}
		return max;
	}

	/*Function to find the minimum intensity of a grayscale image
		Input (BufferedImage): Original Grayscale Image
		Output (int): minimum Intensity of input image
	*/
	public int min_intensity(BufferedImage img){
		int min = 255;
		
		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				int val = img.getRaster().getSample(p, q, 0);

				if (val < min){
					min = val;
				}
			}
		}
		return min;
	}

	public static void main(String[] args) {
		new SegmentImage(args.length==1 ? args[0] : "camera_man.png");
	}
}
