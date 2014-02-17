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
    JToggleButton fixedViewButton = new JToggleButton();
    JButton resetPosButton = new JButton();

	protected Simulator sim;

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
		// make sure we initialize to a consistent state
		this.fixedViewButton.setSelected(sim.fixedPilot);
		if (sim.fixedPilot) {
		      this.autoRotateButton.setSelected(true);
		      sim.visualizer.setAutoRotate(true);
		} else {
		      this.autoRotateButton.setSelected(false);
              sim.visualizer.setAutoRotate(false);
		}
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
        moveTargetButton.setSelected(sim.target.isApplyAccel());
        moveTargetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                moveTargetButton_actionPerformed(e);
            }
        });
        
        fixedViewButton.setText("fixedView");
        fixedViewButton.setSelected(sim.target.isApplyAccel());
        fixedViewButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fixedViewButton_actionPerformed(e);
            }
        });
        
        resetPosButton.setText("reset Position");
        resetPosButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetPosButton_actionPerformed(e);
            }
        });

        contentPane.add(jPanel1, BorderLayout.SOUTH);
		jPanel1.setLayout(new FlowLayout());
        jPanel1.add(autoRotateButton);
        jPanel1.add(moveTargetButton);
        jPanel1.add(fixedViewButton);
        jPanel1.add(resetPosButton);

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
        sim.target.setApplyAccel(moveTargetButton.isSelected());
    }

    void fixedViewButton_actionPerformed(ActionEvent e) {
        if (fixedViewButton.isSelected()) {
            sim.visualizer.setViewerTarget(sim.vehicle);
            autoRotateButton.setSelected(true);
            sim.visualizer.setAutoRotate(true);
            sim.visualizer.initPos();
            sim.visualizer.setViewerPosition(null);
        } else {
            sim.visualizer.setViewerTarget(sim.target);
            sim.visualizer.setViewerPosition(sim.vehicle);
        }
    }

    void resetPosButton_actionPerformed(ActionEvent e) {
        // NED frame
        sim.vehicle.getPosition().set(0,0,-5);
        sim.vehicle.getVelocity().set(0,0,0);
        sim.vehicle.getAcceleration().set(0,0,0);
        sim.target.getPosition().set(5, 0, -5);
    }
}
