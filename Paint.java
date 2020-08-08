import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.imageio.*;

import java.awt.*;


public class Paint extends JPanel implements MouseMotionListener, ActionListener, MouseListener, ChangeListener
{ 
	
	JFrame frame;
	JMenuBar bar;
	JMenu menu, file;
	JMenuItem save, load;
	JButton[] colorOptions;
	JScrollBar penWidth;
	JColorChooser colorChooser;
	JFileChooser fileChooser;
	Color[] colors;
	Color currentColor;
	JButton lineButton = new JButton("");
	JButton rectButton = new JButton("");
	JButton undo = new JButton();
	JButton redo = new JButton();
	private ArrayList<Point> points;
	private ArrayList<ArrayList<Point>> lines;
	private ArrayList<Shape> shapes;
	private Stack<ArrayList<Point>> undoLines;
	private Stack<Shape> undoShapes;
	private Stack<String> commandOrder;
	private Stack<String> undoCommandOrder;
	
	ImageIcon lineImage, rectImage, undoImage, redoImage;
	BufferedImage loadedImage;
	boolean freeLineOn = true, rectangleOn = false;
	int currX = 0, currY = 0, currWidth = 0, currHeight =0;
	boolean first = true;
	String lastRemovedCommand;
	Shape currShape; // = new Shape(currHeight, currWidth, currX, currY, currentColor, 10);
	
	public Paint()
	{
		shapes = new ArrayList<Shape>();
		lines = new ArrayList<ArrayList<Point>>(); 
		points = new ArrayList<Point>();
		frame = new JFrame("Paint");
		frame.add(this);
		bar = new JMenuBar();
		menu = new JMenu("Colors");
		file = new JMenu("File");
		save = new JMenuItem("Save");
		load = new JMenuItem("Load");
		save.addActionListener(this);
		load.addActionListener(this);
		file.add(save);
		file.add(load);
		colorOptions = new JButton[5];
		colors = new Color[] {Color.RED, Color.YELLOW, Color.BLUE, Color.GREEN, Color.BLACK};
		currentColor = colors[0];
		menu.setLayout(new GridLayout(1,7));
		
		for(int x=0;x<5;x++)
		{
			
			colorOptions[x] = new JButton();
			colorOptions[x].addActionListener(this);
			colorOptions[x].putClientProperty("colorIndex",x);
			colorOptions[x].setBackground(colors[x]);
			menu.add(colorOptions[x]);
		}
		colorChooser = new JColorChooser();
		colorChooser.getSelectionModel().addChangeListener(this);
		menu.add(colorChooser);
		bar.add(file);
		bar.add(menu);
		String currDir = System.getProperty("user.dir");
		fileChooser = new JFileChooser(currDir);
		
		lineImage = new ImageIcon("line.png");
		lineImage = new ImageIcon(lineImage.getImage().getScaledInstance(20,20,Image.SCALE_SMOOTH));
		lineButton.setIcon(lineImage);
		lineButton.setFocusPainted(false);
		
		rectImage = new ImageIcon("rectangle.png");
		rectImage = new ImageIcon(rectImage.getImage().getScaledInstance(20,20,Image.SCALE_SMOOTH));
		rectButton.setIcon(rectImage);
		lineButton.setFocusPainted(false);
		
		undoImage = new ImageIcon("undo.png");
		undoImage = new ImageIcon(undoImage.getImage().getScaledInstance(20,20,Image.SCALE_SMOOTH));
		undo.setIcon(undoImage);
		
		redoImage = new ImageIcon("redo.png");
		redoImage = new ImageIcon(redoImage.getImage().getScaledInstance(20,20,Image.SCALE_SMOOTH));
		redo.setIcon(redoImage);
		
		bar.add(lineButton);
		bar.add(rectButton);
		bar.add(undo);
		bar.add(redo);
		penWidth = new JScrollBar(JScrollBar.HORIZONTAL,1,0,1,10);
		bar.add(penWidth); 
		
		this.addMouseMotionListener(this);
		this.addMouseListener(this);
		rectButton.addActionListener(this);
		lineButton.addActionListener(this);
		frame.add(bar,BorderLayout.NORTH);
		frame.setSize(1000,600);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g); 
		Graphics2D g2 = (Graphics2D)g;
		g.setColor(Color.WHITE);
		g.fillRect(0,0,frame.getWidth(),frame.getHeight());
		
