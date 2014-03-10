package me.drton.jmavsim;

/**
 * User: ton Date: 28.11.13 Time: 23:15
 */
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.util.Map;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.Locale;
import javax.media.j3d.PhysicalBody;
import javax.media.j3d.PhysicalEnvironment;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Texture;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.media.j3d.ViewPlatform;
import javax.media.j3d.VirtualUniverse;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.TexCoord2f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.universe.ViewInfo;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileOutputStream;
import java.io.IOException;
import static java.lang.System.out;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import javax.media.j3d.Behavior;
import javax.media.j3d.WakeupCondition;
import javax.media.j3d.WakeupCriterion;
import javax.media.j3d.WakeupOnElapsedFrames;
import javax.media.j3d.WakeupOr;

class CameraView {

    protected static final PhysicalBody physBody = new PhysicalBody();
    protected static final PhysicalEnvironment physEnv = new PhysicalEnvironment();

    protected BranchGroup rootBG = null;
    protected TransformGroup vpTG = null;
    protected ViewPlatform viewPlatform = null;
    protected View view = null;
    protected ViewInfo viewInfo = null;
    protected Canvas3D canvas = null;

    public CameraView() {

        GraphicsConfigTemplate3D gconfigTempl = new GraphicsConfigTemplate3D();
        GraphicsConfiguration gconfig = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getBestConfiguration(gconfigTempl);

        canvas = new Canvas3D(gconfig);

        view = new View();

        viewPlatform = new ViewPlatform();

        view.setPhysicalBody(physBody);
        view.setPhysicalEnvironment(physEnv);
        view.attachViewPlatform(viewPlatform);
        view.addCanvas3D(canvas);

        vpTG = new TransformGroup();
        vpTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        vpTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        vpTG.addChild(viewPlatform);

        viewInfo = new ViewInfo(view);

        rootBG = new BranchGroup();
        rootBG.setCapability(BranchGroup.ALLOW_DETACH);
        rootBG.addChild(vpTG);

    }

    public TransformGroup getViewPlatformTransformGroup() {
        return this.vpTG;
    }

    public BranchGroup getRootBG() {
        return this.rootBG;
    }

    public View getView() {
        return this.view;
    }

    public Canvas3D getCanvas3D() {
        return this.canvas;
    }

}

public class Visualizer {

    static private Logger logger;
    static boolean append = true;

    static {
        logger = Logger.getLogger("Visualizer");
        Handler[] handler = logger.getParent().getHandlers();
        handler[0].setFormatter(new BriefFormatter());
        try {
            String logFileName = FileUtils.getLogFileName("log", "visualizer", append);
            StreamHandler logFileHandler = new StreamHandler(
                    new FileOutputStream(logFileName, append), new BriefFormatter());
            out.println("logfile: " + logFileName);
            logFileHandler.setFormatter(new BriefFormatter());
            logger.addHandler(logFileHandler);
            logger.log(Level.INFO, "\nVisualizer starting: ".concat((new Date()).toString()));
        } catch (SecurityException e) {
            out.println("security exception creating logger");
            System.exit(1);
        } catch (IOException e) {
            out.println("IO exception creating logger");
            System.exit(1);
        }
    }

    private static Color3f white = new Color3f(1.0f, 1.0f, 1.0f);
    private VirtualUniverse universe;
    private Locale locale;
//    protected Canvas3D canvas3D;
    private World world;
    private Simulator sim;
    private BoundingSphere sceneBounds = new BoundingSphere(
            new Point3d(0, 0, 0), 100000.0);
    private final Vector3d fixedPos = new Vector3d(-7.0, 0.0, -5);
    private Vector3d viewerPos = new Vector3d(fixedPos);
    private Vector3d dbgViewerPos = new Vector3d(fixedPos);
    private Transform3D viewerTransform = new Transform3D();
    private VisualObject viewerTarget;
    private MechanicalObject viewerObject;
    private MechanicalObject dbgViewerObject;
    protected CameraView mainCamera;
    private boolean autoRotate = true;

