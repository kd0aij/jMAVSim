package me.drton.jmavsim;

import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileOutputStream;
import java.io.IOException;
import static java.lang.System.out;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import javax.swing.JFrame;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;
import me.drton.jmavsim.vehicle.AbstractMulticopter;
import me.drton.jmavsim.vehicle.AbstractVehicle;
import me.drton.jmavsim.vehicle.Quadcopter;
import org.mavlink.messages.MAVLinkMessage;
import org.mavlink.messages.common.msg_global_position_int;
import org.mavlink.messages.common.msg_heartbeat;
import org.mavlink.messages.common.msg_hil_controls;
import org.mavlink.messages.common.msg_hil_gps;
import org.mavlink.messages.common.msg_hil_sensor;
import org.mavlink.messages.common.msg_statustext;

/**
 * User: ton Date: 26.11.13 Time: 12:33
 */
public class Simulator extends Thread {

    static private Logger logger;
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
    protected static boolean running = true;
    private World world;
    protected AbstractMulticopter vehicle;
    Visualizer visualizer;
    private MAVLinkPort apMavlinkPort;
    private MAVLinkPort gcsMavlinkPort;
    private boolean gotHeartBeat = false;
    private boolean inited = false;
    private int sysId = -1;
    private int componentId = -1;
    private final int sleepInterval = 10;
    private long nextRun = 0;
    private final long msgIntervalGPS = 200;
    private long msgLastGPS = 0;
    private long initTime = 0;
    private final long initDelay = 1000;
    protected Target target;
    protected boolean fixedPilot = false;
    private String mainViewTitle = new String();
    private String dbgViewTitle = new String();

    PerfCounterNano msg_hil_ctr;

    protected static void setRunning(boolean running) {
        Simulator.running = running;
    }

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

    public Simulator() throws IOException, InterruptedException {
        // Create world
        world = new World();
        // Create MAVLink connections
        MAVLinkConnection connHIL = new MAVLinkConnection(world);
        world.addObject(connHIL);
        MAVLinkConnection connCommon = new MAVLinkConnection(world);
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

        // Open ports
        serialMAVLinkPort.open(portName, 230400, 8, 1, 0);
        udpMavLinkPort.open(new InetSocketAddress(14555));
        apMavlinkPort = serialMAVLinkPort;
        gcsMavlinkPort = udpMavLinkPort;
        msg_hil_ctr = new PerfCounterNano(logger, "msg_hil", (long) 10e9);
        msg_hil_ctr.setHist_max(30e-3);  // 30 msec
        msg_hil_ctr.setHist_min(10e-3);  // 10 msec
        // construct GUI
        constructGUI(this);
    }

    public void run() {
        // main loop: run a realtime simulator integration step (world.update)
        nextRun = System.currentTimeMillis() + sleepInterval;
        while (running) {
            try {
                // run a simulation step
                long t = System.currentTimeMillis();

                long timeLeft = Math.max(sleepInterval / 4,
                        nextRun - System.currentTimeMillis());
                nextRun = Math.max(t + sleepInterval / 4, nextRun
                        + sleepInterval);

                final long minSleep = sleepInterval / 4;
                if (timeLeft <= minSleep) {
                    logger.log(Level.INFO,
                            String.format("sync slip: nextRun: %d, timeLeft: %d\n",
                                    nextRun, timeLeft));
                }

                Thread.sleep(timeLeft);
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        try {
            logger.log(Level.INFO, "closing MAVlink ports");
            // Close ports
            apMavlinkPort.close();
            gcsMavlinkPort.close();
            System.exit(0);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            System.exit(1);
        }
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

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                // construct GCS thread class
                Simulator sim;
                try {
                    sim = new Simulator();
                    // start main thread
                    sim.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    protected void constructGUI(Simulator sim) throws HeadlessException {
        // construct Simulator dialog
        cPanel = new ControlFrame(sim);
        cPanel.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        cPanel.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setRunning(false);
            }
        });
        setTitle();
        cPanel.setBounds(100, 100, cPanel.getWidth(), cPanel.getHeight());
        cPanel.setVisible(true);
    }
}