		for(ArrayList<Point> line:lines)
		{
			for(int x =0; x< line.size()-1;x++)
			{
				Point p1 = line.get(x);
				Point p2 = line.get(x+1);
				g.setColor(p1.getColor());
				g2.setStroke(new BasicStroke(p1.getPenWidth()));
				//g.fillOval(p.getX(),p.getY(),4,4);
				g.drawLine(p1.getX(), p1.getY(),p2.getX(),p2.getY());
			}
		}
		
		for(Shape s : shapes)
		{
			g.setColor(s.getColor());
			g2.setStroke(new BasicStroke(s.getPenWidth()));
			
			if(s instanceof Block)
			{
				g2.draw(((Block)s).getRect());
			}
		}
		if(freeLineOn == true)
		{

			for(int x =0; x< points.size()-1;x++)
			{
				Point p1 = points.get(x);
				Point p2 = points.get(x+1);
				g.setColor(p1.getColor());
				g2.setStroke(new BasicStroke(p1.getPenWidth()));
				g.drawLine(p1.getX(), p1.getY(),p2.getX(),p2.getY());
			}
			
		}	
		
	}
	
	public void actionPerformed(ActionEvent event)
	{
		for(int x=0;x<colorOptions.length;x++)
		{
			if(event.getSource()==colorOptions[x])
			{
				//int index = Integer.parseInt(""+((JButton)event.getSource()).getClientProperty("colorIndex"));
				currentColor = colors[x];
			}
		}
		if(event.getSource() == lineButton)
		{
			freeLineOn = true;
			rectangleOn = false;
			lineButton.setBackground(Color.LIGHT_GRAY);
			rectButton.setBackground(null);
		}
		if(event.getSource() == rectButton)
		{
			freeLineOn = false;
			rectangleOn = true;
			lineButton.setBackground(null);
			rectButton.setBackground(Color.LIGHT_GRAY);
		}
		
		if(event.getSource() == undo)
		{
			if(commandOrder.size()>0)
			{
				String lastCommand = commandOrder.pop();
				undoCommandOrder.push(lastCommand);
				if(lastCommand.equals("freeLine"));
				{
					if(lines.size()>0)
					{
						undoLines.push(lines.remove(lines.size()-1));
						repaint();
					}
				}
				
				if(lastCommand.equals("shape"))
				{
					if(shapes.size()>0)
					{
						undoShapes.push(shapes.remove(shapes.size()-1));
						repaint();
					}
				}
			}
		}
		
		if(event.getSource() == redo)
		{
			if(undoCommandOrder.size()>0)
			{
				if(undoCommandOrder.size()>0)
				{
					lastRemovedCommand = undoCommandOrder.pop();
					commandOrder.push(lastRemovedCommand);
					if(lastRemovedCommand.equals("freeLine"))
					{
						if(undoLines.size()>0)
						{
							lines.add(undoLines.pop());
							repaint();
						}
					}
					if(lastRemovedCommand.equals("shape"))
					{
						if(undoShapes.size()>0)
						{
							undoShapes.push(shapes.remove(shapes.size()-1));
							repaint();
						}
					}
				}
			}
		}
		
		if(event.getSource() == save)
		{
			FileFilter filter = new FileNameExtensionFilter("*.png","png");
			fileChooser.setFileFilter(filter);
			if(fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION);
			{
				File file = fileChooser.getSelectedFile();
				try {
					ImageIO.write(createImage(),"png",new File(file.getAbsolutePath()+".png"));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		if(event.getSource() == load)
		{
			fileChooser.showSaveDialog(null);
			File imgFile = fileChooser.getSelectedFile();
			
			try {
				loadedImage = ImageIO.read(imgFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			lines = new ArrayList<ArrayList<Point>>();
			undoLines = new Stack<ArrayList<Point>>();
			points = new ArrayList<Point>();
			shapes = new ArrayList<Shape>();
			undoShapes = new Stack<Shape>();
			commandOrder = new Stack<String>();
			undoCommandOrder = new Stack<String>();
			repaint();
			
		}
		
		
	}
	
	@Override
	public void mouseDragged(MouseEvent event) 
	{
		if(freeLineOn)
			points.add(new Point(event.getX(), event.getY(), currentColor, (int)penWidth.getValue()));
		
		if(rectangleOn)
		{
			if(first)
			{
				currX = event.getX();
				currY = event.getY();
				currShape = new Block(currX, currY, 0, 0, currentColor,(int)penWidth.getValue());
				first = false;
				shapes.add(currShape);
			}
			else
			{
				currWidth = Math.abs(event.getX()-currX);
				currHeight = Math.abs(event.getY()-currY);
				currShape.setWidth(currWidth);
				currShape.setHeight(currHeight);
				
				if(currX<=event.getX() && currY>=event.getY())
				{
					currShape.setY(event.getY());                     //DOESNT LOOK THAT SMOOTH
				}
				else if(currX<=event.getX() && currY>=event.getY())
				{
					currShape.setX(event.getX());
				}
				else if(currX>=event.getX() && currY<=event.getY())
				{
					currShape.setX(event.getX());
					currShape.setY(event.getY());
				}
				repaint();  
			}
		}
		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent arg0) 
	{
		// TODO Auto-generated method stub
		
	}
	
	public static void main (String args[])
	{
		Paint app = new Paint();
	}
	
	
	public class Point
	{
		int x,y;
		Color color;
		int penWidth;
		public Point(int x, int y, Color color, int penWidth)
		{
			this.x = x;
			this.y = y;
			this.color = color;
			this.penWidth = penWidth;
		}
		
		public int getX()
		{
			return x;
		}
		public int getY()
		{
			return y;
		}
		public Color getColor()
		{
			return color;
		}
		public int getPenWidth()
		{
			return penWidth;
		}
	}

	
	public class Shape
	{
		private int x;
		private int y;
		private int width;
		private int height;
		private Color color;
		private int penWidth;
		
		public Shape(int x, int y, int width, int height, Color color, int penWidth)
		{
			this.x = x;
			this.y = y;
			this.penWidth = penWidth;
			this.height = height;
			this.width = width;
			this.color = color;
		}
		
		public int getX()
		{
			return x;
		}
		public void setX(int val)
		{
			x = val;
		}
		
		public int getY()
		{
			return y;
		}
		public void setY(int val)
		{
			y = val;
		}
		
		public int getWidth()
		{
			return width;
		}
		
		public int getHeight()
		{
			return height;
		}
		
		public int getPenWidth()
		{
			return penWidth;
		}
		
		public Color getColor()
		{
			return color;
		}
		
		public void setHeight(int h)
		{
			height = h;
		}
		
		public void setWidth(int w)
		{
			width = w;
		}
	
	}
	
	
	public class Block extends Shape
	{

		public Block(int x, int y, int width, int height, Color color, int penWidth) 
		{
			super(x, y, width, height, color, penWidth);
		}
		
		public Rectangle getRect()
		{
			return new Rectangle(getX(), getY(), getWidth(), getHeight());
		}
		
	}
	
	public BufferedImage createImage()
	{
		int width = this.getWidth();
		int height = this.getHeight();
		BufferedImage img = new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = img.createGraphics();
		this.paint(g2);
		g2.dispose();
		return img;
	}
	
	
	

	@Override
	public void mouseReleased(MouseEvent arg0) 
	{
		if(freeLineOn)
		{
			lines.add(points);
			points = new ArrayList<Point>();
			//commandOrder.push(lastRemovedCommand);
			//commandOrder.push("freeLine");
		}
		if(rectangleOn)
		{
			commandOrder.push("shapes");
			first = true;
			//shapes.add(currShape);
		}
		repaint();	
	}
	
	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mousePressed(MouseEvent arg0) 
	{
		first = true;	
	}


	@Override
	public void stateChanged(ChangeEvent arg0) 
	{
		currentColor = colorChooser.getColor();
		
	}

}




