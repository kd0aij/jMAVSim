package me.drton.jmavsim;

import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileOutputStream;
import java.io.IOException;
import static java.lang.System.out;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import javax.swing.JFrame;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;
import me.drton.jmavsim.vehicle.AbstractMulticopter;
import me.drton.jmavsim.vehicle.Quadcopter;

/**
 * User: ton Date: 26.11.13 Time: 12:33
 */
public class Simulator {

    static public final Logger logger;
    static boolean append = true;

    static {
        logger = Logger.getLogger("Simulator");
        Handler[] handler = logger.getParent().getHandlers();
        handler[0].setFormatter(new BriefFormatter());
        try {
            String logFileName = FileUtils.getLogFileName("log", "simulator", append);
            StreamHandler logFileHandler = new StreamHandler(
                    new FileOutputStream(logFileName, append), new BriefFormatter());
            out.println("logfile: " + logFileName);
            logFileHandler.setFormatter(new BriefFormatter());
            logger.addHandler(logFileHandler);
            logger.log(Level.INFO, "\nSimulator starting: ".concat((new Date()).toString()));
        } catch (SecurityException e) {
            out.println("security exception creating logger");
            System.exit(1);
        } catch (IOException e) {
            out.println("IO exception creating logger");
            System.exit(1);
        }
    }

    protected static ControlFrame cPanel = null;
    private static String portName;
    private final World world;
    protected AbstractMulticopter vehicle;
    Visualizer visualizer;
    MAVLinkConnection connHIL;
    MAVLinkConnection connCommon;
    private static MAVLinkPort apMavlinkPort;
    private static MAVLinkPort gcsMavlinkPort;
    protected Target target;
    protected boolean fixedPilot = false;
    private String mainViewTitle = new String();
    private String dbgViewTitle = new String();

    protected void setDbgTitle() {
        dbgViewTitle = "\t\trightView: ".concat(this.visualizer.getDbgViewType().name());
    }

    protected void setMainTitle() {
        String cameraLoc;
        String auto;

        if (fixedPilot) {
            cameraLoc = "fixed view";
        } else {
            cameraLoc = "cockpit view";
        }
        if (this.visualizer.isAutoRotate()) {
            auto = "autoRotate";
        } else {
            auto = "straight ahead";
        }
        mainViewTitle = String.format("\t\t%s, %s", cameraLoc, auto);
    }

    protected void setTitle() {
        setMainTitle();
        setDbgTitle();
        cPanel.setTitle("jMAVSim".concat(mainViewTitle).concat(dbgViewTitle));
    }

    public Simulator() throws IOException {
        // Create world
        world = new World();
        // Create MAVLink connections
        connHIL = new MAVLinkConnection(world);
        world.addObject(connHIL);
        connCommon = new MAVLinkConnection(world);
        world.addObject(connCommon);
        // Create and ports
        SerialMAVLinkPort serialMAVLinkPort = new SerialMAVLinkPort();
        connCommon.addNode(serialMAVLinkPort);
        connHIL.addNode(serialMAVLinkPort);
        UDPMavLinkPort udpMavLinkPort = new UDPMavLinkPort();
        connCommon.addNode(udpMavLinkPort);
        // Create environment
        SimpleEnvironment simpleEnvironment = new SimpleEnvironment(world);
        simpleEnvironment.setMagField(new Vector3d(0.2f, 0.0f, 0.5f));
        // simpleEnvironment.setWind(new Vector3d(0.0, 5.0, 0.0));
        simpleEnvironment.setGroundLevel(0.0f);
        world.addObject(simpleEnvironment);
        // Create vehicle with sensors
        Vector3d gc = new Vector3d(0.0, 0.0, 0.0); // gravity center
        vehicle = new Quadcopter(world,
                "models/3dr_arducopter_quad_x.obj", "x", 0.33 / 2, 4.0, 0.05,
                0.005, gc);
        vehicle.setMass(0.8);
        Matrix3d I = new Matrix3d();
        // Moments of inertia
        I.m00 = 0.005;  // X
        I.m11 = 0.005;  // Y
        I.m22 = 0.009;  // Z
        vehicle.setMomentOfInertia(I);
        SimpleSensors sensors = new SimpleSensors();
        sensors.initGPS(55.753395, 37.625427);
        vehicle.setSensors(sensors);
        vehicle.setDragMove(0.02);
        vehicle.getPosition().set(0, 0, 0);
        // vehicle.setDragRotate(0.1);
        connHIL.addNode(new MAVLinkHILSystem(10, 0, vehicle));
        world.addObject(vehicle);
        target = new Target(world, 0.3);
// target mass is calculated in its constructor
//        target.setMass(90.0);
        target.initGPS(55.753395, 37.625427);
        target.getPosition().set(0, 0.1, -5);
        connCommon.addNode(new MAVLinkTargetSystem(2, 0, target));
        world.addObject(target);

        // Create visualizer
        visualizer = new Visualizer(world, this);

        // If ViewerPosition is not set to an object, it is fixed
        // In that case, autorotate should default to on and the ViewerTarget
        // should be the vehicle.
        // Two options desired: fixed/autorotate to (target) vehicle
        // and on vehicle, with or without autorotate to (target) target
        if (fixedPilot) {
            visualizer.setViewerTarget(vehicle);
        } else {
            visualizer.setViewerTarget(target);
            visualizer.setViewerPosition(vehicle);
        }
        visualizer.setDbgViewerPosition(vehicle);

        // construct Simulator dialog
        cPanel = new ControlFrame(this);
        cPanel.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        cPanel.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    apMavlinkPort.close();
                    gcsMavlinkPort.close();
                    System.exit(0);
                } catch (IOException ex) {
                    Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
                    System.exit(1);
                }
            }
        });
        setTitle();
        cPanel.setBounds(100, 100, cPanel.getWidth(), cPanel.getHeight());

        // Open ports
        serialMAVLinkPort.open(portName, 230400, 8, 1, 0);
        udpMavLinkPort.open(new InetSocketAddress(14555));
        apMavlinkPort = serialMAVLinkPort;
        gcsMavlinkPort = udpMavLinkPort;

        // place a task on the AWT event queue to display Swing window
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                cPanel.setVisible(true);
            }
        });

    }

    public static void main(String[] args) {
        // parse command-line args
        portName = null;
        String osname = System.getProperty("os.name", "").toLowerCase();
        if (args.length == 0) {
            if (osname.startsWith("windows")) {
                // windows
                portName = "COM6";
            } else if (osname.startsWith("linux")) {
                // linux
                portName = "/dev/ttyACM0";
            } else if (osname.startsWith("mac")) {
                // mac
                portName = "/dev/tty.usbmodem1";
            } else {
                logger.log(Level.INFO, "Sorry, your operating system is not supported");
                System.exit(1);
            }
        } else if (args.length >= 1) {
            portName = args[0];
            logger.log(Level.INFO, "serial port: " + args[0]);
        } else {
            logger.log(Level.INFO, "Usage: java Simulator serialPort");
            logger.log(Level.INFO, "Defaulting serial port to " + portName);
        }
        logger.log(Level.INFO, "OS type: ".concat(osname));
        logger.log(Level.INFO, "Using serial port: ".concat(portName));

        // start up 
        try {
            Simulator sim = new Simulator();
        } catch (IOException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
