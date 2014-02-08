package me.drton.jmavsim;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.LayoutManager2;
import java.awt.event.ActionEvent;
import javax.media.j3d.Canvas3D;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class ControlFrame extends JFrame {
	/**
	 * Initial version of GUI
	 */
	private static final long serialVersionUID = 7943194804039419686L;
	JPanel contentPane;
	JPanel jPanel1 = new JPanel();
	LayoutManager2 layout1 = new BorderLayout();
	JButton autoRot_toggle = new JButton();
	JButton button2 = new JButton();

	protected Simulator sim;
	static boolean autoRotate = false;

	private boolean isFullScreen = false;

	public ControlFrame(Simulator sim) {
		super(GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getDefaultScreenDevice().getDefaultConfiguration());
		this.sim = sim;

		try {
			initFrame();
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("jMAVSim");
		this.pack();
		this.setVisible(true);
		// fullScreen(device);
	}

	public static void main(String[] args) {
		new ControlFrame(null);
	}

	private void initFrame() throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager
					.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		GraphicsEnvironment env = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		env.getDefaultScreenDevice();

		contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(layout1);
		autoRot_toggle.setText("autoRotate");
		autoRot_toggle.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jButton1_actionPerformed(e);
			}
		});
		contentPane.add(jPanel1, BorderLayout.SOUTH);
		jPanel1.setLayout(new FlowLayout());
		jPanel1.add(autoRot_toggle);

		button2.setText("button2");
		jPanel1.add(button2);

		Canvas3D c3d = sim.visualizer.getCanvas3D();
		System.out.println("c3d size: " + c3d.getSize().width + ", " + c3d.getSize().height);
		contentPane.add(c3d, BorderLayout.NORTH);
}

	public void fullScreen(GraphicsDevice device) {
		// isFullScreen = device.isFullScreenSupported();
		// setUndecorated(isFullScreen);
		// setResizable(!isFullScreen);
		if (isFullScreen) {
			// Full-screen mode
			// device.setFullScreenWindow(this);
			validate();
		} else {
			// Windowed mode
			pack();
			setVisible(true);
		}
	}

	void jButton1_actionPerformed(ActionEvent e) {
		System.out.println("button: " + e.getID());
		autoRotate = !sim.visualizer.isAutoRotate();
		sim.visualizer.setAutoRotate(autoRotate);
	}

}
