package me.drton.jmavsim;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.media.j3d.Canvas3D;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.UIManager;

public class ControlFrame extends JFrame {

    /**
     * Initial version of GUI
     */
    private static final long serialVersionUID = 7943194804039419686L;
    JPanel contentPane;
    JPanel mainPanel = new JPanel();
    JPanel dbgPanel = new JPanel();
    // // use a separate JFrame for the heavyweight Canvas3Ds
    // JFrame heavyPanel = new JFrame();
    JRadioButtonMenuItem autoRotateRadioButton;
    JRadioButtonMenuItem fixedViewRadioButton;
    JRadioButtonMenuItem moveTargetRadioButton;

    protected final Simulator sim;

    public ControlFrame(final Simulator sim) {
        super(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration());
        this.sim = sim;

        try {
            initFrame();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // make sure we initialize to a consistent state
        this.fixedViewRadioButton.setSelected(sim.fixedPilot);
        if (sim.fixedPilot) {
            this.autoRotateRadioButton.setSelected(true);
            sim.visualizer.setAutoRotate(true);
        } else {
            this.autoRotateRadioButton.setSelected(false);
            sim.visualizer.setAutoRotate(false);
        }
        this.pack();
        this.setVisible(true);
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
        GraphicsDevice device = env.getDefaultScreenDevice();
        Toolkit tkit = java.awt.Toolkit.getDefaultToolkit();
        Dimension sdm = tkit.getScreenSize();
        Insets insets = tkit.getScreenInsets(device.getDefaultConfiguration());
        int swidth = sdm.width - insets.left;
        if (swidth < 1920)
            this.setPreferredSize(new Dimension(swidth, swidth / 2));
        else
            this.setPreferredSize(new Dimension(1024, 512));

        contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout(new BorderLayout());

        Canvas3D mainCanvas = sim.visualizer.getMainCamera().getCanvas3D();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(mainCanvas, BorderLayout.CENTER);

        Canvas3D dbgCanvas = sim.visualizer.getDbgCamera().getCanvas3D();
        dbgPanel.setLayout(new BorderLayout());
        dbgPanel.add(dbgCanvas, BorderLayout.CENTER);

        // doesn't work in 6.0_45, but does with open jdk 6 and 7
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        JMenuBar menubar = new JMenuBar();
        this.setJMenuBar(menubar);

        JMenu viewControl = new JMenu("View");
        menubar.add(viewControl);
        autoRotateRadioButton = new JRadioButtonMenuItem("autoRotate");
        viewControl.add(autoRotateRadioButton);
        autoRotateRadioButton
                .addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        sim.visualizer.setAutoRotate(autoRotateRadioButton
                                .isSelected());
                        sim.setTitle();
                    }
                });

        fixedViewRadioButton = new JRadioButtonMenuItem("fixedView");
        viewControl.add(fixedViewRadioButton);
        fixedViewRadioButton
                .addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (fixedViewRadioButton.isSelected()) {
                            sim.fixedPilot = true;
                            sim.visualizer.setViewerTarget(sim.vehicle);
                            // autoRotateButton.setSelected(true);
                            // sim.visualizer.setAutoRotate(true);
                            sim.visualizer.initPos();
                            sim.visualizer.setViewerPosition(null);
                        } else {
                            sim.fixedPilot = false;
                            sim.visualizer.setViewerTarget(sim.target);
                            sim.visualizer.setViewerPosition(sim.vehicle);
                        }
                        sim.setTitle();
                    }
                });

        JMenu auxViewControl = new JMenu("Aux View");
        menubar.add(auxViewControl);
        final ButtonGroup auxViewGroup = new ButtonGroup();
        ActionListener viewControlListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (Visualizer.viewTypes viewType : Visualizer.viewTypes.values()) {
                    String vname = auxViewGroup.getSelection().getActionCommand();
                    if (vname.equals(viewType.name())) {
                        sim.visualizer.setDbgViewType(viewType);
                        sim.setTitle();
//                        System.out.println("viewType: " + viewType);
                    }
                }
            }
        };
        for (Visualizer.viewTypes viewType : Visualizer.viewTypes.values()) {
            String name = viewType.name();
            JRadioButtonMenuItem newRB = new JRadioButtonMenuItem(name);
            newRB.setActionCommand(name);
            newRB.addActionListener(viewControlListener);
            if (viewType == sim.visualizer.dbgViewType) {
                newRB.setSelected(true);
            }
            auxViewControl.add(newRB);
            auxViewGroup.add(newRB);
        }

        JMenu targetControl = new JMenu("Target");
        menubar.add(targetControl);
        moveTargetRadioButton = new JRadioButtonMenuItem("move Target");
        targetControl.add(moveTargetRadioButton);
        moveTargetRadioButton
                .addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        sim.target.setApplyAccel(moveTargetRadioButton
                                .isSelected());
                    }
                });

        JMenu reset = new JMenu("Reset");
        menubar.add(reset);
        JMenuItem resetPosition = new JMenuItem("position");
        reset.add(resetPosition);
        resetPosition.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand() == "position") {
                    // NED frame
                    sim.vehicle.getPosition().set(0, 0, -5);
                    sim.vehicle.getVelocity().set(0, 0, 0);
                    sim.vehicle.getAcceleration().set(0, 0, 0);
                    sim.target.getPosition().set(5, 0, -5);
                }
            }
        });

        contentPane.setLayout(new GridLayout(1, 2, 2, 0));
        contentPane.setBackground(Color.black);
        contentPane.add(mainPanel);
        contentPane.add(dbgPanel);
    }
}