    protected CameraView dbgCamera;
    ViewPlatformBehavior dbgViewBehavior;

    static MouseAdapter mouseListener;
    PickCanvas pickCanvas;
    PickResult pickResults;
    TransformGroup moveTGroup;
    TransformGroup curModelTG;
    TransformGroup viewMatrixGroup;

    protected static enum viewTypes {

        stereo, top, chase
    };
    protected viewTypes dbgViewType = viewTypes.stereo;

    public viewTypes getDbgViewType() {
        return dbgViewType;
    }

    public void setDbgViewType(viewTypes viewType) {
        this.dbgViewType = viewType;
    }

    public CameraView getMainCamera() {
        return mainCamera;
    }

    public CameraView getDbgCamera() {
        return dbgCamera;
    }

    public boolean isAutoRotate() {
        return autoRotate;
    }

    public void initPos() {
        viewerPos.set(fixedPos);
        viewerObject = null;
    }
//    static SceneGraphSpecs sceneSpecs = new SceneGraphSpecs();

    public Visualizer(World world, Simulator sim) {
        this.world = world;
        this.sim = sim;
        mouseListener = new PMouseAdapter();
        dbgViewBehavior = new ViewPlatformBehavior(this);
        dbgViewBehavior.setSchedulingBounds(sceneBounds);

        universe = new VirtualUniverse();
        locale = new Locale(universe);
        createEnvironment();

        @SuppressWarnings("rawtypes")
        Map vuMap = VirtualUniverse.getProperties();
        logger.log(Level.INFO, " Java3D version : " + vuMap.get("j3d.version"));
        logger.log(Level.INFO, " Java3D vendor : " + vuMap.get("j3d.vendor"));
        logger.log(Level.INFO, " Java3D renderer: " + vuMap.get("j3d.renderer"));

        dbgCamera = new CameraView();
        TransformGroup vpTG = dbgCamera.getViewPlatformTransformGroup();
        viewMatrixGroup = vpTG;
        View view = dbgCamera.getView();
        view.setProjectionPolicy(View.PERSPECTIVE_PROJECTION);
        dbgCamera.getView().setBackClipDistance(100000.0);
        // set maximum frame rate
        dbgCamera.getView().setMinimumFrameCycleTime(2);

        // set up mouse controlled flight
        Transform3D mt3d = new Transform3D();
        moveTGroup = G3f.mouseBehavior(mt3d, 100);
        moveTGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        moveTGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        moveTGroup.setCapability(TransformGroup.ENABLE_PICK_REPORTING);

        mainCamera = new CameraView();
        view = mainCamera.getView();
        view.setProjectionPolicy(View.PERSPECTIVE_PROJECTION);
        mainCamera.getView().setBackClipDistance(100000.0);
        // set maximum frame rate
        mainCamera.getView().setMinimumFrameCycleTime(2);

        locale.addBranchGraph(dbgCamera.getRootBG());
        locale.addBranchGraph(mainCamera.getRootBG());

        // coordinate frame reference object
        Transform3D wFrameT3D = new Transform3D();
        wFrameT3D.setTranslation(new Vector3d(5, 0, -4));
        TransformGroup wFrameTG = G3f.axesCartesian2(wFrameT3D, new double[]{
            1, 1, 1}, .2f, "world");
        wFrameTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        wFrameTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        moveTGroup.addChild(wFrameTG);
        dbgCamera.getCanvas3D().addMouseListener(mouseListener);

        BranchGroup sceneBG = new BranchGroup();
        for (WorldObject object : world.getObjects()) {
            if (object instanceof VisualObject) {
                moveTGroup.addChild(((VisualObject) object).getBranchGroup());
            }
        }

        sceneBG.addChild(moveTGroup);
        sceneBG.addChild(dbgViewBehavior);

        pickCanvas = new PickCanvas(dbgCamera.getCanvas3D(), sceneBG);
        pickCanvas.setMode(PickCanvas.GEOMETRY_INTERSECT_INFO);
        pickCanvas.setTolerance(1.0f);
        curModelTG = wFrameTG;

        sceneBG.compile();
        locale.addBranchGraph(sceneBG);
    }

