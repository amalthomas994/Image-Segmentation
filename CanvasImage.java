import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;

// Canvas for image display
class CanvasImage extends Canvas {
	BufferedImage image;
	int border=16;

	// initialize the image and mouse control
	public CanvasImage(BufferedImage input) {
		image = input;
		setPreferredSize(new Dimension(image.getWidth()+border, image.getHeight()+border));
		addMouseListener(new ClickListener());
	}
	public CanvasImage(int width, int height) {
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		setPreferredSize(new Dimension(width+border, height+border));
		addMouseListener(new ClickListener());
	}

	// redraw the canvas
	public void paint(Graphics g) {
		// draw boundary
		g.setColor(Color.gray);
		g.drawRect(1, 1, getWidth()-2, getHeight()-2);
		// compute the offset of the image.
		int xoffset = (getWidth() - image.getWidth()) / 2;
		int yoffset = (getHeight() - image.getHeight()) / 2;
		g.drawImage(image, xoffset, yoffset, this);
	}

	// change link to image
	public void resetImage(BufferedImage input) {
		image = input;
		setPreferredSize(new Dimension(image.getWidth()+border, image.getHeight()+border));
		repaint();
	}
	// reset an empty image
	public void resetBuffer(int width, int height) {
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		setPreferredSize(new Dimension(width+border, height+border));
		repaint();
	}
	// reset image based on the input
	public void copyImage(BufferedImage input) {
		Graphics2D g2D = image.createGraphics();
		g2D.drawImage(input, 0, 0, null);
		repaint();
	}

	// listen to mouse click
	class ClickListener extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			if ( e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON3 )
				try {
					ImageIO.write(image, "png", new File("saved.png"));
				} catch ( Exception ex ) {
					ex.printStackTrace();
				}
		}
	}
}
