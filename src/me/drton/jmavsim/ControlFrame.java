package me.drton.jmavsim;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.LayoutManager2;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.media.j3d.Canvas3D;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.UIManager;

public class ControlFrame extends JFrame {
	/**
	 * Initial version of GUI
	 */
	private static final long serialVersionUID = 7943194804039419686L;
	JPanel contentPane;
	JPanel jPanel1 = new JPanel();
	LayoutManager2 layout1 = new BorderLayout();
    JToggleButton autoRotateButton = new JToggleButton();
    JToggleButton moveTargetButton = new JToggleButton();

	protected Simulator sim;
//	static boolean autoRotate = false;

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
        autoRotateButton.setText("autoRotate");
        autoRotateButton.setSelected(sim.visualizer.isAutoRotate());
        autoRotateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                autoRotateButton_actionPerformed(e);
            }
        });
        moveTargetButton.setText("moveTarget");
        moveTargetButton.setSelected(sim.target.isMove());
        moveTargetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                moveTargetButton_actionPerformed(e);
            }
        });
		contentPane.add(jPanel1, BorderLayout.SOUTH);
		jPanel1.setLayout(new FlowLayout());
        jPanel1.add(autoRotateButton);
        jPanel1.add(moveTargetButton);

		Canvas3D c3d = sim.visualizer.getCanvas3D();
		contentPane.add(c3d, BorderLayout.CENTER);
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

    void autoRotateButton_actionPerformed(ActionEvent e) {
        sim.visualizer.setAutoRotate(autoRotateButton.isSelected());
    }

    void moveTargetButton_actionPerformed(ActionEvent e) {
        sim.target.setMove(moveTargetButton.isSelected());
    }
}
