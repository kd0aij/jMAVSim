package me.drton.jmavsim;

import me.drton.jmavsim.vehicle.AbstractVehicle;
import org.mavlink.messages.MAVLinkMessage;
import org.mavlink.messages.common.*;

import javax.vecmath.Vector3d;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * MAVLinkHILSystem is MAVLink bridge between AbstractVehicle and autopilot connected via MAVLink.
 * <p/>
 * User: ton Date: 13.02.14 Time: 22:04
 */
public class MAVLinkHILSystem extends MAVLinkSystem {
    // MAVLinkHILSystem has the same sysID as autopilot, but different componentId
    private int hilComponentId = -1;    // componentId of the autopilot
    private AbstractVehicle vehicle;
    private boolean gotHeartBeat = false;
    private boolean inited = false;
    private long initTime = 0;
    private long initDelay = 1000;
    private long msgIntervalGPS = 200;
    private long msgLastGPS = 0;
    private long msgIntervalSensors = 5;
    private long msgLastSensors = 0;
    IntervalStatsNano msg_hil_ctr;
    IntervalStatsNano msg_hil_ts;
    private long msgTimeStamp = 0;

    /**
     * Create MAVLinkHILSimulator, MAVLink system thet sends simulated sensors to autopilot and passes controls from
     * autopilot to simulator
     *
     * @param sysId       SysId of simulator should be the same as autopilot
     * @param componentId ComponentId of simulator should be different from autopilot
     * @param vehicle
     */
    public MAVLinkHILSystem(int sysId, int componentId, AbstractVehicle vehicle) {
        super(sysId, componentId);
        this.vehicle = vehicle;
        msg_hil_ctr = new IntervalStatsNano(Simulator.logger, "msg_hil", 500, (long) 10e9);
        msg_hil_ts = new IntervalStatsNano(Simulator.logger, "msg_hil_ts", 500, (long) 10e9);
		msgLastGPS = System.currentTimeMillis() + 10000;
    }

    @Override
    public void handleMessage(MAVLinkMessage msg) {
        super.handleMessage(msg);
        long t = System.currentTimeMillis();

        if (msg instanceof msg_hil_controls) {
            
            msg_hil_controls msg_hil = (msg_hil_controls) msg;

            msg_hil_ctr.event(System.nanoTime());
            long timeStamp = msg_hil.time_usec;
            msg_hil_ts.event(1000 * timeStamp);

            List<Double> control = Arrays.asList((double) msg_hil.roll_ailerons, (double) msg_hil.pitch_elevator,
                    (double) msg_hil.yaw_rudder, (double) msg_hil.throttle, (double) msg_hil.aux1,
                    (double) msg_hil.aux2, (double) msg_hil.aux3, (double) msg_hil.aux4);
            vehicle.setControl(control);
        } else if (msg instanceof msg_heartbeat) {
            msg_heartbeat heartbeat = (msg_heartbeat) msg;
            if (!gotHeartBeat && sysId == heartbeat.sysId) {
                hilComponentId = heartbeat.componentId;
                gotHeartBeat = true;
                initTime = t + initDelay;
            }
            if (!inited && t > initTime) {
                System.out.println("Init MAVLink");
                initMavLink();
                inited = true;
            }
            if ((heartbeat.base_mode & 128) == 0) {
                vehicle.setControl(Collections.<Double>emptyList());
            }
        } else if (msg instanceof msg_statustext) {
            System.out.println("MSG: " + ((msg_statustext) msg).getText());
        }
    }

    @Override
    public void update(long t) {
        // Don't call super.update(), because heartbeats already sent by autopilot
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
        sendMessage(msg_sensor);
        // GPS
        if (t - msgLastGPS > msgIntervalGPS) {
            msgLastGPS = t;
            msg_hil_gps msg_gps = new msg_hil_gps(sysId, componentId);
            msg_gps.time_usec = tu;
            GlobalPositionVelocity p = sensors.getGlobalPosition();
            msg_gps.lat = (long) (p.position.lat * 1e7);
            msg_gps.lon = (long) (p.position.lon * 1e7);
            msg_gps.alt = (long) (p.position.alt * 1e3);
            msg_gps.vn = (int) (p.velocity.x * 100);
            msg_gps.ve = (int) (p.velocity.y * 100);
            msg_gps.vd = (int) (p.velocity.z * 100);
            msg_gps.eph = (int) (p.eph * 100);
            msg_gps.epv = (int) (p.epv * 100);
            msg_gps.vel = (int) (p.getSpeed() * 100);
            msg_gps.cog = (int) (p.getCog() / Math.PI * 18000.0);
            msg_gps.fix_type = p.fix;
            msg_gps.satellites_visible = 10;
            sendMessage(msg_gps);
        }
    }

    private void initMavLink() {
        // Set HIL mode
        msg_set_mode msg = new msg_set_mode(sysId, componentId);
        msg.target_system = sysId;
        msg.base_mode = 32;     // HIL, disarmed
        sendMessage(msg);
    }
}
