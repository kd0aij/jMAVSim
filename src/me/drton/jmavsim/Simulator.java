package me.drton.jmavsim;

import me.drton.jmavsim.vehicle.AbstractMulticopter;
import me.drton.jmavsim.vehicle.AbstractVehicle;
import me.drton.jmavsim.vehicle.Quadcopter;
import org.mavlink.messages.MAVLinkMessage;
import org.mavlink.messages.common.*;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import java.awt.HeadlessException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static java.lang.System.*;

/**
 * User: ton Date: 26.11.13 Time: 12:33
 */
public class Simulator extends Thread {
    protected static ControlFrame cPanel = null;
    private static String portName;
    private World world;
    private AbstractVehicle vehicle;
    Visualizer visualizer;
    private MAVLinkPort mavlinkPort;
    private MAVLinkPort mavlinkPort1;
    private boolean gotHeartBeat = false;
    private boolean inited = false;
    private int sysId = -1;
    private int componentId = -1;
    private int sleepInterval = 10;
    private int visualizerSleepInterval = 20;
    private long nextRun = 0;
    private long msgIntervalGPS = 200;
    private long msgLastGPS = 0;
    private long initTime = 0;
    private long initDelay = 1000;
    protected Target target;

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
        // v.setDragRotate(0.1);
        vehicle = v;
        world.addObject(v);
        target = new Target(world, 0.3);
        target.setMass(90.0);
        target.initGPS(55.753395, 37.625427);
        target.getPosition().set(5, 0, -5);
        world.addObject(target);
        // Create visualizer
        visualizer = new Visualizer(world);
        visualizer.setViewerTarget(target);
        // visualizer.setViewerTarget(v);
        visualizer.setViewerPosition(v);
        // Create and open port
        gotHeartBeat = false;
        inited = false;
        SerialMAVLinkPort serialMAVLinkPort = new SerialMAVLinkPort();
        serialMAVLinkPort.open(portName, 230400, 8, 1, 0);
        UDPMavLinkPort udpMavLinkPort = new UDPMavLinkPort();
        udpMavLinkPort.open(new InetSocketAddress(14555));
        mavlinkPort = serialMAVLinkPort;
        mavlinkPort1 = udpMavLinkPort;
        // run();
        // Close ports
        // mavlinkPort.close();
        // mavlinkPort1.close();

        // construct GUI
        constructGUI(this);
    }

    private void initMavLink() throws IOException {
        // Set HIL mode
        org.mavlink.messages.common.msg_set_mode msg = new org.mavlink.messages.common.msg_set_mode(
                sysId, componentId);
        msg.base_mode = 32; // HIL, disarmed
        mavlinkPort.sendMessage(msg);
    }

    private void handleMavLinkMessage(MAVLinkMessage msg) throws IOException {
        long t = System.currentTimeMillis();
        if (msg instanceof msg_hil_controls) {
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
                System.out.println("Init MAVLink");
                initMavLink();
                inited = true;
            }
            if ((msg_heartbeat.base_mode & 128) == 0) {
                vehicle.setControl(Collections.<Double> emptyList());
            }
        } else if (msg instanceof msg_statustext) {
            System.out.println("MSG: " + ((msg_statustext) msg).getText());
        }
    }

    private void sendMavLinkMessages() throws IOException {
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
        mavlinkPort.sendMessage(msg_sensor);
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
            mavlinkPort.sendMessage(msg_gps);

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
            mavlinkPort.sendMessage(msg_target);
        }
    }

    public void run() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    visualizer.update();
                    try {
                        Thread.sleep(visualizerSleepInterval);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }).start();
         new Thread(new Runnable() {
         @Override
         public void run() {
         while (true) {
         MAVLinkMessage msg;
         try {
         msg = mavlinkPort.getNextMessage(true);
         if (msg != null) {
         handleMavLinkMessage(msg);
         mavlinkPort1.sendMessage(msg);
         }
         } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
         }
         }
         }
         }).start();
        nextRun = System.currentTimeMillis() + sleepInterval;
        while (true) {
            try {
//                while (System.currentTimeMillis() < nextRun - sleepInterval * 3
//                        / 4) {
//                    MAVLinkMessage msg = mavlinkPort.getNextMessage(false);
//                    if (msg == null)
//                        break;
//                    handleMavLinkMessage(msg);
//                    mavlinkPort1.sendMessage(msg);
//                }
                while (System.currentTimeMillis() < nextRun - sleepInterval * 3
                        / 4) {
                    MAVLinkMessage msg = mavlinkPort1.getNextMessage(false);
                    if (msg == null)
                        break;
                    mavlinkPort.sendMessage(msg);
                }
                long t = System.currentTimeMillis();
                world.update(t);
                if (mavlinkPort.isOpened() && inited)
                    sendMavLinkMessages();
                long timeLeft = Math.max(sleepInterval / 4,
                        nextRun - System.currentTimeMillis());
                nextRun = Math.max(t + sleepInterval / 4, nextRun
                        + sleepInterval);
                Thread.sleep(timeLeft);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        // parse command-line args
        portName = null;
        if (args.length == 0) {
            String osname = System.getProperty("os.name", "").toLowerCase();
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
                out.println("Sorry, your operating system is not supported");
                System.exit(1);
            }
        } else if (args.length >= 1) {
            portName = args[0];
            out.println("serial port: " + args[0]);
        } else {
            out.println("Usage: java Simulator serialPort");
            out.println("Defaulting serial port to " + portName);
        }
        out.println("Using serial port: " + portName);

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                // construct GCS thread class
                Simulator sim;
                try {
                    sim = new Simulator();
                    // start main thread
                    sim.start();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }

    protected static void constructGUI(Simulator sim) throws HeadlessException {
        // construct Simulator dialog
        sim.visualizer.canvas3D.setSize(800, 600);
        cPanel = new ControlFrame(sim);
        cPanel.setBounds(100, 100, cPanel.getWidth(), cPanel.getHeight());
        cPanel.setVisible(true);
    }
}