    class PMouseAdapter extends MouseAdapter {

        public void mouseClicked(MouseEvent evt) {
            pickCanvas.setShapeLocation(evt);

            pickResults = pickCanvas.pickClosest();
            if (pickResults == null) {
                logger.info("null pickResults");
            } else if (pickResults.numIntersections() > 0) {
                Point3d pickPt
                        = pickResults.getIntersection(0).getClosestVertexCoordinatesVW();

                logger.info("picked point in virtual world: " + pickPt);

                // get transforms to convert pickPt to model frame
                Transform3D moveT3d = new Transform3D();
                moveTGroup.getTransform(moveT3d);
                Transform3D invMoveT3d = new Transform3D(moveT3d);
                invMoveT3d.invert();

                Transform3D modelT3d = new Transform3D();
                Vector3d modelT = new Vector3d();   // original scene translation vector

                curModelTG.getTransform(modelT3d);  // this TG should be a pure translation
                Transform3D invModelT3d = new Transform3D(modelT3d);
                invModelT3d.invert();

                // convert pickPt to model frame coord's
                invMoveT3d.transform(pickPt);  // undo the mouseflight transform
                // we still have the model TransformGroup below us
                invModelT3d.transform(pickPt); // undo the model transform
                logger.info("picked point in model frame: " + pickPt);

                Point3f neg_pickPt = new Point3f(pickPt);
                neg_pickPt.negate();

                // move rotation origin to node centroid
                Transform3D newModelT3d = new Transform3D();
                newModelT3d.set(new Vector3f(neg_pickPt));  // pure translation matrix
                curModelTG.setTransform(newModelT3d);

                // compensate for translation using viewMatrixGroup transform
                Transform3D viewT3d = new Transform3D();
                viewMatrixGroup.getTransform(viewT3d);
                logger.info("initial view transform: \n" + viewT3d);
                Transform3D newViewT3d = new Transform3D(moveT3d);
                newViewT3d.mul(newModelT3d);
                newViewT3d.mul(invModelT3d);
                newViewT3d.mul(invMoveT3d);
                newViewT3d.mul(viewT3d);
                logger.info("new view transform: \n" + viewT3d);
                viewMatrixGroup.setTransform(newViewT3d);
            }
        }
    }

//    public Canvas3D getCanvas3D() {
//        return canvas3D;
//    }
    public void setAutoRotate(boolean autoRotate) {
        this.autoRotate = autoRotate;
    }

    public void setViewerTarget(VisualObject object) {
        this.viewerTarget = object;
    }

    public void setViewerPosition(MechanicalObject object) {
        this.viewerObject = object;
    }

    public void setDbgViewerPosition(MechanicalObject object) {
        this.dbgViewerObject = object;
    }

