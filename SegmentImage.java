import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;

// Main class
public class SegmentImage extends Frame implements ActionListener {
	BufferedImage input;
	int width, height;
	int numK=4;
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
		BufferedImage src_image = source.image;
		int pixel_size = src_image.getColorModel().getPixelSize();

		if ( ((Button)e.getSource()).getLabel().equals("Watershed") ) {
			if ( metrics.getSelectedCheckbox().getLabel().equals("Intensity") ) {
				if (pixel_size == 24){
					src_image = grayscale(src_image);
				}
				target.resetImage(intensity_watershed(src_image));
				// int max_height = max_intensity(src_image);
				// int min_height = min_intensity(src_image);

			}
			if ( metrics.getSelectedCheckbox().getLabel().equals("Gradient") ) {
				target.resetImage(derivatives(src_image));
			}
		}
		if ( ((Button)e.getSource()).getLabel().equals("K-mean clustering") ) {
			System.out.println("HUH");
			target.resetImage(k_means(src_image, numK));
		}
	}

	// moravec implementation
	public BufferedImage derivatives(BufferedImage img) {
		int l, r, dr, dg, db;
		Color clr1, clr2;
		BufferedImage output = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				l = p==0 ? p : p-1;
				r = p==width-1 ? p : p+1;
				clr1 = new Color(img.getRGB(l,q));
				clr2 = new Color(img.getRGB(r,q));
				dr = clr2.getRed() - clr1.getRed();
				dg = clr2.getGreen() - clr1.getGreen();
				db = clr2.getBlue() - clr1.getBlue();
				dr = Math.max(0, Math.min(dr+128, 255));
				dg = Math.max(0, Math.min(dg+128, 255));
				db = Math.max(0, Math.min(db+128, 255));
				output.setRGB(p, q, new Color(dr, dg, db).getRGB());
			}
		}
		return output;
	}

	public BufferedImage intensity_watershed(BufferedImage img){
		BufferedImage output_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int l, r, intensity;
		int left, middle, right, up, down, up_left, up_right, down_left, down_right, red = 0, green = 0, blue = 0;
		
		int max_height = max_intensity(img);
		int min_height = min_intensity(img);
		
		Color black = new Color(0, 0, 0);
		int black_color = black.getRGB();
		Color region1 = new Color(255, 0, 0);
		int region1_color = region1.getRGB();

		for (int i = min_height; i < max_height; i++){
			for ( int q=0 ; q<height ; q++ ) {
				for ( int p=0 ; p<width ; p++ ) {
					intensity = img.getRaster().getSample(p, q, 0);
					int hsv = Color.HSBtoRGB(intensity/255f , 1f, 1f);
					output_image.setRGB(p, q, hsv);
					// if (intensity == i){
					// left = p==0 ? p : p-1;
					// middle = p;
					// right = p==width-1 ? p : p+1;
					// up = q==0 ? q : q-1;
					// down = q==height-1 ? q : q+1;

					// int grad_mag_left = output_image.getRGB(left, q);
					// int grad_mag_middle = output_image.getRGB(middle, q);
					// int grad_mag_right = output_image.getRGB(right, q);
					// int grad_mag_up = output_image.getRGB(p, up);
					// int grad_mag_down = output_image.getRGB(p, down);
					// int grad_mag_up_left = output_image.getRGB(left, up);
					// int grad_mag_up_right = output_image.getRGB(right, up);
					// int grad_mag_down_left = output_image.getRGB(left, down);
					// int grad_mag_down_right = output_image.getRGB(right, down);
					
					// if (grad_mag_left == black_color && 
					// 	grad_mag_right == black_color && 
					// 	grad_mag_up == black_color && 
					// 	grad_mag_down == black_color &&
					// 	grad_mag_up_left == black_color && 
					// 	grad_mag_up_right == black_color && 
					// 	grad_mag_down_left == black_color && 
					// 	grad_mag_down_right == black_color){
					// 		Color n_c = new Color(255, 0, 0);
							
					// 		output_image.setRGB(p, q, n_c.getRGB());
					// 	}
					// else if(grad_mag_left != black_color || 
					// 	grad_mag_right != black_color || 
					// 	grad_mag_up != black_color || 
					// 	grad_mag_down != black_color ||
					// 	grad_mag_up_left != black_color || 
					// 	grad_mag_up_right != black_color || 
					// 	grad_mag_down_left != black_color || 
					// 	grad_mag_down_right != black_color){
					// 		Color n_c = new Color(0, 255, 0);
							
					// 		output_image.setRGB(p, q, n_c.getRGB());
					// }else{
					// 	Color n_c = new Color(0, 0, 255);
							
					// 		output_image.setRGB(p, q, n_c.getRGB());
					// }
					// // intensity = img.getRaster().getSample(p, q, 0);
					// }
				}
			}
		}
		return output_image;
	}

	public BufferedImage k_means(BufferedImage img, int k){
		System.out.println(k);
		BufferedImage output_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = output_image.createGraphics();
		g2d.drawImage(img, 0, 0, null);
		g2d.dispose();
		int x_bounds = width;
		int y_bounds = height;
		int clusters[][] = new int[k][2];
		int cluster_color[][] = new int[k][3];

		int closest_distance = 20000000;
		int closest_k = 0;
		String closest_cluster = "";
		for (int i = 0; i < k; i++){
			int clr_val = (int) (Math.random()*255d);
			// Color clr = new Color((int) (Math.random()*255d), (int) (Math.random()*255d), (int) (Math.random()*255d));
			Color clr = new Color(clr_val, clr_val, clr_val);
			
			cluster_color[i][0] = clr.getRed();
			cluster_color[i][1] = clr.getGreen();
			cluster_color[i][2] = clr.getBlue();


		}
		for (int i = 0; i < k; i++){
			//x coordinate
			clusters[i][0] = (int) (Math.random() * x_bounds);
			//y coordinate
			clusters[i][1] = (int) (Math.random() * y_bounds);
			// System.out.println(cluster_color[i]);

			output_image.setRGB(clusters[i][0], clusters[i][1], new Color(255, 0, 0).getRGB());
		}
		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				for (int i = 0; i < k; i++){
					Color img_color = new Color(img.getRGB(p,q));
					int img_color_r = img_color.getRed();
					int img_color_g = img_color.getGreen();
					int img_color_b = img_color.getBlue();

					int cls_r = cluster_color[i][0];
					int cls_g = cluster_color[i][1];
					int cls_b = cluster_color[i][2];


					int distance = Math.abs(img_color_r - cls_r) + Math.abs(img_color_g - cls_g) + Math.abs(img_color_b - cls_b);
					// distance = Math.abs(distance);
					if (distance < closest_distance){
						closest_distance = distance;
						// closest_cluster = "K: " + k + " x: " + x + " y: " + y;
						closest_k = i;
					}
					// System.out.println(closest_distance);
					// System.out.println(closest_cluster);
				}
				output_image.setRGB(p,q, new Color(cluster_color[closest_k][0], cluster_color[closest_k][1], cluster_color[closest_k][2]).getRGB());
				closest_distance = 2000000;
				closest_k = 0;
				
			}
		}

		for (int i = 0; i < k; i++){
			// //x coordinate
			// clusters[i][0] = (int) (Math.random() * x_bounds);
			// //y coordinate
			// clusters[i][1] = (int) (Math.random() * y_bounds);
			// // System.out.println(cluster_color[i]);

			output_image.setRGB(clusters[i][0], clusters[i][1], new Color(255, 0, 0).getRGB());
		}
		// System.out.println(closest_distance);
		// System.out.println(closest_cluster);

		// print_(clusters);
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
				int red = (int) (c.getRed() * 0.299f);
				int green = (int) (c.getGreen() * 0.587f);
				int blue = (int) (c.getBlue() * 0.114f);
				Color newColor = new Color(
						red + green + blue,
						red + green + blue,
						red + green + blue);
				grayscaleImage.setRGB(j, i, newColor.getRGB());
			}
		}
		return grayscaleImage;
	}

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

	public void print_(Object obj){
		System.out.println(obj);
	}
	public static void main(String[] args) {
		new SegmentImage(args.length==1 ? args[0] : "cells.png");
	}
}
