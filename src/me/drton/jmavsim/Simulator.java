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
    protected AbstractVehicle vehicle;
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
        // Create environment
        SimpleEnvironment simpleEnvironment = new SimpleEnvironment(world);
        simpleEnvironment.setMagField(new Vector3d(0.2f, 0.0f, 0.5f));
        // simpleEnvironment.setWind(new Vector3d(0.0, 5.0, 0.0));
        simpleEnvironment.setGroundLevel(0.0f);
        world.addObject(simpleEnvironment);
        // Create vehicle with sensors
        Vector3d gc = new Vector3d(0.0, 0.0, 0.0); // gravity center
        AbstractMulticopter v = new Quadcopter(world,
                "models/3dr_arducopter_quad_x.obj", "x", 0.33 / 2, 4.0, 0.05,
                0.005, gc);
        v.setMass(0.8);
        Matrix3d I = new Matrix3d();
        // Moments of inertia
        I.m00 = 0.005; // X
        I.m11 = 0.005; // Y
        I.m22 = 0.009; // Z
        v.setMomentOfInertia(I);
        SimpleSensors sensors = new SimpleSensors();
        sensors.initGPS(55.753395, 37.625427);
        v.setSensors(sensors);
        v.setDragMove(0.02);
        v.getPosition().set(0, 0, 0);
        // v.setDragRotate(0.1);
        vehicle = v;
        world.addObject(v);
        target = new Target(world, 0.3);
        target.initGPS(55.753395, 37.625427);
        target.getPosition().set(0, 0.1, -5);
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

        // Create and open port
        gotHeartBeat = false;
        inited = false;
        try {
            SerialMAVLinkPort serialMAVLinkPort = new SerialMAVLinkPort();
            serialMAVLinkPort.open(portName, 230400, 8, 1, 0);
            UDPMavLinkPort udpMavLinkPort = new UDPMavLinkPort();
            udpMavLinkPort.open(new InetSocketAddress(14555));
            apMavlinkPort = serialMAVLinkPort;
            gcsMavlinkPort = udpMavLinkPort;
            msg_hil_ctr = new PerfCounterNano(logger, "msg_hil", (long) 10e9);
            msg_hil_ctr.setHist_max(30e-3);  // 30 msec
            msg_hil_ctr.setHist_min(10e-3);  // 10 msec
        } catch (IOException e) {
            logger.log(Level.SEVERE, "open error: ".concat(e.toString()));
            System.exit(1);
        }
        // construct GUI
        constructGUI(this);
    }

    private void initMavLink() throws IOException {
        // Set HIL mode
        org.mavlink.messages.common.msg_set_mode msg = new org.mavlink.messages.common.msg_set_mode(
                sysId, componentId);
        msg.base_mode = 32; // HIL, disarmed
        apMavlinkPort.sendMessage(msg);
    }

    private void handleMavLinkMessage(MAVLinkMessage msg) throws IOException {
        long t = System.currentTimeMillis();
        if (msg instanceof msg_hil_controls) {
            msg_hil_ctr.event(System.nanoTime());
            msg_hil_controls msg_hil = (msg_hil_controls) msg;
            List<Double> control = Arrays.asList(
                    (double) msg_hil.roll_ailerons,
                    (double) msg_hil.pitch_elevator,
                    (double) msg_hil.yaw_rudder, (double) msg_hil.throttle,
                    (double) msg_hil.aux1, (double) msg_hil.aux2,
                    (double) msg_hil.aux3, (double) msg_hil.aux4);
            vehicle.setControl(control);
        } else if (msg instanceof msg_heartbeat) {
            msg_heartbeat msg_heartbeat = (msg_heartbeat) msg;
            if (!gotHeartBeat) {
                sysId = msg_heartbeat.sysId;
                componentId = msg_heartbeat.componentId;
                gotHeartBeat = true;
                initTime = t + initDelay;
            }
            if (!inited && t > initTime) {
                logger.log(Level.INFO, "Init MAVLink");
                initMavLink();
                inited = true;
            }
            if ((msg_heartbeat.base_mode & 128) == 0) {
                vehicle.setControl(Collections.<Double>emptyList());
            }
        } else if (msg instanceof msg_statustext) {
            logger.log(Level.INFO,
                    String.format("%8.3f: MSG: %s", (t - initTime) / 1000.0,
                            ((msg_statustext) msg).getText()));
        }
    }

    protected void sendMavLinkMessages_ap() {
        if (apMavlinkPort.isOpened() && inited) {
            try {
                long t = System.currentTimeMillis();
                long tu = t * 1000;
                Sensors sensors = vehicle.getSensors();
                // Sensors
                msg_hil_sensor msg_sensor = new msg_hil_sensor(sysId, componentId);
                msg_sensor.time_usec = tu;
                Vector3d acc = sensors.getAcc();
                msg_sensor.xacc = (float) acc.x;
                msg_sensor.yacc = (float) acc.y;
                msg_sensor.zacc = (float) acc.z;
                Vector3d gyro = sensors.getGyro();
                msg_sensor.xgyro = (float) gyro.x;
                msg_sensor.ygyro = (float) gyro.y;
                msg_sensor.zgyro = (float) gyro.z;
                Vector3d mag = sensors.getMag();
                msg_sensor.xmag = (float) mag.x;
                msg_sensor.ymag = (float) mag.y;
                msg_sensor.zmag = (float) mag.z;
                msg_sensor.pressure_alt = (float) sensors.getPressureAlt();
                apMavlinkPort.sendMessage(msg_sensor);
                // GPS
                if (t - msgLastGPS > msgIntervalGPS) {
                    msgLastGPS = t;
                    msg_hil_gps msg_gps = new msg_hil_gps(sysId, componentId);
                    msg_gps.time_usec = tu;
                    GPSPosition gps = sensors.getGPS();
                    msg_gps.lat = (long) (gps.lat * 1e7);
                    msg_gps.lon = (long) (gps.lon * 1e7);
                    msg_gps.alt = (long) (gps.alt * 1e3);
                    msg_gps.vn = (int) (gps.vn * 100);
                    msg_gps.ve = (int) (gps.ve * 100);
                    msg_gps.vd = (int) (gps.vd * 100);
                    msg_gps.eph = (int) (gps.eph * 100);
                    msg_gps.epv = (int) (gps.epv * 100);
                    msg_gps.vel = (int) (gps.getSpeed() * 100);
                    msg_gps.cog = (int) (gps.getCog() / Math.PI * 18000.0);
                    msg_gps.fix_type = 3;
                    msg_gps.satellites_visible = 10;
                    apMavlinkPort.sendMessage(msg_gps);

                    msg_global_position_int msg_target = new msg_global_position_int(2,
                            componentId);
                    GPSPosition target_pos = target.getGPS();
                    msg_target.time_boot_ms = tu;
                    msg_target.lat = (long) (target_pos.lat * 1e7);
                    msg_target.lon = (long) (target_pos.lon * 1e7);
                    msg_target.alt = (long) (target_pos.alt * 1e3);
                    msg_target.vx = (int) (target_pos.vn * 100);
                    msg_target.vy = (int) (target_pos.ve * 100);
                    msg_target.vz = (int) (target_pos.vd * 100);
                    apMavlinkPort.sendMessage(msg_target);

                }
            } catch (IOException ex) {
                Logger.getLogger(Simulator.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void run() {
        // realtime handling of messages from autopilot
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    MAVLinkMessage msg;
                    try {
                        msg = apMavlinkPort.getNextMessage(true);
                        if (msg != null) {
                            handleMavLinkMessage(msg);
                            // forward to GCS
                            gcsMavlinkPort.sendMessage(msg);
                        }
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
            }
        }).start();

        // realtime forwarding of messages from gcs to autopilot
        new Thread(new Runnable() {
            @Override
            public void run() {
                MAVLinkMessage msg;
                try {
                    msg = gcsMavlinkPort.getNextMessage(true);
                    apMavlinkPort.sendMessage(msg);
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        }).start();

        // main loop: run a realtime simulator integration step (world.update)
        nextRun = System.currentTimeMillis() + sleepInterval;
        while (running) {
            try {
                // run a simulation step
                long t = System.currentTimeMillis();

                // send HIL sensor and GPS data to PX4
                if (apMavlinkPort.isOpened() && inited) {
                    sendMavLinkMessages_ap();
                }

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
