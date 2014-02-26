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
import com.sun.j3d.utils.universe.SimpleUniverse;

class CameraView {

    protected static final PhysicalBody physBody = new PhysicalBody();
    protected static final PhysicalEnvironment physEnv = new PhysicalEnvironment();

    protected BranchGroup rootBG = null;
    protected TransformGroup vpTG = null;
    protected ViewPlatform viewPlatform = null;
    protected View view = null;
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
    private static Color3f white = new Color3f(1.0f, 1.0f, 1.0f);
    private VirtualUniverse universe;
    private Locale locale;
//    private SimpleUniverse universe;
    protected Canvas3D canvas3D;
    private World world;
    private BoundingSphere sceneBounds = new BoundingSphere(
            new Point3d(0, 0, 0), 100000.0);
    private Vector3d fixedPos = new Vector3d(-7.0, 0.0, -5);
    private Vector3d viewerPos = new Vector3d(fixedPos);
    private Transform3D viewerTransform = new Transform3D();
    private VisualObject viewerTarget;
    private MechanicalObject viewerPosition;
    protected CameraView mainCamera;
    protected CameraView dbgCamera;

    private boolean autoRotate = true;

    public boolean isAutoRotate() {
        return autoRotate;
    }

    public void initPos() {
        viewerPos.set(fixedPos);
        viewerPosition = null;
    }

    public Visualizer(World world) {
        this.world = world;
        GraphicsConfiguration config = SimpleUniverse
                .getPreferredConfiguration();
        canvas3D = new Canvas3D(config);
        universe = new VirtualUniverse();
        locale = new Locale(universe);
//        universe = new SimpleUniverse(canvas3D);
//        universe.getViewer().getView().setBackClipDistance(100000.0);
        createEnvironment();
        for (WorldObject object : world.getObjects()) {
            if (object instanceof VisualObject)
                locale.addBranchGraph(((VisualObject) object)
                        .getBranchGroup());
        }
        Map vuMap = universe.getProperties();
        System.out.println(" Java3D version : " + vuMap.get("j3d.version"));
        System.out.println(" Java3D vendor : " + vuMap.get("j3d.vendor"));
        System.out.println(" Java3D renderer: " + vuMap.get("j3d.renderer"));
        
        dbgCamera = new CameraView();
        TransformGroup vpTG = dbgCamera.getViewPlatformTransformGroup();
        Transform3D xform = new Transform3D();
        Vector3f vec = new Vector3f(4.0f, 0.0f, 0.0f);
        xform.set(vec);
        Transform3D xform2 = new Transform3D();
        xform2.rotZ(Math.PI / 2);
        xform.mul(xform2);
        xform2.rotX(-Math.PI / 2);
        xform.mul(xform2);
        vpTG.setTransform(xform);
        View view = dbgCamera.getView();
        view.setProjectionPolicy(View.PARALLEL_PROJECTION);
        locale.addBranchGraph(dbgCamera.getRootBG());        

        mainCamera = new CameraView();
        vpTG = mainCamera.getViewPlatformTransformGroup();
        xform = new Transform3D();
        xform2.rotZ(Math.PI / 2);
        xform.mul(xform2);
        xform2.rotX(-Math.PI / 2);
        xform.mul(xform2);
        vpTG.setTransform(xform);
        view = mainCamera.getView();
        view.setProjectionPolicy(View.PARALLEL_PROJECTION);
        locale.addBranchGraph(mainCamera.getRootBG());        

    }

    public Canvas3D getCanvas3D() {
        return canvas3D;
    }

    public void setAutoRotate(boolean autoRotate) {
        this.autoRotate = autoRotate;
    }

    public void setViewerTarget(VisualObject object) {
        this.viewerTarget = object;
    }

    public void setViewerPosition(MechanicalObject object) {
        this.viewerPosition = object;
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

        // coordinate frame reference object
        Transform3D wFrameT3D = new Transform3D();
        wFrameT3D.setTranslation(new Vector3d(5, 0, -4));
        TransformGroup wFrameTG = G3f.axesCartesian2(wFrameT3D,
                new double[] { 1, 1, 1 }, .2f, "world");
        group.addChild(wFrameTG);

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
        if (viewerPosition != null) {
            viewerPos.set(viewerPosition.getPosition());
        }
        Matrix3d mat = new Matrix3d();
        mat.setIdentity();
        Matrix3d m1 = new Matrix3d();
        if (autoRotate) {
            if (viewerTarget != null) {
                Vector3d pos = viewerTarget.getPosition();
                mat.rotZ(Math.PI);
                Vector3d dist = new Vector3d();
                dist.sub(pos, viewerPos);
                m1.rotY(Math.PI / 2);
                mat.mul(m1);
                m1.rotZ(-Math.PI / 2);
                mat.mul(m1);
                m1.rotY(-Math.atan2(pos.y - viewerPos.y, pos.x - viewerPos.x));
                mat.mul(m1);
                if (dist.length() > 1e-6) {
                    m1.rotX(-Math.asin((pos.z - viewerPos.z) / dist.length()));
                    mat.mul(m1);
                }
            }
        } else {
            if (viewerPosition != null) {
                mat.mul(viewerPosition.getRotation());
            }
            m1.rotZ(Math.PI / 2);
            mat.mul(m1);
            m1.rotX(-Math.PI / 2);
            mat.mul(m1);
        }
        viewerTransform.setRotation(mat);
        viewerTransform.setTranslation(viewerPos);
//        universe.getViewingPlatform().getViewPlatformTransform()
//                .setTransform(viewerTransform);
        mainCamera.getViewPlatformTransformGroup().setTransform(viewerTransform);
    }

    public void update() {
        synchronized (world) {
            updateViewer();
        }
    }
}