    private void createEnvironment() {
        BranchGroup group = new BranchGroup();
        // Sky
        BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0),
                1000.0);
        Background bg = new Background();
        bg.setApplicationBounds(bounds);
        BranchGroup backGeoBranch = new BranchGroup();
        Sphere skySphere = new Sphere(1.0f, Sphere.GENERATE_NORMALS
                | Sphere.GENERATE_NORMALS_INWARD
                | Sphere.GENERATE_TEXTURE_COORDS, 32);
        // Sphere.GENERATE_NORMALS | Sphere.GENERATE_NORMALS_INWARD |
        // Sphere.GENERATE_TEXTURE_COORDS, 32);
        Texture texSky = new TextureLoader("environment/sky.jpg", null)
                .getTexture();
        skySphere.getAppearance().setTexture(texSky);
        Transform3D transformSky = new Transform3D();
        // transformSky.setTranslation(new Vector3d(0.0, 0.0, -0.5));
        Matrix3d rot = new Matrix3d();
        rot.rotX(Math.PI / 2);
        transformSky.setRotation(rot);
        TransformGroup tgSky = new TransformGroup(transformSky);
        tgSky.addChild(skySphere);
        backGeoBranch.addChild(tgSky);
        bg.setGeometry(backGeoBranch);
        group.addChild(bg);
        // group.addChild(tgSky);
        // Ground
        QuadArray polygon1 = new QuadArray(4, QuadArray.COORDINATES
                | GeometryArray.TEXTURE_COORDINATE_2);
        polygon1.setCoordinate(0, new Point3f(-1000f, 1000f, 0f));
        polygon1.setCoordinate(1, new Point3f(1000f, 1000f, 0f));
        polygon1.setCoordinate(2, new Point3f(1000f, -1000f, 0f));
        polygon1.setCoordinate(3, new Point3f(-1000f, -1000f, 0f));
        polygon1.setTextureCoordinate(0, 0, new TexCoord2f(0.0f, 0.0f));
        polygon1.setTextureCoordinate(0, 1, new TexCoord2f(10.0f, 0.0f));
        polygon1.setTextureCoordinate(0, 2, new TexCoord2f(10.0f, 10.0f));
        polygon1.setTextureCoordinate(0, 3, new TexCoord2f(0.0f, 10.0f));
        Texture texGround = new TextureLoader("environment/grass2.jpg", null)
                .getTexture();
        Appearance apGround = new Appearance();
        apGround.setTexture(texGround);
        Shape3D ground = new Shape3D(polygon1, apGround);
        Transform3D transformGround = new Transform3D();
        transformGround.setTranslation(new Vector3d(0.0, 0.0, 0.005 + world
                .getEnvironment().getGroundLevel(new Vector3d(0.0, 0.0, 0.0))));
        TransformGroup tgGround = new TransformGroup(transformGround);
        tgGround.addChild(ground);
        group.addChild(tgGround);

        // Light
        DirectionalLight light1 = new DirectionalLight(white, new Vector3f(
                4.0f, 7.0f, 12.0f));
        light1.setInfluencingBounds(sceneBounds);
        group.addChild(light1);
        AmbientLight light2 = new AmbientLight(new Color3f(0.5f, 0.5f, 0.5f));
        light2.setInfluencingBounds(sceneBounds);
        group.addChild(light2);
        locale.addBranchGraph(group);
    }

    private void updateViewer() {
        if (viewerObject != null) {
            viewerPos.set(viewerObject.getPosition());
        }
        if (autoRotate) {
            if (viewerTarget != null) {
                viewerTransform.lookAt(new Point3d(viewerPos), new Point3d(
                        viewerTarget.getPosition()), new Vector3d(0, 0, -1));
                viewerTransform.invert();
            }
        } else {
            Matrix3d mat = new Matrix3d();
            mat.setIdentity();
            Matrix3d m1 = new Matrix3d();
            if (viewerObject != null) {
                mat.mul(viewerObject.getRotation());
            }
            m1.rotZ(Math.PI / 2);
            mat.mul(m1);
            m1.rotX(-Math.PI / 2);
            mat.mul(m1);
            viewerTransform.setRotation(mat);
            viewerTransform.setTranslation(viewerPos);
        }
        mainCamera.getViewPlatformTransformGroup()
                .setTransform(viewerTransform);

        updateDbgView();
    }

    public void update() {
        synchronized (world) {
            updateViewer();
        }
    }

    protected void updateDbgView() {
        Transform3D dbgViewerTransform = new Transform3D(viewerTransform);
        switch (dbgViewType) {
            case top:
                Vector3d trans = new Vector3d(0, 0, -10);
                trans.add(dbgViewerObject.getPosition());
                Matrix3d mat = new Matrix3d();
                mat.setIdentity();
                Matrix3d m1 = new Matrix3d();
//                m1.rotZ(Math.PI / 2);
//                mat.mul(m1);
                m1.rotX(-Math.PI);
                mat.mul(m1);
                dbgViewerTransform.setRotation(mat);
                dbgViewerTransform.setTranslation(trans);
                break;
            case stereo:
                trans = new Vector3d();
                dbgViewerTransform.get(trans);
                Vector3d baseline = new Vector3d(-0.5, 0, 0);
                Matrix3d rot = new Matrix3d();
                dbgViewerTransform.get(rot);
                rot.transform(baseline);
                trans.add(baseline);
                dbgViewerTransform.setTranslation(trans);
                break;
            case chase:
                // position camera 2m behind vehicle, looking toward it
                trans = new Vector3d(-2, 0, 0);
                double[] rpy = G3f.getRPYangles(dbgViewerObject.getRotation());
                Matrix3d zRot = new Matrix3d();
                zRot.rotZ(rpy[2]);
                zRot.transform(trans);
                Vector3d vPos = dbgViewerObject.getPosition();
                dbgViewerPos.set(vPos);
                dbgViewerPos.add(trans);
                dbgViewerTransform.lookAt(new Point3d(dbgViewerPos), new Point3d(
                        vPos), new Vector3d(0, 0, -1));
                dbgViewerTransform.invert();
                dbgViewerTransform.setTranslation(dbgViewerPos);
                break;
        }
        dbgCamera.getViewPlatformTransformGroup().setTransform(
                dbgViewerTransform);
    }

    class ViewPlatformBehavior extends Behavior {

        protected Visualizer vis;
        protected WakeupCondition m_WakeupCondition = null;
        private int frameCount;         // number of elapsed frames
        private long baseTime;
        private long start_nt;          // nanoseconds
        private long last_nt;           // nanoseconds
        private double avg_fps = 0;     // Hz
        private double avg_interval;    // nanoseconds
        private long min_interval = Long.MAX_VALUE; // nanoseconds
        private long max_interval = 0;      // nanoseconds

        public ViewPlatformBehavior(Visualizer vis) {
            this.vis = vis;

            //create the WakeupCriterion for the behavior
            WakeupCriterion criterionArray[] = new WakeupCriterion[1];
            criterionArray[0] = new WakeupOnElapsedFrames(0);

            //save the WakeupCriterion for the behavior
            m_WakeupCondition = new WakeupOr(criterionArray);
        }

        public void initialize() {
            //apply the initial WakeupCriterion
            wakeupOn(m_WakeupCondition);
            baseTime = System.currentTimeMillis();
            start_nt = System.nanoTime();
            frameCount = 0;
        }

        public void processStimulus(java.util.Enumeration criteria) {
            while (criteria.hasMoreElements()) {
                WakeupCriterion wakeUp
                        = (WakeupCriterion) criteria.nextElement();

                // for every frame, run a simulation step and reposition the viewplatform
                if (wakeUp instanceof WakeupOnElapsedFrames) {
                    frameCount++;
                    long t = System.currentTimeMillis();
                    long nt = System.nanoTime();
                    long ent = nt - start_nt;
                    long dnt = nt - last_nt;
                    min_interval = Math.min(dnt, min_interval);
                    max_interval = Math.max(dnt, max_interval);
                    last_nt = nt;

                    try {
                        world.update(t);
                        update();
                    } catch (javax.media.j3d.BadTransformException e) {
                        logger.log(Level.SEVERE, e.getMessage());
                        logger.log(Level.SEVERE, sim.vehicle.transform.toString());
                        e.printStackTrace();
                    }
                    if (ent >= 10e9) {
                        avg_interval = ent / frameCount;
                        avg_fps = 1e9 / avg_interval; // Hz
                        logger.log(Level.INFO,
                                String.format("%10.3f, avg_fps: %5.1f, dt(msec) avg: %5.1f, min: %5.1f, max: %5.1f",
                                        1e-3 * (t - baseTime), avg_fps, 1e-6 * avg_interval,
                                        1e-6 * min_interval, 1e-6 * max_interval));
                        start_nt = nt;
                        frameCount = 0;
                        min_interval = Long.MAX_VALUE;
                        max_interval = 0;
                    }
                }
            }

            //assign the next WakeUpCondition, so we are notified again
            wakeupOn(m_WakeupCondition);
        }
    }
}
