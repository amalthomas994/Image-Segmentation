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
		if ( ((Button)e.getSource()).getLabel().equals("Watershed") )
			if ( metrics.getSelectedCheckbox().getLabel().equals("Gradient") )
		derivatives();
	}

	// moravec implementation
	void derivatives() {
		int l, r, dr, dg, db;
		Color clr1, clr2;

		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				l = p==0 ? p : p-1;
				r = p==width-1 ? p : p+1;
				clr1 = new Color(source.image.getRGB(l,q));
				clr2 = new Color(source.image.getRGB(r,q));
				dr = clr2.getRed() - clr1.getRed();
				dg = clr2.getGreen() - clr1.getGreen();
				db = clr2.getBlue() - clr1.getBlue();
				dr = Math.max(0, Math.min(dr+128, 255));
				dg = Math.max(0, Math.min(dg+128, 255));
				db = Math.max(0, Math.min(db+128, 255));
				target.image.setRGB(p, q, new Color(dr, dg, db).getRGB());
			}
		}
		target.repaint();
	}

	public static void main(String[] args) {
		new SegmentImage(args.length==1 ? args[0] : "camera_man.png");
	}
}
