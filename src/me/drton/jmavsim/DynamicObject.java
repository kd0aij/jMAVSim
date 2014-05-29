package me.drton.jmavsim;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

/**
 * User: ton Date: 02.02.14 Time: 12:01
 */
public abstract class DynamicObject extends KinematicObject {
    protected long lastTime = -1;
    protected double mass = 1.0;
    protected Matrix3d momentOfInertia = new Matrix3d();
    protected Matrix3d momentOfInertiaInv = new Matrix3d();
    protected Vector3d gyroAcc = new Vector3d();
    protected Vector3d angularAcc = new Vector3d();

    public DynamicObject(World world) {
        super(world);
        rotation.rotX(0);
        momentOfInertia.rotZ(0.0);
        momentOfInertiaInv.rotZ(0.0);
    }

    public double getMass() {
        return mass;
    }

    public void setMass(double mass) {
        this.mass = mass;
    }

    public void setMomentOfInertia(Matrix3d momentOfInertia) {
        this.momentOfInertia.set(momentOfInertia);
        this.momentOfInertiaInv.invert(momentOfInertia);
    }

    @Override
    // update physical state, then call super.update to update graphic state
    public void update(long t) {
        if (lastTime >= 0) {
            double dt = (t - lastTime) / 1000.0;
            // Position
            Vector3d dPos = new Vector3d(velocity);
            dPos.scale(dt);
            position.add(dPos);
            // Velocity
            acceleration = getForce();
            acceleration.scale(1.0 / mass);
            acceleration.add(getWorld().getEnvironment().getG());
            if (position.z >= getWorld().getEnvironment().getGroundLevel(position) &&
                    velocity.z + acceleration.z * dt >= 0.0) {
                // On ground
                acceleration.x = -velocity.x / dt;
                acceleration.y = -velocity.y / dt;
                acceleration.z = -velocity.z / dt;
                position.z = getWorld().getEnvironment().getGroundLevel(position);
                //rotationRate.set(0.0, 0.0, 0.0);
            }
            Vector3d dVel = new Vector3d(acceleration);
            dVel.scale(dt);
            velocity.add(dVel);
            // Rotation
            if (rotationRate.length() > 0.0) {
                Matrix3d r = new Matrix3d();
                Vector3d rotationAxis = new Vector3d(rotationRate);
                rotationAxis.normalize();
                r.set(new AxisAngle4d(rotationAxis, rotationRate.length() * dt));
                rotation.mulNormalize(r);
            }
            // Rotation rate
            // use an adaptive dt to integrate omega
            Vector3d exTorque = getTorque();
            int wInc = (int)(1 + 500 * rotationRate.length());
//            System.out.print("wInc: " + wInc + ": ");
            for (int i = 0; i < wInc; i++)
                omegaUpdate(dt / wInc, exTorque);
        }
        lastTime = t;
        super.update(t);
    }

    protected abstract Vector3d getForce();

    protected abstract Vector3d getTorque();

    private void omegaUpdate(double dt, Vector3d exTorque) {
        // gyroscopic torque
        Vector3d Iw = new Vector3d(rotationRate);
        momentOfInertia.transform(Iw);
        gyroAcc.cross(rotationRate, Iw);
        gyroAcc.negate();
        momentOfInertiaInv.transform(gyroAcc);
        // external torque
        angularAcc.set(exTorque);
        momentOfInertiaInv.transform(angularAcc);
        // integrate
        rotationRate.scaleAdd(dt, gyroAcc, rotationRate);
        rotationRate.scaleAdd(dt, angularAcc, rotationRate);
    }
}
